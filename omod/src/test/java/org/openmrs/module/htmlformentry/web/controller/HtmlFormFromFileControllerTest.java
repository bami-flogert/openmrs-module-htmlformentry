package org.openmrs.module.htmlformentry.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for the HFE-01 (arbitrary file read / path traversal) mitigation in
 * {@link HtmlFormFromFileController}. The pure base-directory resolution is tested directly, and the
 * path-confinement logic is exercised through a subclass that stubs the two OpenMRS lookup seams, so
 * neither a running OpenMRS context nor static mocking is required.
 */
public class HtmlFormFromFileControllerTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private final HtmlFormFromFileController controller = new HtmlFormFromFileController();

	/** Subclass that supplies the base-directory inputs instead of looking them up via OpenMRS. */
	private static class TestableController extends HtmlFormFromFileController {

		private String configured;

		private String appDataDir;

		@Override
		String getConfiguredBaseDirectory() {
			return configured;
		}

		@Override
		String getApplicationDataDirectory() {
			return appDataDir;
		}
	}

	// ---------------------------------------------------------------------
	// resolveBaseDirectory — base directory selection + on-demand creation
	// ---------------------------------------------------------------------

	@Test
	public void resolveBaseDirectory_usesConfiguredDirectoryAndCreatesIt() {
		File configured = new File(tempFolder.getRoot(), "custom-forms");

		File base = controller.resolveBaseDirectory(configured.getAbsolutePath(), null);

		assertEquals(configured, base);
		assertTrue(base.isDirectory());
	}

	@Test
	public void resolveBaseDirectory_fallsBackToApplicationDataDirectory() {
		String appData = tempFolder.getRoot().getAbsolutePath();

		File base = controller.resolveBaseDirectory(null, appData);

		assertEquals(new File(appData, "htmlformentry"), base);
		assertTrue(base.isDirectory());
	}

	@Test
	public void resolveBaseDirectory_treatsBlankConfiguredValueAsUnset() {
		String appData = tempFolder.getRoot().getAbsolutePath();

		File base = controller.resolveBaseDirectory("   ", appData);

		assertEquals(new File(appData, "htmlformentry"), base);
	}

	@Test
	public void resolveBaseDirectory_keepsExistingDirectory() throws Exception {
		File existing = tempFolder.newFolder("already-there");

		File base = controller.resolveBaseDirectory(existing.getAbsolutePath(), null);

		assertEquals(existing, base);
		assertTrue(base.isDirectory());
	}

	// ---------------------------------------------------------------------
	// getPreviewBaseDirectory — wires the configured/app-data seams together
	// ---------------------------------------------------------------------

	@Test
	public void getPreviewBaseDirectory_usesConfiguredValueWhenPresent() throws Exception {
		TestableController testable = new TestableController();
		File configured = tempFolder.newFolder("configured-base");
		testable.configured = configured.getAbsolutePath();

		assertEquals(configured, testable.getPreviewBaseDirectory());
	}

	// ---------------------------------------------------------------------
	// resolveSafePreviewFile — rejected (unsafe) inputs
	// ---------------------------------------------------------------------

	@Test
	public void resolveSafePreviewFile_rejectsNull() {
		assertNull(newTestable().resolveSafePreviewFile(null));
	}

	@Test
	public void resolveSafePreviewFile_rejectsBlank() {
		assertNull(newTestable().resolveSafePreviewFile("   "));
	}

	@Test
	public void resolveSafePreviewFile_rejectsParentTraversal() {
		assertNull(newTestable().resolveSafePreviewFile("../../openmrs-server.properties"));
	}

	@Test
	public void resolveSafePreviewFile_rejectsAbsolutePath() {
		// Build an OS-independent absolute path (e.g. "C:\secret.xml" or "/secret.xml").
		File absolute = new File(File.listRoots()[0], "secret.xml");
		assertNull(newTestable().resolveSafePreviewFile(absolute.getAbsolutePath()));
	}

	// ---------------------------------------------------------------------
	// resolveSafePreviewFile — accepted input confined to the base directory
	// ---------------------------------------------------------------------

	@Test
	public void resolveSafePreviewFile_returnsFileInsideBaseDirectory() throws Exception {
		TestableController testable = newTestable();

		File resolved = testable.resolveSafePreviewFile("probe.xml");

		assertNotNull(resolved);
		assertEquals(new File(testable.getPreviewBaseDirectory(), "probe.xml").getCanonicalFile(), resolved);
	}

	/** A controller whose base directory is a fresh temp folder (configured via the app-data seam). */
	private TestableController newTestable() {
		TestableController testable = new TestableController();
		testable.appDataDir = tempFolder.getRoot().getAbsolutePath();
		return testable;
	}

}
