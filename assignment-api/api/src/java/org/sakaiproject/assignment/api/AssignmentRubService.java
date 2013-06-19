package org.sakaiproject.assignment.api;

//DN 2012-11-13: create assignment rubric service
//Purpose: re-use function from gradebook
public interface AssignmentRubService {
	
	public Long getGradableObjectId(String name, String gradebookUid);
	
	public boolean isShowiRubricLink();
	public Long getGradableObjectIdByExternalId(String name , String gradebookUid);
}