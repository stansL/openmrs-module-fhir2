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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Setter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.FhirException;
import org.openmrs.module.fhir2.FhirReference;
import org.openmrs.module.fhir2.FhirTask;
import org.openmrs.module.fhir2.FhirTaskInput;
import org.openmrs.module.fhir2.FhirTaskOutput;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.ProvenanceTranslator;
import org.openmrs.module.fhir2.api.translators.ReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.TaskTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Setter(AccessLevel.PACKAGE)
public class TaskTranslatorImpl implements TaskTranslator {
	
	@Autowired
	private ProvenanceTranslator<FhirTask> provenanceTranslator;
	
	@Autowired
	private ReferenceTranslator referenceTranslator;
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Autowired
	private ConceptService conceptService;
	
	@Override
	public Task toFhirResource(FhirTask openmrsTask) {
		Task fhirTask = null;
		
		if (openmrsTask != null) {
			fhirTask = new Task();
			setFhirTaskFields(openmrsTask, fhirTask);
			fhirTask.addContained(provenanceTranslator.getCreateProvenance(openmrsTask));
			fhirTask.addContained(provenanceTranslator.getUpdateProvenance(openmrsTask));
		}
		
		return fhirTask;
	}
	
	@Override
	public FhirTask toOpenmrsType(Task fhirTask) {
		FhirTask openmrsTask = null;
		
		if (fhirTask != null) {
			openmrsTask = new FhirTask();
			setOpenmrsTaskFields(openmrsTask, fhirTask);
		}
		
		return openmrsTask;
	}
	
	@Override
	public FhirTask toOpenmrsType(FhirTask openmrsTask, Task fhirTask) {
		if (fhirTask != null) {
			if (openmrsTask == null) {
				openmrsTask = new FhirTask();
			}
			setOpenmrsTaskFields(openmrsTask, fhirTask);
		}
		
		return openmrsTask;
	}
	
	private void setFhirTaskFields(FhirTask openmrsTask, Task fhirTask) {
		fhirTask.setId(openmrsTask.getUuid());
		
		if (openmrsTask.getStatus() != null) {
			fhirTask.setStatus(Task.TaskStatus.valueOf(openmrsTask.getStatus().name()));
		}
		if (openmrsTask.getIntent() != null) {
			fhirTask.setIntent(Task.TaskIntent.valueOf(openmrsTask.getIntent().name()));
		}
		
		if (openmrsTask.getBasedOnReferences() != null && !openmrsTask.getBasedOnReferences().isEmpty()) {
			fhirTask.setBasedOn(openmrsTask.getBasedOnReferences().stream().map(referenceTranslator::toFhirResource)
			        .collect(Collectors.toList()));
		}
		
		if (openmrsTask.getEncounterReference() != null) {
			fhirTask.setEncounter(referenceTranslator.toFhirResource(openmrsTask.getEncounterReference()));
		}
		
		if (openmrsTask.getForReference() != null) {
			fhirTask.setFor(referenceTranslator.toFhirResource(openmrsTask.getForReference()));
		}
		
		if (openmrsTask.getOwnerReference() != null) {
			fhirTask.setOwner(referenceTranslator.toFhirResource(openmrsTask.getOwnerReference()));
		}
		
		if (openmrsTask.getInput() != null && !openmrsTask.getInput().isEmpty()) {
			fhirTask.setInput(
			    openmrsTask.getInput().stream().map(this::translateFromInputText).collect(Collectors.toList()));
		}
		
		if (openmrsTask.getOutput() != null && !openmrsTask.getOutput().isEmpty()) {
			fhirTask.setOutput(
			    openmrsTask.getOutput().stream().map(this::translateFromOutputReferences).collect(Collectors.toList()));
		}
		
		fhirTask.setAuthoredOn(openmrsTask.getDateCreated());
		
		fhirTask.setLastModified(openmrsTask.getDateChanged());
		
		fhirTask.setIdentifier(Collections.singletonList(
		    new Identifier().setSystem(FhirConstants.OPENMRS_FHIR_EXT_TASK_IDENTIFIER).setValue(openmrsTask.getUuid())));
		
		fhirTask.getMeta().setLastUpdated(openmrsTask.getDateChanged());
	}
	
