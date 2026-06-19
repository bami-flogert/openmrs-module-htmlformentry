package org.openmrs.module.htmlformentry;

import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Covers audit log paths in {@link FormEntrySession} (null patientId branch).
 * Log message format is asserted in {@link FormEntryAuditLogFormatterTest}; this test
 * verifies the null-patient constructor path completes and leaves session state correct.
 * See docs/03-teststrategie.md §7.6.
 */
public class FormEntrySessionLoggingTest extends BaseModuleContextSensitiveTest {

	@Test
	public void shouldCreateSessionAndLogMetadataWhenPatientIsNull() throws Exception {
		FormEntrySession session = new FormEntrySession(null, "<htmlform></htmlform>", null);

		assertNotNull(session);
		assertThat(session.getPatient(), is(nullValue()));
		assertThat(session.getContext().getMode(), is(Mode.ENTER));
	}

}
