/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assignment.impl;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.sakaiproject.contentreview.exception.QueueException;
import org.sakaiproject.contentreview.service.ContentReviewService;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentContent;
import org.sakaiproject.assignment.api.AssignmentContentEdit;
import org.sakaiproject.assignment.api.AssignmentContentNotEmptyException;
import org.sakaiproject.assignment.api.AssignmentEdit;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.assignment.api.AssignmentSubmissionEdit;
import org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer;
import org.sakaiproject.taggable.api.TaggingManager;
import org.sakaiproject.taggable.api.TaggingProvider;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzPermissionException;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.cover.AuthzGroupService;
import org.sakaiproject.authz.cover.FunctionManager;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.content.api.ResourceType;
import org.sakaiproject.content.api.GroupAwareEntity.AccessMode;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.email.cover.EmailService;
import org.sakaiproject.email.cover.DigestService;
import org.sakaiproject.entity.api.AttachmentContainer;
import org.sakaiproject.entity.api.Edit;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityAccessOverloadException;
import org.sakaiproject.entity.api.EntityCopyrightException;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityNotDefinedException;
import org.sakaiproject.entity.api.EntityPermissionException;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.EntityTransferrer;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.cover.EventTrackingService;
import org.sakaiproject.event.cover.NotificationService;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.id.cover.IdManager;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.CacheRefresher;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.api.SessionBindingEvent;
import org.sakaiproject.tool.api.SessionBindingListener;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.BaseResourcePropertiesEdit;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.util.Blob;
import org.sakaiproject.util.DefaultEntityHandler;
import org.sakaiproject.util.SAXEntityReader;
import org.sakaiproject.util.EmptyIterator;
import org.sakaiproject.util.EntityCollections;
import org.sakaiproject.util.FormattedText;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.SortedIterator;
import org.sakaiproject.util.StorageUser;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Validator;
import org.sakaiproject.util.Web;
import org.sakaiproject.util.Xml;
import org.sakaiproject.util.commonscodec.CommonsCodecBase64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;



/**
 * <p>
 * BaseAssignmentService is the abstract service class for Assignments.
 * </p>
 * <p>
 * The Concrete Service classes extending this are the XmlFile and DbCached storage classes.
 * </p>
 */
public abstract class BaseAssignmentService implements AssignmentService, EntityTransferrer
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(BaseAssignmentService.class);

	/** the resource bundle */
	private static ResourceLoader rb = new ResourceLoader("assignment");

	/** A Storage object for persistent storage of Assignments. */
	protected AssignmentStorage m_assignmentStorage = null;

	/** A Storage object for persistent storage of Assignments. */
	protected AssignmentSubmissionStorage m_submissionStorage = null;

	/** A Cache for this service - Assignments keyed by reference. */
	protected Cache m_assignmentCache = null;

	/** A Cache for this service - AssignmentSubmissions keyed by reference. */
	protected Cache m_submissionCache = null;

	/** The access point URL. */
	protected String m_relativeAccessPoint = null;

	/**********************************************************************************************************************************************************************************************************************************************************
	 * EVENT STRINGS
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** Event for adding an assignment. */
	public static final String EVENT_ADD_ASSIGNMENT = "asn.new.assignment";

	/** Event for adding an assignment. */
	public static final String EVENT_ADD_ASSIGNMENT_CONTENT = "asn.new.assignmentcontent";

	/** Event for adding an assignment submission. */
	public static final String EVENT_ADD_ASSIGNMENT_SUBMISSION = "asn.new.submission";

	/** Event for removing an assignment. */
	public static final String EVENT_REMOVE_ASSIGNMENT = "asn.delete.assignment";

	/** Event for removing an assignment content. */
	public static final String EVENT_REMOVE_ASSIGNMENT_CONTENT = "asn.delete.assignmentcontent";

	/** Event for removing an assignment submission. */
	public static final String EVENT_REMOVE_ASSIGNMENT_SUBMISSION = "asn.delete.submission";

	/** Event for accessing an assignment. */
	public static final String EVENT_ACCESS_ASSIGNMENT = "asn.read.assignment";

	/** Event for accessing an assignment content. */
	public static final String EVENT_ACCESS_ASSIGNMENT_CONTENT = "asn.read.assignmentcontent";

	/** Event for accessing an assignment submission. */
	public static final String EVENT_ACCESS_ASSIGNMENT_SUBMISSION = "asn.read.submission";

	/** Event for updating an assignment. */
	public static final String EVENT_UPDATE_ASSIGNMENT = "asn.revise.assignment";

	/** Event for updating an assignment content. */
	public static final String EVENT_UPDATE_ASSIGNMENT_CONTENT = "asn.revise.assignmentcontent";

	/** Event for updating an assignment submission. */
	public static final String EVENT_UPDATE_ASSIGNMENT_SUBMISSION = "asn.revise.submission";

	/** Event for saving an assignment submission. */
	public static final String EVENT_SAVE_ASSIGNMENT_SUBMISSION = "asn.save.submission";

	/** Event for submitting an assignment submission. */
	public static final String EVENT_SUBMIT_ASSIGNMENT_SUBMISSION = "asn.submit.submission";
	
	/** Event for grading an assignment submission. */
	public static final String EVENT_GRADE_ASSIGNMENT_SUBMISSION = "asn.grade.submission";
	
	private static final String NEW_ASSIGNMENT_DUE_DATE_SCHEDULED = "new_assignment_due_date_scheduled";

	// the file types for zip download
	protected static final String ZIP_COMMENT_FILE_TYPE = ".txt";
	protected static final String ZIP_SUBMITTED_TEXT_FILE_TYPE = ".html";

