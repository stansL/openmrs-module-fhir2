/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.impl;

import java.util.List;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Practitioner;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirPractitionerService;
import org.openmrs.module.fhir2.api.FhirUserService;
import org.openmrs.module.fhir2.api.dao.FhirPractitionerDao;
import org.openmrs.module.fhir2.api.dao.FhirUserDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.PractitionerTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Setter(AccessLevel.PACKAGE)
@Getter(AccessLevel.PROTECTED)
public class FhirPractitionerServiceImpl extends BaseFhirService<Practitioner, Provider> implements FhirPractitionerService {
	
	@Autowired
	private FhirPractitionerDao dao;
	
	@Autowired
	private PractitionerTranslator<Provider> translator;
	
	@Autowired
	private SearchQuery<Provider, Practitioner, FhirPractitionerDao, PractitionerTranslator<Provider>> searchQuery;
	
	@Autowired
	private SearchQuery<User, Practitioner, FhirUserDao, PractitionerTranslator<User>> userSearchQuery;
	
	@Autowired
	private FhirUserService userService;
	
	@Override
	public Practitioner get(String uuid) {
		if (uuid == null) {
			throw new InvalidRequestException("Uuid cannot be null.");
		}
		
		Practitioner result;
		try {
			result = super.get(uuid);
		}
		catch (ResourceNotFoundException e) {
			result = userService.get(uuid);
		}
		
		return result;
	}
	
	@Override
	public IBundleProvider searchForPractitioners(TokenAndListParam identifier, StringAndListParam name,
	        StringAndListParam given, StringAndListParam family, StringAndListParam city, StringAndListParam state,
	        StringAndListParam postalCode, StringAndListParam country, TokenAndListParam id, DateRangeParam lastUpdated) {
		SearchParameterMap theParams = new SearchParameterMap()
		        .addParameter(FhirConstants.IDENTIFIER_SEARCH_HANDLER, identifier)
		        .addParameter(FhirConstants.NAME_SEARCH_HANDLER, FhirConstants.NAME_PROPERTY, name)
		        .addParameter(FhirConstants.NAME_SEARCH_HANDLER, FhirConstants.GIVEN_PROPERTY, given)
		        .addParameter(FhirConstants.NAME_SEARCH_HANDLER, FhirConstants.FAMILY_PROPERTY, family)
		        .addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER, FhirConstants.CITY_PROPERTY, city)
		        .addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER, FhirConstants.STATE_PROPERTY, state)
		        .addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER, FhirConstants.POSTAL_CODE_PROPERTY, postalCode)
		        .addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER, FhirConstants.COUNTRY_PROPERTY, country)
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY, id)
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.LAST_UPDATED_PROPERTY, lastUpdated);
		
		IBundleProvider providerBundle = searchQuery.getQueryResults(theParams, dao, translator);
		IBundleProvider userBundle = userService.searchForUsers(theParams);
		
		if (!providerBundle.isEmpty() && !userBundle.isEmpty()) {
			final Integer providerBundleSize = providerBundle.size();
			List<IBaseResource> theResource = providerBundle.getResources(0,
			    providerBundleSize == null ? Integer.MAX_VALUE : providerBundleSize);
			
			final Integer userBundleSize = userBundle.size();
			theResource.addAll(userBundle.getResources(0, userBundleSize == null ? Integer.MAX_VALUE : userBundleSize));
			
			return new SimpleBundleProvider(theResource);
		} else if (providerBundle.isEmpty() && !userBundle.isEmpty()) {
			return userBundle;
		}
		
		return providerBundle;
	}
}
