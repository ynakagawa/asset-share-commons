package com.adobe.aem.commons.assetshare.components.actions.downloads;

import com.adobe.cq.dam.download.api.DownloadException;
import com.adobe.cq.dam.download.api.DownloadProgress;
import org.osgi.annotation.versioning.ProviderType;

import java.util.List;

@ProviderType
public interface Downloads {

	/**
	 * This model is adapted from a SlingHttpServletRequest object that must have a RequestParameters to indicate the requested DownloadProgresses to return.
	 *
	 * - downloadId: 1 or more request parameters of this name whose values are the allowed downloadIds to include in the result.
	 *
	 * @return a list of the Download Progresses the user has request and also has access to.
	 * @throws DownloadException
	 */
	List<DownloadProgress> getDownloadProgresses() throws DownloadException;
}