//	spring service injection
	
	
	protected ContentReviewService contentReviewService;
	public void setContentReviewService(ContentReviewService contentReviewService) {
		this.contentReviewService = contentReviewService;
	}
	
	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @return the EntityManager collaborator.
	 */
	protected EntityManager entityManager()
	{
		return m_entityManager;
	}
	

	/**
	 * @return the ContentHostingService collaborator.
	 */
	protected ContentHostingService contentHostingService()
	{
		return m_contentHostingService;
	}
	
	/**********************************************************************************************************************************************************************************************************************************************************
	 * Abstractions, etc.
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Construct a Storage object for Assignments.
	 * 
	 * @return The new storage object.
	 */
	protected abstract AssignmentStorage newAssignmentStorage();

	/**
	 * Construct a Storage object for AssignmentSubmissions.
	 * 
	 * @return The new storage object.
	 */
	protected abstract AssignmentSubmissionStorage newSubmissionStorage();

	/**
	 * Access the partial URL that forms the root of resource URLs.
	 * 
	 * @param relative -
	 *        if true, form within the access path only (i.e. starting with /msg)
	 * @return the partial URL that forms the root of resource URLs.
	 */
	protected String getAccessPoint(boolean relative)
	{
		return (relative ? "" : m_serverConfigurationService.getAccessUrl()) + m_relativeAccessPoint;

	} // getAccessPoint

	/**
	 * Access the internal reference which can be used to assess security clearance.
	 * 
	 * @param id
	 *        The assignment id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String assignmentReference(String context, String id)
	{
		String retVal = null;
		if (context == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR + id;
		return retVal;

	} // assignmentReference

	/**
	 * Access the internal reference which can be used to access the resource from within the system.
	 * 
	 * @param id
	 *        The content id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String contentReference(String context, String id)
	{
		String retVal = null;
		if (context == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "c" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "c" + Entity.SEPARATOR + context + Entity.SEPARATOR + id;
		return retVal;

	} // contentReference

	/**
	 * Access the internal reference which can be used to access the resource from within the system.
	 * 
	 * @param id
	 *        The submission id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String submissionReference(String context, String id, String assignmentId)
	{
		String retVal = null;
		if (context == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "s" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "s" + Entity.SEPARATOR + context + Entity.SEPARATOR + assignmentId
					+ Entity.SEPARATOR + id;
		return retVal;

	} // submissionReference

	/**
	 * Access the assignment id extracted from an assignment reference.
	 * 
	 * @param ref
	 *        The assignment reference string.
	 * @return The the assignment id extracted from an assignment reference.
	 */
	protected String assignmentId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	} // assignmentId

	/**
	 * Access the content id extracted from a content reference.
	 * 
	 * @param ref
	 *        The content reference string.
	 * @return The the content id extracted from a content reference.
	 */
	protected String contentId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	} // contentId

	/**
	 * Access the submission id extracted from a submission reference.
	 * 
	 * @param ref
	 *        The submission reference string.
	 * @return The the submission id extracted from a submission reference.
	 */
	protected String submissionId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	} // submissionId

	/**
	 * Check security permission.
	 * 
	 * @param lock -
	 *        The lock id string.
	 * @param resource -
	 *        The resource reference string, or null if no resource is involved.
	 * @return true if allowed, false if not
	 */
	protected boolean unlockCheck(String lock, String resource)
	{
		if (!SecurityService.unlock(lock, resource))
		{
			return false;
		}

		return true;

	}// unlockCheck

	/**
	 * Check security permission.
	 * 
	 * @param lock1
	 *        The lock id string.
	 * @param lock2
	 *        The lock id string.
	 * @param resource
	 *        The resource reference string, or null if no resource is involved.
	 * @return true if either allowed, false if not
	 */
	protected boolean unlockCheck2(String lock1, String lock2, String resource)
	{
		// check the first lock
		if (SecurityService.unlock(lock1, resource)) return true;

		// if the second is different, check that
		if ((lock1 != lock2) && (SecurityService.unlock(lock2, resource))) return true;

		return false;

	} // unlockCheck2

	/**
	 * Check security permission.
	 * 
	 * @param lock -
	 *        The lock id string.
	 * @param resource -
	 *        The resource reference string, or null if no resource is involved.
	 * @exception PermissionException
	 *            Thrown if the user does not have access
	 */
	protected void unlock(String lock, String resource) throws PermissionException
	{
		if (!unlockCheck(lock, resource))
		{
			throw new PermissionException(SessionManager.getCurrentSessionUserId(), lock, resource);
		}

	} // unlock

	/**
	 * Check security permission.
	 * 
	 * @param lock1
	 *        The lock id string.
	 * @param lock2
	 *        The lock id string.
	 * @param resource
	 *        The resource reference string, or null if no resource is involved.
	 * @exception PermissionException
	 *            Thrown if the user does not have access to either.
	 */
	protected void unlock2(String lock1, String lock2, String resource) throws PermissionException
	{
		if (!unlockCheck2(lock1, lock2, resource))
		{
			throw new PermissionException(SessionManager.getCurrentSessionUserId(), lock1 + "/" + lock2, resource);
		}

	} // unlock2

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies and their setter methods
	 *********************************************************************************************************************************************************************************************************************************************************/


	
	/** Dependency: MemoryService. */
	protected MemoryService m_memoryService = null;

	/**
	 * Dependency: MemoryService.
	 * 
	 * @param service
	 *        The MemoryService.
	 */
	public void setMemoryService(MemoryService service)
	{
		m_memoryService = service;
	}
	
	/** Dependency: ContentHostingService. */
	protected ContentHostingService m_contentHostingService = null;

	/**
	 * Dependency:ContentHostingService.
	 * 
	 * @param service
	 *        The ContentHostingService.
	 */
	public void setContentHostingService(ContentHostingService service)
	{
		m_contentHostingService = service;
	}

	/** Configuration: cache, or not. */
	protected boolean m_caching = false;

	/**
	 * Configuration: set the locks-in-db
	 * 
	 * @param path
	 *        The storage path.
	 */
	public void setCaching(String value)
	{
		m_caching = new Boolean(value).booleanValue();
	}

	/** Dependency: EntityManager. */
	protected EntityManager m_entityManager = null;

	/**
	 * Dependency: EntityManager.
	 * 
	 * @param service
	 *        The EntityManager.
	 */
	public void setEntityManager(EntityManager service)
	{
		m_entityManager = service;
	}
	

	/** Dependency: ServerConfigurationService. */
	protected ServerConfigurationService m_serverConfigurationService = null;

	/**
	 * Dependency: ServerConfigurationService.
	 * 
	 * @param service
	 *        The ServerConfigurationService.
	 */
	public void setServerConfigurationService(ServerConfigurationService service)
	{
		m_serverConfigurationService = service;
	}

	/** Dependency: TaggingManager. */
	protected TaggingManager m_taggingManager = null;

	/**
	 * Dependency: TaggingManager.
	 * 
	 * @param manager
	 *        The TaggingManager.
	 */
	public void setTaggingManager(TaggingManager manager)
	{
		m_taggingManager = manager;
	}

	/** Dependency: AssignmentActivityProducer. */
	protected AssignmentActivityProducer m_assignmentActivityProducer = null;

	/**
	 * Dependency: AssignmentActivityProducer.
	 * 
	 * @param assignmentActivityProducer
	 *        The AssignmentActivityProducer.
	 */
	public void setAssignmentActivityProducer(AssignmentActivityProducer assignmentActivityProducer)
	{
		m_assignmentActivityProducer = assignmentActivityProducer;
	}

	/** Dependency: allowGroupAssignments setting */
	protected boolean m_allowGroupAssignments = true;

	/**
	 * Dependency: allowGroupAssignments
	 * 
	 * @param allowGroupAssignments
	 *        the setting
	 */
	public void setAllowGroupAssignments(boolean allowGroupAssignments)
	{
		m_allowGroupAssignments = allowGroupAssignments;
	}
	/**
	 * Get
	 * 
	 * @return allowGroupAssignments
	 */
	public boolean getAllowGroupAssignments()
	{
		return m_allowGroupAssignments;
	}
	
	/** Dependency: allowGroupAssignmentsInGradebook setting */
	protected boolean m_allowGroupAssignmentsInGradebook = true;

	/**
	 * Dependency: allowGroupAssignmentsInGradebook
	 * 
	 * @param allowGroupAssignmentsInGradebook
	 */
	public void setAllowGroupAssignmentsInGradebook(boolean allowGroupAssignmentsInGradebook)
	{
		m_allowGroupAssignmentsInGradebook = allowGroupAssignmentsInGradebook;
	}
	/**
	 * Get
	 * 
	 * @return allowGroupAssignmentsGradebook
	 */
	public boolean getAllowGroupAssignmentsInGradebook()
	{
		return m_allowGroupAssignmentsInGradebook;
	}
	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		m_relativeAccessPoint = REFERENCE_ROOT;
		M_log.info(this + " init()");

		// construct storage helpers and read
		m_assignmentStorage = newAssignmentStorage();
		m_assignmentStorage.open();
		m_submissionStorage = newSubmissionStorage();
		m_submissionStorage.open();

		// make the cache
		if (m_caching)
		{
			m_assignmentCache = m_memoryService
					.newCache(
							"org.sakaiproject.assignment.api.AssignmentService.assignmentCache",
							new AssignmentCacheRefresher(),
							assignmentReference(null, ""));
			m_submissionCache = m_memoryService
					.newCache(
							"org.sakaiproject.assignment.api.AssignmentService.submissionCache",
							new AssignmentSubmissionCacheRefresher(),
							submissionReference(null, "", ""));
		}

		// register as an entity producer
		m_entityManager.registerEntityProducer(this, REFERENCE_ROOT);

		// register functions
		FunctionManager.registerFunction(SECURE_ALL_GROUPS);
		FunctionManager.registerFunction(SECURE_ADD_ASSIGNMENT);
		FunctionManager.registerFunction(SECURE_ADD_ASSIGNMENT_SUBMISSION);
		FunctionManager.registerFunction(SECURE_REMOVE_ASSIGNMENT);
		FunctionManager.registerFunction(SECURE_ACCESS_ASSIGNMENT);
		FunctionManager.registerFunction(SECURE_UPDATE_ASSIGNMENT);
		FunctionManager.registerFunction(SECURE_GRADE_ASSIGNMENT_SUBMISSION);
		FunctionManager.registerFunction(SECURE_ASSIGNMENT_RECEIVE_NOTIFICATIONS);
		FunctionManager.registerFunction(SECURE_SHARE_DRAFTS);
		
 		//if no contentReviewService was set try discovering it
 		if (contentReviewService == null)
 		{
 			contentReviewService = (ContentReviewService) ComponentManager.get(ContentReviewService.class.getName());
 		}
	} // init

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		if (m_caching)
		{
			if (m_assignmentCache != null)
			{
				m_assignmentCache.destroy();
				m_assignmentCache = null;
			}
			if (m_submissionCache != null)
			{
				m_submissionCache.destroy();
				m_submissionCache = null;
			}
		}

		m_assignmentStorage.close();
		m_assignmentStorage = null;
		m_submissionStorage.close();
		m_submissionStorage = null;

		M_log.info(this + " destroy()");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssignmentService implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Creates and adds a new Assignment to the service.
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return The new Assignment object.
	 * @throws IdInvalidException
	 *         if the id contains prohibited characers.
	 * @throws IdUsedException
	 *         if the id is already used in the service.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public Assignment addAssignment(String context) throws PermissionException
	{
		M_log.warn(this + " ENTERING ADD ASSIGNMENT : CONTEXT : " + context);

		String assignmentId = null;
		boolean badId = false;

		do
		{
			badId = !Validator.checkResourceId(assignmentId);
			assignmentId = IdManager.createUuid();

			if (m_assignmentStorage.check(assignmentId)) badId = true;
		}
		while (badId);

		String key = assignmentReference(context, assignmentId);
		
		// security check
		if (!allowAddAssignment(context))
		{
			throw new PermissionException(SessionManager.getCurrentSessionUserId(), SECURE_ADD_ASSIGNMENT, key);
		}

		// storage
		Assignment assignment = m_assignmentStorage.put(assignmentId, context);

		// event for tracking
		((BaseAssignment) assignment).setEvent(EVENT_ADD_ASSIGNMENT);

		
			M_log.warn(this + " LEAVING ADD ASSIGNMENT WITH : ID : " + assignment.getId());

		return assignment;

	} // addAssignment

	/**
	 * Add a new assignment to the directory, from a definition in XML. Must commitEdit() to make official, or cancelEdit() when done!
	 * 
	 * @param el
	 *        The XML DOM Element defining the assignment.
	 * @return A locked AssignmentEdit object (reserving the id).
	 * @exception IdInvalidException
	 *            if the assignment id is invalid.
	 * @exception IdUsedException
	 *            if the assignment id is already used.
	 * @exception PermissionException
	 *            if the current user does not have permission to add an assignnment.
	 */
	public Assignment mergeAssignment(Element el) throws IdInvalidException, IdUsedException, PermissionException
	{
		// construct from the XML
		Assignment assignmentFromXml = new BaseAssignment(el);

		// check for a valid assignment name
		if (!Validator.checkResourceId(assignmentFromXml.getId())) throw new IdInvalidException(assignmentFromXml.getId());

		// check security (throws if not permitted)
		unlock(SECURE_ADD_ASSIGNMENT, assignmentFromXml.getReference());

		// reserve a assignment with this id from the info store - if it's in use, this will return null
		Assignment assignment = m_assignmentStorage.put(assignmentFromXml.getId(), assignmentFromXml.getContext());
		if (assignment == null)
		{
			throw new IdUsedException(assignmentFromXml.getId());
		}

		// transfer from the XML read assignment object to the AssignmentEdit
		((BaseAssignment) assignment).set(assignmentFromXml);

		((BaseAssignment) assignment).setEvent(EVENT_ADD_ASSIGNMENT);

		ResourcePropertiesEdit propertyEdit = (BaseResourcePropertiesEdit)assignment.getProperties();
		try
		{
			Time createTime = propertyEdit.getTimeProperty(ResourceProperties.PROP_CREATION_DATE);
		}
		catch(EntityPropertyNotDefinedException epnde)
		{
			String now = TimeService.newTime().toString();
			propertyEdit.addProperty(ResourceProperties.PROP_CREATION_DATE, now);
		}
		catch(EntityPropertyTypeException epte)
		{
			M_log.error(this + " mergeAssignment error when trying to get creation time property " + epte);
		}

		return assignment;
	}

	/**
	 * Creates and adds a new Assignment to the service which is a copy of an existing Assignment.
	 * 
	 * @param assignmentId -
	 *        The Assignment to be duplicated.
	 * @return The new Assignment object, or null if the original Assignment does not exist.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public Assignment addDuplicateAssignment(String context, String assignmentReference) throws PermissionException,
			IdInvalidException, IdUsedException, IdUnusedException
	{
		
			M_log.warn(this + " ENTERING ADD DUPLICATE ASSIGNMENT WITH ID : " + assignmentReference);

		Assignment retVal = null;

		if (assignmentReference != null)
		{
			String assignmentId = assignmentId(assignmentReference);
			if (!m_assignmentStorage.check(assignmentId))
				throw new IdUnusedException(assignmentId);
			else
			{
				
					M_log.warn(this + " addDuplicateAssignment : assignment exists - will copy");

				Assignment existingAssignment = getAssignment(assignmentReference);

				retVal = addAssignment(context);
				retVal.setTitle(existingAssignment.getTitle() + " - Copy");
				retVal.setSection(existingAssignment.getSection());
				retVal.setOpenTime(existingAssignment.getOpenTime());
				retVal.setDueTime(existingAssignment.getDueTime());
				retVal.setDropDeadTime(existingAssignment.getDropDeadTime());
				retVal.setCloseTime(existingAssignment.getCloseTime());
				retVal.setDraft(true);
				ResourcePropertiesEdit pEdit = (BaseResourcePropertiesEdit) retVal.getProperties();
				pEdit.addAll(existingAssignment.getProperties());
				AssignmentUtil.addLiveProperties(pEdit);
				
				retVal.setInstructions(existingAssignment.getInstructions());
				retVal.setHonorPledge(existingAssignment.getHonorPledge());
				retVal.setTypeOfSubmission(existingAssignment.getTypeOfSubmission());
				retVal.setTypeOfGrade(existingAssignment.getTypeOfGrade());
				retVal.setMaxGradePoint(existingAssignment.getMaxGradePoint());
				retVal.setGroupProject(existingAssignment.getGroupProject());
				retVal.setIndividuallyGraded(existingAssignment.individuallyGraded());
				retVal.setReleaseGrades(existingAssignment.releaseGrades());
				retVal.setAllowAttachments(existingAssignment.getAllowAttachments());


				List tempVector = null;

				Reference tempRef = null;
				Reference newRef = null;
				tempVector = existingAssignment.getAttachments();
				if (tempVector != null)
				{
					for (int z = 0; z < tempVector.size(); z++)
					{
						tempRef = (Reference) tempVector.get(z);
						if (tempRef != null)
						{
							String tempRefId = tempRef.getId();
							String tempRefCollectionId = m_contentHostingService.getContainingCollectionId(tempRefId);
							try
							{
								// get the original attachment display name
								ResourceProperties p = m_contentHostingService.getProperties(tempRefId);
								String displayName = p.getProperty(ResourceProperties.PROP_DISPLAY_NAME);
								// add another attachment instance
								String newItemId = m_contentHostingService.copyIntoFolder(tempRefId, tempRefCollectionId);
								ContentResourceEdit copy = m_contentHostingService.editResource(newItemId);
								// with the same display name
								ResourcePropertiesEdit pedit = copy.getPropertiesEdit();
								pedit.addProperty(ResourceProperties.PROP_DISPLAY_NAME, displayName);
								m_contentHostingService.commitResource(copy, NotificationService.NOTI_NONE);
								newRef = m_entityManager.newReference(copy.getReference());
								retVal.addAttachment(newRef);
							}
							catch (Exception e)
							{
								
									M_log.warn(this + " LEAVING ADD DUPLICATE CONTENT : " + e.toString());
							}	
						}
					}
				}
			}
		}

		
			M_log.warn(this + " ADD DUPLICATE ASSIGNMENT : LEAVING ADD DUPLICATE ASSIGNMENT WITH ID : "
					+ retVal.getId());

		return retVal;
	}

	/**
	 * Access the Assignment with the specified reference.
	 * 
	 * @param assignmentReference -
	 *        The reference of the Assignment.
	 * @return The Assignment corresponding to the reference, or null if it does not exist.
	 * @throws IdUnusedException
	 *         if there is no object with this reference.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public Assignment getAssignment(String assignmentReference) throws IdUnusedException, PermissionException
	{
		M_log.warn(this + " GET ASSIGNMENT : REF : " + assignmentReference);

		// check security on the assignment
		unlockCheck(SECURE_ACCESS_ASSIGNMENT, assignmentReference);
		
		Assignment assignment = findAssignment(assignmentReference);
		
		if (assignment == null) throw new IdUnusedException(assignmentReference);

		// track event
		//EventTrackingService.post(EventTrackingService.newEvent(EVENT_ACCESS_ASSIGNMENT, assignment.getReference(), false));

		return assignment;

	}// getAssignment
	
	protected Assignment findAssignment(String assignmentReference)
	{
		Assignment assignment = null;

		String assignmentId = assignmentId(assignmentReference);

		if ((m_caching) && (m_assignmentCache != null) && (!m_assignmentCache.disabled()))
		{
			// if we have it in the cache, use it
			if (m_assignmentCache.containsKey(assignmentReference))
				assignment = (Assignment) m_assignmentCache.get(assignmentReference);
			if ( assignment == null ) //SAK-12447 cache.get can return null on expired
			{
				assignment = m_assignmentStorage.get(assignmentId);

				// cache the result
				m_assignmentCache.put(assignmentReference, assignment);
			}
		}
		else
		{
			assignment = m_assignmentStorage.get(assignmentId);
		}
		
		return assignment;
	}

	/**
	 * Access all assignment objects - known to us (not from external providers).
	 * 
	 * @return A list of assignment objects.
	 */
	protected List getAssignments(String context)
	{
		return assignments(context, null);

	} // getAssignments
	
	/**
	 * Access all assignment objects - known to us (not from external providers) and accessible by the user
	 * 
	 * @return A list of assignment objects.
	 */
	protected List getAssignments(String context, String userId)
	{
		return assignments(context, userId);

	} // getAssignments

	//
	private List assignments(String context, String userId) 
	{
		if (userId == null)
		{
			userId = SessionManager.getCurrentSessionUserId();
		}
		List assignments = new Vector();

		if ((m_caching) && (m_assignmentCache != null) && (!m_assignmentCache.disabled()))
		{
			// if the cache is complete, use it
			if (m_assignmentCache.isComplete())
			{
				assignments = m_assignmentCache.getAll();
				// TODO: filter by context
			}

			// otherwise get all the assignments from storage
			else
			{
				// Note: while we are getting from storage, storage might change. These can be processed
				// after we get the storage entries, and put them in the cache, and mark the cache complete.
				// -ggolden
				synchronized (m_assignmentCache)
				{
					// if we were waiting and it's now complete...
					if (m_assignmentCache.isComplete())
					{
						assignments = m_assignmentCache.getAll();
						return assignments;
					}

					// save up any events to the cache until we get past this load
					m_assignmentCache.holdEvents();

					assignments = m_assignmentStorage.getAll(context);

					// update the cache, and mark it complete
					for (int i = 0; i < assignments.size(); i++)
					{
						Assignment assignment = (Assignment) assignments.get(i);
						m_assignmentCache.put(assignment.getReference(), assignment);
					}

					m_assignmentCache.setComplete();
					// TODO: not reall, just for context

					// now we are complete, process any cached events
					m_assignmentCache.processEvents();
				}
			}
		}

		else
		{
			// // if we have done this already in this thread, use that
			// assignments = (List) CurrentService.getInThread(context+".assignment.assignments");
			// if (assignments == null)
			// {
			assignments = m_assignmentStorage.getAll(context);
			//				
			// // "cache" the assignments in the current service in case they are needed again in this thread...
			// if (assignments != null)
			// {
			// CurrentService.setInThread(context+".assignment.assignments", assignments);
			// }
			// }
		}

		List rv = new Vector();
		
		// check for the allowed groups of the current end use if we need it, and only once
		Collection allowedGroups = null;
		Site site = null;
		try
		{
			site = SiteService.getSite(context);
		}
		catch (IdUnusedException e)
		{
			M_log.warn(this + " assignments(String, String) " + e.getMessage() + " context=" + context);
		}
		
		for (int x = 0; x < assignments.size(); x++)
		{
			Assignment tempAssignment = (Assignment) assignments.get(x);
			if (tempAssignment.getAccess() == Assignment.AssignmentAccess.GROUPED)
			{
				
				// Can at least one of the designated groups been found
				boolean groupFound = false;
				
				// if grouped, check that the end user has get access to any of this assignment's groups; reject if not

				// check the assignment's groups to the allowed (get) groups for the current user
				Collection asgGroups = tempAssignment.getGroups();

				for (Iterator iAsgGroups=asgGroups.iterator(); site!=null && !groupFound && iAsgGroups.hasNext();)
				{
					String groupId = (String) iAsgGroups.next();
					try
					{
						if (site.getGroup(groupId) != null)
						{
							groupFound = true;
						}
					}
					catch (Exception ee)
					{
						M_log.warn(this + " assignments(String, String) " + ee.getMessage() + " groupId = " + groupId);
					}
					
				}
				
				if (!groupFound)
				{
					// if none of the group exists, mark the assignment as draft and list it
					String assignmentId = tempAssignment.getId();
					try
					{
						AssignmentEdit aEdit = editAssignment(assignmentReference(context, assignmentId));
						aEdit.setDraft(true);
						commitEdit(aEdit);
						rv.add(getAssignment(assignmentId));
					}
					catch (Exception e)
					{
						M_log.warn(this + " assignments(String, String) " + e.getMessage() + " assignment id =" + assignmentId);
						continue;
					}
				}
				else
				{
					// we need the allowed groups, so get it if we have not done so yet
					if (allowedGroups == null)
					{
						allowedGroups = getGroupsAllowGetAssignment(context, userId);
					}
					
					// reject if there is no intersection
					if (!isIntersectionGroupRefsToGroups(asgGroups, allowedGroups)) continue;
					
					rv.add(tempAssignment);
				}
			}
			else
			{
				/// if not reject, add it
				rv.add(tempAssignment);
			}
		}

		return rv;
	}

	/**
	 * See if the collection of group reference strings has at least one group that is in the collection of Group objects.
	 * 
	 * @param groupRefs
	 *        The collection (String) of group references.
	 * @param groups
	 *        The collection (Group) of group objects.
	 * @return true if there is interesection, false if not.
	 */
	protected boolean isIntersectionGroupRefsToGroups(Collection groupRefs, Collection groups)
	{	
		for (Iterator iRefs = groupRefs.iterator(); iRefs.hasNext();)
		{
			String findThisGroupRef = (String) iRefs.next();
			for (Iterator iGroups = groups.iterator(); iGroups.hasNext();)
			{
				String thisGroupRef = ((Group) iGroups.next()).getReference();
				if (thisGroupRef.equals(findThisGroupRef))
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * @deprecated
	 * Get a locked assignment object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	 * 
	 * @param id
	 *        The assignment id string.
	 * @return An AssignmentEdit object for editing.
	 * @exception IdUnusedException
	 *            if not found, or if not an AssignmentEdit object
	 * @exception PermissionException
	 *            if the current user does not have permission to edit this assignment.
	 * @exception InUseException
	 *            if the assignment is being edited by another user.
	 */
	public AssignmentEdit editAssignment(String assignmentReference) throws IdUnusedException, PermissionException, InUseException
	{
		return null;

	} // editAssignment

	/**
	 * Commit the changes made to an AssignmentEdit object, and release the lock.
	 * 
	 * @param assignment
	 *        The AssignmentEdit object to commit.
	 */
	public void saveAssignment(Assignment assignment)
	{
		// check for closed edit
		if (!assignment.isActiveEdit())
		{
			try
			{
				throw new Exception();
			}
			catch (Exception e)
			{
				M_log.warn(this + " commitEdit(): closed AssignmentEdit " + e.getMessage() + " assignment id=" + assignment.getId());
			}
			return;
		}

		// update the properties
		AssignmentUtil.addLiveUpdateProperties(assignment.getPropertiesEdit());

		// complete the edit
		m_assignmentStorage.commit(assignment);

		// track it
		EventTrackingService.post(EventTrackingService.newEvent(((BaseAssignment) assignment).getEvent(), assignment
				.getReference(), true));

	} // commitEdit
	
	/**
	 * @deprecated
	 * Commit the changes made to an AssignmentEdit object, and release the lock.
	 * 
	 * @param assignment
	 *        The AssignmentEdit object to commit.
	 */
	public void commitEdit(AssignmentEdit assignment)
	{
	} // commitEdit

	/**
	 * @deprecated
	 * Cancel the changes made to a AssignmentEdit object, and release the lock.
	 * 
	 * @param assignment
	 *        The AssignmentEdit object to commit.
	 */
	public void cancelEdit(AssignmentEdit assignment)
	{
	} // cancelEdit(Assignment)

	/**
	 * @deprecated
	 * Removes this Assignment and all references to it.
	 * 
	 * @param assignment -
	 *        The Assignment to remove.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public void removeAssignment(AssignmentEdit assignment) throws PermissionException
	{
	}
	/**
	 * Removes this Assignment and all references to it.
	 * 
	 * @param assignment -
	 *        The Assignment to remove.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public void removeAssignment(Assignment assignment) throws PermissionException
	{
		if (assignment != null)
		{
			M_log.warn(this + " removeAssignment with id : " + assignment.getId());

			if (!assignment.isActiveEdit())
			{
				try
				{
					throw new Exception();
				}
				catch (Exception e)
				{
					M_log.warn(this + " removeAssignment(): closed AssignmentEdit" + e.getMessage() + " assignment id=" + assignment.getId());
				}
				return;
			}

			// CHECK PERMISSION
			unlock(SECURE_REMOVE_ASSIGNMENT, assignment.getReference());

			// complete the edit
			m_assignmentStorage.remove(assignment);

			// track event
			EventTrackingService.post(EventTrackingService.newEvent(EVENT_REMOVE_ASSIGNMENT, assignment.getReference(), true));

			// remove any realm defined for this resource
			try
			{
				AuthzGroupService.removeAuthzGroup(assignment.getReference());
			}
			catch (AuthzPermissionException e)
			{
				M_log.warn(this + " removeAssignment: removing realm for assignment reference=" + assignment.getReference() + " : " + e.getMessage());
			}
		}

	}// removeAssignment

	/**
	 * @deprecated
	 * Creates and adds a new AssignmentContent to the service.
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return AssignmentContent The new AssignmentContent object.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public AssignmentContentEdit addAssignmentContent(String context) throws PermissionException
	{
		return null;

	}// addAssignmentContent

	/**
	 * @deprecated
	 * Add a new AssignmentContent to the directory, from a definition in XML. Must commitEdit() to make official, or cancelEdit() when done!
	 * 
	 * @param el
	 *        The XML DOM Element defining the AssignmentContent.
	 * @return A locked AssignmentContentEdit object (reserving the id).
	 * @exception IdInvalidException
	 *            if the AssignmentContent id is invalid.
	 * @exception IdUsedException
	 *            if the AssignmentContent id is already used.
	 * @exception PermissionException
	 *            if the current user does not have permission to add an AssignnmentContent.
	 */
	public AssignmentContentEdit mergeAssignmentContent(Element el) throws IdInvalidException, IdUsedException, PermissionException
	{
		return null;
	}

	/**
	 * @deprecated
	 * Creates and adds a new AssignmentContent to the service which is a copy of an existing AssignmentContent.
	 * 
	 * @param context -
	 *        From DefaultId.getChannel(RunData)
	 * @param contentReference -
	 *        The id of the AssignmentContent to be duplicated.
	 * @return AssignmentContentEdit The new AssignmentContentEdit object, or null if the original does not exist.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public AssignmentContentEdit addDuplicateAssignmentContent(String context, String contentReference) throws PermissionException,
			IdInvalidException, IdUnusedException
	{
		return null;
	}

	/**
	 * @deprecated
	 * Access the AssignmentContent with the specified reference.
	 * 
	 * @param contentReference -
	 *        The reference of the AssignmentContent.
	 * @return The AssignmentContent corresponding to the reference, or null if it does not exist.
	 * @throws IdUnusedException
	 *         if there is no object with this reference.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public AssignmentContent getAssignmentContent(String contentReference) throws IdUnusedException, PermissionException
	{
		return null;

	}// getAssignmentContent

	/**
	 * @deprecated
	 * Access all AssignmentContent objects - known to us (not from external providers).
	 * 
	 * @return A list of AssignmentContent objects.
	 */
	protected List getAssignmentContents(String context)
	{
		return null;

	} // getAssignmentContents

	/**
	 * @deprecated
	 * Get a locked AssignmentContent object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	 * 
	 * @param id
	 *        The content id string.
	 * @return An AssignmentContentEdit object for editing.
	 * @exception IdUnusedException
	 *            if not found, or if not an AssignmentContentEdit object
	 * @exception PermissionException
	 *            if the current user does not have permission to edit this content.
	 * @exception InUseException
	 *            if the assignment is being edited by another user.
	 */
	public AssignmentContentEdit editAssignmentContent(String contentReference) throws IdUnusedException, PermissionException,
			InUseException
	{
		return null;

	} // editAssignmentContent

	/**
	 * @deprecated
	 * Commit the changes made to an AssignmentContentEdit object, and release the lock.
	 * 
	 * @param content
	 *        The AssignmentContentEdit object to commit.
	 */
	public void commitEdit(AssignmentContentEdit content)
	{

	} // commitEdit(AssignmentContent)

	/**
	 * @deprecated
	 * Cancel the changes made to a AssignmentContentEdit object, and release the lock.
	 * 
	 * @param content
	 *        The AssignmentContentEdit object to commit.
	 */
	public void cancelEdit(AssignmentContentEdit content)
	{

	} // cancelEdit(Content)

	/**
	 * @deprecated
	 * Removes an AssignmentContent
	 * 
	 * @param content -
	 *        the AssignmentContent to remove.
	 * @throws an
	 *         AssignmentContentNotEmptyException if this content still has related Assignments.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public void removeAssignmentContent(AssignmentContentEdit content) throws AssignmentContentNotEmptyException,
			PermissionException
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public AssignmentSubmission addSubmission(String context, String assignmentId, String submitterId) throws PermissionException
	{
		M_log.warn(this + " ENTERING ADD SUBMISSION");

		String submissionId = null;
		boolean badId = false;

		do
		{
			badId = !Validator.checkResourceId(submissionId);
			submissionId = IdManager.createUuid();

			if (m_submissionStorage.check(submissionId)) badId = true;
		}
		while (badId);

		String key = submissionReference(context, submissionId, assignmentId);

		M_log.warn(this + " ADD SUBMISSION : SUB REF : " + key);

		unlock(SECURE_ADD_ASSIGNMENT_SUBMISSION, key);

		M_log.warn(this + " ADD SUBMISSION : UNLOCKED");

		// storage
		AssignmentSubmission submission = m_submissionStorage.put(submissionId, assignmentId, submitterId, null, null, null);

		submission.setContext(context);
		
		
			M_log.warn(this + " LEAVING ADD SUBMISSION : REF : " + submission.getReference());

		// event for tracking
		((BaseAssignmentSubmission) submission).setEvent(EVENT_ADD_ASSIGNMENT_SUBMISSION);

		return submission;
	}

	/**
	 * Add a new AssignmentSubmission to the directory, from a definition in XML. Must commitEdit() to make official, or cancelEdit() when done!
	 * 
	 * @param el
	 *        The XML DOM Element defining the submission.
	 * @return A locked AssignmentSubmissionEdit object (reserving the id).
	 * @exception IdInvalidException
	 *            if the submission id is invalid.
	 * @exception IdUsedException
	 *            if the submission id is already used.
	 * @exception PermissionException
	 *            if the current user does not have permission to add a submission.
	 */
	public AssignmentSubmission mergeSubmission(Element el) throws IdInvalidException, IdUsedException, PermissionException
	{
		// construct from the XML
		BaseAssignmentSubmission submissionFromXml = new BaseAssignmentSubmission(el);

		// check for a valid submission name
		if (!Validator.checkResourceId(submissionFromXml.getId())) throw new IdInvalidException(submissionFromXml.getId());

		// check security (throws if not permitted)
		unlock(SECURE_ADD_ASSIGNMENT_SUBMISSION, submissionFromXml.getReference());

		// reserve a submission with this id from the info store - if it's in use, this will return null
		AssignmentSubmission submission = m_submissionStorage.put(	submissionFromXml.getId(), 
																		submissionFromXml.getAssignmentId(),
																		submissionFromXml.getSubmitterIdString(),
																		(submissionFromXml.getTimeSubmitted() != null)?String.valueOf(submissionFromXml.getTimeSubmitted().getTime()):null,
																		Boolean.valueOf(submissionFromXml.getSubmitted()).toString(),
																		Boolean.valueOf(submissionFromXml.getGraded()).toString());
		if (submission == null)
		{
			throw new IdUsedException(submissionFromXml.getId());
		}

		// transfer from the XML read submission object to the SubmissionEdit
		((BaseAssignmentSubmission) submission).set(submissionFromXml);

		((BaseAssignmentSubmission) submission).setEvent(EVENT_ADD_ASSIGNMENT_SUBMISSION);

		return submission;
	}

	/**
	 * @deprecated
	 * Get a locked AssignmentSubmission object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	 * 
	 * @param submissionrReference -
	 *        the reference for the submission.
	 * @return An AssignmentSubmissionEdit object for editing.
	 * @exception IdUnusedException
	 *            if not found, or if not an AssignmentSubmissionEdit object
	 * @exception PermissionException
	 *            if the current user does not have permission to edit this submission.
	 * @exception InUseException
	 *            if the assignment is being edited by another user.
	 */
	public AssignmentSubmissionEdit editSubmission(String submissionReference) throws IdUnusedException, PermissionException,
			InUseException
	{
		return null;

	} // editSubmission
	
	/**
	 * Commit the changes made to an AssignmentSubmissionEdit object, and release the lock.
	 * 
	 * @param submission
	 *        The AssignmentSubmissionEdit object to commit.
	 */
	public void saveSubmission(AssignmentSubmission submission)
	{
		String submissionRef = submission.getReference();

		// update the properties
		AssignmentUtil.addLiveUpdateProperties(submission.getPropertiesEdit());

		submission.setTimeLastModified(TimeService.newTime());

		// complete the edit
		m_submissionStorage.commit(submission);

		try
		{
			AssignmentSubmission s = getSubmission(submissionRef);
			
			Assignment a = s.getAssignment();
			
			Time returnedTime = s.getTimeReturned();
			Time submittedTime = s.getTimeSubmitted();
			
			// track it
			if (!s.getSubmitted())
			{
				// saving a submission
				EventTrackingService.post(EventTrackingService.newEvent(EVENT_SAVE_ASSIGNMENT_SUBMISSION, submissionRef, true));
			}
			else if (returnedTime == null && !s.getReturned() && (submittedTime == null /*grading non-submissions*/
																|| (submittedTime != null && (s.getTimeLastModified().getTime() - submittedTime.getTime()) > 1000*60 /*make sure the last modified time is at least one minute after the submit time*/)))
			{
				// graded and saved before releasing it
				EventTrackingService.post(EventTrackingService.newEvent(EVENT_GRADE_ASSIGNMENT_SUBMISSION, submissionRef, true));
			}
			else if (returnedTime != null && s.getGraded() && (submittedTime == null/*returning non-submissions*/ 
											|| (submittedTime != null && returnedTime.after(submittedTime))/*returning normal submissions*/ 
											|| (submittedTime != null && submittedTime.after(returnedTime) && s.getTimeLastModified().after(submittedTime))/*grading the resubmitted assignment*/))
			{
				// releasing a submitted assignment or releasing grade to an unsubmitted assignment
				EventTrackingService.post(EventTrackingService.newEvent(EVENT_GRADE_ASSIGNMENT_SUBMISSION, submissionRef, true));
			}
			else if (submittedTime == null) /*grading non-submission*/
			{
				// releasing a submitted assignment or releasing grade to an unsubmitted assignment
				EventTrackingService.post(EventTrackingService.newEvent(EVENT_GRADE_ASSIGNMENT_SUBMISSION, submissionRef, true));
			}
			else
			{
				// submitting a submission
				EventTrackingService.post(EventTrackingService.newEvent(EVENT_SUBMIT_ASSIGNMENT_SUBMISSION, submissionRef, true));
			
				// only doing the notification for real online submissions
				if (a.getTypeOfSubmission() != Assignment.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
				{
					// instructor notification
					notificationToInstructors(s, a);
					
					// student notification, whether the student gets email notification once he submits an assignment
					notificationToStudent(s);
				}
			}
				
			
		}
		catch (IdUnusedException e)
		{
			M_log.warn(this + " commitEdit(), submissionId=" + submissionRef, e);
		}
		catch (PermissionException e)
		{
			M_log.warn(this + " commitEdit(), submissionId=" + submissionRef, e);
		}

	} // saveSubmission(Submission)

	/**
	 * @deprecated
	 * Commit the changes made to an AssignmentSubmissionEdit object, and release the lock.
	 * 
	 * @param submission
	 *        The AssignmentSubmissionEdit object to commit.
	 */
	public void commitEdit(AssignmentSubmissionEdit submission)
	{

	} // commitEdit(Submission)

	/**
	 * send notification to instructor type of users if necessary
	 * @param s
	 * @param a
	 */
	private void notificationToInstructors(AssignmentSubmission s, Assignment a) 
	{
		String notiOption = a.getProperties().getProperty(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE);
		if (notiOption != null && !notiOption.equals(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_NONE))
		{
			// need to send notification email
			String context = s.getContext();
			
			// compare the list of users with the receive.notifications and list of users who can actually grade this assignment
			List receivers = allowReceiveSubmissionNotificationUsers(context);
			List allowGradeAssignmentUsers = allowGradeAssignmentUsers(a.getReference());
			receivers.retainAll(allowGradeAssignmentUsers);
			
			String submitterId = s.getSubmitterIdString();
			
			// filter out users who's not able to grade this submission
			List finalReceivers = new Vector();
			
			HashSet receiverSet = new HashSet();
			if (a.getAccess().equals(Assignment.AssignmentAccess.GROUPED))
			{
				Collection groups = a.getGroups();
				for (Iterator gIterator = groups.iterator(); gIterator.hasNext();)
				{
					String g = (String) gIterator.next();
					try
					{
						AuthzGroup aGroup = AuthzGroupService.getAuthzGroup(g);
						if (aGroup.isAllowed(submitterId, AssignmentService.SECURE_ADD_ASSIGNMENT_SUBMISSION))
						{
							for (Iterator rIterator = receivers.iterator(); rIterator.hasNext();)
							{
								User rUser = (User) rIterator.next();
								String rUserId = rUser.getId();
								if (!receiverSet.contains(rUserId) && aGroup.isAllowed(rUserId, AssignmentService.SECURE_GRADE_ASSIGNMENT_SUBMISSION))
								{
									finalReceivers.add(rUser);
									receiverSet.add(rUserId);
								}
							}
						}
					}
					catch (Exception e)
					{
						M_log.warn(this + " notificationToInstructors, group id =" + g + " " + e.getMessage());
					}
				}
			}
			else
			{
				finalReceivers.addAll(receivers);
			}
			
			String messageBody = getNotificationMessage(s);
			
			if (notiOption.equals(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_EACH))
			{
				// send the message immidiately
				EmailService.sendToUsers(finalReceivers, getHeaders(null), messageBody);
			}
			else if (notiOption.equals(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DIGEST))
			{
				// digest the message to each user
				for (Iterator iReceivers = finalReceivers.iterator(); iReceivers.hasNext();)
				{
					User user = (User) iReceivers.next();
					DigestService.digest(user.getId(), getSubject(), messageBody);
				}
			}
		}
	}

	/**
	 * send notification to student if necessary
	 * @param s
	 */
	private void notificationToStudent(AssignmentSubmission s) 
	{
		if (m_serverConfigurationService.getBoolean("assignment.submission.confirmation.email", true))
		{
			//send notification
			User u = UserDirectoryService.getCurrentUser();
			
			if (StringUtil.trimToNull(u.getEmail()) != null)
			{
				List receivers = new Vector();
				receivers.add(u);
				
				EmailService.sendToUsers(receivers, getHeaders(u.getEmail()), getNotificationMessage(s));
			}
		}
	}
	
	protected List<String> getHeaders(String receiverEmail)
	{
		List<String> rv = new Vector<String>();
		
		rv.add("MIME-Version: 1.0");
		rv.add("Content-Type: multipart/alternative; boundary=\""+MULTIPART_BOUNDARY+"\"");
		// set the subject
		rv.add(getSubject());

		// from
		rv.add(getFrom());
		
		// to
		if (StringUtil.trimToNull(receiverEmail) != null)
		{
			rv.add("To: " + receiverEmail);
		}
		
		return rv;
	}
	
	protected String getSubject()
	{
		return rb.getString("noti.subject.label") + " " + rb.getString("noti.subject.content");
	}
	
	protected String getFrom()
	{
		return "From: " + "\"" + m_serverConfigurationService.getString("ui.service", "Sakai") + "\"<no-reply@"+ m_serverConfigurationService.getServerName() + ">";
	}
	
	private final String MULTIPART_BOUNDARY = "======sakai-multi-part-boundary======";
	private final String BOUNDARY_LINE = "\n\n--"+MULTIPART_BOUNDARY+"\n";
	private final String TERMINATION_LINE = "\n\n--"+MULTIPART_BOUNDARY+"--\n\n";
	private final String MIME_ADVISORY = "This message is for MIME-compliant mail readers.";
	
	/**
	 * Get the message for the email.
	 * 
	 * @param event
	 *        The event that matched criteria to cause the notification.
	 * @return the message for the email.
	 */
	protected String getNotificationMessage(AssignmentSubmission s)
	{	
		StringBuilder message = new StringBuilder();
		message.append(MIME_ADVISORY);
		message.append(BOUNDARY_LINE);
		message.append(plainTextHeaders());
		message.append(plainTextContent(s));
		message.append(BOUNDARY_LINE);
		message.append(htmlHeaders());
		message.append(htmlPreamble());
		message.append(htmlContent(s));
		message.append(htmlEnd());
		message.append(TERMINATION_LINE);
		return message.toString();
	}
	
	protected String plainTextHeaders() {
		return "Content-Type: text/plain\n\n";
	}
	
	protected String plainTextContent(AssignmentSubmission s) {
		return htmlContent(s);
	}
	
	protected String htmlHeaders() {
		return "Content-Type: text/html\n\n";
	}
	
	protected String htmlPreamble() {
		StringBuilder buf = new StringBuilder();
		buf.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n");
		buf.append("    \"http://www.w3.org/TR/html4/loose.dtd\">\n");
		buf.append("<html>\n");
		buf.append("  <head><title>");
		buf.append(getSubject());
		buf.append("</title></head>\n");
		buf.append("  <body>\n");
		return buf.toString();
	}
	
	protected String htmlEnd() {
		return "\n  </body>\n</html>\n";
	}

	private String htmlContent(AssignmentSubmission s) 
	{
		String newline = "<br />\n";
		
		Assignment a = s.getAssignment();
		
		String context = s.getContext();
		
		String siteTitle = "";
		String siteId = "";
		try
		{
			Site site = SiteService.getSite(context);
			siteTitle = site.getTitle();
			siteId = site.getId();
		}
		catch (Exception ee)
		{
			M_log.warn(this + " htmlContent(), site id =" + context + " " + ee.getMessage());
		}
		
		StringBuilder buffer = new StringBuilder();
		// site title and id
		buffer.append(rb.getString("noti.site.title") + " " + siteTitle + newline);
		buffer.append(rb.getString("noti.site.id") + " " + siteId +newline + newline);
		// assignment title and due date
		buffer.append(rb.getString("noti.assignment") + " " + a.getTitle()+newline);
		buffer.append(rb.getString("noti.assignment.duedate") + " " + a.getDueTime().toStringLocalFull()+newline + newline);
		// submitter name and id
		User[] submitters = s.getSubmitters();
		String submitterNames = "";
		String submitterIds = "";
		for (int i = 0; i<submitters.length; i++)
		{
			User u = (User) submitters[i];
			if (i>0)
			{
				submitterNames = submitterNames.concat("; ");
				submitterIds = submitterIds.concat("; ");
			}
			submitterNames = submitterNames.concat(u.getDisplayName());
			submitterIds = submitterIds.concat(u.getDisplayId());
		}
		buffer.append(rb.getString("noti.student") + " " + submitterNames);
		if (submitterIds.length() != 0)
		{
			buffer.append("( " + submitterIds + " )");
		}
		buffer.append(newline + newline);
		
		// submit time
		buffer.append(rb.getString("noti.submit.id") + " " + s.getId() + newline);
		
		// submit time 
		buffer.append(rb.getString("noti.submit.time") + " " + s.getTimeSubmitted().toStringLocalFull() + newline + newline);
		
		// submit text
		String text = StringUtil.trimToNull(s.getSubmittedText());
		if ( text != null)
		{
			buffer.append(rb.getString("noti.submit.text") + newline + newline + Validator.escapeHtmlFormattedText(text) + newline + newline);
		}
		
		// attachment if any
		List attachments = s.getSubmittedAttachments();
		if (attachments != null && attachments.size() >0)
		{
			buffer.append(rb.getString("noti.submit.attachments") + newline + newline);
			for (int j = 0; j<attachments.size(); j++)
			{
				Reference r = (Reference) attachments.get(j);
				buffer.append(r.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME) + "(" + r.getProperties().getPropertyFormatted(ResourceProperties.PROP_CONTENT_LENGTH)+ ")\n");
			}
		}
		
		return buffer.toString();
	}

	/**
	 * @deprecated
	 * Cancel the changes made to a AssignmentSubmissionEdit object, and release the lock.
	 * 
	 * @param submission
	 *        The AssignmentSubmissionEdit object to commit.
	 */
	public void cancelEdit(AssignmentSubmissionEdit submission)
	{

	} // cancelEdit(Submission)
	
	/**
	 * @deprecated
	 * Removes an AssignmentSubmission and all references to it
	 * 
	 * @param submission -
	 *        the AssignmentSubmission to remove.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public void removeSubmission(AssignmentSubmissionEdit submission) throws PermissionException
	{
	}

	/**
	 * Removes an AssignmentSubmission and all references to it
	 * 
	 * @param submission -
	 *        the AssignmentSubmission to remove.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public void removeSubmission(AssignmentSubmission submission) throws PermissionException
	{
		if (submission != null)
		{
			if (!submission.isActiveEdit())
			{
				try
				{
					throw new Exception();
				}
				catch (Exception e)
				{
					M_log.warn(this + " removeSubmission(): closed AssignmentSubmissionEdit id=" + submission.getId()  + " "  + e.getMessage());
				}
				return;
			}

			// check security
			unlock(SECURE_REMOVE_ASSIGNMENT_SUBMISSION, submission.getReference());

			// complete the edit
			m_submissionStorage.remove(submission);

			// track event
			EventTrackingService.post(EventTrackingService.newEvent(EVENT_REMOVE_ASSIGNMENT_SUBMISSION, submission.getReference(),
					true));

			// remove any realm defined for this resource
			try
			{
				AuthzGroupService.removeAuthzGroup(AuthzGroupService.getAuthzGroup(submission.getReference()));
			}
			catch (AuthzPermissionException e)
			{
				M_log.warn(this + " removeSubmission: removing realm for : " + submission.getReference() + " : " + e.getMessage());
			}
			catch (GroupNotDefinedException e)
			{
				M_log.warn(this + " removeSubmission: cannot find group for submission " + submission.getReference() + " : " + e.getMessage());
			}
		}
	}// removeSubmission

	/**
	 *@inheritDoc
	 */
	public int getSubmissionsSize(String context)
	{
		int size = 0;
		
		List submissions = getSubmissions(context);
		if (submissions != null)
		{
			size = submissions.size();
		}
		return size;
	}
	
	/**
	 * Access all AssignmentSubmission objects - known to us (not from external providers).
	 * 
	 * @return A list of AssignmentSubmission objects.
	 */
	protected List getSubmissions(String context)
	{
		List submissions = new Vector();

		if ((m_caching) && (m_submissionCache != null) && (!m_submissionCache.disabled()))
		{
			// if the cache is complete, use it
			if (m_submissionCache.isComplete())
			{
				submissions = m_submissionCache.getAll();
				// TODO: filter by context
			}

			// otherwise get all the submissions from storage
			else
			{
				// Note: while we are getting from storage, storage might change. These can be processed
				// after we get the storage entries, and put them in the cache, and mark the cache complete.
				// -ggolden
				synchronized (m_submissionCache)
				{
					// if we were waiting and it's now complete...
					if (m_submissionCache.isComplete())
					{
						submissions = m_submissionCache.getAll();
						return submissions;
					}

					// save up any events to the cache until we get past this load
					m_submissionCache.holdEvents();

					submissions = m_submissionStorage.getAll(context);

					// update the cache, and mark it complete
					for (int i = 0; i < submissions.size(); i++)
					{
						AssignmentSubmission submission = (AssignmentSubmission) submissions.get(i);
						m_submissionCache.put(submission.getReference(), submission);
					}

					m_submissionCache.setComplete();
					// TODO: not really! just for context

					// now we are complete, process any cached events
					m_submissionCache.processEvents();
				}
			}
		}

		else
		{
			// // if we have done this already in this thread, use that
			// submissions = (List) CurrentService.getInThread(context+".assignment.submissions");
			// if (submissions == null)
			// {
			submissions = m_submissionStorage.getAll(context);
			//
			// // "cache" the submissions in the current service in case they are needed again in this thread...
			// if (submissions != null)
			// {
			// CurrentService.setInThread(context+".assignment.submissions", submissions);
			// }
			// }
		}

		return submissions;

	} // getAssignmentSubmissions

	/**
	 * Access list of all AssignmentContents created by the User.
	 * 
	 * @param owner -
	 *        The User who's AssignmentContents are requested.
	 * @return Iterator over all AssignmentContents owned by this User.
	 */
	public Iterator getAssignmentContents(User owner)
	{
		Vector retVal = new Vector();
		AssignmentContent aContent = null;
		List allContents = getAssignmentContents(owner.getId());

		for (int x = 0; x < allContents.size(); x++)
		{
			aContent = (AssignmentContent) allContents.get(x);
			if (aContent.getCreator().equals(owner.getId()))
			{
				retVal.add(aContent);
			}
		}

		if (retVal.isEmpty())
			return new EmptyIterator();
		else
			return retVal.iterator();

	}// getAssignmentContents(User)

	/**
	 * Access all the Assignments which have the specified AssignmentContent.
	 * 
	 * @param content -
	 *        The particular AssignmentContent.
	 * @return Iterator over all the Assignments with the specified AssignmentContent.
	 */
	public Iterator getAssignments(AssignmentContent content)
	{
		Vector retVal = new Vector();
		String contentReference = null;
		String tempContentReference = null;

		if (content != null)
		{
			contentReference = content.getReference();
			List allAssignments = getAssignments(content.getContext());
			Assignment tempAssignment = null;

			for (int y = 0; y < allAssignments.size(); y++)
			{
				tempAssignment = (Assignment) allAssignments.get(y);
				tempContentReference = tempAssignment.getContentReference();
				if (tempContentReference != null)
				{
					if (tempContentReference.equals(contentReference))
					{
						retVal.add(tempAssignment);
					}
				}
			}
		}

		if (retVal.isEmpty())
			return new EmptyIterator();
		else
			return retVal.iterator();
	}

	/**
	 * Access all the Assignemnts associated with the context
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return Iterator over all the Assignments associated with the context and the user.
	 */
	public Iterator getAssignmentsForContext(String context)
	{
		M_log.warn(this + " GET ASSIGNMENTS FOR CONTEXT : CONTEXT : " + context);
		
		return assignmentsForContextAndUser(context, null);

	}
	
	/**
	 * Access all the Assignemnts associated with the context and the user
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel()
	 * @return Iterator over all the Assignments associated with the context and the user
	 */
	public Iterator getAssignmentsForContext(String context, String userId)
	{
		M_log.warn(this + " GET ASSIGNMENTS FOR CONTEXT : CONTEXT : " + context);
		
		return assignmentsForContextAndUser(context, userId);

	}

	/**
	 * get proper assignments for specified context and user
	 * @param context
	 * @param user
	 * @return
	 */
	private Iterator assignmentsForContextAndUser(String context, String userId) 
	{
		Assignment tempAssignment = null;
		Vector retVal = new Vector();
		List allAssignments = null;

		if (context != null)
		{
			allAssignments = getAssignments(context, userId);
			
			for (int x = 0; x < allAssignments.size(); x++)
			{
				tempAssignment = (Assignment) allAssignments.get(x);

				if ((context.equals(tempAssignment.getContext()))
						|| (context.equals(getGroupNameFromContext(tempAssignment.getContext()))))
				{
					retVal.add(tempAssignment);
				}
			}
		}

		if (retVal.isEmpty())
			return new EmptyIterator();
		else
			return retVal.iterator();
	}

	/**
	 * @inheritDoc
	 */
	public List getListAssignmentsForContext(String context)
	{
		M_log.warn(this + " getListAssignmetsForContext : CONTEXT : " + context);
		Assignment tempAssignment = null;
		Vector retVal = new Vector();
		List allAssignments = new Vector();

		if (context != null)
		{
			allAssignments = getAssignments(context);
			for (int x = 0; x < allAssignments.size(); x++)
			{
				tempAssignment = (Assignment) allAssignments.get(x);
				
				if ((context.equals(tempAssignment.getContext()))
						|| (context.equals(getGroupNameFromContext(tempAssignment.getContext()))))
				{
					String deleted = tempAssignment.getProperties().getProperty(ResourceProperties.PROP_ASSIGNMENT_DELETED);
					if (deleted == null || deleted.equals(""))
					{
						// not deleted, show it
						if (tempAssignment.getDraft())
						{
							// who can see the draft assigment
							if (isDraftAssignmentVisible(tempAssignment, context))
							{
								retVal.add(tempAssignment);
							}
						}
						else
						{
							retVal.add(tempAssignment);
						}
					}
				}
			}
		}

		return retVal;

	}
	
	/**
	 * who can see the draft assignment
	 * @param assignment
	 * @param context
	 * @return
	 */
	private boolean isDraftAssignmentVisible(Assignment assignment, String context) 
	{
		return SecurityService.isSuperUser() // super user can always see it
			|| assignment.getCreator().equals(UserDirectoryService.getCurrentUser().getId()) // the creator can see it
			|| (unlockCheck(SECURE_SHARE_DRAFTS, SiteService.siteReference(context)) && isCurrentUserInSameRoleAsCreator(assignment.getCreator(), context)); // same role user with share draft permission
	}
	
	/**
	 * is current user has same role as the specified one?
	 * @param assignment
	 * @param context
	 * @return
	 */
	private boolean isCurrentUserInSameRoleAsCreator(String creatorUserId, String context) 
	{	
		try {
			User currentUser = UserDirectoryService.getCurrentUser();
			
			AuthzGroup group = AuthzGroupService.getAuthzGroup(SiteService.siteReference(context));
			
			Member currentUserMember = group.getMember(currentUser.getId());
			Member creatorMember = group.getMember(creatorUserId);
			Role role = currentUserMember.getRole();
		
			return role != null && role.getId().equals(creatorMember.getRole().getId());
		
		} catch (GroupNotDefinedException gnde) {
			M_log.warn("No group defined for this site " + context);
		}
		
		return false;
	}
	
	/**
	 * Access a User's AssignmentSubmission to a particular Assignment.
	 * 
	 * @param assignmentReference
	 *        The reference of the assignment.
	 * @param person -
	 *        The User who's Submission you would like.
	 * @return AssignmentSubmission The user's submission for that Assignment.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public AssignmentSubmission getSubmission(String assignmentReference, User person)
	{
		AssignmentSubmission submission = null;

		String assignmentId = assignmentId(assignmentReference);
		
		if ((assignmentReference != null) && (person != null))
		{
			submission = m_submissionStorage.get(assignmentId, person.getId());
		}

		if (submission != null)
		{
			try
			{
				unlock2(SECURE_ACCESS_ASSIGNMENT_SUBMISSION, SECURE_ACCESS_ASSIGNMENT, submission.getReference());
			}
			catch (PermissionException e)
			{
				return null;
			}
		}

		return submission;
	}

	/**
	 * @inheritDoc
	 */
	public AssignmentSubmission getSubmission(List submissions, User person) 
	{
		AssignmentSubmission retVal = null;
		
		for (int z = 0; z < submissions.size(); z++)
		{
			AssignmentSubmission sub = (AssignmentSubmission) submissions.get(z);
			if (sub != null)
			{
				List submitters = sub.getSubmitterIds();
				for (int a = 0; a < submitters.size(); a++)
				{
					String aUserId = (String) submitters.get(a);
					
						M_log.warn(this + " getSubmission(List, User) comparing aUser id : " + aUserId + " and chosen user id : "
								+ person.getId());
					if (aUserId.equals(person.getId()))
					{
						
							M_log.warn(this + " getSubmission(List, User) found a match : return value is " + sub.getId());
						retVal = sub;
					}
				}
			}
		}

		return retVal;
	}
	
	/**
	 * Get the submissions for an assignment.
	 * 
	 * @param assignment -
	 *        the Assignment who's submissions you would like.
	 * @return Iterator over all the submissions for an Assignment.
	 */
	public List getSubmissions(Assignment assignment)
	{
		List retVal = new Vector();

		if (assignment != null)
		{
			retVal = getSubmissions(assignment.getId());
		}
		
		return retVal;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getSubmittedSubmissionsCount(String assignmentId)
	{
		return m_submissionStorage.getSubmittedSubmissionsCount(assignmentId);
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getUngradedSubmissionsCount(String assignmentId)
	{
		return m_submissionStorage.getUngradedSubmissionsCount(assignmentId);
		
	}

	/**
	 * Access the AssignmentSubmission with the specified id.
	 * 
	 * @param submissionReference -
	 *        The reference of the AssignmentSubmission.
	 * @return The AssignmentSubmission corresponding to the id, or null if it does not exist.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public AssignmentSubmission getSubmission(String submissionReference) throws IdUnusedException, PermissionException
	{
		M_log.warn(this + " GET SUBMISSION : REF : " + submissionReference);
		
		// check permission
		unlock2(SECURE_ACCESS_ASSIGNMENT_SUBMISSION, SECURE_ACCESS_ASSIGNMENT, submissionReference);

		AssignmentSubmission submission = null;

		String submissionId = submissionId(submissionReference);

		if ((m_caching) && (m_submissionCache != null) && (!m_submissionCache.disabled()))
		{
			// if we have it in the cache, use it
			if (m_submissionCache.containsKey(submissionReference))
				submission = (AssignmentSubmission) m_submissionCache.get(submissionReference);
			if ( submission == null ) //SAK-12447 cache.get can return null on expired
 			{
				submission = m_submissionStorage.get(submissionId);

				// cache the result
				m_submissionCache.put(submissionReference, submission);
			}
		}

		else
		{
			// // if we have done this already in this thread, use that
			// submission = (AssignmentSubmission) CurrentService.getInThread(submissionId+".assignment.submission");
			// if (submission == null)
			// {
			submission = m_submissionStorage.get(submissionId);
			//				
			// // "cache" the submission in the current service in case they are needed again in this thread...
			// if (submission != null)
			// {
			// CurrentService.setInThread(submissionId+".assignment.submission", submission);
			// }
			// }
		}

		if (submission == null) throw new IdUnusedException(submissionId);

		// track event
		// EventTrackingService.post(EventTrackingService.newEvent(EVENT_ACCESS_ASSIGNMENT_SUBMISSION, submission.getReference(), false));

		return submission;

	}// getAssignmentSubmission

	/**
	 * Return the reference root for use in resource references and urls.
	 * 
	 * @return The reference root for use in resource references and urls.
	 */
	protected String getReferenceRoot()
	{
		return REFERENCE_ROOT;
	}

	/**
	 * check permissions for addAssignment().
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel()
	 * @return true if the user is allowed to addAssignment(...), false if not.
	 */
	public boolean allowAddGroupAssignment(String context)
	{
		// base the check for SECURE_ADD on the site, any of the site's groups, and the channel
		// if the user can SECURE_ADD anywhere in that mix, they can add an assignment
		// this stack is not the normal azg set for channels, so use a special refernce to get this behavior
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + REF_TYPE_ASSIGNMENT_GROUPS + Entity.SEPARATOR + "a"
				+ Entity.SEPARATOR + context + Entity.SEPARATOR;

		
		{
			M_log.warn(this + " allowAddGroupAssignment with resource string : " + resourceString);
			M_log.warn("                                    context string : " + context);
		}

		// check security on the channel (throws if not permitted)
		return unlockCheck(SECURE_ADD_ASSIGNMENT, resourceString);

	} // allowAddGroupAssignment

	/**
	 * @inheritDoc
	 */
	public boolean allowReceiveSubmissionNotification(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;

		
		{
			M_log.warn(this + " allowReceiveSubmissionNotification with resource string : " + resourceString);
		}

		// checking allow at the site level
		if (unlockCheck(SECURE_ASSIGNMENT_RECEIVE_NOTIFICATIONS, resourceString)) return true;
		
		return false;
	}
	
	/**
	 * @inheritDoc
	 */
	public List allowReceiveSubmissionNotificationUsers(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;
		
		{
			M_log.warn(this + " allowReceiveSubmissionNotificationUsers with resource string : " + resourceString);
			M_log.warn("                                   				 	context string : " + context);
		}
		return SecurityService.unlockUsers(SECURE_ASSIGNMENT_RECEIVE_NOTIFICATIONS, resourceString);

	} // allowAddAssignmentUsers
	
	/**
	 * @inheritDoc
	 */
	public boolean allowAddAssignment(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;
		// base the check for SECURE_ADD_ASSIGNMENT on the site and any of the site's groups
		// if the user can SECURE_ADD_ASSIGNMENT anywhere in that mix, they can add an assignment
		// this stack is not the normal azg set for site, so use a special refernce to get this behavior

		
		{
			M_log.warn(this + " allowAddAssignment with resource string : " + resourceString);
		}

		// checking allow at the site level
		if (unlockCheck(SECURE_ADD_ASSIGNMENT, resourceString)) return true;

		// if not, see if the user has any groups to which adds are allowed
		return (!getGroupsAllowAddAssignment(context).isEmpty());
	}

	/**
	 * @inheritDoc
	 */
	public boolean allowAddSiteAssignment(String context)
	{
		// check for assignments that will be site-wide:
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context  + Entity.SEPARATOR;

		
		{
			M_log.warn(this + " allowAddSiteAssignment with resource string : " + resourceString);
		}

		// check security on the channel (throws if not permitted)
		return unlockCheck(SECURE_ADD_ASSIGNMENT, resourceString);
	}

	/**
	 * @inheritDoc
	 */
	public boolean allowAllGroups(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;

		
		{
			M_log.warn(this + " allowAllGroups with resource string : " + resourceString);
		}

		// checking all.groups
		if (unlockCheck(SECURE_ALL_GROUPS, resourceString)) return true;

		// if not
		return false;
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection getGroupsAllowAddAssignment(String context)
	{
		return getGroupsAllowFunction(SECURE_ADD_ASSIGNMENT, context, null);
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection getGroupsAllowGradeAssignment(String context, String assignmentReference)
	{
		Collection rv = new Vector();
		if (allowGradeSubmission(assignmentReference))
		{
			// only if the user is allowed to group at all
			Collection allAllowedGroups = getGroupsAllowFunction(SECURE_GRADE_ASSIGNMENT_SUBMISSION, context, null);
			try
			{
				Assignment a = getAssignment(assignmentReference);
				if (a.getAccess() == Assignment.AssignmentAccess.SITE)
				{
					// for site-scope assignment, return all groups
					rv = allAllowedGroups;
				}
				else
				{
					Collection aGroups = a.getGroups();
					// for grouped assignment, return only those also allowed for grading
					for (Iterator i = allAllowedGroups.iterator(); i.hasNext();)
					{
						Group g = (Group) i.next();
						if (aGroups.contains(g.getReference()))
						{
							rv.add(g);
						}
					}
				}
			}
			catch (Exception e)
			{
				M_log.info(this + " getGroupsAllowGradeAssignment " + e.getMessage() + assignmentReference);
			}
		}
			
		return rv;
	}

	/** 
	 * @inherit
	 */
	public boolean allowGetAssignment(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;

		
		{
			M_log.warn(this + " allowGetAssignment with resource string : " + resourceString);
		}

		return unlockCheck(SECURE_ACCESS_ASSIGNMENT, resourceString);
	}

	/**
	 * @inheritDoc
	 */
	public Collection getGroupsAllowGetAssignment(String context)
	{
		return getGroupsAllowFunction(SECURE_ACCESS_ASSIGNMENT, context, null);
	}
	
	// for specified user
	private Collection getGroupsAllowGetAssignment(String context, String userId)
	{
		return getGroupsAllowFunction(SECURE_ACCESS_ASSIGNMENT, context, userId);
	}

	/**
	 * Check permissions for updateing an Assignment.
	 * 
	 * @param assignmentReference -
	 *        The Assignment's reference.
	 * @return True if the current User is allowed to update the Assignment, false if not.
	 */
	public boolean allowUpdateAssignment(String assignmentReference)
	{
		M_log.warn(this + " allowUpdateAssignment with resource string : " + assignmentReference);

		return unlockCheck(SECURE_UPDATE_ASSIGNMENT, assignmentReference);
	}

	/**
	 * Check permissions for removing an Assignment.
	 * 
	 * @return True if the current User is allowed to remove the Assignment, false if not.
	 */
	public boolean allowRemoveAssignment(String assignmentReference)
	{
		M_log.warn(this + " allowRemoveAssignment " + assignmentReference);

		// check security (throws if not permitted)
		return unlockCheck(SECURE_REMOVE_ASSIGNMENT, assignmentReference);
	}

	/**
	 * @inheritDoc
	 */
	public Collection getGroupsAllowRemoveAssignment(String context)
	{
		return getGroupsAllowFunction(SECURE_REMOVE_ASSIGNMENT, context, null);
	}

	/**
	 * Get the groups of this channel's contex-site that the end user has permission to "function" in.
	 * 
	 * @param function
	 *        The function to check
	 */
	protected Collection getGroupsAllowFunction(String function, String context, String userId)
	{	
		Collection rv = new Vector();
		try
		{
			// get the site groups
			Site site = SiteService.getSite(context);
			Collection groups = site.getGroups();

			if (SecurityService.isSuperUser())
			{
				// for super user, return all groups
				return groups;
			}
			else if (userId == null)
			{
				// for current session user
				userId = SessionManager.getCurrentSessionUserId();
			}
			
			// if the user has SECURE_ALL_GROUPS in the context (site), select all site groups
			if (AuthzGroupService.isAllowed(userId, SECURE_ALL_GROUPS, SiteService.siteReference(context)) && unlockCheck(function, SiteService.siteReference(context)))
			{
				return groups;
			}

			// otherwise, check the groups for function

			// get a list of the group refs, which are authzGroup ids
			Collection groupRefs = new Vector();
			for (Iterator i = groups.iterator(); i.hasNext();)
			{
				Group group = (Group) i.next();
				groupRefs.add(group.getReference());
			}

			// ask the authzGroup service to filter them down based on function
			groupRefs = AuthzGroupService.getAuthzGroupsIsAllowed(userId,
					function, groupRefs);

			// pick the Group objects from the site's groups to return, those that are in the groupRefs list
			for (Iterator i = groups.iterator(); i.hasNext();)
			{
				Group group = (Group) i.next();
				if (groupRefs.contains(group.getReference()))
				{
					rv.add(group);
				}
			}
		}
		catch (IdUnusedException e)
		{
			M_log.warn(this + " getGroupsAllowFunction idunused :" + context + " : " + e.getMessage());
		}

		return rv;
		
	}

	/** ***********************************************check permissions for AssignmentContent object ******************************************* */
	/**
	 * Check permissions for get AssignmentContent
	 * 
	 * @param contentReference -
	 *        The AssignmentContent reference.
	 * @return True if the current User is allowed to access the AssignmentContent, false if not.
	 */
	public boolean allowGetAssignmentContent(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "c" + Entity.SEPARATOR + context + Entity.SEPARATOR;

		
		{
			M_log.warn(this + " allowGetAssignmentContent with resource string : " + resourceString);
		}

		// check security (throws if not permitted)
		return unlockCheck(SECURE_ACCESS_ASSIGNMENT_CONTENT, resourceString);
	}

	/**
	 * Check permissions for updating AssignmentContent
	 * 
	 * @param contentReference -
	 *        The AssignmentContent reference.
	 * @return True if the current User is allowed to update the AssignmentContent, false if not.
	 */
	public boolean allowUpdateAssignmentContent(String contentReference)
	{
		
			M_log.warn(this + " allowUpdateAssignmentContent with resource string : " + contentReference);

		// check security (throws if not permitted)
		return unlockCheck(SECURE_UPDATE_ASSIGNMENT_CONTENT, contentReference);
	}

	/**
	 * Check permissions for adding an AssignmentContent.
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return True if the current User is allowed to add an AssignmentContent, false if not.
	 */
	public boolean allowAddAssignmentContent(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "c" + Entity.SEPARATOR + context + Entity.SEPARATOR;
		M_log.warn(this + "allowAddAssignmentContent with resource string : " + resourceString);

		// check security (throws if not permitted)
		if (unlockCheck(SECURE_ADD_ASSIGNMENT_CONTENT, resourceString)) return true;
		
		// if not, see if the user has any groups to which adds are allowed
		return (!getGroupsAllowAddAssignment(context).isEmpty());
	}

	/**
	 * Check permissions for remove the AssignmentContent
	 * 
	 * @param contentReference -
	 *        The AssignmentContent reference.
	 * @return True if the current User is allowed to remove the AssignmentContent, false if not.
	 */
	public boolean allowRemoveAssignmentContent(String contentReference)
	{
		
			M_log.warn(this + " allowRemoveAssignmentContent with referece string : " + contentReference);

		// check security (throws if not permitted)
		return unlockCheck(SECURE_REMOVE_ASSIGNMENT_CONTENT, contentReference);
	}

	/**
	 * Check permissions for add AssignmentSubmission
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return True if the current User is allowed to add an AssignmentSubmission, false if not.
	 */
	public boolean allowAddSubmission(String context)
	{
		// check security (throws if not permitted)
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "s" + Entity.SEPARATOR + context + Entity.SEPARATOR;

		M_log.warn(this + " allowAddSubmission with resource string : " + resourceString);

		return unlockCheck(SECURE_ADD_ASSIGNMENT_SUBMISSION, resourceString);
	}
	
	/**
	 * Get the list of Users who can do certain function for this assignment
	 * @inheritDoc
	 */
	public List allowAssignmentFunctionUsers(String assignmentReference, String function)
	{
		List rv = new Vector();
		
		rv = SecurityService.unlockUsers(function, assignmentReference);
		
		// get the list of users who have SECURE_ALL_GROUPS
		List allGroupUsers = new Vector();
		try
		{
			String contextRef = SiteService.siteReference(getAssignment(assignmentReference).getContext());
			allGroupUsers = SecurityService.unlockUsers(SECURE_ALL_GROUPS, contextRef);
			// remove duplicates
			allGroupUsers.removeAll(rv);
		}
		catch (Exception e)
		{
			M_log.warn(this + "allowAssignmentFunctionUsers " + e.getMessage() + " assignmentReference=" + assignmentReference + " function=" + function);
		}
		
		// combine two lists together
		rv.addAll(allGroupUsers);
		
		return rv;
	}
	
	/**
	 * Get the List of Users who can addSubmission() for this assignment.
	 * 
	 * @param assignmentReference -
	 *        a reference to an assignment
	 * @return the List (User) of users who can addSubmission() for this assignment.
	 */
	public List allowAddSubmissionUsers(String assignmentReference)
	{
		return allowAssignmentFunctionUsers(assignmentReference, SECURE_ADD_ASSIGNMENT_SUBMISSION);

	} // allowAddSubmissionUsers
	
	/**
	 * Get the List of Users who can grade submission for this assignment.
	 * 
	 * @param assignmentReference -
	 *        a reference to an assignment
	 * @return the List (User) of users who can grade submission for this assignment.
	 */
	public List allowGradeAssignmentUsers(String assignmentReference)
	{
		return allowAssignmentFunctionUsers(assignmentReference, SECURE_GRADE_ASSIGNMENT_SUBMISSION);

	} // allowGradeAssignmentUsers
	
	/**
	 * @inheritDoc
	 * @param context
	 * @return
	 */
	public List allowAddAnySubmissionUsers(String context)
	{
		List rv = new Vector();
		
		try
		{
			AuthzGroup group = AuthzGroupService.getAuthzGroup(SiteService.siteReference(context));
			
			// get the roles which are allowed for submission but not for all_site control
			Set rolesAllowSubmission = group.getRolesIsAllowed(SECURE_ADD_ASSIGNMENT_SUBMISSION);
			Set rolesAllowAllSite = group.getRolesIsAllowed(SECURE_ALL_GROUPS);
			rolesAllowSubmission.removeAll(rolesAllowAllSite);
			
			for (Iterator iRoles = rolesAllowSubmission.iterator(); iRoles.hasNext(); )
			{
				rv.addAll(group.getUsersHasRole((String) iRoles.next()));
			}
		}
		catch (Exception e)
		{
			M_log.warn(this + " allowAddAnySubmissionUsers " + e.getMessage() + " context=" + context);
		}
		
		return rv;
		
	}

	/**
	 * Get the List of Users who can add assignment
	 * 
	 * @param assignmentReference -
	 *        a reference to an assignment
	 * @return the List (User) of users who can addSubmission() for this assignment.
	 */
	public List allowAddAssignmentUsers(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;
		
		{
			M_log.warn(this + " allowAddAssignmentUsers with resource string : " + resourceString);
			M_log.warn("                                    	context string : " + context);
		}
		return SecurityService.unlockUsers(SECURE_ADD_ASSIGNMENT, resourceString);

	} // allowAddAssignmentUsers

	/**
	 * Check permissions for accessing a Submission.
	 * 
	 * @param submissionReference -
	 *        The Submission's reference.
	 * @return True if the current User is allowed to get the AssignmentSubmission, false if not.
	 */
	public boolean allowGetSubmission(String submissionReference)
	{
		M_log.warn(this + " allowGetSubmission with resource string : " + submissionReference);

		return unlockCheck2(SECURE_ACCESS_ASSIGNMENT_SUBMISSION, SECURE_ACCESS_ASSIGNMENT, submissionReference);
	}

	/**
	 * Check permissions for updating Submission.
	 * 
	 * @param submissionReference -
	 *        The Submission's reference.
	 * @return True if the current User is allowed to update the AssignmentSubmission, false if not.
	 */
	public boolean allowUpdateSubmission(String submissionReference)
	{
		M_log.warn(this + " allowUpdateSubmission with resource string : " + submissionReference);

		return unlockCheck2(SECURE_UPDATE_ASSIGNMENT_SUBMISSION, SECURE_UPDATE_ASSIGNMENT, submissionReference);
	}

	/**
	 * Check permissions for remove Submission
	 * 
	 * @param submissionReference -
	 *        The Submission's reference.
	 * @return True if the current User is allowed to remove the AssignmentSubmission, false if not.
	 */
	public boolean allowRemoveSubmission(String submissionReference)
	{
		M_log.warn(this + " allowRemoveSubmission with resource string : " + submissionReference);

		// check security (throws if not permitted)
		return unlockCheck(SECURE_REMOVE_ASSIGNMENT_SUBMISSION, submissionReference);
	}

	public boolean allowGradeSubmission(String assignmentReference)
	{
		
		{
			M_log.warn(this + " allowGradeSubmission with resource string : " + assignmentReference);
		}
		return unlockCheck(SECURE_GRADE_ASSIGNMENT_SUBMISSION, assignmentReference);
	}

	/**
	 * Access the grades spreadsheet for the reference, either for an assignment or all assignments in a context.
	 * 
	 * @param ref
	 *        The reference, either to a specific assignment, or just to an assignment context.
	 * @return The grades spreadsheet bytes.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public byte[] getGradesSpreadsheet(String ref) throws IdUnusedException, PermissionException
	{
		String typeGradesString = new String(REF_TYPE_GRADES + Entity.SEPARATOR);
		String context = ref.substring(ref.indexOf(typeGradesString) + typeGradesString.length());

		// get site title for display purpose
		String siteTitle = "";
		try
		{
			Site s = SiteService.getSite(context);
			siteTitle = s.getTitle();
		}
		catch (Exception e)
		{
			// ignore exception
			M_log.warn(this + ":getGradesSpreadsheet cannot get site context=" + context + e.getMessage());
		}
		
		// does current user allowed to grade any assignment?
		boolean allowGradeAny = false;
		List assignmentsList = getListAssignmentsForContext(context);
		for (int iAssignment = 0; !allowGradeAny && iAssignment<assignmentsList.size(); iAssignment++)
		{
			if (allowGradeSubmission(((Assignment) assignmentsList.get(iAssignment)).getReference()))
			{
				allowGradeAny = true;
			}
		}
		
		if (!allowGradeAny)
		{
			// not permitted to download the spreadsheet
			return null;
		}
		else
		{
			short rowNum = 0;
			HSSFWorkbook wb = new HSSFWorkbook();
			HSSFSheet sheet = wb.createSheet(Validator.escapeZipEntry(siteTitle));
	
			// Create a row and put some cells in it. Rows are 0 based.
			HSSFRow row = sheet.createRow(rowNum++);
	
			row.createCell((short) 0).setCellValue(rb.getString("download.spreadsheet.title"));
	
			// empty line
			row = sheet.createRow(rowNum++);
			row.createCell((short) 0).setCellValue("");
	
			// site title
			row = sheet.createRow(rowNum++);
			row.createCell((short) 0).setCellValue(rb.getString("download.spreadsheet.site") + siteTitle);
	
			// download time
			row = sheet.createRow(rowNum++);
			row.createCell((short) 0).setCellValue(
					rb.getString("download.spreadsheet.date") + TimeService.newTime().toStringLocalFull());
	
			// empty line
			row = sheet.createRow(rowNum++);
			row.createCell((short) 0).setCellValue("");
	
			HSSFCellStyle style = wb.createCellStyle();
	
			// this is the header row number
			short headerRowNumber = rowNum;
			// set up the header cells
			row = sheet.createRow(rowNum++);
			short cellNum = 0;
			
			// user enterprise id column
			HSSFCell cell = row.createCell(cellNum++);
			cell.setCellStyle(style);
			cell.setCellValue(rb.getString("download.spreadsheet.column.name"));
	
			// user name column
			cell = row.createCell(cellNum++);
			cell.setCellStyle(style);
			cell.setCellValue(rb.getString("download.spreadsheet.column.userid"));
			
			// starting from this row, going to input user data
			Iterator assignments = new SortedIterator(assignmentsList.iterator(), new AssignmentComparator("duedate", "true"));
	
			// site members excluding those who can add assignments
			List members = new Vector();
			// hashtable which stores the Excel row number for particular user
			Hashtable user_row = new Hashtable();
			
			List allowAddAnySubmissionUsers = allowAddAnySubmissionUsers(context);
			for (Iterator iUserIds = new SortedIterator(allowAddAnySubmissionUsers.iterator(), new AssignmentComparator("sortname", "true")); iUserIds.hasNext();)
			{
				String userId = (String) iUserIds.next();
				try
				{
					User u = UserDirectoryService.getUser(userId);
					members.add(u);
					// create the column for user first
					row = sheet.createRow(rowNum);
					// update user_row Hashtable
					user_row.put(u.getId(), new Integer(rowNum));
					// increase row
					rowNum++;
					// put user displayid and sortname in the first two cells
					cellNum = 0;
					row.createCell(cellNum++).setCellValue(u.getSortName());
					row.createCell(cellNum).setCellValue(u.getDisplayId());
				}
				catch (Exception e)
				{
					M_log.warn(this + " getGradesSpreadSheet " + e.getMessage() + " userId = " + userId);
				}
			}
				
			int index = 0;
			// the grade data portion starts from the third column, since the first two are used for user's display id and sort name
			while (assignments.hasNext())
			{
				Assignment a = (Assignment) assignments.next();
				
				int assignmentType = a.getContent().getTypeOfGrade();
				
				// for column header, check allow grade permission based on each assignment
				if(!a.getDraft() && allowGradeSubmission(a.getReference()))
				{
					// put in assignment title as the column header
					rowNum = headerRowNumber;
					row = sheet.getRow(rowNum++);
					cellNum = (short) (index + 2);
					cell = row.createCell(cellNum); // since the first two column is taken by student id and name
					cell.setCellStyle(style);
					cell.setCellValue(a.getTitle());
					
					for (int loopNum = 0; loopNum < members.size(); loopNum++)
					{
						// prepopulate the column with the "no submission" string
						row = sheet.getRow(rowNum++);
						cell = row.createCell(cellNum);
						cell.setCellType(1);
						cell.setCellValue(rb.getString("listsub.nosub"));
					}

					// begin to populate the column for this assignment, iterating through student list
					for (Iterator sIterator=getSubmissions(a).iterator(); sIterator.hasNext();)
					{
						AssignmentSubmission submission = (AssignmentSubmission) sIterator.next();
						
						String userId = (String) submission.getSubmitterIds().get(0);
						
						if (user_row.containsKey(userId))
						{	
							// find right row
							row = sheet.getRow(((Integer)user_row.get(userId)).intValue());
						
							if (submission.getGraded() && submission.getGradeReleased() && submission.getGrade() != null)
							{
								// graded and released
								if (assignmentType == 3)
								{
									try
									{
										// numeric cell type?
										String grade = submission.getGradeDisplay();
										Float.parseFloat(grade);
			
										// remove the String-based cell first
										cell = row.getCell(cellNum);
										row.removeCell(cell);
										// add number based cell
										cell=row.createCell(cellNum);
										cell.setCellType(0);
										cell.setCellValue(Float.parseFloat(grade));
			
										style = wb.createCellStyle();
										style.setDataFormat(wb.createDataFormat().getFormat("#,##0.0"));
										cell.setCellStyle(style);
									}
									catch (Exception e)
									{
										// if the grade is not numeric, let's make it as String type
										row.removeCell(cell);
										cell=row.createCell(cellNum);
										cell.setCellType(1);
										cell.setCellValue(submission.getGrade());
									}
								}
								else
								{
									// String cell type
									cell = row.getCell(cellNum);
									cell.setCellValue(submission.getGrade());
								}
							}
							else
							{
								// no grade available yet
								cell = row.getCell(cellNum);
								cell.setCellValue("");
							}
						} // if
					}
				}
				
				index++;
				
			}
			
			// output
			Blob b = new Blob();
			try
			{
				wb.write(b.outputStream());
			}
			catch (IOException e)
			{
				M_log.warn(this + " getGradesSpreadsheet Can not output the grade spread sheet for reference= " + ref);
			}
			
			return b.getBytes();
		}

	} // getGradesSpreadsheet

	/**
	 * Access the submissions zip for the assignment reference.
	 * 
	 * @param ref
	 *        The assignment reference.
	 * @return The submissions zip bytes.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public void getSubmissionsZip(OutputStream outputStream, String ref) throws IdUnusedException, PermissionException
 	{
		M_log.warn(this + ": getSubmissionsZip reference=" + ref);

		byte[] rv = null;

		try
		{
			Assignment a = getAssignment(assignmentReferenceFromSubmissionsZipReference(ref));
			String contextString = a.getContext();
			String groupReference = groupReferenceFromSubmissionsZipReference(ref);
			List allSubmissions = getSubmissions(a);
			List submissions = new Vector();
			
			// group or site
			String authzGroupId = "";
			if (groupReference == null)
			{
				// view all groups
				if (allowAllGroups(contextString))
				{
					// if have site level control
					submissions = allSubmissions;
				}
				else
				{
					// iterate through all allowed-grade-group
					Collection gCollection = getGroupsAllowGradeAssignment(contextString, a.getReference());
					// prevent multiple entries
					HashSet userIdSet = new HashSet();
					for (Iterator iGCollection = gCollection.iterator(); iGCollection.hasNext();)
					{
						Group g = (Group) iGCollection.next();
						String gReference = g.getReference();
						try
						{
							AuthzGroup group = AuthzGroupService.getAuthzGroup(gReference);
							Set grants = group.getUsers();
							for (int i = 0; i<allSubmissions.size();i++)
							{
								// see if the submitters is in the group
								AssignmentSubmission s = (AssignmentSubmission) allSubmissions.get(i);
								String submitterId = s.getSubmitterIdString();
								if (!userIdSet.contains(submitterId) && grants.contains(submitterId))
								{
									submissions.add(s);
									userIdSet.add(submitterId);
								}
							}
						}
						catch (Exception ee)
						{
							M_log.info(this + " getSubmissionsZip " + ee.getMessage() + " group reference=" + gReference);
						}
					}
					
				}
			}
			else
			{
				// just one group
				try
				{
					AuthzGroup group = AuthzGroupService.getAuthzGroup(groupReference);
					Set grants = group.getUsers();
					for (int i = 0; i<allSubmissions.size();i++)
					{
						// see if the submitters is in the group
						AssignmentSubmission s = (AssignmentSubmission) allSubmissions.get(i);
						if (grants.contains(s.getSubmitterIdString()))
						{
							submissions.add(s);
						}
					}
				}
				catch (Exception ee)
				{
					M_log.info(this +  " getSubmissionsZip " + ee.getMessage() + " group reference=" + groupReference);
				}
				
			}

			StringBuilder exceptionMessage = new StringBuilder();

			if (allowGradeSubmission(a.getReference()))
			{
				zipSubmissions(a.getReference(), a.getTitle(), a.getContent().getTypeOfGradeString(a.getContent().getTypeOfGrade()), a.getContent().getTypeOfSubmission(), submissions.iterator(), outputStream, exceptionMessage);

				if (exceptionMessage.length() > 0)
				{
					// log any error messages
					
						M_log.warn(this + " getSubmissionsZip ref=" + ref + exceptionMessage.toString());
				}
			}
		}
		catch (IdUnusedException e)
		{
			
				M_log.warn(this + "getSubmissionsZip -IdUnusedException Unable to get assignment " + ref);
			throw new IdUnusedException(ref);
		}
		catch (PermissionException e)
		{
			M_log.warn(this + " getSubmissionsZip -PermissionException Not permitted to get assignment " + ref);
			throw new PermissionException(SessionManager.getCurrentSessionUserId(), SECURE_ACCESS_ASSIGNMENT, ref);
		}

	} // getSubmissionsZip

	protected void zipSubmissions(String assignmentReference, String assignmentTitle, String gradeTypeString, int typeOfSubmission, Iterator submissions, OutputStream outputStream, StringBuilder exceptionMessage) 
	{
		try
		{
			ZipOutputStream out = new ZipOutputStream(outputStream);

			// create the folder structor - named after the assignment's title
			String root = Validator.escapeZipEntry(assignmentTitle) + Entity.SEPARATOR;

			String submittedText = "";
			if (!submissions.hasNext())
			{
				exceptionMessage.append("There is no submission yet. ");
			}
			
			// the buffer used to store grade information
			StringBuilder gradesBuffer = new StringBuilder(assignmentTitle + "," + gradeTypeString + "\n\n");
			gradesBuffer.append(rb.getString("grades.id") + "," + rb.getString("grades.eid") + "," + rb.getString("grades.lastname") + "," + rb.getString("grades.firstname") + "," + rb.getString("grades.grade") + "\n");

			// allow add assignment members
			List allowAddSubmissionUsers = allowAddSubmissionUsers(assignmentReference);
			
			// Create the ZIP file
			String submittersName = "";
			int count = 1;
			while (submissions.hasNext())
			{
				AssignmentSubmission s = (AssignmentSubmission) submissions.next();
				
				if (s.getSubmitted())
				{
					// get the submission user id and see if the user is still in site
					String userId = (String) s.getSubmitterIds().get(0);
					try
					{
						User u = UserDirectoryService.getUser(userId);
						if (allowAddSubmissionUsers.contains(u))
						{
							count = 1;
							submittersName = root;
							
							User[] submitters = s.getSubmitters();
							String submittersString = "";
							for (int i = 0; i < submitters.length; i++)
							{
								if (i > 0)
								{
									submittersString = submittersString.concat("; ");
								}
								String fullName = submitters[i].getSortName();
								// in case the user doesn't have first name or last name
								if (fullName.indexOf(",") == -1)
								{
									fullName=fullName.concat(",");
								}
								submittersString = submittersString.concat(fullName);
								// add the eid to the end of it to guarantee folder name uniqness
								submittersString = submittersString + "(" + submitters[i].getEid() + ")";
								gradesBuffer.append(submitters[i].getDisplayId() + "," + submitters[i].getEid() + "," + fullName + "," + s.getGradeDisplay() + "\n");
							}
							
							if (StringUtil.trimToNull(submittersString) != null)
							{
								submittersName = submittersName.concat(StringUtil.trimToNull(submittersString));
								submittedText = s.getSubmittedText();
		
								boolean added = false;
								while (!added)
								{
									try
									{
										submittersName = submittersName.concat("/");
										// create the folder structure - named after the submitter's name
										if (typeOfSubmission != Assignment.ATTACHMENT_ONLY_ASSIGNMENT_SUBMISSION)
										{
											// create the text file only when a text submission is allowed
											ZipEntry textEntry = new ZipEntry(submittersName + submittersString + "_submissionText" + ZIP_SUBMITTED_TEXT_FILE_TYPE);
											out.putNextEntry(textEntry);
											byte[] text = submittedText.getBytes();
											out.write(text);
											textEntry.setSize(text.length);
											out.closeEntry();
										}
										
										// record submission timestamp
										if (s.getSubmitted() && s.getTimeSubmitted() != null)
										{
											ZipEntry textEntry = new ZipEntry(submittersName + "timestamp.txt");
											out.putNextEntry(textEntry);
											byte[] b = (s.getTimeSubmitted().toString()).getBytes();
											out.write(b);
											textEntry.setSize(b.length);
											out.closeEntry();
										}
										// create a feedbackText file into zip
										ZipEntry fTextEntry = new ZipEntry(submittersName + "feedbackText.html");
										out.putNextEntry(fTextEntry);
										byte[] fText = s.getFeedbackText().getBytes();
										out.write(fText);
										fTextEntry.setSize(fText.length);
										out.closeEntry();
										
										// the comments.txt file to show instructor's comments
										ZipEntry textEntry = new ZipEntry(submittersName + "comments" + ZIP_COMMENT_FILE_TYPE);
										out.putNextEntry(textEntry);
										byte[] b = FormattedText.encodeUnicode(s.getFeedbackComment()).getBytes();
										out.write(b);
										textEntry.setSize(b.length);
										out.closeEntry();
										
										// create an attachment folder for the feedback attachments
										String feedbackSubAttachmentFolder = submittersName + rb.getString("download.feedback.attachment") + "/";
										ZipEntry feedbackSubAttachmentFolderEntry = new ZipEntry(feedbackSubAttachmentFolder);
										out.putNextEntry(feedbackSubAttachmentFolderEntry);
										out.closeEntry();
		
										// create a attachment folder for the submission attachments
										String sSubAttachmentFolder = submittersName + rb.getString("download.submission.attachment") + "/";
										ZipEntry sSubAttachmentFolderEntry = new ZipEntry(sSubAttachmentFolder);
										out.putNextEntry(sSubAttachmentFolderEntry);
										out.closeEntry();
										// add all submission attachment into the submission attachment folder
										zipAttachments(out, submittersName, sSubAttachmentFolder, s.getSubmittedAttachments());
										// add all feedback attachment folder
										zipAttachments(out, submittersName, feedbackSubAttachmentFolder, s.getFeedbackAttachments());
		
										added = true;
									}
									catch (IOException e)
									{
										exceptionMessage.append("Can not establish the IO to create zip file for user "
												+ submittersName);
										M_log.warn(this + " zipSubmissions --IOException unable to create the zip file for user"
												+ submittersName);
										submittersName = submittersName.substring(0, submittersName.length() - 1) + "_" + count++;
									}
								}	//while
							} // if
						}
					}
					catch (Exception e)
					{
						M_log.warn(this + " zipSubmissions " + e.getMessage() + " userId = " + userId);
					}
				} // if the user is still in site

			} // while -- there is submission

			// create a grades.csv file into zip
			ZipEntry gradesCSVEntry = new ZipEntry(root + "grades.csv");
			out.putNextEntry(gradesCSVEntry);
			byte[] grades = gradesBuffer.toString().getBytes();
			out.write(grades);
			gradesCSVEntry.setSize(grades.length);
			out.closeEntry();
			
			// Complete the ZIP file
			out.finish();
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			exceptionMessage.append("Can not establish the IO to create zip file. ");
			M_log.warn(this + " zipSubmissions IOException unable to create the zip file for assignment "
					+ assignmentTitle);
		}
	}



	private void zipAttachments(ZipOutputStream out, String submittersName, String sSubAttachmentFolder, List attachments) {
		int attachedUrlCount = 0;
		for (int j = 0; j < attachments.size(); j++)
		{
			Reference r = (Reference) attachments.get(j);
			try
			{
				ContentResource resource = m_contentHostingService.getResource(r.getId());

				String contentType = resource.getContentType();
				
				ResourceProperties props = r.getProperties();
				String displayName = props.getPropertyFormatted(props.getNamePropDisplayName());

				// for URL content type, encode a redirect to the body URL
				if (contentType.equalsIgnoreCase(ResourceProperties.TYPE_URL))
				{
					displayName = "attached_URL_" + attachedUrlCount;
					attachedUrlCount++;
				}

				// buffered stream input
				InputStream content = resource.streamContent();
				byte data[] = new byte[1024 * 10];
				BufferedInputStream bContent = new BufferedInputStream(content, data.length);
				
				ZipEntry attachmentEntry = new ZipEntry(sSubAttachmentFolder + displayName);
				out.putNextEntry(attachmentEntry);
				int bCount = -1;
				while ((bCount = bContent.read(data, 0, data.length)) != -1) 
				{
					out.write(data, 0, bCount);
				}
				out.closeEntry();
				content.close();
			}
			catch (PermissionException e)
			{
				M_log.warn(this + " zipAttachments--PermissionException submittersName="
						+ submittersName + " attachment reference=" + r);
			}
			catch (IdUnusedException e)
			{
				M_log.warn(this + " zipAttachments--IdUnusedException submittersName="
						+ submittersName + " attachment reference=" + r);
			}
			catch (TypeException e)
			{
				M_log.warn(this + " zipAttachments--TypeException: submittersName="
						+ submittersName + " attachment reference=" + r);
			}
			catch (IOException e)
			{
				M_log.warn(this + " zipAttachments--IOException: Problem in creating the attachment file: submittersName="
								+ submittersName + " attachment reference=" + r);
			}
			catch (ServerOverloadException e)
			{
				M_log.warn(this + " zipAttachments--ServerOverloadException: submittersName="
						+ submittersName + " attachment reference=" + r);
			}
		} // for
	}

	/**
	 * Get the string to form an assignment grade spreadsheet
	 * 
	 * @param context
	 *        The assignment context String
	 * @param assignmentId
	 *        The id for the assignment object; when null, indicates all assignment in that context
	 */
	public String gradesSpreadsheetReference(String context, String assignmentId)
	{
		// based on all assignment in that context
		String s = REFERENCE_ROOT + Entity.SEPARATOR + REF_TYPE_GRADES + Entity.SEPARATOR + context;
		if (assignmentId != null)
		{
			// based on the specified assignment only
			s = s.concat(Entity.SEPARATOR + assignmentId);
		}

		return s;

	} // gradesSpreadsheetReference

	/**
	 * Get the string to form an assignment submissions zip file
	 * 
	 * @param context
	 *        The assignment context String
	 * @param assignmentReference
	 *        The reference for the assignment object;
	 */
	public String submissionsZipReference(String context, String assignmentReference)
	{
		// based on the specified assignment
		return REFERENCE_ROOT + Entity.SEPARATOR + REF_TYPE_SUBMISSIONS + Entity.SEPARATOR + context + Entity.SEPARATOR
				+ assignmentReference;

	} // submissionsZipReference

	/**
	 * Decode the submissionsZipReference string to get the assignment reference String
	 * 
	 * @param sReference
	 *        The submissionZipReference String
	 * @return The assignment reference String
	 */
	private String assignmentReferenceFromSubmissionsZipReference(String sReference)
	{
		// remove the String part relating to submissions zip reference
		if (sReference.indexOf(Entity.SEPARATOR +"site") == -1)
		{
			return sReference.substring(sReference.lastIndexOf(Entity.SEPARATOR + "assignment"));
		}
		else
		{
			return sReference.substring(sReference.lastIndexOf(Entity.SEPARATOR + "assignment"), sReference.indexOf(Entity.SEPARATOR +"site"));
		}

	} // assignmentReferenceFromSubmissionsZipReference
	
	/**
	 * Decode the submissionsZipReference string to get the group reference String
	 * 
	 * @param sReference
	 *        The submissionZipReference String
	 * @return The group reference String
	 */
	private String groupReferenceFromSubmissionsZipReference(String sReference)
	{
		// remove the String part relating to submissions zip reference
		if (sReference.indexOf(Entity.SEPARATOR +"site") != -1)
		{
			return sReference.substring(sReference.lastIndexOf(Entity.SEPARATOR + "site"));
		}
		else
		{
			return null;
		}

	} // assignmentReferenceFromSubmissionsZipReference

	/**********************************************************************************************************************************************************************************************************************************************************
	 * ResourceService implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * {@inheritDoc}
	 */
	public String getLabel()
	{
		return "assignment";
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean willArchiveMerge()
	{
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public HttpAccess getHttpAccess()
	{
		return new HttpAccess()
		{
			public void handleAccess(HttpServletRequest req, HttpServletResponse res, Reference ref,
					Collection copyrightAcceptedRefs) throws EntityPermissionException, EntityNotDefinedException,
					EntityAccessOverloadException, EntityCopyrightException
			{
				if (SessionManager.getCurrentSessionUserId() == null)
				{
					// fail the request, user not logged in yet.
				}
				else
				{
					try
					{
						if (REF_TYPE_SUBMISSIONS.equals(ref.getSubType()))
						{
							res.setContentType("application/zip");
							res.setHeader("Content-Disposition", "attachment; filename = bulk_download.zip");
							 
							OutputStream out = null;
							try
							{
							    out = res.getOutputStream();
							    
							    // get the submissions zip blob
							    getSubmissionsZip(out, ref.getReference());
							    
							    out.flush();
							    out.close();
							}
							catch (Throwable ignore)
							{
							    M_log.error(this + " getHttpAccess handleAccess " + ignore.getMessage() + " ref=" + ref.getReference());
							}
							finally
							{
							    if (out != null)
							    {
							        try
							        {
							            out.close();
							        }
							        catch (Throwable ignore)
							        {
							        	M_log.warn(this + ": handleAccess 1 " + ignore.getMessage());
							        }
							    }
							}
						}
	
						else if (REF_TYPE_GRADES.equals(ref.getSubType()))
						{
							// get the grades spreadsheet blob
							byte[] spreadsheet = getGradesSpreadsheet(ref.getReference());
	
							if (spreadsheet != null)
							{
								res.setContentType("application/vnd.ms-excel");
								res.setHeader("Content-Disposition", "attachment; filename = export_grades_file.xls");
	
								OutputStream out = null;
								try
								{
									out = res.getOutputStream();
									out.write(spreadsheet);
									out.flush();
									out.close();
								}
								catch (Throwable ignore)
								{
									M_log.warn(this + ": handleAccess 2 " + ignore.getMessage());
								}
								finally
								{
									if (out != null)
									{
										try
										{
											out.close();
										}
										catch (Throwable ignore)
										{
											M_log.warn(this + ": handleAccess 3 " + ignore.getMessage());
										}
									}
								}
							}
						}
						else
						{
							throw new IdUnusedException(ref.getReference());
						}
					}
					catch (Throwable t)
					{
						throw new EntityNotDefinedException(ref.getReference());
					}
				}
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean parseEntityReference(String reference, Reference ref)
	{
		if (reference.startsWith(REFERENCE_ROOT))
		{
			String id = null;
			String subType = null;
			String container = null;
			String context = null;

			String[] parts = StringUtil.split(reference, Entity.SEPARATOR);
			// we will get null, assignment, [a|c|s|grades|submissions], context, [auid], id

			if (parts.length > 2)
			{
				subType = parts[2];

				if (parts.length > 3)
				{
					// context is the container
					context = parts[3];

					// submissions have the assignment unique id as a container
					if ("s".equals(subType))
					{
						if (parts.length > 5)
						{
							container = parts[4];
							id = parts[5];
						}
					}

					// others don't
					else
					{
						if (parts.length > 4)
						{
							id = parts[4];
						}
					}
				}
			}

			ref.set(APPLICATION_ID, subType, id, container, context);

			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public Entity getEntity(Reference ref)
	{
		Entity rv = null;

		try
		{
			// is it an AssignmentContent object
			if (REF_TYPE_CONTENT.equals(ref.getSubType()))
			{
				rv = getAssignmentContent(ref.getReference());
			}
			// is it an Assignment object
			else if (REF_TYPE_ASSIGNMENT.equals(ref.getSubType()))
			{
				rv = getAssignment(ref.getReference());
			}
			// is it an AssignmentSubmission object
			else if (REF_TYPE_SUBMISSION.equals(ref.getSubType()))
			{
				rv = getSubmission(ref.getReference());
			}
			else
				M_log.warn(this + "getEntity(): unknown message ref subtype: " + ref.getSubType() + " in ref: " + ref.getReference());
		}
		catch (PermissionException e)
		{
			M_log.warn(this + "getEntity(): " + e + " ref=" + ref.getReference());
		}
		catch (IdUnusedException e)
		{
			M_log.warn(this + "getEntity(): " + e + " ref=" + ref.getReference());
		}
		catch (NullPointerException e)
		{
			M_log.warn(this + "getEntity(): " + e + " ref=" + ref.getReference());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getEntityAuthzGroups(Reference ref, String userId)
	{
		Collection rv = new Vector();

		// for AssignmentService assignments:
		// if access set to SITE, use the assignment and site authzGroups.
		// if access set to GROUPED, use the assignment, and the groups, but not the site authzGroups.
		// if the user has SECURE_ALL_GROUPS in the context, ignore GROUPED access and treat as if SITE

		try
		{
			// for assignment
			if (REF_TYPE_ASSIGNMENT.equals(ref.getSubType()))
			{
				// assignment
				rv.add(ref.getReference());
				
				boolean grouped = false;
				Collection groups = null;

				// check SECURE_ALL_GROUPS - if not, check if the assignment has groups or not
				// TODO: the last param needs to be a ContextService.getRef(ref.getContext())... or a ref.getContextAuthzGroup() -ggolden
				if ((userId == null) || ((!SecurityService.isSuperUser(userId)) && (!AuthzGroupService.isAllowed(userId, SECURE_ALL_GROUPS, SiteService.siteReference(ref.getContext())))))
				{
					// get the channel to get the message to get group information
					// TODO: check for efficiency, cache and thread local caching usage -ggolden
					if (ref.getId() != null)
					{
						Assignment a = findAssignment(ref.getReference());
						if (a != null)
						{
							grouped = Assignment.AssignmentAccess.GROUPED == a.getAccess();
							groups = a.getGroups();
						}
					}
				}

				if (grouped)
				{
					// groups
					rv.addAll(groups);
				}

				// not grouped
				else
				{
					// site
					ref.addSiteContextAuthzGroup(rv);
				}
			}
			else
			{
				rv.add(ref.getReference());
				
				// for content and submission, use site security setting
				ref.addSiteContextAuthzGroup(rv);
			}
		}
		catch (Throwable e)
		{
			M_log.warn(this + " getEntityAuthzGroups(): " + e.getMessage() + " ref=" + ref.getReference());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntityUrl(Reference ref)
	{
		String rv = null;

		try
		{
			// is it an AssignmentContent object
			if (REF_TYPE_CONTENT.equals(ref.getSubType()))
			{
				AssignmentContent c = getAssignmentContent(ref.getReference());
				rv = c.getUrl();
			}
			// is it an Assignment object
			else if (REF_TYPE_ASSIGNMENT.equals(ref.getSubType()))
			{
				Assignment a = getAssignment(ref.getReference());
				rv = a.getUrl();
			}
			// is it an AssignmentSubmission object
			else if (REF_TYPE_SUBMISSION.equals(ref.getSubType()))
			{
				AssignmentSubmission s = getSubmission(ref.getReference());
				rv = s.getUrl();
			}
			else
				M_log.warn(this + " getEntityUrl(): unknown message ref subtype: " + ref.getSubType() + " in ref: " + ref.getReference());
		}
		catch (PermissionException e)
		{
			M_log.warn(this + "getEntityUrl(): " + e + " ref=" + ref.getReference());
		}
		catch (IdUnusedException e)
		{
			M_log.warn(this + "getEntityUrl(): " + e + " ref=" + ref.getReference());
		}
		catch (NullPointerException e)
		{
			M_log.warn(this + "getEntityUrl(): " + e + " ref=" + ref.getReference());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments)
	{
		// prepare the buffer for the results log
		StringBuilder results = new StringBuilder();

		// String assignRef = assignmentReference(siteId, SiteService.MAIN_CONTAINER);
		results.append("archiving " + getLabel() + " context " + Entity.SEPARATOR + siteId + Entity.SEPARATOR
				+ SiteService.MAIN_CONTAINER + ".\n");

		// start with an element with our very own (service) name
		Element element = doc.createElement(AssignmentService.class.getName());
		((Element) stack.peek()).appendChild(element);
		stack.push(element);

		Iterator assignmentsIterator = getAssignmentsForContext(siteId);

		while (assignmentsIterator.hasNext())
		{
			Assignment assignment = (Assignment) assignmentsIterator.next();

			// archive this assignment
			Element el = assignment.toXml(doc, stack);
			element.appendChild(el);

			// in order to make the assignment.xml have a better structure
			// the content id attribute removed from the assignment node
			// the content will be a child of assignment node
			el.removeAttribute("assignmentcontent");

			// then archive the related content
			AssignmentContent content = (AssignmentContent) assignment.getContent();
			if (content != null)
			{
				Element contentEl = content.toXml(doc, stack);

				// assignment node has already kept the context info
				contentEl.removeAttribute("context");

				// collect attachments
				List atts = content.getAttachments();

				for (int i = 0; i < atts.size(); i++)
				{
					Reference ref = (Reference) atts.get(i);
					// if it's in the attachment area, and not already in the list
					if ((ref.getReference().startsWith("/content/attachment/")) && (!attachments.contains(ref)))
					{
						attachments.add(ref);
					}

					// in order to make assignment.xml has the consistent format with the other xml files
					// move the attachments to be the children of the content, instead of the attributes
					String attributeString = "attachment" + i;
					String attRelUrl = contentEl.getAttribute(attributeString);
					contentEl.removeAttribute(attributeString);
					Element attNode = doc.createElement("attachment");
					attNode.setAttribute("relative-url", attRelUrl);
					contentEl.appendChild(attNode);

				} // for

				// make the content a childnode of the assignment node
				el.appendChild(contentEl);

				Iterator submissionsIterator = getSubmissions(assignment).iterator();
				while (submissionsIterator.hasNext())
				{
					AssignmentSubmission submission = (AssignmentSubmission) submissionsIterator.next();

					// archive this assignment
					Element submissionEl = submission.toXml(doc, stack);
					el.appendChild(submissionEl);

				}
			} // if
		} // while

		stack.pop();

		return results.toString();

	} // archive

	/**
	 * Replace the WT user id with the new qualified id
	 * 
	 * @param el
	 *        The XML element holding the perproties
	 * @param useIdTrans
	 *        The HashMap to track old WT id to new CTools id
	 */
	protected void WTUserIdTrans(Element el, Map userIdTrans)
	{
		NodeList children4 = el.getChildNodes();
		int length4 = children4.getLength();
		for (int i4 = 0; i4 < length4; i4++)
		{
			Node child4 = children4.item(i4);
			if (child4.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element4 = (Element) child4;
				if (element4.getTagName().equals("property"))
				{
					String creatorId = "";
					String modifierId = "";
					if (element4.hasAttribute("CHEF:creator"))
					{
						if ("BASE64".equalsIgnoreCase(element4.getAttribute("enc")))
						{
							creatorId = Xml.decodeAttribute(element4, "CHEF:creator");
						}
						else
						{
							creatorId = element4.getAttribute("CHEF:creator");
						}
						String newCreatorId = (String) userIdTrans.get(creatorId);
						if (newCreatorId != null)
						{
							Xml.encodeAttribute(element4, "CHEF:creator", newCreatorId);
							element4.setAttribute("enc", "BASE64");
						}
					}
					else if (element4.hasAttribute("CHEF:modifiedby"))
					{
						if ("BASE64".equalsIgnoreCase(element4.getAttribute("enc")))
						{
							modifierId = Xml.decodeAttribute(element4, "CHEF:modifiedby");
						}
						else
						{
							modifierId = element4.getAttribute("CHEF:modifiedby");
						}
						String newModifierId = (String) userIdTrans.get(modifierId);
						if (newModifierId != null)
						{
							Xml.encodeAttribute(element4, "CHEF:creator", newModifierId);
							element4.setAttribute("enc", "BASE64");
						}
					}
				}
			}
		}

	} // WTUserIdTrans

	/**
	 * {@inheritDoc}
	 */
	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans,
			Set userListAllowImport)
	{
		// prepare the buffer for the results log
		StringBuilder results = new StringBuilder();

		int count = 0;

		try
		{
			// pass the DOM to get new assignment ids, and adjust attachments
			NodeList children2 = root.getChildNodes();

			int length2 = children2.getLength();
			for (int i2 = 0; i2 < length2; i2++)
			{
				Node child2 = children2.item(i2);
				if (child2.getNodeType() == Node.ELEMENT_NODE)
				{
					Element element2 = (Element) child2;

					if (element2.getTagName().equals("assignment"))
					{
						// a flag showing if continuing merging the assignment
						boolean goAhead = true;
						AssignmentContentEdit contentEdit = null;

						// element2 now - assignment node
						// adjust the id of this assignment
						// String newId = IdManager.createUuid();
						element2.setAttribute("id", IdManager.createUuid());
						element2.setAttribute("context", siteId);

						// cloneNode(false) - no children cloned
						Element el2clone = (Element) element2.cloneNode(false);

						// traverse this assignment node first to check if the person who last modified, has the right role.
						// if no right role, mark the flag goAhead to be false.
						NodeList children3 = element2.getChildNodes();
						int length3 = children3.getLength();
						for (int i3 = 0; i3 < length3; i3++)
						{
							Node child3 = children3.item(i3);
							if (child3.getNodeType() == Node.ELEMENT_NODE)
							{
								Element element3 = (Element) child3;

								// add the properties childnode to the clone of assignment node
								if (element3.getTagName().equals("properties"))
								{
									NodeList children6 = element3.getChildNodes();
									int length6 = children6.getLength();
									for (int i6 = 0; i6 < length6; i6++)
									{
										Node child6 = children6.item(i6);
										if (child6.getNodeType() == Node.ELEMENT_NODE)
										{
											Element element6 = (Element) child6;

											if (element6.getTagName().equals("property"))
											{
												if (element6.getAttribute("name").equalsIgnoreCase("CHEF:modifiedby"))
												{
													if ("BASE64".equalsIgnoreCase(element6.getAttribute("enc")))
													{
														String creatorId = Xml.decodeAttribute(element6, "value");
														if (!userListAllowImport.contains(creatorId)) goAhead = false;
													}
													else
													{
														String creatorId = element6.getAttribute("value");
														if (!userListAllowImport.contains(creatorId)) goAhead = false;
													}
												}
											}
										}
									}
								}
							}
						} // for

						// then, go ahead to merge the content and assignment
						if (goAhead)
						{
							for (int i3 = 0; i3 < length3; i3++)
							{
								Node child3 = children3.item(i3);
								if (child3.getNodeType() == Node.ELEMENT_NODE)
								{
									Element element3 = (Element) child3;

									// add the properties childnode to the clone of assignment node
									if (element3.getTagName().equals("properties"))
									{
										// add the properties childnode to the clone of assignment node
										el2clone.appendChild(element3.cloneNode(true));
									}
									else if (element3.getTagName().equals("content"))
									{
										// element3 now- content node
										// adjust the id of this content
										String newContentId = IdManager.createUuid();
										element3.setAttribute("id", newContentId);
										element3.setAttribute("context", siteId);

										// clone the content node without the children of <properties>
										Element el3clone = (Element) element3.cloneNode(false);

										// update the assignmentcontent id in assignment node
										String assignContentId = "/assignment/c/" + siteId + "/" + newContentId;
										el2clone.setAttribute("assignmentcontent", assignContentId);

										// for content node, process the attachment or properties kids
										NodeList children5 = element3.getChildNodes();
										int length5 = children5.getLength();
										int attCount = 0;
										for (int i5 = 0; i5 < length5; i5++)
										{
											Node child5 = children5.item(i5);
											if (child5.getNodeType() == Node.ELEMENT_NODE)
											{
												Element element5 = (Element) child5;

												// for the node of "properties"
												if (element5.getTagName().equals("properties"))
												{
													// for the file from WT, preform userId translation when needed
													if (!userIdTrans.isEmpty())
													{
														WTUserIdTrans(element3, userIdTrans);
													}
												} // for the node of properties
												el3clone.appendChild(element5.cloneNode(true));

												// for "attachment" children
												if (element5.getTagName().equals("attachment"))
												{
													// map the attachment area folder name
													// filter out the invalid characters in the attachment id
													// map the attachment area folder name
													String oldUrl = element5.getAttribute("relative-url");
													if (oldUrl.startsWith("/content/attachment/" + fromSiteId + "/"))
													{
														String newUrl = "/content/attachment/" + siteId + oldUrl.substring(("/content/attachment" + fromSiteId).length());
														element5.setAttribute("relative-url", Validator.escapeQuestionMark(newUrl));
														
														// transfer attachment, replace the context string and add new attachment if necessary
														newUrl = transferAttachment(fromSiteId, siteId, null, oldUrl.substring("/content".length()));
														element5.setAttribute("relative-url", Validator.escapeQuestionMark(newUrl));
														
														newUrl = (String) attachmentNames.get(oldUrl);
														if (newUrl != null)
														{
															if (newUrl.startsWith("/attachment/"))
																newUrl = "/content".concat(newUrl);

															element5.setAttribute("relative-url", Validator
																	.escapeQuestionMark(newUrl));
														}
													}

													// map any references to this site to the new site id
													else if (oldUrl.startsWith("/content/group/" + fromSiteId + "/"))
													{
														String newUrl = "/content/group/" + siteId
																+ oldUrl.substring(("/content/group/" + fromSiteId).length());
														element5.setAttribute("relative-url", Validator.escapeQuestionMark(newUrl));
													}
													// put the attachment back to the attribute field of content
													// to satisfy the input need of mergeAssignmentContent
													String attachmentString = "attachment" + attCount;
													el3clone.setAttribute(attachmentString, element5.getAttribute("relative-url"));
													attCount++; 

												} // if
											} // if
										} // for

										// create a newassignment content
										contentEdit = mergeAssignmentContent(el3clone);
										commitEdit(contentEdit);
									}
								}
							} // for

 							// when importing, refer to property to determine draft status
							if ("false".equalsIgnoreCase(m_serverConfigurationService.getString("import.importAsDraft")))
							{
								String draftAttribute = el2clone.getAttribute("draft");
								if (draftAttribute.equalsIgnoreCase("true") || draftAttribute.equalsIgnoreCase("false"))
									el2clone.setAttribute("draft", draftAttribute);
								else
									el2clone.setAttribute("draft", "true");
							}
							else
							{
								el2clone.setAttribute("draft", "true");
							}

							// merge in this assignment
							Assignment edit = mergeAssignment(el2clone);
							saveAssignment(edit);

							count++;
						} // if goAhead
					} // if
				} // if
			} // for
		}
		catch (Exception any)
		{
			M_log.warn(this + " merge(): exception: " + any.getMessage() + " siteId=" + siteId + " from site id=" + fromSiteId);
		}

		results.append("merging assignment " + siteId + " (" + count + ") assignments.\n");
		return results.toString();

	} // merge

	/**
	 * {@inheritDoc}
	 */
	public String[] myToolIds()
	{
		String[] toolIds = { "sakai.assignment", "sakai.assignment.grades" };
		return toolIds;
	}

	/**
	 * {@inheritDoc}
	 */
	public void transferCopyEntities(String fromContext, String toContext, List resourceIds)
	{
		// import Assignment objects
		Iterator oAssignments = getAssignmentsForContext(fromContext);
		while (oAssignments.hasNext())
		{
			Assignment oAssignment = (Assignment) oAssignments.next();
			String oAssignmentId = oAssignment.getId();

			boolean toBeImported = true;
			if (resourceIds != null && resourceIds.size() > 0)
			{
				// if there is a list for import assignments, only import those assignments and relative submissions
				toBeImported = false;
				for (int m = 0; m < resourceIds.size() && !toBeImported; m++)
				{
					if (((String) resourceIds.get(m)).equals(oAssignmentId))
					{
						toBeImported = true;
					}
				}
			}

			if (toBeImported)
			{
				Assignment nAssignment = null;

				if (!m_assignmentStorage.check(oAssignmentId))
				{

				}
				else
				{
					try
					{
						// add new assignment
						nAssignment = addAssignment(toContext);
						// attribute
						nAssignment.setAllowAttachments(oAssignment.getAllowAttachments());
						nAssignment.setContext(toContext);
						nAssignment.setGroupProject(oAssignment.getGroupProject());
						nAssignment.setHonorPledge(oAssignment.getHonorPledge());
						nAssignment.setIndividuallyGraded(oAssignment.individuallyGraded());
						nAssignment.setInstructions(oAssignment.getInstructions());
						nAssignment.setMaxGradePoint(oAssignment.getMaxGradePoint());
						nAssignment.setReleaseGrades(oAssignment.releaseGrades());
						nAssignment.setTimeLastModified(oAssignment.getTimeLastModified());
						nAssignment.setTypeOfGrade(oAssignment.getTypeOfGrade());
						nAssignment.setTypeOfSubmission(oAssignment.getTypeOfSubmission());
						// attachment
						List oAttachments = oAssignment.getAttachments();
						List nAttachments = m_entityManager.newReferenceList();
						for (int n = 0; n < oAttachments.size(); n++)
						{
							Reference oAttachmentRef = (Reference) oAttachments.get(n);
							String oAttachmentId = ((Reference) oAttachments.get(n)).getId();
							if (oAttachmentId.indexOf(fromContext) != -1)
							{
								// transfer attachment, replace the context string and add new attachment if necessary
								transferAttachment(fromContext, toContext, nAttachments, oAttachmentId);
							}
							else
							{
								nAttachments.add(oAttachmentRef);
							}
						}
						nAssignment.replaceAttachments(nAttachments);
						
						nAssignment.setCloseTime(oAssignment.getCloseTime());
						nAssignment.setContext(toContext);
						
						// when importing, refer to property to determine draft status
						if ("false".equalsIgnoreCase(m_serverConfigurationService.getString("import.importAsDraft")))
						{
							nAssignment.setDraft(oAssignment.getDraft());
						}
						else
						{
							nAssignment.setDraft(true);
						}
						
						nAssignment.setDropDeadTime(oAssignment.getDropDeadTime());
						nAssignment.setDueTime(oAssignment.getDueTime());
						nAssignment.setOpenTime(oAssignment.getOpenTime());
						nAssignment.setSection(oAssignment.getSection());
						nAssignment.setTitle(oAssignment.getTitle());
						// properties
						ResourcePropertiesEdit p = nAssignment.getPropertiesEdit();
						p.clear();
						p.addAll(oAssignment.getProperties());
						
						// one more touch on the gradebook-integration link
						if (StringUtil.trimToNull(p.getProperty(PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT)) != null)
						{
							// assignments are imported as drafts;
							// mark the integration with "add" for now, later when user posts the assignment, the corresponding assignment will be created in gradebook.
							p.removeProperty(PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
							p.addProperty(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, GRADEBOOK_INTEGRATION_ADD);
						}
						
						// update live properties
						AssignmentUtil.addLiveProperties(p);
						// complete the edit
						m_assignmentStorage.commit(nAssignment);
						
						try {
							if (m_taggingManager.isTaggable()) {
								for (TaggingProvider provider : m_taggingManager
										.getProviders()) {
									provider
											.transferCopyTags(
													m_assignmentActivityProducer
															.getActivity(oAssignment),
													m_assignmentActivityProducer
															.getActivity(nAssignment));
								}
							}
						} catch (PermissionException pe) {
							M_log.error(this + " transferCopyEntities " + pe.toString()  + " oAssignmentId=" + oAssignment.getId() + " nAssignmentId=" + nAssignment.getId());
						}
					} catch (Exception e)
					{
						M_log.error(this + " transferCopyEntities oAssignmentId=" + oAssignment.getId() + " nAssignmentId=" + nAssignment.getId());
					}
				} // if-else
			} // if
		} // for
	} // importResources

	/**
	 * manipulate the transfered attachment
	 * @param fromContext
	 * @param toContext
	 * @param nAttachments
	 * @param oAttachmentId
	 * @return the new reference
	 */

	private String transferAttachment(String fromContext, String toContext,
			List nAttachments, String oAttachmentId) 
	{
		String rv = "";
		
		// replace old site id with new site id in attachments
		String nAttachmentId = oAttachmentId.replaceAll(fromContext, toContext);
		try
		{
			ContentResource attachment = m_contentHostingService.getResource(nAttachmentId);
			if (nAttachments != null)
			{
				nAttachments.add(m_entityManager.newReference(attachment.getReference()));
			}
			rv = attachment.getReference();
		}
		catch (IdUnusedException e)
		{
			try
			{
				ContentResource oAttachment = m_contentHostingService.getResource(oAttachmentId);
				try
				{
					if (m_contentHostingService.isAttachmentResource(nAttachmentId))
					{
						// add the new resource into attachment collection area
						ContentResource attachment = m_contentHostingService.addAttachmentResource(
								Validator.escapeResourceName(oAttachment.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME)), 
								toContext, 
								ToolManager.getTool("sakai.assignment.grades").getTitle(), 
								oAttachment.getContentType(), 
								oAttachment.getContent(), 
								oAttachment.getProperties());
						rv = attachment.getReference();
						// add to attachment list
						if (nAttachments != null)
						{
							nAttachments.add(m_entityManager.newReference(rv));
						}
					}
					else
					{
						// add the new resource into resource area
						ContentResource attachment = m_contentHostingService.addResource(
								Validator.escapeResourceName(oAttachment.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME)),
								ToolManager.getCurrentPlacement().getContext(), 
								1, 
								oAttachment.getContentType(), 
								oAttachment.getContent(), 
								oAttachment.getProperties(), 
								NotificationService.NOTI_NONE);
						rv = attachment.getReference();
						// add to attachment list
						if (nAttachments != null)
						{
							nAttachments.add(m_entityManager.newReference(rv));
						}
					}
				}
				catch (Exception eeAny)
				{
					// if the new resource cannot be added
					M_log.warn(this + " transferCopyEntities: cannot add new attachment with id=" + nAttachmentId + " " + eeAny.getMessage());
				}
			}
			catch (Exception eAny)
			{
				// if cannot find the original attachment, do nothing.
				M_log.warn(this + " transferCopyEntities: cannot find the original attachment with id=" + oAttachmentId + " " + eAny.getMessage());
			}
		}
		catch (Exception any)
		{
			M_log.warn(this + " transferCopyEntities" + any.getMessage() + " oAttachmentId=" + oAttachmentId + " nAttachmentId=" + nAttachmentId);
		}
		
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntityDescription(Reference ref)
	{
		String rv = "Assignment: " + ref.getReference();
		
		try
		{
			// is it an AssignmentContent object
			if (REF_TYPE_CONTENT.equals(ref.getSubType()))
			{
				AssignmentContent c = getAssignmentContent(ref.getReference());
				rv = "AssignmentContent: " + c.getId() + " (" + c.getContext() + ")";
			}
			// is it an Assignment object
			else if (REF_TYPE_ASSIGNMENT.equals(ref.getSubType()))
			{
				Assignment a = getAssignment(ref.getReference());
				rv = "Assignment: " + a.getId() + " (" + a.getContext() + ")";
			}
			// is it an AssignmentSubmission object
			else if (REF_TYPE_SUBMISSION.equals(ref.getSubType()))
			{
				AssignmentSubmission s = getSubmission(ref.getReference());
				rv = "AssignmentSubmission: " + s.getId() + " (" + s.getContext() + ")";
			}
			else
				M_log.warn(this + " getEntityDescription(): unknown message ref subtype: " + ref.getSubType() + " in ref: " + ref.getReference());
		}
		catch (PermissionException e)
		{
			M_log.warn(this + " getEntityDescription(): " + e.getMessage() + " ref=" + ref.getReference());
		}
		catch (IdUnusedException e)
		{
			M_log.warn(this + " getEntityDescription(): " + e.getMessage() + " ref=" + ref.getReference());
		}
		catch (NullPointerException e)
		{
			M_log.warn(this + " getEntityDescription(): " + e.getMessage() + " ref=" + ref.getReference());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public ResourceProperties getEntityResourceProperties(Reference ref)
	{
		ResourceProperties rv = null;

		try
		{
			// is it an AssignmentContent object
			if (REF_TYPE_CONTENT.equals(ref.getSubType()))
			{
				AssignmentContent c = getAssignmentContent(ref.getReference());
				rv = c.getProperties();
			}
			// is it an Assignment object
			else if (REF_TYPE_ASSIGNMENT.equals(ref.getSubType()))
			{
				Assignment a = getAssignment(ref.getReference());
				rv = a.getProperties();
			}
			// is it an AssignmentSubmission object
			else if (REF_TYPE_SUBMISSION.equals(ref.getSubType()))
			{
				AssignmentSubmission s = getSubmission(ref.getReference());
				rv = s.getProperties();
			}
			else
				M_log.warn(this + " getEntityResourceProperties: unknown message ref subtype: " + ref.getSubType() + " in ref: " + ref.getReference());
		}
		catch (PermissionException e)
		{
			M_log.warn(this + " getEntityResourceProperties(): " + e.getMessage() + " ref=" + ref.getReference());
		}
		catch (IdUnusedException e)
		{
			M_log.warn(this + " getEntityResourceProperties(): " + e.getMessage() + " ref=" + ref.getReference());
		}
		catch (NullPointerException e)
		{
			M_log.warn(this + " getEntityResourceProperties(): " + e.getMessage() + " ref=" + ref.getReference());
		}

		return rv;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean canSubmit(String context, Assignment a)
	{
		// return false if not allowed to submit at all
		if (!allowAddSubmission(context)) return false;
		
		String userId = SessionManager.getCurrentSessionUserId();
		try
		{
			// get user
			User u = UserDirectoryService.getUser(userId);
			
			Time currentTime = TimeService.newTime();
			
			// return false if the assignment is draft or is not open yet
			Time openTime = a.getOpenTime();
			if (a.getDraft() || (openTime != null && openTime.after(currentTime)))
			{
				return false;
			}
			
			// return false if the current time has passed the assignment close time
			Time closeTime = a.getCloseTime();
			
			// get user's submission
			AssignmentSubmission submission = null;
			
			submission = getSubmission(a.getReference(), u);
			if (submission != null)
			{
				closeTime = submission.getCloseTime();
			}
			
			if (submission == null || (submission != null && submission.getTimeSubmitted() == null))
			{
				// if there is no submission yet
				if (closeTime != null && currentTime.after(closeTime))
				{
					return false;
				}
				else
				{
					return true;
				}
			}
			else
			{
				if (!submission.getSubmitted() && !(closeTime != null && currentTime.after(closeTime)))
				{
					// return true for drafted submissions
					return true;
				}
				else
				{
					// returned 
					if (submission.getResubmissionNum()!=0 && currentTime.before(closeTime))
					{
						// return true for returned submission but allow for resubmit and before the close time
						return true;
					}
					else
					{
						// return false otherwise
						return false;
					}
				}
			}
		}
		catch (UserNotDefinedException e)
		{
			// cannot find user
			M_log.warn(this + " canSubmit(String, Assignment) " + e.getMessage() + " assignment ref=" + a.getReference());
			return false;
		}
	}
	
	/**********************************************************************************************************************************************************************************************************************************************************
	 * Assignment Storage
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected interface AssignmentStorage
	{
		/**
		 * Open.
		 */
		public void open();

		/**
		 * Close.
		 */
		public void close();

		/**
		 * Check if an Assignment by this id exists.
		 * 
		 * @param id
		 *        The assignment id.
		 * @return true if an Assignment by this id exists, false if not.
		 */
		public boolean check(String id);

		/**
		 * Get the Assignment with this id, or null if not found.
		 * 
		 * @param id
		 *        The Assignment id.
		 * @return The Assignment with this id, or null if not found.
		 */
		public Assignment get(String id);

		/**
		 * Get all Assignments.
		 * 
		 * @return The list of all Assignments.
		 */
		public List getAll(String context);

		/**
		 * Add a new Assignment with this id.
		 * 
		 * @param id
		 *        The Assignment id.
		 * @param context
		 *        The context.
		 * @return The locked Assignment object with this id, or null if the id is in use.
		 */
		public Assignment put(String id, String context);

		/**
		 * Get a lock on the Assignment with this id, or null if a lock cannot be gotten.
		 * 
		 * @param id
		 *        The Assignment id.
		 * @return The locked Assignment with this id, or null if this records cannot be locked.
		 */
		public Assignment edit(String id);

		/**
		 * Commit the changes and release the lock.
		 * 
		 * @param Assignment
		 *        The Assignment to commit.
		 */
		public void commit(Assignment assignment);

		/**
		 * Cancel the changes and release the lock.
		 * 
		 * @param Assignment
		 *        The Assignment to commit.
		 */
		public void cancel(Assignment assignment);

		/**
		 * Remove this Assignment.
		 * 
		 * @param Assignment
		 *        The Assignment to remove.
		 */
		public void remove(Assignment assignment);

	} // AssignmentStorage
	
	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssignmentSubmission Storage
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected interface AssignmentSubmissionStorage
	{
		/**
		 * Open.
		 */
		public void open();

		/**
		 * Close.
		 */
		public void close();

		/**
		 * Check if a AssignmentSubmission by this id exists.
		 * 
		 * @param id
		 *        The AssignmentSubmission id.
		 * @return true if a AssignmentSubmission by this id exists, false if not.
		 */
		public boolean check(String id);
		
		/**
		 * Get the AssignmentSubmission with this id, or null if not found.
		 * 
		 * @param id
		 *        The AssignmentSubmission id.
		 * @return The AssignmentSubmission with this id, or null if not found.
		 */
		public AssignmentSubmission get(String id);
		
		/**
		 * Get the AssignmentSubmission with this assignment id and user id.
		 * 
		 * @param assignmentId
		 *        The Assignment id.
		 * @param userId
		 * 		  The user id
		 * @return The AssignmentSubmission with this id, or null if not found.
		 */
		public AssignmentSubmission get(String assignmentId, String userId);
		
		/**
		 * Get the number of submissions which has been submitted.
		 * 
		 * @param assignmentId -
		 *        the id of Assignment who's submissions you would like.
		 * @return List over all the submissions for an Assignment.
		 */
		public int getSubmittedSubmissionsCount(String assignmentId);
		
		/**
		 * Get the number of submissions which has not been submitted and graded.
		 * 
		 * @param assignment -
		 *        the Assignment who's submissions you would like.
		 * @return List over all the submissions for an Assignment.
		 */
		public int getUngradedSubmissionsCount(String assignmentId);

		/**
		 * Get all AssignmentSubmissions.
		 * 
		 * @return The list of all AssignmentSubmissions.
		 */
		public List getAll(String context);

		/**
		 * Add a new AssignmentSubmission with this id.
		 * 
		 * @param id
		 *        The AssignmentSubmission id.
		 * @param context
		 *        The context.
		 * @return The locked AssignmentSubmission object with this id, or null if the id is in use.
		 */
		public AssignmentSubmission put(String id, String assignmentId, String submitterId, String submitTime, String submitted, String graded);

		/**
		 * Get a lock on the AssignmentSubmission with this id, or null if a lock cannot be gotten.
		 * 
		 * @param id
		 *        The AssignmentSubmission id.
		 * @return The locked AssignmentSubmission with this id, or null if this records cannot be locked.
		 */
		public AssignmentSubmission edit(String id);

		/**
		 * Commit the changes and release the lock.
		 * 
		 * @param AssignmentSubmission
		 *        The AssignmentSubmission to commit.
		 */
		public void commit(AssignmentSubmission submission);

		/**
		 * Cancel the changes and release the lock.
		 * 
		 * @param AssignmentSubmission
		 *        The AssignmentSubmission to commit.
		 */
		public void cancel(AssignmentSubmission submission);

		/**
		 * Remove this AssignmentSubmission.
		 * 
		 * @param AssignmentSubmission
		 *        The AssignmentSubmission to remove.
		 */
		public void remove(AssignmentSubmission submission);

	} // AssignmentSubmissionStorage

	protected String getGroupNameFromContext(String context)
	{
		String retVal = "";

		if (context != null)
		{
			int index = context.indexOf("group-");
			if (index != -1)
			{
				String[] parts = StringUtil.splitFirst(context, "-");
				if (parts.length > 1)
				{
					retVal = parts[1];
				}
			}
			else
			{
				retVal = context;
			}
		}

		return retVal;
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * StorageUser implementations (no container)
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssignmentStorageUser implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected class AssignmentStorageUser implements StorageUser, SAXEntityReader
	{
		private Map<String,Object> m_services;
		
		/**
		 * Construct a new continer given just an id.
		 * 
		 * @param id
		 *        The id for the new object.
		 * @return The new container Resource.
		 */
		public Entity newContainer(String ref)
		{
			return null;
		}

		/**
		 * Construct a new container resource, from an XML element.
		 * 
		 * @param element
		 *        The XML.
		 * @return The new container resource.
		 */
		public Entity newContainer(Element element)
		{
			return null;
		}

		/**
		 * Construct a new container resource, as a copy of another
		 * 
		 * @param other
		 *        The other contianer to copy.
		 * @return The new container resource.
		 */
		public Entity newContainer(Entity other)
		{
			return null;
		}
		
		/**
		 * Construct a new continer given just an id.
		 * 
		 * @param id
		 *        The id for the new object.
		 * @return The new containe Resource.
		 */
		public Edit newContainerEdit(String ref)
		{
			return null;
		}

		/**
		 * Construct a new container resource, from an XML element.
		 * 
		 * @param element
		 *        The XML.
		 * @return The new container resource.
		 */
		public Edit newContainerEdit(Element element)
		{
			return null;
		}

		/**
		 * Construct a new container resource, as a copy of another
		 * 
		 * @param other
		 *        The other contianer to copy.
		 * @return The new container resource.
		 */
		public Edit newContainerEdit(Entity other)
		{
			return null;
		}

		/**
		 * Construct a new resource given just an id.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param id
		 *        The id for the new object.
		 * @param others
		 *        (options) array of objects to load into the Resource's fields.
		 * @return The new resource.
		 */
		public Entity newResource(Entity container, String id, Object[] others)
		{
			return new BaseAssignment(id, (String) others[0]);
		}

		/**
		 * Construct a new resource, from an XML element.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param element
		 *        The XML.
		 * @return The new resource from the XML.
		 */
		public Entity newResource(Entity container, Element element)
		{
			return new BaseAssignment(element);
		}

		/**
		 * Construct a new resource from another resource of the same type.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param other
		 *        The other resource.
		 * @return The new resource as a copy of the other.
		 */
		public Entity newResource(Entity container, Entity other)
		{
			return new BaseAssignment((Assignment) other);
		}
		
		/**
		 * Construct a new resource given just an id.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param id
		 *        The id for the new object.
		 * @param others
		 *        (options) array of objects to load into the Resource's fields.
		 * @return The new resource.
		 */
		public Edit newResourceEdit(Entity container, String id, Object[] others)
		{
			BaseAssignment e = new BaseAssignment(id, (String) others[0]);
			e.activate();
			return e;
		}

		/**
		 * Construct a new resource, from an XML element.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param element
		 *        The XML.
		 * @return The new resource from the XML.
		 */
		public Edit newResourceEdit(Entity container, Element element)
		{
			BaseAssignment e = new BaseAssignment(element);
			e.activate();
			return e;
		}

		/**
		 * Construct a new resource from another resource of the same type.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param other
		 *        The other resource.
		 * @return The new resource as a copy of the other.
		 */
		public Edit newResourceEdit(Entity container, Entity other)
		{
			BaseAssignment e = new BaseAssignment((Assignment) other);
			e.activate();
			return e;
		}

		/**
		 * Collect the fields that need to be stored outside the XML (for the resource).
		 * 
		 * @return An array of field values to store in the record outside the XML (for the resource).
		 */
		public Object[] storageFields(Entity r)
		{
			Object rv[] = new Object[1];
			rv[0] = ((Assignment) r).getContext();
			return rv;
		}

		/**
		 * Check if this resource is in draft mode.
		 * 
		 * @param r
		 *        The resource.
		 * @return true if the resource is in draft mode, false if not.
		 */
		public boolean isDraft(Entity r)
		{
			return false;
		}

		/**
		 * Access the resource owner user id.
		 * 
		 * @param r
		 *        The resource.
		 * @return The resource owner user id.
		 */
		public String getOwnerId(Entity r)
		{
			return null;
		}

		/**
		 * Access the resource date.
		 * 
		 * @param r
		 *        The resource.
		 * @return The resource date.
		 */
		public Time getDate(Entity r)
		{
			return null;
		}
		
		/***********************************************************************
		 * SAXEntityReader
		 */
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.sakaiproject.util.SAXEntityReader#getDefaultHandler(java.util.Map)
		 */
		public DefaultEntityHandler getDefaultHandler(final Map<String, Object> services)
		{
			return new DefaultEntityHandler()
			{

				/*
				 * (non-Javadoc)
				 * 
				 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
				 *      java.lang.String, java.lang.String,
				 *      org.xml.sax.Attributes)
				 */
				@Override
				public void startElement(String uri, String localName, String qName,
						Attributes attributes) throws SAXException
				{
					if (doStartElement(uri, localName, qName, attributes))
					{
						if (entity == null)
						{
							if ("assignment".equals(qName))
							{
								BaseAssignment ba = new BaseAssignment();
								entity = ba;
								setContentHandler(ba.getContentHandler(services), uri,
										localName, qName, attributes);
							}
							else
							{
								M_log.warn(this + " AssignmentStorageUser getDefaultHandler startElement Unexpected Element in XML [" + qName + "]");
							}

						}
					}
				}

			};
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.sakaiproject.util.SAXEntityReader#getServices()
		 */
		public Map<String, Object> getServices()
		{
			if (m_services == null)
			{
				m_services = new HashMap<String, Object>();
			}
			return m_services;
		}

	}// AssignmentStorageUser

	/**********************************************************************************************************************************************************************************************************************************************************
	 * SubmissionStorageUser implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected class AssignmentSubmissionStorageUser implements StorageUser, SAXEntityReader
	{
		private Map<String,Object> m_services;
		
		/**
		 * Construct a new continer given just an id.
		 * 
		 * @param id
		 *        The id for the new object.
		 * @return The new container Resource.
		 */
		public Entity newContainer(String ref)
		{
			return null;
		}

		/**
		 * Construct a new container resource, from an XML element.
		 * 
		 * @param element
		 *        The XML.
		 * @return The new container resource.
		 */
		public Entity newContainer(Element element)
		{
			return null;
		}

		/**
		 * Construct a new container resource, as a copy of another
		 * 
		 * @param other
		 *        The other contianer to copy.
		 * @return The new container resource.
		 */
		public Entity newContainer(Entity other)
		{
			return null;
		}
		
		/**
		 * Construct a new continer given just an id.
		 * 
		 * @param id
		 *        The id for the new object.
		 * @return The new containe Resource.
		 */
		public Edit newContainerEdit(String ref)
		{
			return null;
		}

		/**
		 * Construct a new container resource, from an XML element.
		 * 
		 * @param element
		 *        The XML.
		 * @return The new container resource.
		 */
		public Edit newContainerEdit(Element element)
		{
			return null;
		}

		/**
		 * Construct a new container resource, as a copy of another
		 * 
		 * @param other
		 *        The other contianer to copy.
		 * @return The new container resource.
		 */
		public Edit newContainerEdit(Entity other)
		{
			return null;
		}

		/**
		 * Construct a new resource given just an id.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param id
		 *        The id for the new object.
		 * @param others
		 *        (options) array of objects to load into the Resource's fields.
		 * @return The new resource.
		 */
		public Entity newResource(Entity container, String id, Object[] others)
		{
			return new BaseAssignmentSubmission(id, (String) others[0], (String) others[1], (String) others[2], (String) others[3], (String) others[4]);
		}

		/**
		 * Construct a new resource, from an XML element.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param element
		 *        The XML.
		 * @return The new resource from the XML.
		 */
		public Entity newResource(Entity container, Element element)
		{
			return new BaseAssignmentSubmission(element);
		}

		/**
		 * Construct a new resource from another resource of the same type.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param other
		 *        The other resource.
		 * @return The new resource as a copy of the other.
		 */
		public Entity newResource(Entity container, Entity other)
		{
			return new BaseAssignmentSubmission((AssignmentSubmission) other);
		}
		
		/**
		 * Construct a new rsource given just an id.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param id
		 *        The id for the new object.
		 * @param others
		 *        (options) array of objects to load into the Resource's fields.
		 * @return The new resource.
		 */
		public Edit newResourceEdit(Entity container, String id, Object[] others)
		{
			BaseAssignmentSubmission e = new BaseAssignmentSubmission(id, (String) others[0], (String) others[1], (String) others[2], (String) others[3], (String) others[4]);
			e.activate();
			return e;
		}

		/**
		 * Construct a new resource, from an XML element.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param element
		 *        The XML.
		 * @return The new resource from the XML.
		 */
		public Edit newResourceEdit(Entity container, Element element)
		{
			BaseAssignmentSubmission e = new BaseAssignmentSubmission(element);
			e.activate();
			return e;
		}

		/**
		 * Construct a new resource from another resource of the same type.
		 * 
		 * @param container
		 *        The Resource that is the container for the new resource (may be null).
		 * @param other
		 *        The other resource.
		 * @return The new resource as a copy of the other.
		 */
		public Edit newResourceEdit(Entity container, Entity other)
		{
			BaseAssignmentSubmission e = new BaseAssignmentSubmission((AssignmentSubmission) other);
			e.activate();
			return e;
		}

		/**
		 * Collect the fields that need to be stored outside the XML (for the resource).
		 * 
		 * @return An array of field values to store in the record outside the XML (for the resource).
		 */
		public Object[] storageFields(Entity r)
		{
			/*"context", "SUBMITTER_ID", "SUBMIT_TIME", "SUBMITTED", "GRADED"*/
			Object rv[] = new Object[5];
			rv[0] = ((AssignmentSubmission) r).getAssignmentId();
			
			User[] submitters = ((AssignmentSubmission) r).getSubmitters();
			if(submitters != null && submitters[0] != null) 
			{
 				rv[1] = submitters[0].getId();
			} else {
				M_log.error(new Exception(this + " AssignmentSubmissionStorageUser storageFields Unique constraint is in force -- submitter[0] cannot be null"));
 			}
			
			Time submitTime = ((AssignmentSubmission) r).getTimeSubmitted();
			rv[2] = (submitTime != null)?String.valueOf(submitTime.getTime()):null;
			
			rv[3] = Boolean.valueOf(((AssignmentSubmission) r).getSubmitted()).toString();
			
			rv[4] = Boolean.valueOf(((AssignmentSubmission) r).getGraded()).toString();
			
			return rv;
		}

		/**
		 * Check if this resource is in draft mode.
		 * 
		 * @param r
		 *        The resource.
		 * @return true if the resource is in draft mode, false if not.
		 */
		public boolean isDraft(Entity r)
		{
			return false;
		}

		/**
		 * Access the resource owner user id.
		 * 
		 * @param r
		 *        The resource.
		 * @return The resource owner user id.
		 */
		public String getOwnerId(Entity r)
		{
			return null;
		}

		/**
		 * Access the resource date.
		 * 
		 * @param r
		 *        The resource.
		 * @return The resource date.
		 */
		public Time getDate(Entity r)
		{
			return null;
		}
		
		/***********************************************************************
		 * SAXEntityReader
		 */
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.sakaiproject.util.SAXEntityReader#getDefaultHandler(java.util.Map)
		 */
		public DefaultEntityHandler getDefaultHandler(final Map<String, Object> services)
		{
			return new DefaultEntityHandler()
			{

				/*
				 * (non-Javadoc)
				 * 
				 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
				 *      java.lang.String, java.lang.String,
				 *      org.xml.sax.Attributes)
				 */
				@Override
				public void startElement(String uri, String localName, String qName,
						Attributes attributes) throws SAXException
				{
					if (doStartElement(uri, localName, qName, attributes))
					{
						if (entity == null)
						{
							if ("submission".equals(qName))
							{
								BaseAssignmentSubmission bas = new BaseAssignmentSubmission();
								entity = bas;
								setContentHandler(bas.getContentHandler(services), uri,
										localName, qName, attributes);
							}
							else
							{
								M_log.warn(this + " AssignmentSubmissionStorageUser getDefaultHandler startElement: Unexpected Element in XML [" + qName + "]");
							}

						}
					}
				}

			};
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.sakaiproject.util.SAXEntityReader#getServices()
		 */
		public Map<String, Object> getServices()
		{
			if (m_services == null)
			{
				m_services = new HashMap<String, Object>();
			}
			return m_services;
		}

	}// SubmissionStorageUser

	/**********************************************************************************************************************************************************************************************************************************************************
	 * CacheRefresher implementations (no container)
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssignmentCacheRefresher implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected class AssignmentCacheRefresher implements CacheRefresher
	{
		/**
		 * Get a new value for this key whose value has already expired in the cache.
		 * 
		 * @param key
		 *        The key whose value has expired and needs to be refreshed.
		 * @param oldValue
		 *        The old expired value of the key.
		 * @return a new value for use in the cache for this key; if null, the entry will be removed.
		 */
		public Object refresh(Object key, Object oldValue, Event event)
		{

			// key is a reference, but our storage wants an id
			String id = assignmentId((String) key);

			// get whatever we have from storage for the cache for this vale
			Assignment assignment = m_assignmentStorage.get(id);

			M_log.warn(this + " AssignmentCacheRefresher:refresh(): " + key + " : " + id);

			return assignment;

		} // refresh

	}// AssignmentCacheRefresher

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssignmentSubmissionCacheRefresher implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected class AssignmentSubmissionCacheRefresher implements CacheRefresher
	{
		/**
		 * Get a new value for this key whose value has already expired in the cache.
		 * 
		 * @param key
		 *        The key whose value has expired and needs to be refreshed.
		 * @param oldValue
		 *        The old expired value of the key.
		 * @return a new value for use in the cache for this key; if null, the entry will be removed.
		 */
		public Object refresh(Object key, Object oldValue, Event event)
		{

			// key is a reference, but our storage wants an id
			String id = submissionId((String) key);

			// get whatever we have from storage for the cache for this vale
			AssignmentSubmission submission = m_submissionStorage.get(id);

			M_log.warn(this + " AssignmentSubmissionCacheRefresher:refresh(): " + key + " : " + id);

			return submission;

		} // refresh

	}// AssignmentSubmissionCacheRefresher
	
	/**
	 * the AssignmentComparator clas
	 */
	private class AssignmentComparator implements Comparator
	{	
		/**
		 * the criteria
		 */
		String m_criteria = null;

		/**
		 * the criteria
		 */
		String m_asc = null;

		/**
		 * constructor
		 * @param criteria
		 *        The sort criteria string
		 * @param asc
		 *        The sort order string. TRUE_STRING if ascending; "false" otherwise.
		 */
		public AssignmentComparator(String criteria, String asc)
		{
			m_criteria = criteria;
			m_asc = asc;
		} // constructor

		/**
		 * implementing the compare function
		 * 
		 * @param o1
		 *        The first object
		 * @param o2
		 *        The second object
		 * @return The compare result. 1 is o1 < o2; -1 otherwise
		 */
		public int compare(Object o1, Object o2)
		{
			int result = -1;

			/** *********** fo sorting assignments ****************** */
			if (m_criteria.equals("duedate"))
			{
				// sorted by the assignment due date
				Time t1 = ((Assignment) o1).getDueTime();
				Time t2 = ((Assignment) o2).getDueTime();

				if (t1 == null)
				{
					result = -1;
				}
				else if (t2 == null)
				{
					result = 1;
				}
				else if (t1.before(t2))
				{
					result = -1;
				}
				else
				{
					result = 1;
				}
			}
			else if (m_criteria.equals("sortname"))
			{
				// sorted by the user's display name
				String s1 = null;
				String userId1 = (String) o1;
				if (userId1 != null)
				{
					try
					{
						User u1 = UserDirectoryService.getUser(userId1);
						s1 = u1!=null?u1.getSortName():null;
					}
					catch (Exception e)
					{
						M_log.warn(this + " AssignmentComparator.compare " + e.getMessage() + " id=" + userId1);
					}
				}
					
				String s2 = null;
				String userId2 = (String) o2;
				if (userId2 != null)
				{
					try
					{
						User u2 = UserDirectoryService.getUser(userId2);
						s2 = u2!=null?u2.getSortName():null;
					}
					catch (Exception e)
					{
						M_log.warn(this + " AssignmentComparator.compare " + e.getMessage() + " id=" + userId2);
					}
				}

				if (s1 == null)
				{
					result = -1;
				}
				else if (s2 == null)
				{
					result = 1;
				}
				else
				{
					result = s1.compareTo(s2);
				}
			}
			
			// sort ascending or descending
			if (m_asc.equals(Boolean.FALSE.toString()))
			{
				result = -result;
			}
			return result;
		}
	}
	
	public void transferCopyEntities(String fromContext, String toContext, List ids, boolean cleanup)
	{	
		try
		{
			if(cleanup == true)
			{
				SecurityService.pushAdvisor(new SecurityAdvisor() 
				{
					public SecurityAdvice isAllowed(String userId, String function, String reference)       
					{    
						return SecurityAdvice.ALLOWED;       
					} 
				});

				String toSiteId = toContext;
				Iterator assignmentsIter = getAssignmentsForContext(toSiteId);
				while (assignmentsIter.hasNext())
				{
					try 
					{
						Assignment assignment = (Assignment) assignmentsIter.next();
						String assignmentId = assignment.getId();
						AssignmentEdit aEdit = editAssignment(assignmentId);
						try
						{
							removeAssignmentContent(editAssignmentContent(aEdit.getContent().getReference()));
						}
						catch (Exception eee)
						{
							M_log.warn("removeAssignmentContent error:" + eee);
						}
						try
						{
							removeAssignment(aEdit);
						}
						catch (Exception eeee)
						{
							M_log.warn("removeAssignment error:" + eeee);
						}
					}
					catch(Exception ee)
					{
						M_log.warn("removeAssignment process error:" + ee);
					}
				}
				   
			}
			transferCopyEntities(fromContext, toContext, ids);
		}
		catch (Exception e)
		{
			M_log.info("transferCopyEntities: End removing Assignmentt data" + e);
		}
		finally
		{
			SecurityService.popAdvisor();
		}
	}

} // BaseAssignmentService

