package org.openmrs.module.htmlformentry;

import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * Metadata-only audit log message formatting (NFR-S1/S2). Numeric IDs only — no PII or free text.
 */
public final class FormEntryAuditLogFormatter {

	private FormEntryAuditLogFormatter() {
	}

	public static String formatSessionCreated(Integer patientId, Integer userId) {
		StringBuilder sb = new StringBuilder(80);
		sb.append("FormEntrySession created:");
		appendAuditField(sb, "patientId", patientId);
		appendAuditField(sb, "userId", userId);
		sb.append(" action=session.created");
		return sb.toString();
	}

	public static String formatSubmitSuccess(Integer patientId, Integer userId, Integer htmlFormId,
			Integer encounterId, Mode mode) {
		StringBuilder sb = new StringBuilder(120);
		sb.append("Form submission completed:");
		appendAuditField(sb, "patientId", patientId);
		appendAuditField(sb, "userId", userId);
		appendAuditField(sb, "htmlFormId", htmlFormId);
		appendAuditField(sb, "encounterId", encounterId);
		sb.append(" mode=").append(safeModeName(mode));
		sb.append(" action=submit.success");
		return sb.toString();
	}

	public static String formatObsReference(Integer obsId, Integer conceptId) {
		StringBuilder sb = new StringBuilder(32);
		sb.append("obsId=");
		if (obsId != null) {
			sb.append(obsId.intValue());
		} else {
			sb.append("none");
		}
		sb.append(" conceptId=");
		if (conceptId != null) {
			sb.append(conceptId.intValue());
		} else {
			sb.append("none");
		}
		return sb.toString();
	}

	public static String formatObsDatetimeDebugMessage(Obs obs) {
		StringBuilder sb = new StringBuilder(64);
		sb.append("Set obsDatetime for obsId=");
		if (obs != null && obs.getObsId() != null) {
			sb.append(obs.getObsId().intValue());
		} else {
			sb.append("none");
		}
		sb.append(" conceptId=");
		if (obs != null && obs.getConcept() != null && obs.getConcept().getConceptId() != null) {
			sb.append(obs.getConcept().getConceptId().intValue());
		} else {
			sb.append("none");
		}
		return sb.toString();
	}

	public static String formatVoidObsDebugMessage(Integer obsId) {
		StringBuilder sb = new StringBuilder(32);
		sb.append("voiding obs obsId=");
		if (obsId != null) {
			sb.append(obsId.intValue());
		} else {
			sb.append("none");
		}
		return sb.toString();
	}

	public static String formatRelationshipDebugMessage(String action, Integer relationshipTypeId,
			Integer relationshipId) {
		StringBuilder sb = new StringBuilder(64);
		sb.append(action).append(" relationship relationshipTypeId=");
		if (relationshipTypeId != null) {
			sb.append(relationshipTypeId.intValue());
		} else {
			sb.append("none");
		}
		sb.append(" relationshipId=");
		if (relationshipId != null) {
			sb.append(relationshipId.intValue());
		} else {
			sb.append("none");
		}
		return sb.toString();
	}

	static void appendAuditField(StringBuilder sb, String fieldName, Integer value) {
		sb.append(' ').append(fieldName).append('=');
		if (value != null) {
			sb.append(value.intValue());
		} else {
			sb.append("none");
		}
	}

	static String safeModeName(Mode mode) {
		return mode != null ? mode.name() : "unknown";
	}

}