	private void setOpenmrsTaskFields(FhirTask openmrsTask, Task fhirTask) {
		if (openmrsTask.getUuid() == null) {
			openmrsTask.setUuid(fhirTask.getId());
		}
		if (fhirTask.hasStatus()) {
			try {
				openmrsTask.setStatus(FhirTask.TaskStatus.valueOf(fhirTask.getStatus().name()));
			}
			catch (IllegalArgumentException ex) {
				openmrsTask.setStatus(FhirTask.TaskStatus.UNKNOWN);
			}
		}
		if (fhirTask.hasIntent()) {
			try {
				openmrsTask.setIntent(FhirTask.TaskIntent.valueOf(fhirTask.getIntent().name()));
			}
			catch (IllegalArgumentException ex) {
				openmrsTask.setIntent(FhirTask.TaskIntent.ORDER);
			}
		}
		
		if (!fhirTask.getBasedOn().isEmpty()) {
			Set<FhirReference> basedOnReferences = fhirTask.getBasedOn().stream().map(referenceTranslator::toOpenmrsType)
			        .collect(Collectors.toSet());
			Set<FhirReference> tryTwoRef = new HashSet<>();
			for (Reference ref : fhirTask.getBasedOn()) {
				tryTwoRef.add(referenceTranslator.toOpenmrsType(ref));
			}
			
			openmrsTask.setBasedOnReferences(tryTwoRef);
			
			if (openmrsTask.getBasedOnReferences().size() != fhirTask.getBasedOn().size()) {
				throw new FhirException("Could not save all references for task " + openmrsTask.getUuid());
			}
		}
		
		if (!fhirTask.getEncounter().isEmpty()) {
			openmrsTask.setEncounterReference(referenceTranslator.toOpenmrsType(fhirTask.getEncounter()));
		}
		
		if (!fhirTask.getFor().isEmpty()) {
			openmrsTask.setForReference(referenceTranslator.toOpenmrsType(fhirTask.getFor()));
		}
		
		if (!fhirTask.getOwner().isEmpty()) {
			openmrsTask.setOwnerReference(referenceTranslator.toOpenmrsType(fhirTask.getOwner()));
		}
		
		if (!fhirTask.getInput().isEmpty()) {
			openmrsTask.setInput(fhirTask.getInput().stream().map(this::translateToInputText).collect(Collectors.toSet()));
		}
		
		if (!fhirTask.getOutput().isEmpty()) {
			openmrsTask.setOutput(
			    fhirTask.getOutput().stream().map(this::translateToOutputReference).collect(Collectors.toSet()));
		}
		
		fhirTask.setIdentifier(Collections.singletonList(
		    new Identifier().setSystem(FhirConstants.OPENMRS_URI + "/identifier").setValue(openmrsTask.getUuid())));
		
		openmrsTask.setName(FhirConstants.TASK + "/" + fhirTask.getId());
		
	}
	
	private FhirTaskOutput translateToOutputReference(Task.TaskOutputComponent taskOutputComponent) {
		FhirReference outputReference = referenceTranslator.toOpenmrsType((Reference) taskOutputComponent.getValue());
		Concept type = null;
		FhirTaskOutput output = new FhirTaskOutput();
		
		if (taskOutputComponent.hasType()) {
			type = conceptTranslator.toOpenmrsType(taskOutputComponent.getType());
			output.setType(type);
		}
		
		output.setValueReference(outputReference);
		
		output.setName("TaskOutputComponent/" + output.getUuid());
		
		return output;
	}
	
	private FhirTaskInput translateToInputText(Task.ParameterComponent parameterComponent) {
		Concept type = conceptTranslator.toOpenmrsType(parameterComponent.getType());
		FhirTaskInput input = new FhirTaskInput();
		
		input.setType(type);
		input.setValueText(parameterComponent.getValue().toString());
		input.setName("ParameterComponent/" + input.getUuid());
		
		return input;
	}
	
	private Task.TaskOutputComponent translateFromOutputReferences(FhirTaskOutput openmrsOutput) {
		Reference ref = referenceTranslator.toFhirResource(openmrsOutput.getValueReference());
		CodeableConcept type = conceptTranslator.toFhirResource(openmrsOutput.getType());
		
		return new Task.TaskOutputComponent().setType(type).setValue(ref);
	}
	
	private Task.ParameterComponent translateFromInputText(FhirTaskInput openmrsInput) {
		CodeableConcept type = conceptTranslator.toFhirResource(openmrsInput.getType());
		
		return new Task.ParameterComponent().setType(type).setValue(new StringType().setValue(openmrsInput.getValueText()));
	}
}
