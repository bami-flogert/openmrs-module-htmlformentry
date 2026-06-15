package org.openmrs.htmlformentry.web.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.TestUtil;
import org.openmrs.module.htmlformentry.compatibility.EncounterServiceCompatibility;
import org.openmrs.module.htmlformentry.web.controller.HtmlFormEntryController;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Characterization tests for {@link HtmlFormEntryController#getFormEntrySession}.
 * Traceability: docs/03-teststrategie.md §7.4 (T1–T9).
 */
public class HtmlFormEntryControllerTest extends BaseModuleContextSensitiveTest {

	private static final String HTML_FORM_XML = "org/openmrs/module/htmlformentry/include/simplestForm.xml";
	private static final int PATIENT_ID = 2;
	private static final Date ENCOUNTER_DATE_OLD = new GregorianCalendar(2010, Calendar.JANUARY, 1).getTime();
	private static final Date ENCOUNTER_DATE_RECENT = new GregorianCalendar(2010, Calendar.JANUARY, 10).getTime();

	private HtmlFormEntryController controller;
	private MockHttpServletRequest request;
	private Patient patient;
	private Form form;
	private int htmlFormId;

	@Before
	public void setUp() throws Exception {
		HtmlFormEntryService service = Context.getService(HtmlFormEntryService.class);

		form = new Form();
		form.setName("ControllerTestForm");
		form.setVersion("1.0");
		form.setPublished(true);
		form.setEncounterType(Context.getEncounterService().getEncounterType(1));
		form = Context.getFormService().saveForm(form);

		HtmlForm htmlForm = new HtmlForm();
		htmlForm.setForm(form);
		htmlForm.setXmlData(new TestUtil().loadXmlFromFile(HTML_FORM_XML));
		htmlForm = service.saveHtmlForm(htmlForm);
		htmlFormId = htmlForm.getId();

		patient = Context.getPatientService().getPatient(PATIENT_ID);

		controller = new HtmlFormEntryController();
		EncounterServiceCompatibility encounterServiceCompatibility = Context.getRegisteredComponent(
				"htmlformentry.EncounterServiceCompatibility", EncounterServiceCompatibility.class);
		ReflectionTestUtils.setField(controller, "encounterServiceCompatibility", encounterServiceCompatibility);

		request = new MockHttpServletRequest();
		request.setSession(new MockHttpSession());
	}

	// T1 — Must
	@Test
	public void shouldCreateEnterSessionWithHtmlFormId() throws Exception {
		request.setParameter("mode", "enter");

		FormEntrySession session = invoke(PATIENT_ID, null, htmlFormId, null, null, null, null);

		Assert.assertNotNull(session);
		Assert.assertEquals(Mode.ENTER, session.getContext().getMode());
		Assert.assertNull(session.getEncounter());
	}

	// T2 — Must
	@Test
	public void shouldCreateViewSessionWithEncounterId() throws Exception {
		Encounter saved = saveEncounter(ENCOUNTER_DATE_RECENT);

		request.setParameter("encounterId", String.valueOf(saved.getEncounterId()));

		FormEntrySession session = invoke(null, null, null, null, null, null, null);

		Assert.assertNotNull(session);
		Assert.assertEquals(Mode.VIEW, session.getContext().getMode());
		Assert.assertEquals(saved.getEncounterId(), session.getEncounter().getEncounterId());
	}

	// T3 — Must
	@Test
	public void shouldSelectFirstEncounterWhenWhichIsFirst() throws Exception {
		Encounter first = saveEncounter(ENCOUNTER_DATE_OLD);
		Encounter last = saveEncounter(ENCOUNTER_DATE_RECENT);

		request.setParameter("which", "first");
		request.setParameter("mode", "view");

		FormEntrySession session = invoke(PATIENT_ID, form.getFormId(), null, null, null, null, null);

		Assert.assertEquals(first.getEncounterId(), session.getEncounter().getEncounterId());
		Assert.assertNotEquals(last.getEncounterId(), session.getEncounter().getEncounterId());
	}

	// T4 — Must
	@Test
	public void shouldSelectLastEncounterWhenWhichIsLast() throws Exception {
		Encounter first = saveEncounter(ENCOUNTER_DATE_OLD);
		Encounter last = saveEncounter(ENCOUNTER_DATE_RECENT);

		request.setParameter("which", "last");
		request.setParameter("mode", "view");

		FormEntrySession session = invoke(PATIENT_ID, form.getFormId(), null, null, null, null, null);

		Assert.assertEquals(last.getEncounterId(), session.getEncounter().getEncounterId());
		Assert.assertNotEquals(first.getEncounterId(), session.getEncounter().getEncounterId());
	}

	// T4b — Must
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowWhenWhichIsInvalid() throws Exception {
		request.setParameter("which", "invalid");
		request.setParameter("mode", "view");

		invoke(PATIENT_ID, form.getFormId(), null, null, null, null, null);
	}

	// T5 — Must
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowWhenNoHtmlFormForFormId() throws Exception {
		Form bareForm = new Form();
		bareForm.setName("Form without HtmlForm");
		bareForm.setVersion("1.0");
		bareForm.setPublished(true);
		bareForm.setEncounterType(Context.getEncounterService().getEncounterType(1));
		bareForm = Context.getFormService().saveForm(bareForm);

		request.setParameter("mode", "enter");

		invoke(PATIENT_ID, bareForm.getFormId(), null, null, null, null, null);
	}

	// T6 — Should
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowWhenEncounterNotFound() throws Exception {
		request.setParameter("encounterId", "99999");

		invoke(null, null, null, null, null, null, null);
	}

	// T7 — Must
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowWhenNoPatientInViewMode() throws Exception {
		invoke(null, null, htmlFormId, null, null, null, null);
	}

	// T8 — Should
	@Test
	public void shouldSetReturnUrlOnSession() throws Exception {
		request.setParameter("mode", "enter");
		String returnUrl = "/module/htmlformentry/list.form";

		FormEntrySession session = invoke(PATIENT_ID, null, htmlFormId, returnUrl, null, null, null);

		Assert.assertEquals(returnUrl, session.getReturnUrl());
	}

	// T9 — Should
	@Test(expected = RuntimeException.class)
	public void shouldThrowWhenFormModified() throws Exception {
		request.setParameter("mode", "enter");

		invoke(PATIENT_ID, null, htmlFormId, null, 0L, null, null);
	}

	private FormEntrySession invoke(Integer patientId, Integer formId, Integer htmlFormId, String returnUrl,
	                                Long formModifiedTimestamp, Long encounterModifiedTimestamp, String hasChangedInd)
	        throws Exception {
		return controller.getFormEntrySession(request, patientId, formId, htmlFormId, returnUrl, formModifiedTimestamp,
				encounterModifiedTimestamp, hasChangedInd);
	}

	private Encounter saveEncounter(Date encounterDatetime) {
		Encounter encounter = new Encounter();
		encounter.setPatient(patient);
		encounter.setForm(form);
		encounter.setEncounterDatetime(encounterDatetime);
		encounter.setLocation(Context.getLocationService().getLocation(2));
		encounter.setEncounterType(Context.getEncounterService().getEncounterType(1));
		return Context.getEncounterService().saveEncounter(encounter);
	}
}
