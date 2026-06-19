package org.openmrs.module.htmlformentry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit tests for {@link FormEntrySession#validateNotModifiedSinceTimestamps(Long, Long)}.
 * <p/>
 * This method was moved here from
 * {@code HtmlFormEntryController#getFormEntrySession} as part of the maintainability PoC (Move
 * Method / SRP). It only depends on the session's own state and on
 * {@code Context.getMessageSourceService()}, so we build a {@link FormEntrySession} instance via
 * {@link Whitebox#newInstance(Class)} (bypassing its heavyweight constructors, which require a
 * full Spring/DB context) and use {@link Whitebox#setInternalState} to control the
 * formModifiedTimestamp / encounterModifiedTimestamp / encounter fields directly. This keeps the
 * test fast and dependency-free (NFR-M4, testability).
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class FormEntrySessionValidateNotModifiedSinceTimestampsTest {

    private static final String FORM_MODIFIED_MESSAGE = "Form was modified";

    private static final String ENCOUNTER_MODIFIED_MESSAGE = "Encounter was modified";

    private FormEntrySession session;

    @Before
    public void setup() {
        session = Whitebox.newInstance(FormEntrySession.class);

        MessageSourceService messageSourceService = mock(MessageSourceService.class);
        PowerMockito.when(messageSourceService.getMessage("htmlformentry.error.formModifiedBeforeSubmission"))
                .thenReturn(FORM_MODIFIED_MESSAGE);
        PowerMockito.when(messageSourceService.getMessage("htmlformentry.error.encounterModifiedBeforeSubmission"))
                .thenReturn(ENCOUNTER_MODIFIED_MESSAGE);

        PowerMockito.mockStatic(Context.class);
        PowerMockito.when(Context.getMessageSourceService()).thenReturn(messageSourceService);
    }

    @Test
    public void shouldNotThrow_whenFormModifiedTimestampMatches() {
        Whitebox.setInternalState(session, "formModifiedTimestamp", 1000L);

        session.validateNotModifiedSinceTimestamps(1000L, null);

        // no exception expected
    }

    @Test
    public void shouldThrow_whenFormModifiedTimestampDoesNotMatch() {
        Whitebox.setInternalState(session, "formModifiedTimestamp", 1000L);

        try {
            session.validateNotModifiedSinceTimestamps(999L, null);
            fail("Expected a RuntimeException because the form was modified");
        } catch (RuntimeException ex) {
            assertEquals(FORM_MODIFIED_MESSAGE, ex.getMessage());
        }
    }

    @Test
    public void shouldNotThrow_whenFormModifiedTimestampIsNull() {
        Whitebox.setInternalState(session, "formModifiedTimestamp", 1000L);

        // null means "client did not send a timestamp", so the check should be skipped
        // even though the value below would not match
        session.validateNotModifiedSinceTimestamps(null, null);

        // no exception expected
    }

    @Test
    public void shouldNotThrow_whenEncounterModifiedTimestampMatches() {
        Whitebox.setInternalState(session, "encounter", new Encounter());
        Whitebox.setInternalState(session, "encounterModifiedTimestamp", 2000L);

        session.validateNotModifiedSinceTimestamps(null, 2000L);

        // no exception expected
    }

    @Test
    public void shouldThrow_whenEncounterModifiedTimestampDoesNotMatch() {
        Whitebox.setInternalState(session, "encounter", new Encounter());
        Whitebox.setInternalState(session, "encounterModifiedTimestamp", 2000L);

        try {
            session.validateNotModifiedSinceTimestamps(null, 1999L);
            fail("Expected a RuntimeException because the encounter was modified");
        } catch (RuntimeException ex) {
            assertEquals(ENCOUNTER_MODIFIED_MESSAGE, ex.getMessage());
        }
    }

    @Test
    public void shouldNotThrow_whenEncounterModifiedTimestampIsNull() {
        Whitebox.setInternalState(session, "encounter", new Encounter());
        Whitebox.setInternalState(session, "encounterModifiedTimestamp", 2000L);

        // null means "client did not send a timestamp", so the check should be skipped
        session.validateNotModifiedSinceTimestamps(null, null);

        // no exception expected
    }

    @Test
    public void shouldSkipEncounterCheck_whenSessionHasNoEncounter() {
        Whitebox.setInternalState(session, "encounter", (Encounter) null);
        Whitebox.setInternalState(session, "encounterModifiedTimestamp", 2000L);

        // even though the timestamp "mismatches", there is no encounter to validate against
        session.validateNotModifiedSinceTimestamps(null, 1999L);

        // no exception expected
    }

    @Test
    public void shouldCheckBothTimestamps_whenBothAreProvided() {
        Whitebox.setInternalState(session, "formModifiedTimestamp", 1000L);
        Whitebox.setInternalState(session, "encounter", new Encounter());
        Whitebox.setInternalState(session, "encounterModifiedTimestamp", 2000L);

        session.validateNotModifiedSinceTimestamps(1000L, 2000L);

        // no exception expected
    }
}
