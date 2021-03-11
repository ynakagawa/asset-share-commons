/*
 * Asset Share Commons
 *
 * Copyright (C) 2019 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.adobe.aem.commons.assetshare.content.async.download.impl;

import com.adobe.aem.commons.assetshare.content.AssetModel;
import com.adobe.aem.commons.assetshare.content.async.download.AsyncDownload;
import com.adobe.aem.commons.assetshare.util.DownloadHelper;
import com.adobe.aem.commons.assetshare.util.ServletHelper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONObject;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.methods=POST",
                "sling.servlet.resourceTypes=asset-share-commons/actions/download",
                "sling.servlet.resourceTypes=asset-share-commons/actions/share",
                "sling.servlet.selectors=async-download-renditions",
                "sling.servlet.extensions=zip"
        }
)
public class AsyncDownloadServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(AsyncDownloadServlet.class);

    private static final String DOWNLOAD_COUNT = "count";
    private static final String DOWNLOAD_ID = "downlaodID";

    @Reference
    private ServletHelper servletHelper;
    
    @Reference
    private DownloadHelper downloadHelper;
    
    @Reference
    private AsyncDownload asyncDownload;


    @Override
    protected final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        servletHelper.addSlingBindings(request, response);

        final List<AssetModel> assets = downloadHelper.getAssets(request);
        PrintWriter printWriter = response.getWriter();


        try {
        	response.setCharacterEncoding("UTF-8");
        	response.setContentType("application/json;charset=UTF-8");
        	String downlaodID = asyncDownload.createDownload(request.getResourceResolver(),assets);
        	
        	JSONObject responseObject = new JSONObject();
        	responseObject.put(DOWNLOAD_COUNT, assets.size());
        	responseObject.put(DOWNLOAD_ID, downlaodID);

        	printWriter.write(responseObject.toString());
        } catch (Exception e) {
        	log.error("Error While processing Async download ",e);
            throw new ServletException(e);
        }
    }
    
}