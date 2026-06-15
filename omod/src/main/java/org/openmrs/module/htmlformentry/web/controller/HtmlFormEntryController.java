package org.openmrs.module.htmlformentry.web.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.compatibility.EncounterServiceCompatibility;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The controller for entering/viewing a form.
 * <p/>
 * Handles {@code htmlFormEntry.form} requests. Renders view {@code htmlFormEntry.jsp}.
 * <p/>
 * TODO: getFormEntrySession still orchestrates request parsing and session construction.
 * Mode-resolution, encounter/HtmlForm/patient/"which" resolution, the patient-required-for-
 * session check and the modified-timestamp concurrency checks have already been extracted into
 * small methods on this controller (or moved to {@link FormEntrySession}) as part of the
 * maintainability PoC. Remaining candidates: extracting the encounter-vs-no-encounter branch
 * itself, and the {@link FormEntrySession} construction + post-construction setup at the end of
 * the method.
 */
@Controller
public class HtmlFormEntryController {
    
    protected final Log log = LogFactory.getLog(getClass());
    public final static String closeDialogView = "/module/htmlformentry/closeDialog";
    public final static String FORM_IN_PROGRESS_KEY = "HTML_FORM_IN_PROGRESS_KEY";
    public final static String FORM_IN_PROGRESS_VALUE = "HTML_FORM_IN_PROGRESS_VALUE";
    public final static String FORM_PATH = "/module/htmlformentry/htmlFormEntry";

