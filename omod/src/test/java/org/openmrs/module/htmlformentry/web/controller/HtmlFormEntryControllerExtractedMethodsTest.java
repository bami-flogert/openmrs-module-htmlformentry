package org.openmrs.module.htmlformentry.web.controller;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.compatibility.EncounterServiceCompatibility;
import org.powermock.reflect.Whitebox;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for methods extracted from {@link HtmlFormEntryController#getFormEntrySession}.
 * End-to-end behaviour: {@link org.openmrs.htmlformentry.web.controller.HtmlFormEntryControllerTest} (T1-T9).
 * resolveFormEntryContext paths that need Context are covered by characterization tests.
 * Traceability: docs/03-teststrategie.md section 7.2b.
 */
public class HtmlFormEntryControllerExtractedMethodsTest {

    private final HtmlFormEntryController controller = new HtmlFormEntryController();

    @Test
    public void resolveMode_shouldReturnEnter_whenModeParamIsEnter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("mode", "enter");

        assertThat(controller.resolveMode(request), is(Mode.ENTER));
    }

    @Test
    public void resolveMode_shouldReturnEdit_whenModeParamIsEdit() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("mode", "edit");

        assertThat(controller.resolveMode(request), is(Mode.EDIT));
    }

    @Test
    public void resolveMode_shouldBeCaseInsensitive() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("mode", "ENTER");

        assertThat(controller.resolveMode(request), is(Mode.ENTER));
    }

    @Test
    public void resolveMode_shouldDefaultToView_whenModeParamIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(controller.resolveMode(request), is(Mode.VIEW));
    }

    @Test
    public void resolveMode_shouldDefaultToView_whenModeParamIsUnrecognized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("mode", "somethingElse");

        assertThat(controller.resolveMode(request), is(Mode.VIEW));
    }

    @Test
    public void resolvePatient_shouldReturnNull_whenPersonIdIsNull() {
        assertNull(controller.resolvePatient(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveHtmlForm_shouldThrow_whenNeitherIdProvided() {
        controller.resolveHtmlForm(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveWhichEncounter_shouldThrow_whenPatientIsNull() {
        controller.resolveWhichEncounter("first", null, new Form());
    }

    @Test
    public void resolveWhichEncounter_shouldReturnFirstEncounter_whenWhichIsFirst() {
        Patient patient = new Patient();
        Form form = new Form();
        Encounter first = new Encounter();
        Encounter last = new Encounter();
        List<Encounter> encs = Arrays.asList(first, last);

        EncounterServiceCompatibility encounterServiceCompatibility = mock(EncounterServiceCompatibility.class);
        when(encounterServiceCompatibility.getEncounters(patient, null, null, null,
                java.util.Collections.singleton(form), null, null, null, null, false)).thenReturn(encs);
        Whitebox.setInternalState(controller, "encounterServiceCompatibility", encounterServiceCompatibility);

        Encounter result = controller.resolveWhichEncounter("first", patient, form);

        assertThat(result, is(first));
    }

    @Test
    public void resolveWhichEncounter_shouldReturnLastEncounter_whenWhichIsLast() {
        Patient patient = new Patient();
        Form form = new Form();
        Encounter first = new Encounter();
        Encounter last = new Encounter();
        List<Encounter> encs = Arrays.asList(first, last);

        EncounterServiceCompatibility encounterServiceCompatibility = mock(EncounterServiceCompatibility.class);
        when(encounterServiceCompatibility.getEncounters(patient, null, null, null,
                java.util.Collections.singleton(form), null, null, null, null, false)).thenReturn(encs);
        Whitebox.setInternalState(controller, "encounterServiceCompatibility", encounterServiceCompatibility);

        Encounter result = controller.resolveWhichEncounter("last", patient, form);

        assertThat(result, is(last));
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveWhichEncounter_shouldThrow_whenWhichIsNotFirstOrLast() {
        Patient patient = new Patient();
        Form form = new Form();

        EncounterServiceCompatibility encounterServiceCompatibility = mock(EncounterServiceCompatibility.class);
        when(encounterServiceCompatibility.getEncounters(patient, null, null, null,
                java.util.Collections.singleton(form), null, null, null, null, false)).thenReturn(Arrays.asList(new Encounter()));
        Whitebox.setInternalState(controller, "encounterServiceCompatibility", encounterServiceCompatibility);

        controller.resolveWhichEncounter("middle", patient, form);
    }

    @Test
    public void resolvePatientForSession_shouldReturnGivenPatient_whenPatientIsNotNull() {
        Patient patient = new Patient();

        Patient result = controller.resolvePatientForSession(patient, Mode.VIEW, 1, 2);

        assertThat(result, is(patient));
    }

    @Test
    public void resolvePatientForSession_shouldCreateNewPatient_whenEnterModeAndNoPatient() {
        Patient result = controller.resolvePatientForSession(null, Mode.ENTER, null, null);

        assertNotNull(result);
        assertNull(result.getPatientId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolvePatientForSession_shouldThrow_whenNotEnterModeAndNoPatient() {
        controller.resolvePatientForSession(null, Mode.VIEW, 1, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolvePatientForSession_shouldThrow_whenEditModeAndNoPatient() {
        controller.resolvePatientForSession(null, Mode.EDIT, null, 2);
    }
}
