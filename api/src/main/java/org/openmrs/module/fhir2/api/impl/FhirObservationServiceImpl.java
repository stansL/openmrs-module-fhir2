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

import java.util.Collection;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.AccessLevel;
import lombok.Setter;
import org.hl7.fhir.r4.model.Observation;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.fhir2.api.dao.FhirObservationDao;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Setter(AccessLevel.PACKAGE)
public class FhirObservationServiceImpl implements FhirObservationService {
	
	@Autowired
	FhirObservationDao dao;
	
	@Autowired
	ObservationTranslator observationTranslator;
	
	@Override
	@Transactional(readOnly = true)
	public Observation getObservationByUuid(String uuid) {
		return observationTranslator.toFhirResource(dao.getObsByUuid(uuid));
	}
	
	@Override
	@Transactional(readOnly = true)
	public Collection<Observation> searchForObservations(ReferenceAndListParam encounterReference,
	        ReferenceAndListParam patientReference, ReferenceParam hasMemberReference, TokenAndListParam valueConcept,
	        DateRangeParam valueDateParam, QuantityAndListParam valueQuantityParam, StringAndListParam valueStringParam,
	        DateRangeParam date, TokenAndListParam code, SortSpec sort) {
		return dao
		        .searchForObservations(encounterReference, patientReference, hasMemberReference, valueConcept,
		            valueDateParam, valueQuantityParam, valueStringParam, date, code, sort)
		        .stream().map(observationTranslator::toFhirResource).collect(Collectors.toList());
	}
	
	@Override
	public Observation saveObservation(Observation observation) {
		return observationTranslator.toFhirResource(dao.saveObs(observationTranslator.toOpenmrsType(observation)));
	}
	
	//	@Override
	//	public Observation updateObservation(String uuid, Observation observation) {
	//		if (observation.getId() == null) {
	//			throw new InvalidRequestException("Observation resource is missing id.");
	//		}
	//
	//		Obs obsGroup = new Obs();
	//
	//		if (uuid != null) {
	//			obsGroup = dao.getObsGroupByUuid(uuid);
	//		}
	//
	//		if (obsGroup == null) {
	//			throw new MethodNotAllowedException("No Diagnostic Report found to update.");
	//		}
	//
	//		return translator.toFhirResource(dao.saveObsGroup(translator.toOpenmrsType(obsGroup, diagnosticReport)));
	//		return null;
	//	}
	
}
