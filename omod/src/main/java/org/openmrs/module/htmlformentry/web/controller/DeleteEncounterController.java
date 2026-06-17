package org.openmrs.module.htmlformentry.web.controller;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Used to delete an encounter. Handles {@code deleteEncounter.form}.
 * <p/>
 * Format: {@code POST deleteEncounters.form?encounterId=123&reason=reason_for_voiding}.
 * <p/>
 * Redirects to the dashboard for the current Patient.
 */
@Controller
public class DeleteEncounterController {

	@RequestMapping(method=RequestMethod.POST, value="/module/htmlformentry/deleteEncounter")
    public ModelAndView handleRequest(@RequestParam("encounterId") Integer encounterId,
    								  @RequestParam("htmlFormId") Integer htmlFormId,
                                      @RequestParam(value="reason", required=false) String reason,
                                      @RequestParam(value="returnUrl", required=false) String returnUrl,
                                      HttpServletRequest request, HttpServletResponse response) throws Exception {
        // HFE-02 (CWE-862): voiding an encounter requires the proper privilege; a bare
        // logged-in session is no longer sufficient.
        requireDeletePrivilege();

        // HFE-02 (CWE-352): reject cross-site POSTs. Only same-origin requests are honoured,
        // which blocks the tokenless cross-site form used in the CSRF PoC.
        if (!isSameOrigin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cross-site request blocked.");
            return null;
        }

        Encounter enc = findEncounter(encounterId);
        // HFE-02 (CWE-639): fail closed on an unknown/invalid encounterId instead of NPE-ing.
        if (enc == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Encounter not found.");
            return null;
        }
        Integer ptId = enc.getPatient().getPatientId();
        voidEncounter(enc, htmlFormId, reason);

        // HFE-03 (CWE-601): never redirect to an attacker-controlled absolute/external URL.
        // Only a relative path within this application is allowed; anything else falls back
        // to the patient dashboard.
        if (!isSafeRelativeUrl(returnUrl)) {
        	returnUrl = request.getContextPath() + "/patientDashboard.form?patientId=" + ptId;
        }
        return new ModelAndView(new RedirectView(returnUrl));
    }

    /**
     * Seam around the privilege check so it can be exercised without booting a full OpenMRS context.
     */
    void requireDeletePrivilege() {
        Context.requirePrivilege("Delete Encounters");
    }

    /**
     * Seam around the encounter lookup so the controller flow can be unit-tested without a context.
     */
    Encounter findEncounter(Integer encounterId) {
        return Context.getEncounterService().getEncounter(encounterId);
    }

    /**
     * Seam that performs the actual void + save. Kept separate so the request-handling and security
     * branches can be unit-tested without touching the persistence layer.
     */
    void voidEncounter(Encounter enc, Integer htmlFormId, String reason) {
        HtmlFormEntryService hfes = Context.getService(HtmlFormEntryService.class);
        HtmlForm form = hfes.getHtmlForm(htmlFormId);
        HtmlFormEntryUtil.voidEncounter(enc, form, reason);
        Context.getEncounterService().saveEncounter(enc);
    }

    /**
     * CSRF defence (HFE-02): a state-changing POST is only accepted when the {@code Origin} (or, as
     * a fallback, {@code Referer}) header names the same host as the request. A missing or
     * mismatching header is treated as cross-site and rejected, which stops the tokenless
     * cross-site form POST used by the CSRF proof-of-concept.
     */
    boolean isSameOrigin(HttpServletRequest request) {
        String source = request.getHeader("Origin");
        if (!StringUtils.hasText(source)) {
            source = request.getHeader("Referer");
        }
        if (!StringUtils.hasText(source)) {
            return false;
        }
        try {
            String sourceHost = new URI(source).getHost();
            return sourceHost != null && sourceHost.equalsIgnoreCase(request.getServerName());
        }
        catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Open-redirect defence (HFE-03): accepts only relative same-application paths. The value must
     * start with a single '/', must not be protocol-relative ("//host"), must not embed a scheme
     * ("://") and must not contain a backslash (which some browsers normalise to '/'). Everything
     * else — including absolute external URLs — is rejected so the caller uses the safe default.
     */
    boolean isSafeRelativeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        if (!url.startsWith("/") || url.startsWith("//")) {
            return false;
        }
        return !url.contains("://") && !url.contains("\\");
    }

}
