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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.TestOrder;
import org.openmrs.module.fhir2.FhirTestConstants;
import org.openmrs.module.fhir2.api.dao.FhirServiceRequestDao;
import org.openmrs.module.fhir2.api.translators.ServiceRequestTranslator;

@RunWith(MockitoJUnitRunner.class)
public class FhirServiceRequestServiceImplTest {
	
	private static final String SERVICE_REQUEST_UUID = "249b9094-b812-4b0c-a204-0052a05c657f";
	
	private static final ServiceRequest.ServiceRequestStatus STATUS = ServiceRequest.ServiceRequestStatus.ACTIVE;
	
	private static final ServiceRequest.ServiceRequestPriority PRIORITY = ServiceRequest.ServiceRequestPriority.ROUTINE;
	
	private static final String LOINC_CODE = "1000-1";
	
	@Mock
	private ServiceRequestTranslator<TestOrder> translator;
	
	@Mock
	private FhirServiceRequestDao<TestOrder> dao;
	
	private FhirServiceRequestServiceImpl serviceRequestService;
	
	private ServiceRequest fhirServiceRequest;
	
	private TestOrder order;
	
	@Before
	public void setUp() {
		serviceRequestService = new FhirServiceRequestServiceImpl();
		serviceRequestService.setDao(dao);
		serviceRequestService.setTranslator(translator);
		
		order = new TestOrder();
		order.setUuid(SERVICE_REQUEST_UUID);
		
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding loincCoding = codeableConcept.addCoding();
		loincCoding.setSystem(FhirTestConstants.LOINC_SYSTEM_URL);
		loincCoding.setCode(LOINC_CODE);
		
		fhirServiceRequest = new ServiceRequest();
		fhirServiceRequest.setId(SERVICE_REQUEST_UUID);
		fhirServiceRequest.setStatus(STATUS);
		fhirServiceRequest.setPriority(PRIORITY);
	}
	
	@Test
	public void shouldRetrieveServiceRequestByUUID() {
		when(dao.get(SERVICE_REQUEST_UUID)).thenReturn(order);
		when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
		
		ServiceRequest result = serviceRequestService.get(SERVICE_REQUEST_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(SERVICE_REQUEST_UUID));
	}
	
	@Test
	public void searchForAllServiceRequests_shouldReturnAllServiceRequests() {
		Collection<TestOrder> testOrders = new ArrayList<>();
		TestOrder order = new TestOrder();
		order.setUuid(SERVICE_REQUEST_UUID);
		testOrders.add(order);
		
		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setId(SERVICE_REQUEST_UUID);
		
		when(dao.searchForTestOrders()).thenReturn(testOrders);
		when(translator.toFhirResource(order)).thenReturn(serviceRequest);
		
		Collection<ServiceRequest> results = serviceRequestService.searchForServiceRequests();
		
		assertThat(results, notNullValue());
		assertThat(results, not(empty()));
		assertThat(results, hasItem(hasProperty("id", equalTo(SERVICE_REQUEST_UUID))));
	}
}
