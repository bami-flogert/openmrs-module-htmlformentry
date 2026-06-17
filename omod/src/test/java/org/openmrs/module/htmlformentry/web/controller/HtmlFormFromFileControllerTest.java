package org.openmrs.module.htmlformentry.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit tests for the HFE-01 (arbitrary file read / path traversal) mitigation in
 * {@link HtmlFormFromFileController#resolveSafePreviewFile(String)}. The static OpenMRS lookups used
 * by the base-directory resolution ({@link Context}, {@link OpenmrsUtil}) are mocked so the path
 * confinement logic can be tested in isolation. Only those dependency classes are prepared for
 * PowerMock — the controller itself is loaded normally and remains instrumented for coverage.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, OpenmrsUtil.class })
public class HtmlFormFromFileControllerTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private final HtmlFormFromFileController controller = new HtmlFormFromFileController();

	/** Points the configurable global property at the given directory. */
	private void useConfiguredBaseDir(File baseDir) {
		mockStatic(Context.class);
		AdministrationService adminService = mock(AdministrationService.class);
		when(Context.getAdministrationService()).thenReturn(adminService);
		when(adminService.getGlobalProperty(HtmlFormFromFileController.GP_PREVIEW_BASE_DIR))
		        .thenReturn(baseDir.getAbsolutePath());
	}

	// ---------------------------------------------------------------------
	// Rejected (unsafe) inputs — return null before any file system access
	// ---------------------------------------------------------------------

	@Test
	public void resolveSafePreviewFile_rejectsNull() {
		assertNull(controller.resolveSafePreviewFile(null));
	}

	@Test
	public void resolveSafePreviewFile_rejectsBlank() {
		assertNull(controller.resolveSafePreviewFile("   "));
	}

	@Test
	public void resolveSafePreviewFile_rejectsParentTraversal() {
		assertNull(controller.resolveSafePreviewFile("../../openmrs-server.properties"));
	}

	@Test
	public void resolveSafePreviewFile_rejectsAbsolutePath() {
		// Build an OS-independent absolute path (e.g. "C:\secret.xml" or "/secret.xml").
		File absolute = new File(File.listRoots()[0], "secret.xml");
		assertNull(controller.resolveSafePreviewFile(absolute.getAbsolutePath()));
	}

	// ---------------------------------------------------------------------
	// Accepted inputs — confined to the whitelisted base directory
	// ---------------------------------------------------------------------

	@Test
	public void resolveSafePreviewFile_returnsFileInsideConfiguredBaseDir() throws Exception {
		File baseDir = tempFolder.newFolder("forms");
		useConfiguredBaseDir(baseDir);

		File resolved = controller.resolveSafePreviewFile("probe.xml");

		assertNotNull(resolved);
		assertEquals(new File(baseDir, "probe.xml").getCanonicalFile(), resolved);
	}

	@Test
	public void resolveSafePreviewFile_fallsBackToAppDataDirAndCreatesIt() {
		mockStatic(Context.class);
		AdministrationService adminService = mock(AdministrationService.class);
		when(Context.getAdministrationService()).thenReturn(adminService);
		// No global property configured -> default <appdata>/htmlformentry base directory.
		when(adminService.getGlobalProperty(HtmlFormFromFileController.GP_PREVIEW_BASE_DIR))
		        .thenReturn(null);

		File appDataDir = tempFolder.getRoot();
		mockStatic(OpenmrsUtil.class);
		when(OpenmrsUtil.getApplicationDataDirectory()).thenReturn(appDataDir.getAbsolutePath());

		File resolved = controller.resolveSafePreviewFile("note.xml");

		assertNotNull(resolved);
		// The default base directory must have been created on demand.
		assertTrue(new File(appDataDir, "htmlformentry").isDirectory());
	}

}
