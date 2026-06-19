package org.openmrs.module.htmlformentry.web.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * The controller for previewing a HtmlForm by loading the xml file that defines that HtmlForm from
 * disk.
 * <p/>
 * Handles {@code htmlFormFromFile.form} requests. Renders view {@code htmlFormFromFile.jsp}.
 */
@Controller
public class HtmlFormFromFileController {
	
	private static final String TEMP_HTML_FORM_FILE_PREFIX = "html_form_";
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	@RequestMapping("/module/htmlformentry/htmlFormFromFile.form")
	public void handleRequest(Model model, @RequestParam(value = "filePath", required = false) String filePath,
	                          @RequestParam(value = "patientId", required = false) Integer pId,
	                          @RequestParam(value = "isFileUpload", required = false) boolean isFileUpload,
	                          HttpServletRequest request) throws Exception {

        Context.requirePrivilege("Manage Forms");
		
		if (log.isDebugEnabled())
			log.debug("In reference data...");
		
		model.addAttribute("previewHtml", "");
		String message = "";
		File f = null;
		try {
			if (isFileUpload) {
				MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
				MultipartFile multipartFile = multipartRequest.getFile("htmlFormFile");
				if (multipartFile != null) {
					//use the same file for the logged in user
					f = new File(SystemUtils.JAVA_IO_TMPDIR, TEMP_HTML_FORM_FILE_PREFIX
					        + Context.getAuthenticatedUser().getSystemId());
					if (!f.exists())
						f.createNewFile();
					
					filePath = f.getAbsolutePath();
					FileOutputStream fileOut = new FileOutputStream(f);
					IOUtils.copy(multipartFile.getInputStream(), fileOut);
					fileOut.close();
				}
			} else {
				if (StringUtils.hasText(filePath)) {
					// HFE-01: never trust the raw request parameter. Confine the read to a
					// whitelisted base directory; reject absolute paths and ".." traversal.
					f = resolveSafePreviewFile(filePath);
				} else {
					message = "You must specify a file path to preview from file";
				}
			}
			
			if (f != null && f.exists() && f.canRead()) {
				model.addAttribute("filePath", filePath);
				
				StringWriter writer = new StringWriter();
				IOUtils.copy(new FileInputStream(f), writer, "UTF-8");
				String xml = writer.toString();
				
				Patient p = null;
				if (pId != null) {
					p = Context.getPatientService().getPatient(pId);
				} else {
					p = HtmlFormEntryUtil.getFakePerson();
				}
				HtmlForm fakeForm = new HtmlForm();
				fakeForm.setXmlData(xml);
				FormEntrySession fes = new FormEntrySession(p, null, Mode.ENTER, fakeForm, request.getSession());
				String html = fes.getHtmlToDisplay();
				if (fes.getFieldAccessorJavascript() != null) {
                	html += "<script>" + fes.getFieldAccessorJavascript() + "</script>";
                }
				model.addAttribute("previewHtml", html);
				//clear the error message
				message = "";
			} else {
				message = "Please specify a valid file path or select a valid file.";
			}
		}
		catch (Exception e) {
			log.error("An error occurred while loading the html.", e);
			message = "An error occurred while loading the html. " + e.getMessage();
		}
		
		model.addAttribute("message", message);
		model.addAttribute("isFileUpload", isFileUpload);
	}

	/**
	 * Global property that may override the directory under which form files are allowed to be
	 * previewed. When unset, a dedicated {@code htmlformentry} folder inside the OpenMRS application
	 * data directory is used.
	 */
	static final String GP_PREVIEW_BASE_DIR = "htmlformentry.previewBaseDirectory";

	/**
	 * Resolves the base directory that form-preview files must live in. The directory is created
	 * when it does not yet exist so a fresh install has a safe, empty drop-zone instead of falling
	 * back to the (secret-bearing) working/application directory.
	 */
	File getPreviewBaseDirectory() {
		return resolveBaseDirectory(getConfiguredBaseDirectory(), getApplicationDataDirectory());
	}

	/** Seam around the global-property lookup so the base-dir resolution can be unit-tested. */
	String getConfiguredBaseDirectory() {
		return Context.getAdministrationService().getGlobalProperty(GP_PREVIEW_BASE_DIR);
	}

	/** Seam around the application-data-directory lookup so it can be stubbed in tests. */
	String getApplicationDataDirectory() {
		return OpenmrsUtil.getApplicationDataDirectory();
	}

	/**
	 * Pure resolution of the preview base directory, split out from the static OpenMRS lookups so it
	 * can be unit-tested. Uses {@code configured} when set, otherwise an {@code htmlformentry} folder
	 * inside {@code applicationDataDirectory}, and creates the directory when it does not yet exist.
	 */
	File resolveBaseDirectory(String configured, String applicationDataDirectory) {
		File base;
		if (StringUtils.hasText(configured)) {
			base = new File(configured);
		} else {
			base = new File(applicationDataDirectory, "htmlformentry");
		}
		if (!base.exists()) {
			base.mkdirs();
		}
		return base;
	}

	/**
	 * Resolves a user-supplied path against the preview base directory and guarantees the result
	 * stays inside it (HFE-01 mitigation). Absolute paths and parent-directory traversal are
	 * rejected, and the canonicalised result (symlinks resolved) must still be contained by the base
	 * directory. Returns {@code null} for any unsafe or empty path, so the caller falls through to
	 * the same generic "valid file path" message and no file-existence oracle is leaked.
	 */
	File resolveSafePreviewFile(String filePath) {
		if (!StringUtils.hasText(filePath)) {
			return null;
		}
		// Reject obvious traversal / absolute references up front.
		if (filePath.contains("..") || new File(filePath).isAbsolute()) {
			return null;
		}
		try {
			File base = getPreviewBaseDirectory();
			File resolved = new File(base, filePath).getCanonicalFile();
			String basePath = base.getCanonicalPath() + File.separator;
			if (!(resolved.getCanonicalPath() + File.separator).startsWith(basePath)) {
				return null;
			}
			return resolved;
		}
		catch (IOException e) {
			log.warn("Rejected unsafe preview path: " + filePath, e);
			return null;
		}
	}
}
