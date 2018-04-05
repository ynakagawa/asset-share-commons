/*
 * Asset Share Commons
 *
 * Copyright [2017]  Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*global jQuery: false, AssetShare: false, console: false*/

jQuery((function(ns, semanticModal, licenseModal) {
    "use strict";
    AssetShare.SemanticUI.Modals.DownloadModal = (function () {
        var DOWNLOAD_URL = ns.Data.val("download-url"),
            DOWNLOAD_MODAL_ID = "dm-download-modal",
            DOWNLOAD_BUTTON_ID = "download-asset",
            DM_IMAGE_PRESETS = "dm-image-presets";

        function getId() {
            return DOWNLOAD_MODAL_ID;
        }

        function getUrl() {
            return DOWNLOAD_URL;
        }

        function getModal(formDataOrAssetPath, licensed) {
            var formData = formDataOrAssetPath,
                downloadModal;

            if (typeof formDataOrAssetPath === 'string') {
                formData = new ns.FormData();
                formData.add("path", formDataOrAssetPath);
            }

            downloadModal = {
                id: DOWNLOAD_MODAL_ID,
                url: DOWNLOAD_URL,
                data: formData.serialize(),
                options: {}
            };

            if (licensed) {
                downloadModal.options.show = function (modal) {
                    modal.modal("attach events", ns.Elements.selector([licenseModal.id(), licenseModal.acceptId()]));
                };
            } else {
                downloadModal.options.show = function (modal) {
                    modal.modal('show');
                };
            }

            return downloadModal;
        }

        function download(e) {
            var path = ns.Data.attr(this, "asset"),
                license = ns.Data.attr(this, "license"),
                downloadModal = getModal(path, license);

            e.preventDefault();
            e.stopPropagation();

            if (license && licenseModal.modal(path)) {
                semanticModal.show([licenseModal.modal(path), downloadModal]);
            } else {
                semanticModal.show([downloadModal]);
            }
        }

        function setImagePresetProperty(e) {
            var selectedPreset = $(this).dropdown('get value');
            // set the hidden input value with the imagepreset selected - s7exportsettings={imagepreset:presetname}
            $(e.target).val(encodeURIComponent("{imagepreset:" + selectedPreset + "}"));
        }

        /** REGISTER EVENTS WHEN DOCUMENT IS READY **/
        $((function registerEvents() {
            $("body").on("click", ns.Elements.selector([DOWNLOAD_BUTTON_ID]), download);
            $("body").on("change", ns.Elements.selector([DM_IMAGE_PRESETS]), setImagePresetProperty);
        }()));

        return {
            id: getId,
            url: getUrl,
            modal: getModal,
            download: download
        };
    }());
}(AssetShare,
    AssetShare.SemanticUI.Modal,
    AssetShare.SemanticUI.Modals.LicenseModal)));