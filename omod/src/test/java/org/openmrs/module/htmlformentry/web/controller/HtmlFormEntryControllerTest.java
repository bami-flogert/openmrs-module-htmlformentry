package org.openmrs.module.htmlformentry.web.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Unit tests for the small pieces extracted from
 * {@link HtmlFormEntryController#getFormEntrySession}.
 *
 * resolveMode() is dependency-free (no database/Spring context needed).
 * resolveHtmlFormForEncounter() calls the static service-locator methods
 * Context.getFormService() and HtmlFormEntryUtil.getService(), so PowerMock
 * is used to mock those static calls - no real database or Spring context
 * is started.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(HtmlFormEntryUtil.class)
public class HtmlFormEntryControllerTest {

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
    @PrepareForTest({ Context.class, HtmlFormEntryUtil.class })
    public void resolveHtmlFormForEncounter_shouldUseFormId_whenFormIdProvided() {
        mockStatic(Context.class);
        mockStatic(HtmlFormEntryUtil.class);

        FormService formService = mock(FormService.class);
        HtmlFormEntryService htmlFormEntryService = mock(HtmlFormEntryService.class);

        Form form = new Form();
        form.setFormId(7);
        HtmlForm expectedHtmlForm = new HtmlForm();

        when(Context.getFormService()).thenReturn(formService);
        when(formService.getForm(7)).thenReturn(form);
        when(HtmlFormEntryUtil.getService()).thenReturn(htmlFormEntryService);
        when(htmlFormEntryService.getHtmlFormByForm(form)).thenReturn(expectedHtmlForm);

        HtmlForm result = controller.resolveHtmlFormForEncounter(7, new Encounter());

        assertThat(result, is(expectedHtmlForm));
    }

    @Test(expected = IllegalArgumentException.class)
    @PrepareForTest({ Context.class, HtmlFormEntryUtil.class })
    public void resolveHtmlFormForEncounter_shouldThrow_whenFormIdProvidedButNoHtmlFormFound() {
        mockStatic(Context.class);
        mockStatic(HtmlFormEntryUtil.class);

        FormService formService = mock(FormService.class);
        HtmlFormEntryService htmlFormEntryService = mock(HtmlFormEntryService.class);

        Form form = new Form();
        form.setFormId(7);

        when(Context.getFormService()).thenReturn(formService);
        when(formService.getForm(7)).thenReturn(form);
        when(HtmlFormEntryUtil.getService()).thenReturn(htmlFormEntryService);
        when(htmlFormEntryService.getHtmlFormByForm(form)).thenReturn(null);

        controller.resolveHtmlFormForEncounter(7, new Encounter());
    }

    @Test
    public void resolveHtmlFormForEncounter_shouldUseEncounterForm_whenFormIdNotProvided() {
        mockStatic(HtmlFormEntryUtil.class);

        HtmlFormEntryService htmlFormEntryService = mock(HtmlFormEntryService.class);

        Form encounterForm = new Form();
        encounterForm.setFormId(3);
        HtmlForm expectedHtmlForm = new HtmlForm();

        Encounter encounter = new Encounter();
        encounter.setForm(encounterForm);

        when(HtmlFormEntryUtil.getService()).thenReturn(htmlFormEntryService);
        when(htmlFormEntryService.getHtmlFormByForm(encounterForm)).thenReturn(expectedHtmlForm);

        HtmlForm result = controller.resolveHtmlFormForEncounter(null, encounter);

        assertThat(result, is(expectedHtmlForm));
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveHtmlFormForEncounter_shouldThrow_whenFormIdNotProvidedAndEncounterFormHasNoHtmlForm() {
        mockStatic(HtmlFormEntryUtil.class);

        HtmlFormEntryService htmlFormEntryService = mock(HtmlFormEntryService.class);

        Form encounterForm = new Form();
        encounterForm.setFormId(3);

        Encounter encounter = new Encounter();
        encounter.setForm(encounterForm);

        when(HtmlFormEntryUtil.getService()).thenReturn(htmlFormEntryService);
        when(htmlFormEntryService.getHtmlFormByForm(encounterForm)).thenReturn(null);

        controller.resolveHtmlFormForEncounter(null, encounter);
    }
}
