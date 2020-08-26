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

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Task;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.FhirTask;
import org.openmrs.module.fhir2.api.FhirTaskService;
import org.openmrs.module.fhir2.api.dao.FhirTaskDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.TaskTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PACKAGE)
public class FhirTaskServiceImpl extends BaseFhirService<Task, FhirTask> implements FhirTaskService {
	
	@Autowired
	private FhirTaskDao dao;
	
	@Autowired
	private TaskTranslator translator;
	
	@Autowired
	private SearchQuery<FhirTask, Task, FhirTaskDao, TaskTranslator> searchQuery;
	
	/**
	 * Get collection of tasks corresponding to the provided search parameters
	 *
	 * @param basedOnReference A reference list to basedOn resources
	 * @param ownerReference A reference list to owner resources
	 * @param status A list of statuses
	 * @param id The UUID of the requested task
	 * @param lastUpdated A date range corresponding to when the Tasks were last updated
	 * @param sort The sort parameters for the search results
	 * @return the collection of Tasks that match the search parameters
	 */
	@Override
	@Transactional(readOnly = true)
	public IBundleProvider searchForTasks(ReferenceAndListParam basedOnReference, ReferenceAndListParam ownerReference,
	        TokenAndListParam status, TokenAndListParam id, DateRangeParam lastUpdated, SortSpec sort) {
		
		SearchParameterMap theParams = new SearchParameterMap()
		        .addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference)
		        .addParameter(FhirConstants.OWNER_REFERENCE_SEARCH_HANDLER, ownerReference)
		        .addParameter(FhirConstants.STATUS_SEARCH_HANDLER, status)
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY, id)
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.LAST_UPDATED_PROPERTY, lastUpdated)
		        .setSortSpec(sort);
		
		return searchQuery.getQueryResults(theParams, dao, translator);
	}
	
	/**
	 * Save task to the DB
	 *
	 * @param task the task to save
	 * @return the saved task
	 */
	@Override
	public Task saveTask(Task task) {
		return translator.toFhirResource(dao.saveTask(translator.toOpenmrsType(task)));
	}
	
	/**
	 * Save task to the DB, or update task if one exists with given UUID
	 *
	 * @param uuid the uuid of the task to update
	 * @param task the task to save
	 * @return the saved task
	 */
	@Override
	public Task updateTask(String uuid, Task task) {
		uuid = handleIdentifier(task, uuid);
		
		FhirTask openmrsTask = null;
		
		if (uuid != null) {
			openmrsTask = dao.getTaskByUuid(task.getId());
		}
		
		if (openmrsTask == null) {
			throw new MethodNotAllowedException("No Task found to update. Use Post to create new Tasks.");
		}
		
		return translator.toFhirResource(dao.saveTask(translator.toOpenmrsType(openmrsTask, task)));
	}
	
	/**
	 * Get a list of Tasks associated with the given Resource with the given Uuid through the basedOn
	 * relation
	 *
	 * @param uuid the uuid of the associated resource
	 * @param clazz the class of the associated resource
	 * @return the saved task
	 */
	@Override
	public Collection<Task> getTasksByBasedOn(Class<? extends DomainResource> clazz, String uuid) {
		Collection<Task> associatedTasks = new ArrayList<>();
		
		if (clazz == ServiceRequest.class) {
			associatedTasks = dao.getTasksByBasedOnUuid(clazz, uuid).stream().map(translator::toFhirResource)
			        .collect(Collectors.toList());
		}
		
		return associatedTasks;
	}
	
	private String handleIdentifier(Task task, String uuid) {
		String openmrsUuid = task.getId();
		
		if (task.hasIdentifier()) {
			Identifier openmrsIdentifier = task.getIdentifier().stream().filter(i -> i.getSystem().contains("openmrs"))
			        .findFirst().orElse(null);
			if (openmrsIdentifier != null) {
				openmrsUuid = openmrsIdentifier.getValue();
				uuid = openmrsUuid;
			}
			
			task.setId(openmrsUuid);
		}
		
		if (openmrsUuid == null) {
			throw new InvalidRequestException("Task resource is missing id.");
		}
		
		if (openmrsUuid != uuid) {
			throw new InvalidRequestException("Task id and provided uuid do not match");
		}
		
		return openmrsUuid;
	}
}
