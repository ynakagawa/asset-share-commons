/*
 * Asset Share Commons
 *
 * Copyright (C) 2017 Adobe
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

package com.adobe.aem.commons.assetshare.components.predicates.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Required;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.aem.commons.assetshare.components.predicates.AbstractPredicate;
import com.adobe.aem.commons.assetshare.components.predicates.PropertyPredicate;
import com.adobe.aem.commons.assetshare.components.predicates.impl.options.JsonOptionItem;
import com.adobe.aem.commons.assetshare.components.predicates.impl.options.SelectedOptionItem;
import com.adobe.aem.commons.assetshare.components.predicates.impl.options.UnselectedOptionItem;
import com.adobe.aem.commons.assetshare.search.impl.predicateevaluators.PropertyValuesPredicateEvaluator;
import com.adobe.aem.commons.assetshare.util.PredicateUtil;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.form.OptionItem;
import com.adobe.cq.wcm.core.components.models.form.Options;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

@Model(
		adaptables = {SlingHttpServletRequest.class},
		adapters = {PropertyPredicate.class, ComponentExporter.class},
		resourceType = {PropertyPredicateImpl.RESOURCE_TYPE},
		defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
		)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class PropertyPredicateImpl extends AbstractPredicate implements PropertyPredicate, Options {

	protected static final String RESOURCE_TYPE = "asset-share-commons/components/search/property";
	protected static final String PN_TYPE = "type";
	protected String valueFromRequest = null;
	protected ValueMap valuesFromRequest = null;
	protected static final String DAM_RESOURCE_TYPE = "dam:Asset";
	@Self
	@Required
	private SlingHttpServletRequest request;

	@Self
	@Required
	private Options coreOptions;

	@ValueMapValue
	private String label;

	@ValueMapValue
	private String property;

	@ValueMapValue
	private String operation;

	@ValueMapValue
	private Boolean expanded;

	@ValueMapValue(name = PropertyPredicateImpl.PN_TYPE)
	private String typeString;

	@ValueMapValue
	@Named("and")
	@Default(booleanValues = false)
	private boolean and;

	@ValueMapValue
	private String source;

	@ValueMapValue
	private String jsonSource;

	@PostConstruct
	protected void init() {
		initPredicate(request, coreOptions);
	}

	/* Options - Core Component Delegates */

	public List<OptionItem> getItems() {
		final ValueMap initialValues = getInitialValues();
		final List<OptionItem> processedOptionItems = new ArrayList<>();
		final boolean useDefaultSelected = !isParameterizedSearchRequest();

		if (source.equals("json")){
			List<JsonOption> jsonOptionItems = getJsonItems();
			for (JsonOption jsonOption : jsonOptionItems) {
				processedOptionItems.add(new JsonOptionItem(jsonOption.text, jsonOption.value, PredicateUtil.isOptionInInitialValues(jsonOption.value, initialValues)));
			}
		} else{
			coreOptions.getItems().stream()
			.forEach(optionItem -> {
				if (PredicateUtil.isOptionInInitialValues(optionItem, initialValues)) {
					processedOptionItems.add(new SelectedOptionItem(optionItem));
				} else if (useDefaultSelected) {
					processedOptionItems.add(optionItem);
				} else {
					processedOptionItems.add(new UnselectedOptionItem(optionItem));
				}
			});
		}
		return processedOptionItems;
	}

	public Type getType() {
		return coreOptions.getType();
	}

	/* Property Predicate Specific */

	public String getSubType() {
		//support variation of Checkboxes
		return typeString;
	}

	@Override
	public String getProperty() {
		return property;
	}

	@Override
	public String getValuesKey() {
		return PropertyValuesPredicateEvaluator.VALUES;
	}

	@Override
	public boolean hasOperation() {
		return StringUtils.isNotBlank(getOperation());
	}

	@Override
	public String getOperation() {
		return operation;
	}

	public boolean hasAnd() {
		return and;
	}

	public Boolean getAnd() {
		return and;
	}

	@Override
	public String getName() {
		return PropertyValuesPredicateEvaluator.PREDICATE_NAME;
	}

	@Override
	public boolean isReady() {
		return coreOptions.getItems().size() > 0;
	}

	@Override
	public String getInitialValue() {
		if (valueFromRequest == null) {
			valueFromRequest = PredicateUtil.getInitialValue(request, this, PropertyValuesPredicateEvaluator.VALUES);
		}

		return valueFromRequest;
	}

	@Override
	public ValueMap getInitialValues() {
		if (valuesFromRequest == null) {
			valuesFromRequest = PredicateUtil.getInitialValues(request, this, PropertyValuesPredicateEvaluator.VALUES);
		}

		return valuesFromRequest;
	}

	@Nonnull
	@Override
	public String getExportedType() {
		return RESOURCE_TYPE;
	}

	private List<JsonOption> getJsonItems(){
		List<JsonOption> options = new ArrayList<>();
		// Get option from json type asset and inject via jsonSource filepath
		@Nullable
		Resource resource = request.getResourceResolver().getResource(jsonSource);
		String resourceType = resource.getResourceType();
		if(resourceType != null && resourceType.equalsIgnoreCase(DAM_RESOURCE_TYPE)) {
			Asset jsonAsset = DamUtil.resolveToAsset(request.getResourceResolver().getResource(jsonSource));

			try(InputStream stream = jsonAsset.getOriginal().adaptTo(InputStream.class);
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(stream, StandardCharsets.UTF_8))) {

				Gson gson = new Gson();
				JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
				if (jsonObject.isJsonObject()){
					JsonElement optionsJson = jsonObject.get("options");
					options = gson.fromJson(optionsJson, new TypeToken<List<JsonOption>>() {}.getType());
					return options;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			// End of reading options from Json asset
		}else {
			// Start of reading options from generic list  /etc/acs-commons/lists/audience/jcr:content/list
			Iterable<Resource> dropdownListItem = resource.getChildren();
			for (Resource optionResource : dropdownListItem) {
				ValueMap valueMap = optionResource.getValueMap();
				options.add(new JsonOption(valueMap.get("jcr:title",String.class), valueMap.get("value",String.class)));
			}
			return options;
		}
		return null;
	}

	protected class JsonOption {
		private String text;
		private String value;

		public JsonOption(String text, String value) {
			this.text = text;
			this.value = value;
		}


		public String getText() {
			return text;
		}


		public String getValue() {
			return value;
		}

	}
}