    @RequestMapping(value = "/module/htmlformentry/loadSession.form", method = RequestMethod.POST)
    @ResponseBody
    public String loadSession(@RequestParam("data") String data) throws Exception {
        byte[] bytes = org.apache.commons.codec.binary.Base64.decodeBase64(data);
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes));
        Object obj = ois.readObject();
        return "Loaded object of type: " + obj.getClass().getName();
    }
   
    // A place to store data that will persist longer than a session, but won't
 	// persist beyond application restart
    private static Map<User, Map<String, Object>> volatileUserData = new WeakHashMap<User, Map<String, Object>>();
    
    @Autowired
    private EncounterServiceCompatibility encounterServiceCompatibility;
    
    @RequestMapping(method=RequestMethod.GET, value=FORM_PATH)
    public void showForm() {
    	// Intentionally blank. All work is done in the getFormEntrySession method 
    }
    
    @ModelAttribute("command")
    public FormEntrySession getFormEntrySession(HttpServletRequest request,
                                                // @RequestParam doesn't pick up query parameters (in the url) in a POST, so I'm handling encounterId, modeParam, and which specially
                                                /*@RequestParam(value="mode", required=false) String modeParam,*/
                                                /*@RequestParam(value="encounterId", required=false) Integer encounterId,*/
                                                /*@RequestParam(value="which", required=false) String which,*/
                                                @RequestParam(value="patientId", required=false) Integer patientId,
                                                /*@RequestParam(value="personId", required=false) Integer personId,*/
                                                @RequestParam(value="formId", required=false) Integer formId,
                                                @RequestParam(value="htmlformId", required=false) Integer htmlFormId,
                                                @RequestParam(value="returnUrl", required=false) String returnUrl,
                                                @RequestParam(value="formModifiedTimestamp", required=false) Long formModifiedTimestamp,
                                                @RequestParam(value="encounterModifiedTimestamp", required=false) Long encounterModifiedTimestamp,
                                                @RequestParam(value="hasChangedInd", required=false) String hasChangedInd) throws Exception {

    	long ts = System.currentTimeMillis();

        Mode mode = resolveMode(request);

    	Integer personId = null;

    	if (StringUtils.hasText(request.getParameter("personId"))) {
    		personId = Integer.valueOf(request.getParameter("personId"));
    	}

        Patient patient = null;
    	Encounter encounter = null;
    	HtmlForm htmlForm = null;

    	if (StringUtils.hasText(request.getParameter("encounterId"))) {

    		String encounterId = request.getParameter("encounterId");
    		encounter = resolveEncounterById(encounterId);
    		patient = encounter.getPatient();
    		patientId = patient.getPatientId();
            personId = patient.getPersonId();
            
            htmlForm = resolveHtmlFormForEncounter(formId, encounter);

    	} else { // no encounter specified

    		// get person from patientId/personId (register module uses patientId, htmlformentry uses personId)
			if (patientId != null) {
				personId = patientId;
			}
			patient = resolvePatient(personId);

			htmlForm = resolveHtmlForm(htmlFormId, formId);

			String which = request.getParameter("which");
			if (StringUtils.hasText(which)) {
				encounter = resolveWhichEncounter(which, patient, htmlForm.getForm());
			}
    	}
    	
		patient = resolvePatientForSession(patient, mode, personId, patientId);

        FormEntrySession session = null;
		if (encounter != null) {
			session = new FormEntrySession(patient, encounter, mode, htmlForm, request.getSession());
		} 
		else {
			session = new FormEntrySession(patient, htmlForm, request.getSession());
		}

        if (StringUtils.hasText(returnUrl)) {
            session.setReturnUrl(returnUrl);
        }

        // Since we're not using a sessionForm, we need to check that the underlying form/encounter
        // were not modified after the client took these timestamps (optimistic-concurrency check).
        session.validateNotModifiedSinceTimestamps(formModifiedTimestamp, encounterModifiedTimestamp);

        if (hasChangedInd != null) session.setHasChangedInd(hasChangedInd);

        // ensure we've generated the form's HTML (and thus set up the submission actions, etc) before we do anything
        session.getHtmlToDisplay();

        setVolatileUserData(FORM_IN_PROGRESS_KEY, session);
       
        log.info("Took " + (System.currentTimeMillis() - ts) + " ms");
        
        return session;
    }

    /**
     * Determines the form-entry mode based on the "mode" request parameter.
     * Defaults to {@link Mode#VIEW} when the parameter is missing or unrecognized.
     *
     * @param request the incoming request
     * @return the resolved {@link Mode}
     */
    Mode resolveMode(HttpServletRequest request) {
        String modeParam = request.getParameter("mode");
        if ("enter".equalsIgnoreCase(modeParam)) {
            return Mode.ENTER;
        }
        else if ("edit".equalsIgnoreCase(modeParam)) {
            return Mode.EDIT;
        }
        return Mode.VIEW;
    }

    /**
     * Determines the {@link HtmlForm} to use for a form-entry session that is based on an
     * existing encounter.
     * <p/>
     * If formId is specified, looks up the HtmlForm associated with that form (this is
     * allowed to differ from the encounter's own form, e.g. for HtmlFormFlowsheet).
     * Otherwise falls back to the HtmlForm associated with the encounter's own form.
     *
     * @param formId optional form id that overrides the encounter's own form
     * @param encounter the encounter the session is based on
     * @return the resolved {@link HtmlForm}
     * @throws IllegalArgumentException if no matching HtmlForm can be found
     */
    HtmlForm resolveHtmlFormForEncounter(Integer formId, Encounter encounter) {
        HtmlForm htmlForm;
        if (formId != null) {
            Form form = Context.getFormService().getForm(formId);
            htmlForm = HtmlFormEntryUtil.getService().getHtmlFormByForm(form);
            if (htmlForm == null) {
                throw new IllegalArgumentException("No HtmlForm associated with formId " + formId);
            }
        }
        else {
            htmlForm = HtmlFormEntryUtil.getService().getHtmlFormByForm(encounter.getForm());
            if (htmlForm == null) {
                throw new IllegalArgumentException("The form for the specified encounter (" + encounter.getForm() + ") does not have an HtmlForm associated with it");
            }
        }
        return htmlForm;
    }

    /**
     * Looks up the {@link Encounter} identified by the "encounterId" request parameter, which may
     * be either a numeric encounter id or a UUID.
     *
     * @param encounterId the "encounterId" request parameter value (numeric id or UUID)
     * @return the matching {@link Encounter}
     * @throws IllegalArgumentException if no encounter matches encounterId
     */
    Encounter resolveEncounterById(String encounterId) {
        Encounter encounter;
        try {
            encounter = Context.getEncounterService().getEncounter(Integer.valueOf(encounterId));
        } catch (NumberFormatException ex) {
            encounter = Context.getEncounterService().getEncounterByUuid(encounterId);
        }
        if (encounter == null) {
            throw new IllegalArgumentException("No encounter with id=" + encounterId);
        }
        return encounter;
    }

    /**
     * Looks up the {@link Patient} for the given person/patient id.
     *
     * @param personId the person/patient id to look up (the register module uses patientId,
     *            htmlformentry uses personId; the caller is responsible for picking the right
     *            one), or {@code null} if no patient should be resolved
     * @return the matching {@link Patient}, or {@code null} if personId is {@code null}
     */
    Patient resolvePatient(Integer personId) {
        if (personId == null) {
            return null;
        }
        return Context.getPatientService().getPatient(personId);
    }

    /**
     * Determines the {@link HtmlForm} to use for a form-entry session that is <em>not</em> based
     * on an existing encounter.
     * <p/>
     * If htmlFormId is specified, that HtmlForm is used directly. Otherwise, if formId is
     * specified, the HtmlForm associated with that {@link Form} is used.
     *
     * @param htmlFormId optional id of the HtmlForm to use directly
     * @param formId optional id of the Form whose associated HtmlForm should be used (only
     *            consulted if htmlFormId is not specified)
     * @return the resolved {@link HtmlForm}
     * @throws IllegalArgumentException if neither htmlFormId nor formId is specified, or no
     *             matching HtmlForm can be found
     */
    HtmlForm resolveHtmlForm(Integer htmlFormId, Integer formId) {
        HtmlForm htmlForm = null;
        if (htmlFormId != null) {
            htmlForm = HtmlFormEntryUtil.getService().getHtmlForm(htmlFormId);
        } else if (formId != null) {
            Form form = Context.getFormService().getForm(formId);
            htmlForm = HtmlFormEntryUtil.getService().getHtmlFormByForm(form);
        }
        if (htmlForm == null) {
            throw new IllegalArgumentException("You must specify either an htmlFormId or a formId for a valid html form");
        }
        return htmlForm;
    }

    /**
     * Resolves the "first" or "last" existing {@link Encounter} for the given patient and form,
     * for form-entry sessions that specify the "which" request parameter.
     *
     * @param which "first" or "last"
     * @param patient the patient whose encounters should be searched
     * @param form the form whose encounters should be searched
     * @return the first or last matching {@link Encounter}, ordered as returned by
     *         {@link EncounterServiceCompatibility#getEncounters}
     * @throws IllegalArgumentException if patient is {@code null}, or which is neither "first"
     *             nor "last"
     */
    Encounter resolveWhichEncounter(String which, Patient patient, Form form) {
        if (patient == null) {
            throw new IllegalArgumentException("Cannot specify 'which' without specifying a person/patient");
        }
        List<Encounter> encs = encounterServiceCompatibility.getEncounters(patient, null, null, null, Collections.singleton(form), null, null, null, null, false);
        if ("first".equals(which)) {
            return encs.get(0);
        } else if ("last".equals(which)) {
            return encs.get(encs.size() - 1);
        } else {
            throw new IllegalArgumentException("which must be 'first' or 'last'");
        }
    }

    /**
     * Ensures a non-{@code null} {@link Patient} is available for constructing the
     * {@link FormEntrySession}.
     * <p/>
     * In {@link Mode#ENTER}, a new (unsaved) {@link Patient} is created if none was resolved from
     * the request. In any other mode, a patient must already have been resolved.
     *
     * @param patient the patient resolved so far (may be {@code null})
     * @param mode the form-entry mode
     * @param personId the personId used to resolve patient, for the error message if none was
     *            resolved
     * @param patientId the patientId used to resolve patient, for the error message if none was
     *            resolved
     * @return a non-{@code null} {@link Patient}
     * @throws IllegalArgumentException if mode is not {@link Mode#ENTER} and patient is
     *             {@code null}
     */
    Patient resolvePatientForSession(Patient patient, Mode mode, Integer personId, Integer patientId) {
        if (patient != null) {
            return patient;
        }
        if (mode == Mode.ENTER) {
            return new Patient();
        }
        throw new IllegalArgumentException("No patient with id of personId=" + personId + " or patientId=" + patientId);
    }

    /**
	 * Get a piece of information for the currently authenticated user. This information is stored
	 * only temporarily. When a new module is loaded or the server is restarted, this information
	 * will disappear. If there is not information by this key, null is returned TODO: This needs to
	 * be refactored/removed
	 * 
	 * @param key identifying string for the information
	 * @return the information stored
	 */
    public static Object getVolatileUserData(String key) {
		User u = Context.getAuthenticatedUser();
		if (u == null) {
			throw new APIAuthenticationException();
		}
		Map<String, Object> myData = volatileUserData.get(u);
		if (myData == null) {
			return null;
		} else {
			return myData.get(key);
		}
	}
    
    /**
	 * Set a piece of information for the currently authenticated user. This information is stored
	 * only temporarily. When a new module is loaded or the server is restarted, this information
	 * will disappear
	 * 
	 * @param key identifying string for this information
	 * @param value information to be stored
	 */
    public static void setVolatileUserData(String key, Object value) {
		User u = Context.getAuthenticatedUser();
		if (u == null) {
			throw new APIAuthenticationException();
		}
		Map<String, Object> myData = volatileUserData.get(u);
		if (myData == null) {
			myData = new HashMap<String, Object>();
			volatileUserData.put(u, myData);
		}
		myData.put(key, value);
	}
    
    /*
     * I'm using a return type of ModelAndView so I can use RedirectView rather than "redirect:" and preserve the fact that
     * returnUrl values from the pre-annotated-controller days will have the context path already
     */
    @RequestMapping(method=RequestMethod.POST, value=FORM_PATH)
    public ModelAndView handleSubmit(@ModelAttribute("command") FormEntrySession session,
                               Errors errors,
                               HttpServletRequest request,
                               Model model) throws Exception {
    	try {
            List<FormSubmissionError> validationErrors = session.getSubmissionController().validateSubmission(session.getContext(), request);
            if (validationErrors != null && validationErrors.size() > 0) {
                errors.reject("Fix errors");
            }
        } catch (Exception ex) {
            log.error("Exception during form validation", ex);
            errors.reject("Exception during form validation, see log for more details: " + ex);
        }
        
        if (errors.hasErrors()) {
        	return new ModelAndView(FORM_PATH, "command", session);
        }
        
        // no form validation errors, proceed with submission
        
        session.prepareForSubmit();

		if (session.getContext().getMode() == Mode.ENTER && session.hasPatientTag() && session.getPatient() == null 
				&& (session.getSubmissionActions().getPersonsToCreate() == null || session.getSubmissionActions().getPersonsToCreate().size() == 0))
			throw new IllegalArgumentException("This form is not going to create an Patient");

        if (session.getContext().getMode() == Mode.ENTER && session.hasEncouterTag() && (session.getSubmissionActions().getEncountersToCreate() == null || session.getSubmissionActions().getEncountersToCreate().size() == 0))
            throw new IllegalArgumentException("This form is not going to create an encounter"); 
        
    	try {
            session.getSubmissionController().handleFormSubmission(session, request);
            HtmlFormEntryUtil.getService().applyActions(session);
            String successView = session.getAfterSaveUrlTemplate();
            if (successView != null) {
                successView = successView.replaceAll("\\{\\{patient.id\\}\\}", session.getPatient().getId().toString());
                successView = successView.replaceAll("\\{\\{encounter.id\\}\\}", session.getEncounter().getId().toString());
                successView = request.getContextPath() + "/" + successView;
            } else {
                successView = session.getReturnUrlWithParameters();
            }
            if (successView == null)
                successView = request.getContextPath() + "/patientDashboard.form" + getQueryPrameters(request, session);
            if (StringUtils.hasText(request.getParameter("closeAfterSubmission"))) {
            	return new ModelAndView(closeDialogView, "dialogToClose", request.getParameter("closeAfterSubmission"));
            } else {
            	return new ModelAndView(new RedirectView(successView));
            }
        } catch (ValidationException ex) {
            log.error("Invalid input:", ex);
            errors.reject(ex.getMessage());
        } catch (BadFormDesignException ex) {
            log.error("Bad Form Design:", ex);
            errors.reject(ex.getMessage());
        } catch (Exception ex) {
            log.error("Exception trying to submit form", ex);
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            errors.reject("Exception! " + ex.getMessage() + "<br/>" + sw.toString());
        }
        
        // if we get here it's because we caught an error trying to submit/apply
        return new ModelAndView(FORM_PATH, "command", session);
    }

	protected String getQueryPrameters(HttpServletRequest request, FormEntrySession formEntrySession) {
		return "?patientId=" + formEntrySession.getPatient().getPersonId();
	}
}
