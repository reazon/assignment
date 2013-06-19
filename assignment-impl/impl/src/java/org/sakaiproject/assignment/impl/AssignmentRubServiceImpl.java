
package org.sakaiproject.assignment.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.sakaiproject.tool.gradebook.iRubric.GradableObjectRubric;
import com.reazon.tool.irubric.IRubricManager;
import org.sakaiproject.assignment.api.AssignmentRubService;

//DN 2012-11-13: create assignment rubric service
//Purpose: re-use function from gradebook
public class AssignmentRubServiceImpl extends HibernateDaoSupport implements AssignmentRubService {
	
	private final static Log Log = LogFactory.getLog(AssignmentRubServiceImpl.class);
	
	/**
	 * Init
	 */
	public void init()
	{
		Log.info("init()");
	}
	   
	/**
	* Destroy
	*/
	public void destroy()
	{
		Log.info("destroy()");
	}
	
	//get funtion from gradebook
	private IRubricManager rubricManager;
	public void setRubricManager(IRubricManager rubricManager) {
		this.rubricManager = rubricManager;
	}
	
	public IRubricManager getRubricManager() {
		return rubricManager;
	}
	
	//DN 2012-11-13: get gradableobject id by name and gradebookUId
	public Long getGradableObjectId(final String name, final String gradebookUid) {
		
		Long gradeObjectId = rubricManager.getGradableObjectId(name, gradebookUid);
		return gradeObjectId;

	}

	//DN 2012-11-13: get gradableobject id by externalid and gradebookUId
	public Long getGradableObjectIdByExternalId(final String name, final String gradebookUid) {

		Long gradeObjectId = rubricManager.getGradableObjectIdByExternalId(name, gradebookUid);
		return gradeObjectId;
		
	}	

	//DN 2013-04-17: show irubric link (rubric enable) 
	public boolean isShowiRubricLink() {
	    return rubricManager.isShowiRubricLink();
	}	
}
