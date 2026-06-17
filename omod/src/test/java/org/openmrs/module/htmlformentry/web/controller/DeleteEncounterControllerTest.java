package org.openmrs.module.htmlformentry.web.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.web.servlet.ModelAndView;

/**
 * Unit tests for the HFE-02 (CSRF / missing-authorization) and HFE-03 (open-redirect) mitigations in
 * {@link DeleteEncounterController}. The pure helpers are tested directly; the early-exit branches of
 * {@code handleRequest} are driven with a mocked static {@link Context} so no full context-sensitive
 * test has to be booted. Only {@code Context} is prepared for PowerMock, so the controller under test
 * is still instrumented normally for coverage.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class DeleteEncounterControllerTest {

	private final DeleteEncounterController controller = new DeleteEncounterController();

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
	// handleRequest — early-exit security branches
	// ---------------------------------------------------------------------

	@Test
	public void handleRequest_blocksCrossSitePostWithForbidden() throws Exception {
		mockStatic(Context.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		// Cross-site: Origin host does not match the server name.
		when(request.getHeader("Origin")).thenReturn("http://evil.example.com");
		when(request.getServerName()).thenReturn("localhost");

		ModelAndView result = controller.handleRequest(3, 6, "reason", null, request, response);

		assertNull(result);
		verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
	}

	@Test
	public void handleRequest_returnsNotFoundForUnknownEncounter() throws Exception {
		mockStatic(Context.class);
		EncounterService encounterService = mock(EncounterService.class);
		when(Context.getEncounterService()).thenReturn(encounterService);
		when(encounterService.getEncounter(999)).thenReturn(null);

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		// Same-origin so the request passes the CSRF check and reaches the lookup.
		when(request.getHeader("Origin")).thenReturn("http://localhost:8080");
		when(request.getServerName()).thenReturn("localhost");

		ModelAndView result = controller.handleRequest(999, 6, "reason", null, request, response);

		assertNull(result);
		verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
	}

}
