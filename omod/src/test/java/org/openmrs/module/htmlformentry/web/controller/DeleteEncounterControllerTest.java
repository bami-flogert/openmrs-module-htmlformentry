package org.openmrs.module.htmlformentry.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Unit tests for the HFE-02 (CSRF / missing-authorization / IDOR) and HFE-03 (open-redirect)
 * mitigations in {@link DeleteEncounterController}. The pure defence helpers are tested directly; the
 * {@code handleRequest} flow is exercised through a subclass that overrides the OpenMRS seams, so no
 * full context-sensitive test has to be booted.
 */
public class DeleteEncounterControllerTest {

	private final DeleteEncounterController controller = new DeleteEncounterController();

	/** Subclass that stubs the static-OpenMRS seams so the request flow can run without a context. */
	private static class TestableController extends DeleteEncounterController {

		private Encounter encounter;

		private boolean voided;

		@Override
		void requireDeletePrivilege() {
			// no-op: privilege enforcement is provided by the OpenMRS core at runtime
		}

		@Override
		Encounter findEncounter(Integer encounterId) {
			return encounter;
		}

		@Override
		void voidEncounter(Encounter enc, Integer htmlFormId, String reason) {
			voided = true;
		}
	}

	private static HttpServletRequest sameOriginRequest() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Origin")).thenReturn("http://localhost:8080");
		when(request.getServerName()).thenReturn("localhost");
		when(request.getContextPath()).thenReturn("/openmrs");
		return request;
	}

	// ---------------------------------------------------------------------
	// HFE-03 — isSafeRelativeUrl (open-redirect defence)
	// ---------------------------------------------------------------------

	@Test
	public void isSafeRelativeUrl_acceptsRelativeApplicationPath() {
		assertTrue(controller.isSafeRelativeUrl("/patientDashboard.form?patientId=7"));
	}

	@Test
	public void isSafeRelativeUrl_rejectsNull() {
		assertFalse(controller.isSafeRelativeUrl(null));
	}

	@Test
	public void isSafeRelativeUrl_rejectsBlank() {
		assertFalse(controller.isSafeRelativeUrl("   "));
	}

	@Test
	public void isSafeRelativeUrl_rejectsAbsoluteExternalUrl() {
		assertFalse(controller.isSafeRelativeUrl("https://evil.example.com/phish"));
	}

	@Test
	public void isSafeRelativeUrl_rejectsProtocolRelativeUrl() {
		assertFalse(controller.isSafeRelativeUrl("//evil.example.com"));
	}

	@Test
	public void isSafeRelativeUrl_rejectsNonSlashStart() {
		assertFalse(controller.isSafeRelativeUrl("patientDashboard.form"));
	}

	@Test
	public void isSafeRelativeUrl_rejectsEmbeddedScheme() {
		assertFalse(controller.isSafeRelativeUrl("/redirect?to=http://evil.example.com"));
	}

	@Test
	public void isSafeRelativeUrl_rejectsBackslash() {
		assertFalse(controller.isSafeRelativeUrl("/path\\evil"));
	}

	// ---------------------------------------------------------------------
	// HFE-02 — isSameOrigin (CSRF defence)
	// ---------------------------------------------------------------------

	@Test
	public void isSameOrigin_trueWhenOriginHostMatches() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Origin")).thenReturn("http://localhost:8080");
		when(request.getServerName()).thenReturn("localhost");
		assertTrue(controller.isSameOrigin(request));
	}

	@Test
	public void isSameOrigin_falseWhenOriginHostDiffers() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Origin")).thenReturn("http://evil.example.com");
		when(request.getServerName()).thenReturn("localhost");
		assertFalse(controller.isSameOrigin(request));
	}

	@Test
	public void isSameOrigin_fallsBackToRefererHeader() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Origin")).thenReturn(null);
		when(request.getHeader("Referer")).thenReturn("http://localhost/openmrs/page.form");
		when(request.getServerName()).thenReturn("localhost");
		assertTrue(controller.isSameOrigin(request));
	}

	@Test
	public void isSameOrigin_falseWhenNoOriginOrRefererPresent() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Origin")).thenReturn(null);
		when(request.getHeader("Referer")).thenReturn(null);
		assertFalse(controller.isSameOrigin(request));
	}

	@Test
	public void isSameOrigin_falseOnMalformedSourceUri() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		// A space makes this an invalid URI, exercising the URISyntaxException branch.
		when(request.getHeader("Origin")).thenReturn("http://exa mple.com");
		when(request.getServerName()).thenReturn("localhost");
		assertFalse(controller.isSameOrigin(request));
	}

	// ---------------------------------------------------------------------
	// handleRequest — full flow through the overridden seams
	// ---------------------------------------------------------------------

	@Test
	public void handleRequest_blocksCrossSitePostWithForbidden() throws Exception {
		TestableController testable = new TestableController();
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		// Cross-site: Origin host does not match the server name.
		when(request.getHeader("Origin")).thenReturn("http://evil.example.com");
		when(request.getServerName()).thenReturn("localhost");

		ModelAndView result = testable.handleRequest(3, 6, "reason", null, request, response);

		assertNull(result);
		verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
	}

	@Test
	public void handleRequest_returnsNotFoundForUnknownEncounter() throws Exception {
		TestableController testable = new TestableController();
		testable.encounter = null;
		HttpServletResponse response = mock(HttpServletResponse.class);

		ModelAndView result = testable.handleRequest(999, 6, "reason", null, sameOriginRequest(), response);

		assertNull(result);
		verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
	}

	@Test
	public void handleRequest_voidsAndRedirectsToSafeRelativeReturnUrl() throws Exception {
		TestableController testable = new TestableController();
		testable.encounter = encounterForPatient(7);
		HttpServletResponse response = mock(HttpServletResponse.class);

		ModelAndView result = testable.handleRequest(3, 6, "reason",
		        "/patientDashboard.form?patientId=7", sameOriginRequest(), response);

		assertTrue(testable.voided);
		assertEquals("/patientDashboard.form?patientId=7", redirectUrl(result));
		verify(response, never()).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
	}

	@Test
	public void handleRequest_replacesUnsafeReturnUrlWithDashboard() throws Exception {
		TestableController testable = new TestableController();
		testable.encounter = encounterForPatient(7);
		HttpServletResponse response = mock(HttpServletResponse.class);

		ModelAndView result = testable.handleRequest(3, 6, "reason",
		        "https://evil.example.com/phish", sameOriginRequest(), response);

		// The attacker-controlled URL is dropped in favour of the internal dashboard path.
		assertEquals("/openmrs/patientDashboard.form?patientId=7", redirectUrl(result));
	}

	private static Encounter encounterForPatient(Integer patientId) {
		Patient patient = mock(Patient.class);
		when(patient.getPatientId()).thenReturn(patientId);
		Encounter encounter = mock(Encounter.class);
		when(encounter.getPatient()).thenReturn(patient);
		return encounter;
	}

	private static String redirectUrl(ModelAndView result) {
		return ((RedirectView) result.getView()).getUrl();
	}

}
