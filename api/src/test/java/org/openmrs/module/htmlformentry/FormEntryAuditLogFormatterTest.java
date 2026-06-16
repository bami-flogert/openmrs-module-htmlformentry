package org.openmrs.module.htmlformentry;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class FormEntryAuditLogFormatterTest {

	@Test
	public void formatSessionCreated_shouldUseMetadataOnly() {
		String msg = FormEntryAuditLogFormatter.formatSessionCreated(2, 1);
		assertThat(msg, is("FormEntrySession created: patientId=2 userId=1 action=session.created"));
		assertThat(msg, not(containsString("names=")));
		assertThat(msg, not(containsString("dob=")));
	}

	@Test
	public void formatSessionCreated_shouldUseNoneForNullIds() {
		String msg = FormEntryAuditLogFormatter.formatSessionCreated(null, null);
		assertThat(msg, is("FormEntrySession created: patientId=none userId=none action=session.created"));
	}

	@Test
	public void formatSubmitSuccess_shouldIncludeModeAndAction() {
		String msg = FormEntryAuditLogFormatter.formatSubmitSuccess(2, 1, 3, 102, Mode.ENTER);
		assertThat(msg, containsString("patientId=2"));
		assertThat(msg, containsString("userId=1"));
		assertThat(msg, containsString("htmlFormId=3"));
		assertThat(msg, containsString("encounterId=102"));
		assertThat(msg, containsString("mode=ENTER"));
		assertThat(msg, containsString("action=submit.success"));
	}

	@Test
	public void formatSubmitSuccess_shouldUseNoneForNullFields() {
		String msg = FormEntryAuditLogFormatter.formatSubmitSuccess(null, null, null, null, null);
		assertThat(msg, containsString("patientId=none"));
		assertThat(msg, containsString("mode=unknown"));
		assertThat(msg, containsString("action=submit.success"));
	}

	@Test
	public void formatObsDatetimeDebugMessage_shouldUseObsAndConceptIdsOnly() {
		Concept concept = new Concept(7);
		Obs obs = new Obs();
		obs.setObsId(1);
		obs.setConcept(concept);
		String msg = FormEntryAuditLogFormatter.formatObsDatetimeDebugMessage(obs);
		assertThat(msg, is("Set obsDatetime for obsId=1 conceptId=7"));
	}

	@Test
	public void formatObsDatetimeDebugMessage_shouldHandleNullObs() {
		String msg = FormEntryAuditLogFormatter.formatObsDatetimeDebugMessage(null);
		assertThat(msg, is("Set obsDatetime for obsId=none conceptId=none"));
	}

	@Test
	public void formatVoidObsDebugMessage_shouldUseObsIdOnly() {
		assertThat(FormEntryAuditLogFormatter.formatVoidObsDebugMessage(42),
				is("voiding obs obsId=42"));
		assertThat(FormEntryAuditLogFormatter.formatVoidObsDebugMessage(null),
				is("voiding obs obsId=none"));
	}

	@Test
	public void formatRelationshipDebugMessage_shouldUseNumericIdsOnly() {
		String msg = FormEntryAuditLogFormatter.formatRelationshipDebugMessage("creating", 5, 9);
		assertThat(msg, is("creating relationship relationshipTypeId=5 relationshipId=9"));
	}

	@Test
	public void formatRelationshipDebugMessage_shouldHandleNullIds() {
		String msg = FormEntryAuditLogFormatter.formatRelationshipDebugMessage("voiding", null, 9);
		assertThat(msg, is("voiding relationship relationshipTypeId=none relationshipId=9"));
	}

	@Test
	public void formatRelationshipDebugMessage_shouldHandleAllNullIds() {
		String msg = FormEntryAuditLogFormatter.formatRelationshipDebugMessage("editing", null, null);
		assertThat(msg, is("editing relationship relationshipTypeId=none relationshipId=none"));
	}

	@Test
	public void formatObsReference_shouldUseMetadataOnly() {
		assertThat(FormEntryAuditLogFormatter.formatObsReference(1, 3), is("obsId=1 conceptId=3"));
		assertThat(FormEntryAuditLogFormatter.formatObsReference(null, null), is("obsId=none conceptId=none"));
	}

	@Test
	public void formatObsDatetimeDebugMessage_shouldHandleNullObsId() {
		Obs obs = new Obs();
		obs.setConcept(new Concept(7));
		String msg = FormEntryAuditLogFormatter.formatObsDatetimeDebugMessage(obs);
		assertThat(msg, is("Set obsDatetime for obsId=none conceptId=7"));
	}

	@Test
	public void formatObsDatetimeDebugMessage_shouldHandleNullConcept() {
		Obs obs = new Obs();
		obs.setObsId(1);
		String msg = FormEntryAuditLogFormatter.formatObsDatetimeDebugMessage(obs);
		assertThat(msg, is("Set obsDatetime for obsId=1 conceptId=none"));
	}

	@Test
	public void appendAuditField_shouldAppendNumericValue() {
		StringBuilder sb = new StringBuilder();
		FormEntryAuditLogFormatter.appendAuditField(sb, "htmlFormId", 5);
		assertThat(sb.toString(), is(" htmlFormId=5"));
	}

	@Test
	public void safeModeName_shouldReturnModeName() {
		assertThat(FormEntryAuditLogFormatter.safeModeName(Mode.EDIT), is("EDIT"));
		assertThat(FormEntryAuditLogFormatter.safeModeName(Mode.VIEW), is("VIEW"));
	}

}
