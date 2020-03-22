/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.translators.impl;

import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.Setter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Concept;
import org.openmrs.ConceptNumeric;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationBasedOnReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationCategoryTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationEffectiveDatetimeTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationInterpretationTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceRangeTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationStatusTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationValueTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ProvenanceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Setter(AccessLevel.PACKAGE)
public class ObservationTranslatorImpl implements ObservationTranslator {
	
	@Autowired
	private ObservationStatusTranslator observationStatusTranslator;
	
	@Autowired
	private ObservationReferenceTranslator observationReferenceTranslator;
	
	@Autowired
	private ObservationValueTranslator observationValueTranslator;
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Autowired
	private ObservationCategoryTranslator categoryTranslator;
	
	@Autowired
	private EncounterReferenceTranslator encounterReferenceTranslator;
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	private ObservationInterpretationTranslator interpretationTranslator;
	
	@Autowired
	private ObservationReferenceRangeTranslator referenceRangeTranslator;
	
	@Autowired
	private ProvenanceTranslator<Obs> provenanceTranslator;
	
	@Autowired
	private ObservationBasedOnReferenceTranslator basedOnReferenceTranslator;
	
	@Autowired
	private ObservationEffectiveDatetimeTranslator datetimeTranslator;
	
	@Override
	public Observation toFhirResource(Obs observation) {
		if (observation == null) {
			return null;
		}
		
		Observation obs = new Observation();
		obs.setId(observation.getUuid());
		obs.setStatus(observationStatusTranslator.toFhirResource(observation));
		
		obs.setEncounter(encounterReferenceTranslator.toFhirResource(observation.getEncounter()));
		
		Person obsPerson = observation.getPerson();
		if (obsPerson != null) {
			try {
				obs.setSubject(patientReferenceTranslator.toFhirResource((Patient) observation.getPerson()));
			}
			catch (ClassCastException ignored) {}
		}
		
		obs.setCode(conceptTranslator.toFhirResource(observation.getConcept()));
		obs.addCategory(categoryTranslator.toFhirResource(observation.getConcept()));
		
		if (observation.isObsGrouping()) {
			for (Obs groupObs : observation.getGroupMembers()) {
				obs.addHasMember(observationReferenceTranslator.toFhirResource(groupObs));
			}
		}
		
		obs.setValue(observationValueTranslator.toFhirResource(observation));
		
		obs.addInterpretation(interpretationTranslator.toFhirResource(observation));
		
		if (observation.getValueNumeric() != null) {
			Concept concept = observation.getConcept();
			if (concept instanceof ConceptNumeric) {
				obs.setReferenceRange(referenceRangeTranslator.toFhirResource((ConceptNumeric) concept));
			}
			
		}
		obs.getMeta().setLastUpdated(observation.getDateChanged());
		obs.addContained(provenanceTranslator.getCreateProvenance(observation));
		obs.addContained(provenanceTranslator.getUpdateProvenance(observation));
		obs.setIssued(observation.getDateCreated());
		obs.setEffective(datetimeTranslator.toFhirResource(observation));
		obs.addBasedOn(basedOnReferenceTranslator.toFhirResource(observation.getOrder()));
		
		return obs;
	}
	
	@Override
	public Obs toOpenmrsType(Observation fhirObservation) {
		return toOpenmrsType(new Obs(), fhirObservation);
	}
	
	@Override
	public Obs toOpenmrsType(Obs existingObs, Observation observation, Supplier<Obs> groupedObsFactory) {
		if (existingObs == null) {
			return null;
		}
		
		if (observation == null) {
			return existingObs;
		}
		
		existingObs.setUuid(observation.getId());
		
		existingObs = setObsFields(existingObs, observation);
		
		return existingObs;
	}
	
	@Override
	public Obs toOpenmrsType(Observation observation) {
		Obs newObs = new Obs();
		
		if (observation == null) {
			return null;
		}
		
		if (observation.hasId()) {
			newObs.setUuid(observation.getId());
		}
		
		return setObsFields(newObs, observation);
	}
	
	private Obs setObsFields(Obs obs, Observation observation) {
		observationStatusTranslator.toOpenmrsType(obs, observation.getStatus());
		
		if (observation.hasEncounter()) {
			obs.setEncounter(encounterReferenceTranslator.toOpenmrsType(observation.getEncounter()));
		}
		
		if (observation.hasSubject()) {
			obs.setPerson(patientReferenceTranslator.toOpenmrsType(observation.getSubject()));
		}
		
		if (observation.hasCode()) {
			obs.setConcept(conceptTranslator.toOpenmrsType(observation.getCode()));
		}
		
		if (observation.hasValue()) {
			obs = observationValueTranslator.toOpenmrsType(obs, observation.getValue());
		}
		
		for (Reference reference : observation.getHasMember()) {
			obs.addGroupMember(observationReferenceTranslator.toOpenmrsType(reference));
		}
		
		if (observation.getInterpretation().size() > 0) {
			interpretationTranslator.toOpenmrsType(obs, observation.getInterpretation().get(0));
		}
		
		datetimeTranslator.toOpenmrsType(obs, observation.getEffectiveDateTimeType());
		
		if (observation.hasBasedOn()) {
			obs.setOrder(basedOnReferenceTranslator.toOpenmrsType(observation.getBasedOn().get(0)));
		}
		
		return obs;
	}
}
