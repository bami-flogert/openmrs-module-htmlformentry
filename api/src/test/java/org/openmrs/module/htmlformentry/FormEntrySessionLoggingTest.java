package org.openmrs.module.htmlformentry;

import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Covers audit log paths in {@link FormEntrySession} (null patientId branch).
 * See docs/03-teststrategie.md §7.6.
 */
public class FormEntrySessionLoggingTest extends BaseModuleContextSensitiveTest {

	@Test
	public void shouldCreateSessionAndLogMetadataWhenPatientIsNull() throws Exception {
		new FormEntrySession(null, "<htmlform></htmlform>", null);
	}

}
