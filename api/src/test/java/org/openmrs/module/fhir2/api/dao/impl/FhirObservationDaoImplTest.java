/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Collection;
import java.util.Date;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir2.TestFhirSpringConfiguration;
import org.openmrs.module.fhir2.api.dao.FhirObservationDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = TestFhirSpringConfiguration.class, inheritLocations = false)
public class FhirObservationDaoImplTest extends BaseModuleContextSensitiveTest {
	
	private static final String OBS_DATA_XML = "org/openmrs/module/fhir2/api/dao/impl/FhirObservationDaoImplTest_initial_data_suppl.xml";
	
	private static final String OBS_UUID = "39fb7f47-e80a-4056-9285-bd798be13c63";
	
	private static final String NEW_UUID = "655b64a2-1513-4f07-9d1c-0da7fa80840a";

	private static final String BAD_OBS_UUID = "121b73a6-e1a4-4424-8610-d5765bf2fdf7";

	private static final String OBS_CONCEPT_UUID = "c607c80f-1ea9-4da3-bb88-6276ce8868dd";
	
	private static final String OBS_CONCEPT_ID = "5089";
	
	@Autowired
	private FhirObservationDao dao;
	
	@Autowired
	@Qualifier("patientService")
	private PatientService patientService;

	@Autowired
	@Qualifier("conceptService")
	private ConceptService conceptService;

	@Autowired
	@Qualifier("obsService")
	private ObsService obsService;

	@Before
	public void setup() throws Exception {
		executeDataSet(OBS_DATA_XML);
	}
	
	@Test
	public void get_shouldGetObsByUuid() {
		Obs result = dao.get(OBS_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), equalTo(OBS_UUID));
	}
	
	@Test
	public void get_shouldReturnNullIfObsNotFoundByUuid() {
		Obs result = dao.get(BAD_OBS_UUID);
		
		assertThat(result, nullValue());
	}
	
	@Test
	public void saveObs_shouldSaveNewObs() {
		Obs newObs = new Obs();

		newObs.setUuid(NEW_UUID);
		newObs.setObsDatetime(new Date());
		newObs.setPerson(patientService.getPatient(7));
		newObs.setConcept(conceptService.getConcept(5085));

		Obs result = dao.saveObs(newObs);

		assertThat(result, notNullValue());
		assertThat(result.getUuid(), equalTo(NEW_UUID));
	}

	@Test
	public void saveObsGroup_shouldUpdateExistingObsGroup() {
		Obs existingObs = dao.getObsByUuid(OBS_UUID);
		Date newDate = new Date();
		existingObs.setObsDatetime(newDate);

		Obs result = dao.saveObs(existingObs);

		assertThat(result, notNullValue());
		assertThat(result.getUuid(), equalTo(OBS_UUID));
		assertThat(result.getObsDatetime(), equalTo(newDate));
	}

	@Test
	public void search_shouldReturnSearchQuery() {
		TokenAndListParam code = new TokenAndListParam();
		TokenParam codingToken = new TokenParam();
		codingToken.setValue(OBS_CONCEPT_ID);
		code.addAnd(codingToken);
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.CODED_SEARCH_HANDLER, code);
		Collection<Obs> obs = dao.search(theParams);
		
		assertThat(obs, notNullValue());
	}
	
}
