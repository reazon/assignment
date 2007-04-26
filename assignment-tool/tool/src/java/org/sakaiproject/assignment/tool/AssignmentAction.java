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

package org.sakaiproject.assignment.tool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.sakaiproject.announcement.api.AnnouncementChannel;
import org.sakaiproject.announcement.api.AnnouncementMessageEdit;
import org.sakaiproject.announcement.api.AnnouncementMessageHeaderEdit;
import org.sakaiproject.announcement.api.AnnouncementService;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentContentEdit;
import org.sakaiproject.assignment.api.AssignmentEdit;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.assignment.api.AssignmentSubmissionEdit;
import org.sakaiproject.assignment.cover.AssignmentService;
import org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer;
import org.sakaiproject.assignment.taggable.api.TaggingHelperInfo;
import org.sakaiproject.assignment.taggable.api.TaggingManager;
import org.sakaiproject.assignment.taggable.api.TaggingProvider;
import org.sakaiproject.assignment.taggable.tool.DecoratedTaggingProvider;
import org.sakaiproject.assignment.taggable.tool.DecoratedTaggingProvider.Pager;
import org.sakaiproject.assignment.taggable.tool.DecoratedTaggingProvider.Sort;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.PermissionsHelper;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityAdvisor.SecurityAdvice;
import org.sakaiproject.authz.cover.AuthzGroupService;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.calendar.api.Calendar;
import org.sakaiproject.calendar.api.CalendarEvent;
import org.sakaiproject.calendar.api.CalendarService;
import org.sakaiproject.cheftool.Context;
import org.sakaiproject.cheftool.JetspeedRunData;
import org.sakaiproject.cheftool.PagedResourceActionII;
import org.sakaiproject.cheftool.PortletConfig;
import org.sakaiproject.cheftool.RunData;
import org.sakaiproject.cheftool.VelocityPortlet;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentTypeImageService;
import org.sakaiproject.content.api.FilePickerHelper;
import org.sakaiproject.content.cover.ContentHostingService;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.entity.cover.EntityManager;
import org.sakaiproject.event.api.SessionState;
import org.sakaiproject.event.cover.EventTrackingService;
import org.sakaiproject.event.cover.NotificationService;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.javax.PagingPosition;
import org.sakaiproject.service.gradebook.shared.AssignmentHasIllegalPointsException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeBreakdown;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.FileItem;
import org.sakaiproject.util.FormattedText;
import org.sakaiproject.util.ParameterParser;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.SortedIterator;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Validator;
import org.sakaiproject.contentreview.service.ContentReviewService;
/**
 * <p>
 * AssignmentAction is the action class for the assignment tool.
 * </p>
 */
public class AssignmentAction extends PagedResourceActionII
{
	private static ResourceLoader rb = new ResourceLoader("assignment");

	
	private static final Boolean allowReviewService = ServerConfigurationService.getBoolean("assignment.useContentReview", false);
	
	/** Is the review service available? */
	private static final String ALLOW_REVIEW_SERVICE = "allow_review_service";
	
	/** Is review service enabled? */ 
	private static final String ENABLE_REVIEW_SERVICE = "enable_review_service";
	
	private static final String NEW_ASSIGNMENT_USE_REVIEW_SERVICE = "new_assignment_use_review_service";
	
	private static final String NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW = "new_assignment_allow_student_view";
	
	
	
	
	/** The attachments */
	private static final String ATTACHMENTS = "Assignment.attachments";

	/** The content type image lookup service in the State. */
	private static final String STATE_CONTENT_TYPE_IMAGE_SERVICE = "Assignment.content_type_image_service";

	/** The calendar service in the State. */
	private static final String STATE_CALENDAR_SERVICE = "Assignment.calendar_service";

	/** The announcement service in the State. */
	private static final String STATE_ANNOUNCEMENT_SERVICE = "Assignment.announcement_service";

	/** The calendar object */
	private static final String CALENDAR = "calendar";

	/** The announcement channel */
	private static final String ANNOUNCEMENT_CHANNEL = "announcement_channel";

	/** The state mode */
	private static final String STATE_MODE = "Assignment.mode";

	/** The context string */
	private static final String STATE_CONTEXT_STRING = "Assignment.context_string";

	/** The user */
	private static final String STATE_USER = "Assignment.user";

	// SECTION MOD
	/** Used to keep track of the section info not currently being used. */
	private static final String STATE_SECTION_STRING = "Assignment.section_string";

	/** **************************** sort assignment ********************** */
	/** state sort * */
	private static final String SORTED_BY = "Assignment.sorted_by";

	/** state sort ascendingly * */
	private static final String SORTED_ASC = "Assignment.sorted_asc";

	/** sort by assignment title */
	private static final String SORTED_BY_TITLE = "title";

	/** sort by assignment section */
	private static final String SORTED_BY_SECTION = "section";

	/** sort by assignment due date */
	private static final String SORTED_BY_DUEDATE = "duedate";

	/** sort by assignment open date */
	private static final String SORTED_BY_OPENDATE = "opendate";

	/** sort by assignment status */
	private static final String SORTED_BY_ASSIGNMENT_STATUS = "assignment_status";

	/** sort by assignment submission status */
	private static final String SORTED_BY_SUBMISSION_STATUS = "submission_status";

	/** sort by assignment number of submissions */
	private static final String SORTED_BY_NUM_SUBMISSIONS = "num_submissions";

	/** sort by assignment number of ungraded submissions */
	private static final String SORTED_BY_NUM_UNGRADED = "num_ungraded";

	/** sort by assignment submission grade */
	private static final String SORTED_BY_GRADE = "grade";

	/** sort by assignment maximun grade available */
	private static final String SORTED_BY_MAX_GRADE = "max_grade";

	/** sort by assignment range */
	private static final String SORTED_BY_FOR = "for";

	/** sort by group title */
	private static final String SORTED_BY_GROUP_TITLE = "group_title";

	/** sort by group description */
	private static final String SORTED_BY_GROUP_DESCRIPTION = "group_description";

	/** *************************** sort submission in instructor grade view *********************** */
	/** state sort submission* */
	private static final String SORTED_GRADE_SUBMISSION_BY = "Assignment.grade_submission_sorted_by";

	/** state sort submission ascendingly * */
	private static final String SORTED_GRADE_SUBMISSION_ASC = "Assignment.grade_submission_sorted_asc";

	/** state sort submission by submitters last name * */
	private static final String SORTED_GRADE_SUBMISSION_BY_LASTNAME = "sorted_grade_submission_by_lastname";

	/** state sort submission by submit time * */
	private static final String SORTED_GRADE_SUBMISSION_BY_SUBMIT_TIME = "sorted_grade_submission_by_submit_time";

	/** state sort submission by submission status * */
	private static final String SORTED_GRADE_SUBMISSION_BY_STATUS = "sorted_grade_submission_by_status";

	/** state sort submission by submission grade * */
	private static final String SORTED_GRADE_SUBMISSION_BY_GRADE = "sorted_grade_submission_by_grade";

	/** state sort submission by submission released * */
	private static final String SORTED_GRADE_SUBMISSION_BY_RELEASED = "sorted_grade_submission_by_released";
	
	/** state sort submissuib by content review score **/
	private static final String SORTED_GRADE_SUBMISSION_CONTENTREVIEW = "sorted_grade_submission_by_contentreview";

	/** *************************** sort submission *********************** */
	/** state sort submission* */
	private static final String SORTED_SUBMISSION_BY = "Assignment.submission_sorted_by";

	/** state sort submission ascendingly * */
	private static final String SORTED_SUBMISSION_ASC = "Assignment.submission_sorted_asc";

	/** state sort submission by submitters last name * */
	private static final String SORTED_SUBMISSION_BY_LASTNAME = "sorted_submission_by_lastname";

	/** state sort submission by submit time * */
	private static final String SORTED_SUBMISSION_BY_SUBMIT_TIME = "sorted_submission_by_submit_time";

	/** state sort submission by submission grade * */
	private static final String SORTED_SUBMISSION_BY_GRADE = "sorted_submission_by_grade";

	/** state sort submission by submission status * */
	private static final String SORTED_SUBMISSION_BY_STATUS = "sorted_submission_by_status";

	/** state sort submission by submission released * */
	private static final String SORTED_SUBMISSION_BY_RELEASED = "sorted_submission_by_released";

	/** state sort submission by assignment title */
	private static final String SORTED_SUBMISSION_BY_ASSIGNMENT = "sorted_submission_by_assignment";

	/** state sort submission by max grade */
	private static final String SORTED_SUBMISSION_BY_MAX_GRADE = "sorted_submission_by_max_grade";

	/** ******************** student's view assignment submission ****************************** */
	/** the assignment object been viewing * */
	private static final String VIEW_SUBMISSION_ASSIGNMENT_REFERENCE = "Assignment.view_submission_assignment_reference";

	/** the submission text to the assignment * */
	private static final String VIEW_SUBMISSION_TEXT = "Assignment.view_submission_text";

	/** the submission answer to Honor Pledge * */
	private static final String VIEW_SUBMISSION_HONOR_PLEDGE_YES = "Assignment.view_submission_honor_pledge_yes";

	/** ***************** student's preview of submission *************************** */
	/** the assignment id * */
	private static final String PREVIEW_SUBMISSION_ASSIGNMENT_REFERENCE = "preview_submission_assignment_reference";

	/** the submission text * */
	private static final String PREVIEW_SUBMISSION_TEXT = "preview_submission_text";

	/** the submission honor pledge answer * */
	private static final String PREVIEW_SUBMISSION_HONOR_PLEDGE_YES = "preview_submission_honor_pledge_yes";

	/** the submission attachments * */
	private static final String PREVIEW_SUBMISSION_ATTACHMENTS = "preview_attachments";

	/** the flag indicate whether the to show the student view or not */
	private static final String PREVIEW_ASSIGNMENT_STUDENT_VIEW_HIDE_FLAG = "preview_assignment_student_view_hide_flag";

	/** the flag indicate whether the to show the assignment info or not */
	private static final String PREVIEW_ASSIGNMENT_ASSIGNMENT_HIDE_FLAG = "preview_assignment_assignment_hide_flag";

	/** the assignment id */
	private static final String PREVIEW_ASSIGNMENT_ASSIGNMENT_ID = "preview_assignment_assignment_id";

	/** the assignment content id */
	private static final String PREVIEW_ASSIGNMENT_ASSIGNMENTCONTENT_ID = "preview_assignment_assignmentcontent_id";

	/** ************** view assignment ***************************************** */
	/** the hide assignment flag in the view assignment page * */
	private static final String VIEW_ASSIGNMENT_HIDE_ASSIGNMENT_FLAG = "view_assignment_hide_assignment_flag";

	/** the hide student view flag in the view assignment page * */
	private static final String VIEW_ASSIGNMENT_HIDE_STUDENT_VIEW_FLAG = "view_assignment_hide_student_view_flag";

	/** ******************* instructor's view assignment ***************************** */
	private static final String VIEW_ASSIGNMENT_ID = "view_assignment_id";

	/** ******************* instructor's edit assignment ***************************** */
	private static final String EDIT_ASSIGNMENT_ID = "edit_assignment_id";

	/** ******************* instructor's delete assignment ids ***************************** */
	private static final String DELETE_ASSIGNMENT_IDS = "delete_assignment_ids";

	/** ******************* flags controls the grade assignment page layout ******************* */
	private static final String GRADE_ASSIGNMENT_EXPAND_FLAG = "grade_assignment_expand_flag";

	private static final String GRADE_SUBMISSION_EXPAND_FLAG = "grade_submission_expand_flag";
	
	private static final String GRADE_NO_SUBMISSION_DEFAULT_GRADE = "grade_no_submission_default_grade";

	/** ******************* instructor's grade submission ***************************** */
	private static final String GRADE_SUBMISSION_ASSIGNMENT_ID = "grade_submission_assignment_id";

	private static final String GRADE_SUBMISSION_SUBMISSION_ID = "grade_submission_submission_id";

	private static final String GRADE_SUBMISSION_FEEDBACK_COMMENT = "grade_submission_feedback_comment";

	private static final String GRADE_SUBMISSION_FEEDBACK_TEXT = "grade_submission_feedback_text";

	private static final String GRADE_SUBMISSION_FEEDBACK_ATTACHMENT = "grade_submission_feedback_attachment";

	private static final String GRADE_SUBMISSION_GRADE = "grade_submission_grade";

	private static final String GRADE_SUBMISSION_ASSIGNMENT_EXPAND_FLAG = "grade_submission_assignment_expand_flag";

	private static final String GRADE_SUBMISSION_ALLOW_RESUBMIT = "grade_submission_allow_resubmit";
	
	/** ******************* instructor's export assignment ***************************** */
	private static final String EXPORT_ASSIGNMENT_REF = "export_assignment_ref";

	private static final String EXPORT_ASSIGNMENT_ID = "export_assignment_id";

	/** ****************** instructor's new assignment ****************************** */
	private static final String NEW_ASSIGNMENT_TITLE = "new_assignment_title";

	// open date
	private static final String NEW_ASSIGNMENT_OPENMONTH = "new_assignment_openmonth";

	private static final String NEW_ASSIGNMENT_OPENDAY = "new_assignment_openday";

	private static final String NEW_ASSIGNMENT_OPENYEAR = "new_assignment_openyear";

	private static final String NEW_ASSIGNMENT_OPENHOUR = "new_assignment_openhour";

	private static final String NEW_ASSIGNMENT_OPENMIN = "new_assignment_openmin";

	private static final String NEW_ASSIGNMENT_OPENAMPM = "new_assignment_openampm";

	// due date
	private static final String NEW_ASSIGNMENT_DUEMONTH = "new_assignment_duemonth";

	private static final String NEW_ASSIGNMENT_DUEDAY = "new_assignment_dueday";

	private static final String NEW_ASSIGNMENT_DUEYEAR = "new_assignment_dueyear";

	private static final String NEW_ASSIGNMENT_DUEHOUR = "new_assignment_duehour";

	private static final String NEW_ASSIGNMENT_DUEMIN = "new_assignment_duemin";

	private static final String NEW_ASSIGNMENT_DUEAMPM = "new_assignment_dueampm";

	// close date
	private static final String NEW_ASSIGNMENT_ENABLECLOSEDATE = "new_assignment_enableclosedate";

	private static final String NEW_ASSIGNMENT_CLOSEMONTH = "new_assignment_closemonth";

	private static final String NEW_ASSIGNMENT_CLOSEDAY = "new_assignment_closeday";

	private static final String NEW_ASSIGNMENT_CLOSEYEAR = "new_assignment_closeyear";

	private static final String NEW_ASSIGNMENT_CLOSEHOUR = "new_assignment_closehour";

	private static final String NEW_ASSIGNMENT_CLOSEMIN = "new_assignment_closemin";

	private static final String NEW_ASSIGNMENT_CLOSEAMPM = "new_assignment_closeampm";

	private static final String NEW_ASSIGNMENT_ATTACHMENT = "new_assignment_attachment";

	private static final String NEW_ASSIGNMENT_SECTION = "new_assignment_section";

	private static final String NEW_ASSIGNMENT_SUBMISSION_TYPE = "new_assignment_submission_type";

	private static final String NEW_ASSIGNMENT_GRADE_TYPE = "new_assignment_grade_type";

	private static final String NEW_ASSIGNMENT_GRADE_POINTS = "new_assignment_grade_points";

	private static final String NEW_ASSIGNMENT_DESCRIPTION = "new_assignment_instructions";

	private static final String NEW_ASSIGNMENT_DUE_DATE_SCHEDULED = "new_assignment_due_date_scheduled";

	private static final String NEW_ASSIGNMENT_OPEN_DATE_ANNOUNCED = "new_assignment_open_date_announced";

	private static final String NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE = "new_assignment_check_add_honor_pledge";

	private static final String NEW_ASSIGNMENT_HIDE_OPTION_FLAG = "new_assignment_hide_option_flag";

	private static final String NEW_ASSIGNMENT_FOCUS = "new_assignment_focus";

	private static final String NEW_ASSIGNMENT_DESCRIPTION_EMPTY = "new_assignment_description_empty";

	private static final String NEW_ASSIGNMENT_RANGE = "new_assignment_range";

	private static final String NEW_ASSIGNMENT_GROUPS = "new_assignment_groups";
	
	/**************************** assignment year range *************************/
	private static final String NEW_ASSIGNMENT_YEAR_RANGE_FROM = "new_assignment_year_range_from";
	private static final String NEW_ASSIGNMENT_YEAR_RANGE_TO = "new_assignment_year_range_to";
	
	// submission level of resubmit due time
	private static final String ALLOW_RESUBMIT_CLOSEMONTH = "allow_resubmit_closeMonth";
	private static final String ALLOW_RESUBMIT_CLOSEDAY = "allow_resubmit_closeDay";
	private static final String ALLOW_RESUBMIT_CLOSEYEAR = "allow_resubmit_closeYear";
	private static final String ALLOW_RESUBMIT_CLOSEHOUR = "allow_resubmit_closeHour";
	private static final String ALLOW_RESUBMIT_CLOSEMIN = "allow_resubmit_closeMin";
	private static final String ALLOW_RESUBMIT_CLOSEAMPM = "allow_resubmit_closeAMPM";

	private static final String ATTACHMENTS_MODIFIED = "attachments_modified";

	/** **************************** instructor's view student submission ***************** */
	// the show/hide table based on member id
	private static final String STUDENT_LIST_SHOW_TABLE = "STUDENT_LIST_SHOW_TABLE";

	/** **************************** student view grade submission id *********** */
	private static final String VIEW_GRADE_SUBMISSION_ID = "view_grade_submission_id";
	
	// alert for grade exceeds max grade setting
	private static final String GRADE_GREATER_THAN_MAX_ALERT = "grade_greater_than_max_alert";

	/** **************************** modes *************************** */
	/** The list view of assignments */
   private static final String MODE_LIST_ASSIGNMENTS = "lisofass1"; // set in velocity template

	/** The student view of an assignment submission */
	private static final String MODE_STUDENT_VIEW_SUBMISSION = "Assignment.mode_view_submission";
	
	/** The student view of an assignment submission confirmation */
	private static final String MODE_STUDENT_VIEW_SUBMISSION_CONFIRMATION = "Assignment.mode_view_submission_confirmation";

	/** The student preview of an assignment submission */
	private static final String MODE_STUDENT_PREVIEW_SUBMISSION = "Assignment.mode_student_preview_submission";

	/** The student view of graded submission */
	private static final String MODE_STUDENT_VIEW_GRADE = "Assignment.mode_student_view_grade";

	/** The student view of assignments */
	private static final String MODE_STUDENT_VIEW_ASSIGNMENT = "Assignment.mode_student_view_assignment";

	/** The instructor view of creating a new assignment or editing an existing one */
	private static final String MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT = "Assignment.mode_instructor_new_edit_assignment";

	/** The instructor view to delete an assignment */
	private static final String MODE_INSTRUCTOR_DELETE_ASSIGNMENT = "Assignment.mode_instructor_delete_assignment";

	/** The instructor view to grade an assignment */
	private static final String MODE_INSTRUCTOR_GRADE_ASSIGNMENT = "Assignment.mode_instructor_grade_assignment";

	/** The instructor view to grade a submission */
	private static final String MODE_INSTRUCTOR_GRADE_SUBMISSION = "Assignment.mode_instructor_grade_submission";

	/** The instructor view of preview grading a submission */
	private static final String MODE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION = "Assignment.mode_instructor_preview_grade_submission";

	/** The instructor preview of one assignment */
	private static final String MODE_INSTRUCTOR_PREVIEW_ASSIGNMENT = "Assignment.mode_instructor_preview_assignments";

	/** The instructor view of one assignment */
	private static final String MODE_INSTRUCTOR_VIEW_ASSIGNMENT = "Assignment.mode_instructor_view_assignments";

	/** The instructor view to list students of an assignment */
	private static final String MODE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT = "lisofass2"; // set in velocity template

	/** The instructor view of assignment submission report */
	private static final String MODE_INSTRUCTOR_REPORT_SUBMISSIONS = "grarep"; // set in velocity template
	
	/** The instructor view of uploading all from archive file */
	private static final String MODE_INSTRUCTOR_UPLOAD_ALL = "uploadAll"; 

	/** The student view of assignment submission report */
	private static final String MODE_STUDENT_VIEW = "stuvie"; // set in velocity template

	/** ************************* vm names ************************** */
	/** The list view of assignments */
	private static final String TEMPLATE_LIST_ASSIGNMENTS = "_list_assignments";

	/** The student view of assignment */
	private static final String TEMPLATE_STUDENT_VIEW_ASSIGNMENT = "_student_view_assignment";

	/** The student view of showing an assignment submission */
	private static final String TEMPLATE_STUDENT_VIEW_SUBMISSION = "_student_view_submission";
	
	/** The student view of an assignment submission confirmation */
	private static final String TEMPLATE_STUDENT_VIEW_SUBMISSION_CONFIRMATION = "_student_view_submission_confirmation";

	/** The student preview an assignment submission */
	private static final String TEMPLATE_STUDENT_PREVIEW_SUBMISSION = "_student_preview_submission";

	/** The student view of graded submission */
	private static final String TEMPLATE_STUDENT_VIEW_GRADE = "_student_view_grade";

	/** The instructor view to create a new assignment or edit an existing one */
	private static final String TEMPLATE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT = "_instructor_new_edit_assignment";

	/** The instructor view to edit assignment */
	private static final String TEMPLATE_INSTRUCTOR_DELETE_ASSIGNMENT = "_instructor_delete_assignment";

	/** The instructor view to edit assignment */
	private static final String TEMPLATE_INSTRUCTOR_GRADE_SUBMISSION = "_instructor_grading_submission";

	/** The instructor preview to edit assignment */
	private static final String TEMPLATE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION = "_instructor_preview_grading_submission";

	/** The instructor view to grade the assignment */
	private static final String TEMPLATE_INSTRUCTOR_GRADE_ASSIGNMENT = "_instructor_list_submissions";

	/** The instructor preview of assignment */
	private static final String TEMPLATE_INSTRUCTOR_PREVIEW_ASSIGNMENT = "_instructor_preview_assignment";

	/** The instructor view of assignment */
	private static final String TEMPLATE_INSTRUCTOR_VIEW_ASSIGNMENT = "_instructor_view_assignment";

	/** The instructor view to edit assignment */
	private static final String TEMPLATE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT = "_instructor_student_list_submissions";

	/** The instructor view to assignment submission report */
	private static final String TEMPLATE_INSTRUCTOR_REPORT_SUBMISSIONS = "_instructor_report_submissions";

	/** The instructor view to upload all information from archive file */
	private static final String TEMPLATE_INSTRUCTOR_UPLOAD_ALL = "_instructor_uploadAll";

	/** The opening mark comment */
	private static final String COMMENT_OPEN = "{{";

	/** The closing mark for comment */
	private static final String COMMENT_CLOSE = "}}";

	/** The selected view */
	private static final String STATE_SELECTED_VIEW = "state_selected_view";

	/** The configuration choice of with grading option or not */
	private static final String WITH_GRADES = "with_grades";

	/** The alert flag when doing global navigation from improper mode */
	private static final String ALERT_GLOBAL_NAVIGATION = "alert_global_navigation";

	/** The total list item before paging */
	private static final String STATE_PAGEING_TOTAL_ITEMS = "state_paging_total_items";

	/** is current user allowed to grade assignment? */
	private static final String STATE_ALLOW_GRADE_SUBMISSION = "state_allow_grade_submission";

	/** property for previous feedback attachments **/
	private static final String PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS = "prop_submission_previous_feedback_attachments";
	
	/** the user and submission list for list of submissions page */
	private static final String USER_SUBMISSIONS = "user_submissions";
	
	/** ************************* Taggable constants ************************** */
	/** identifier of tagging provider that will provide the appropriate helper */
	private static final String PROVIDER_ID = "providerId";

	/** Reference to an activity */
	private static final String ACTIVITY_REF = "activityRef";
	
	/** Reference to an item */
	private static final String ITEM_REF = "itemRef";
	
	/** session attribute for list of decorated tagging providers */
	private static final String PROVIDER_LIST = "providerList";
	
	// whether the choice of emails instructor submission notification is available in the installation
	private static final String ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS = "assignment.instructor.notifications";
	
	// default for whether or how the instructor receive submission notification emails, none(default)|each|digest
	private static final String ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DEFAULT = "assignment.instructor.notifications.default";
	
	/****************************** Upload all screen ***************************/
	private static final String UPLOAD_ALL_HAS_SUBMISSIONS = "upload_all_has_submissions";
	private static final String UPLOAD_ALL_HAS_GRADEFILE = "upload_all_has_gradefile";
	private static final String UPLOAD_ALL_HAS_COMMENTS= "upload_all_has_comments";
	private static final String UPLOAD_ALL_RELEASE_GRADES = "upload_all_release_grades";
	
	/**
	 * central place for dispatching the build routines based on the state name
	 */
	public String buildMainPanelContext(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		String template = null;

		context.put("tlang", rb);

		context.put("cheffeedbackhelper", this);

		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);

		// allow add assignment?
		boolean allowAddAssignment = AssignmentService.allowAddAssignment(contextString);
		context.put("allowAddAssignment", Boolean.valueOf(allowAddAssignment));

		Object allowGradeSubmission = state.getAttribute(STATE_ALLOW_GRADE_SUBMISSION);

		// allow update site?
		context.put("allowUpdateSite", Boolean
						.valueOf(SiteService.allowUpdateSite((String) state.getAttribute(STATE_CONTEXT_STRING))));

		
		//Is the review service allowed?
		context.put("allowReviewService", allowReviewService);

		// grading option
		context.put("withGrade", state.getAttribute(WITH_GRADES));

		String mode = (String) state.getAttribute(STATE_MODE);

		if (!mode.equals(MODE_LIST_ASSIGNMENTS))
		{
			// allow grade assignment?
			if (state.getAttribute(STATE_ALLOW_GRADE_SUBMISSION) == null)
			{
				state.setAttribute(STATE_ALLOW_GRADE_SUBMISSION, Boolean.FALSE);
			}
			context.put("allowGradeSubmission", state.getAttribute(STATE_ALLOW_GRADE_SUBMISSION));
		}

		if (mode.equals(MODE_LIST_ASSIGNMENTS))
		{
			// build the context for the student assignment view
			template = build_list_assignments_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_STUDENT_VIEW_ASSIGNMENT))
		{
			// the student view of assignment
			template = build_student_view_assignment_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_STUDENT_VIEW_SUBMISSION))
		{
			// disable auto-updates while leaving the list view
			justDelivered(state);

			// build the context for showing one assignment submission
			template = build_student_view_submission_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_STUDENT_VIEW_SUBMISSION_CONFIRMATION))
		{
			// build the context for showing one assignment submission confirmation
			template = build_student_view_submission_confirmation_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_STUDENT_PREVIEW_SUBMISSION))
		{
			// build the context for showing one assignment submission
			template = build_student_preview_submission_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_STUDENT_VIEW_GRADE))
		{
			// disable auto-updates while leaving the list view
			justDelivered(state);

			// build the context for showing one graded submission
			template = build_student_view_grade_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT))
		{
			// allow add assignment?
			boolean allowAddSiteAssignment = AssignmentService.allowAddSiteAssignment(contextString);
			context.put("allowAddSiteAssignment", Boolean.valueOf(allowAddSiteAssignment));

			// disable auto-updates while leaving the list view
			justDelivered(state);

			// build the context for the instructor's create new assignment view
			template = build_instructor_new_edit_assignment_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_INSTRUCTOR_DELETE_ASSIGNMENT))
		{
			if (state.getAttribute(DELETE_ASSIGNMENT_IDS) != null)
			{
				// disable auto-updates while leaving the list view
				justDelivered(state);

				// build the context for the instructor's delete assignment
				template = build_instructor_delete_assignment_context(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_GRADE_ASSIGNMENT))
		{
			if (allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, build the context for the instructor's grade assignment
				template = build_instructor_grade_assignment_context(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_GRADE_SUBMISSION))
		{
			if (allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, disable auto-updates while leaving the list view
				justDelivered(state);

				// build the context for the instructor's grade submission
				template = build_instructor_grade_submission_context(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION))
		{
			if ( allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, build the context for the instructor's preview grade submission
				template = build_instructor_preview_grade_submission_context(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_PREVIEW_ASSIGNMENT))
		{
			// build the context for preview one assignment
			template = build_instructor_preview_assignment_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_INSTRUCTOR_VIEW_ASSIGNMENT))
		{
			// disable auto-updates while leaving the list view
			justDelivered(state);

			// build the context for view one assignment
			template = build_instructor_view_assignment_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT))
		{
			if ( allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, build the context for the instructor's create new assignment view
				template = build_instructor_view_students_assignment_context(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_REPORT_SUBMISSIONS))
		{
			if ( allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, build the context for the instructor's view of report submissions
				template = build_instructor_report_submissions(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_UPLOAD_ALL))
		{
			if ( allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, build the context for the instructor's view of uploading all info from archive file
				template = build_instructor_upload_all(portlet, context, data, state);
			}
		}

		if (template == null)
		{
			// default to student list view
			state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
			template = build_list_assignments_context(portlet, context, data, state);
		}

		return template;

	} // buildNormalContext

		
	/**
	 * build the student view of showing an assignment submission
	 */
	protected String build_student_view_submission_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		context.put("context", contextString);

		User user = (User) state.getAttribute(STATE_USER);
		String currentAssignmentReference = (String) state.getAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE);
		Assignment assignment = null;
		try
		{
			assignment = AssignmentService.getAssignment(currentAssignmentReference);
			context.put("assignment", assignment);
			context.put("canSubmit", Boolean.valueOf(AssignmentService.canSubmit(contextString, assignment)));
			if (assignment.getContent().getTypeOfSubmission() == Assignment.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
			{
				context.put("nonElectronicType", Boolean.TRUE);
			}
			AssignmentSubmission s = AssignmentService.getSubmission(assignment.getReference(), user);
			if (s != null)
			{
				context.put("submission", s);
				ResourceProperties p = s.getProperties();
				if (p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT) != null)
				{
					context.put("prevFeedbackText", p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT));
				}

				if (p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT) != null)
				{
					context.put("prevFeedbackComment", p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT));
				}
				
				if (p.getProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS) != null)
				{
					context.put("prevFeedbackAttachments", getPrevFeedbackAttachments(p));
				}
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannot_find_assignment"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot16"));
		}

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.TaggingManager");
		if (taggingManager.isTaggable() && assignment != null)
		{
			addProviders(context, state);
			addActivity(context, assignment);
			context.put("taggable", Boolean.valueOf(true));
		}

		// name value pairs for the vm
		context.put("name_submission_text", VIEW_SUBMISSION_TEXT);
		context.put("value_submission_text", state.getAttribute(VIEW_SUBMISSION_TEXT));
		context.put("name_submission_honor_pledge_yes", VIEW_SUBMISSION_HONOR_PLEDGE_YES);
		context.put("value_submission_honor_pledge_yes", state.getAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES));
		context.put("attachments", state.getAttribute(ATTACHMENTS));
		
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("currentTime", TimeService.newTime());

		boolean allowSubmit = AssignmentService.allowAddSubmission((String) state.getAttribute(STATE_CONTEXT_STRING));
		if (!allowSubmit)
		{
			addAlert(state, rb.getString("not_allowed_to_submit"));
		}
		context.put("allowSubmit", new Boolean(allowSubmit));

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_STUDENT_VIEW_SUBMISSION;

	} // build_student_view_submission_context

	/**
	 * build the student view of showing an assignment submission confirmation
	 */
	protected String build_student_view_submission_confirmation_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		context.put("context", contextString);
		
		// get user information
		User user = (User) state.getAttribute(STATE_USER);
		context.put("user_name", user.getDisplayName());
		context.put("user_id", user.getDisplayId());
		
		// get site information
		try
		{
			// get current site
			Site site = SiteService.getSite(contextString);
			context.put("site_title", site.getTitle());
		}
		catch (Exception ignore)
		{
			Log.warn("chef", this + ignore.getMessage() + " siteId= " + contextString);
		}
		
		// get assignment and submission information
		String currentAssignmentReference = (String) state.getAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE);
		try
		{
			Assignment currentAssignment = AssignmentService.getAssignment(currentAssignmentReference);
			context.put("assignment_title", currentAssignment.getTitle());
			AssignmentSubmission s = AssignmentService.getSubmission(currentAssignment.getReference(), user);
			if (s != null)
			{
				context.put("submission_id", s.getId());
				context.put("submit_time", s.getTimeSubmitted().toStringLocalFull());
				List attachments = s.getSubmittedAttachments();
				if (attachments != null && attachments.size()>0)
				{
					context.put("submit_attachments", s.getSubmittedAttachments());
				}
				context.put("submit_text", StringUtil.trimToNull(s.getSubmittedText()));
				context.put("email_confirmation", Boolean.valueOf(ServerConfigurationService.getBoolean("assignment.submission.confirmation.email", true)));
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannot_find_assignment"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot16"));
		}	

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_STUDENT_VIEW_SUBMISSION_CONFIRMATION;

	} // build_student_view_submission_confirmation_context
	
	/**
	 * build the student view of assignment
	 */
	protected String build_student_view_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		context.put("context", state.getAttribute(STATE_CONTEXT_STRING));

		String aId = (String) state.getAttribute(VIEW_ASSIGNMENT_ID);

		Assignment assignment = null;
		
		try
		{
			assignment = AssignmentService.getAssignment(aId);
			context.put("assignment", assignment);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannot_find_assignment"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.TaggingManager");
		if (taggingManager.isTaggable() && assignment != null)
		{
			addProviders(context, state);
			addActivity(context, assignment);
			context.put("taggable", Boolean.valueOf(true));
		}

		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("gradeTypeTable", gradeTypeTable());
		context.put("userDirectoryService", UserDirectoryService.getInstance());

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_STUDENT_VIEW_ASSIGNMENT;

	} // build_student_view_submission_context

	/**
	 * build the student preview of showing an assignment submission
	 */
	protected String build_student_preview_submission_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		User user = (User) state.getAttribute(STATE_USER);
		String aReference = (String) state.getAttribute(PREVIEW_SUBMISSION_ASSIGNMENT_REFERENCE);

		try
		{
			context.put("assignment", AssignmentService.getAssignment(aReference));
			context.put("submission", AssignmentService.getSubmission(aReference, user));
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot16"));
		}

		context.put("text", state.getAttribute(PREVIEW_SUBMISSION_TEXT));
		context.put("honor_pledge_yes", state.getAttribute(PREVIEW_SUBMISSION_HONOR_PLEDGE_YES));
		context.put("attachments", state.getAttribute(PREVIEW_SUBMISSION_ATTACHMENTS));
		
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_STUDENT_PREVIEW_SUBMISSION;

	} // build_student_preview_submission_context

	/**
	 * build the student view of showing a graded submission
	 */
	protected String build_student_view_grade_context(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));

		AssignmentSubmission submission = null;
		try
		{
			submission = AssignmentService.getSubmission((String) state.getAttribute(VIEW_GRADE_SUBMISSION_ID));
			context.put("assignment", submission.getAssignment());
			context.put("submission", submission);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_get_submission"));
		}

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.TaggingManager");
		if (taggingManager.isTaggable() && submission != null)
		{
			AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
					.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");
			List<DecoratedTaggingProvider> providers = addProviders(context, state);
			List<TaggingHelperInfo> itemHelpers = new ArrayList<TaggingHelperInfo>();
			for (DecoratedTaggingProvider provider : providers)
			{
				TaggingHelperInfo helper = provider.getProvider()
						.getItemHelperInfo(
								assignmentActivityProducer.getItem(
										submission,
										UserDirectoryService.getCurrentUser()
												.getId()).getReference());
				if (helper != null)
				{
					itemHelpers.add(helper);
				}
			}
			addItem(context, submission, UserDirectoryService.getCurrentUser().getId());
			addActivity(context, submission.getAssignment());
			context.put("itemHelpers", itemHelpers);
			context.put("taggable", Boolean.valueOf(true));
		}

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_STUDENT_VIEW_GRADE;

	} // build_student_view_grade_context

	/**
	 * build the view of assignments list
	 */
	protected String build_list_assignments_context(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.TaggingManager");
		if (taggingManager.isTaggable())
		{
			context.put("producer", ComponentManager
					.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer"));
			context.put("providers", taggingManager.getProviders());
			context.put("taggable", Boolean.valueOf(true));
		}
		
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		context.put("contextString", contextString);
		context.put("user", state.getAttribute(STATE_USER));
		context.put("service", AssignmentService.getInstance());
		context.put("TimeService", TimeService.getInstance());
		context.put("LongObject", new Long(TimeService.newTime().getTime()));
		context.put("currentTime", TimeService.newTime());
		String sortedBy = (String) state.getAttribute(SORTED_BY);
		String sortedAsc = (String) state.getAttribute(SORTED_ASC);
		// clean sort criteria
		if (sortedBy.equals(SORTED_BY_GROUP_TITLE) || sortedBy.equals(SORTED_BY_GROUP_DESCRIPTION))
		{
			sortedBy = SORTED_BY_DUEDATE;
			sortedAsc = Boolean.TRUE.toString();
			state.setAttribute(SORTED_BY, sortedBy);
			state.setAttribute(SORTED_ASC, sortedAsc);
		}
		context.put("sortedBy", sortedBy);
		context.put("sortedAsc", sortedAsc);
		if (state.getAttribute(STATE_SELECTED_VIEW) != null)
		{
			context.put("view", state.getAttribute(STATE_SELECTED_VIEW));
		}

		GradebookService g = null;
		String gradebookUid = null;
		boolean gradebookDefined = AssignmentService.isGradebookDefined();
		if (gradebookDefined)
		{
			g = (GradebookService) (org.sakaiproject.service.gradebook.shared.GradebookService) ComponentManager.get("org.sakaiproject.service.gradebook.GradebookService");
			gradebookUid = ToolManager.getInstance().getCurrentPlacement().getContext();
		}
			
		List assignments = prepPage(state);
		
		// make sure for all non-electronic submission type of assignment, the submission number matches the number of site members
		for (int i = 0; i < assignments.size(); i++)
		{
			Assignment a = (Assignment) assignments.get(i);
			if (a.getContent().getTypeOfSubmission() == Assignment.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
			{
				List allowAddSubmissionUsers = AssignmentService.allowAddSubmissionUsers(a.getReference());
				List submissions = AssignmentService.getSubmissions(a);
				if (submissions != null && allowAddSubmissionUsers != null && allowAddSubmissionUsers.size() != submissions.size())
				{
					// if there is any newly added user who doesn't have a submission object yet, add the submission
					try
					{
						addSubmissionsForNonElectronicAssignment(state, AssignmentService.editAssignment(a.getReference())); 
					}
					catch (Exception e)
					{
						Log.warn("chef", this + e.getMessage());
					}
				}
			}
			
			// update externally maintained assignment entry in Gradebook
			if (gradebookDefined)
			{
				updateExternalAssignmentInGB(a, g, gradebookUid);
			}
		}

		context.put("assignments", assignments.iterator());

		// allow get assignment
		context.put("allowGetAssignment", Boolean.valueOf(AssignmentService.allowGetAssignment(contextString)));
		
		// test whether user user can grade at least one assignment
		// and update the state variable.
		boolean allowGradeSubmission = false;
		for (Iterator aIterator=assignments.iterator(); !allowGradeSubmission && aIterator.hasNext(); )
		{
			if (AssignmentService.allowGradeSubmission(((Assignment) aIterator.next()).getReference()))
			{
				allowGradeSubmission = true;
			}
		}
		state.setAttribute(STATE_ALLOW_GRADE_SUBMISSION, new Boolean(allowGradeSubmission));
		context.put("allowGradeSubmission", state.getAttribute(STATE_ALLOW_GRADE_SUBMISSION));

		// allow remove assignment?
		boolean allowRemoveAssignment = false;
		for (Iterator aIterator=assignments.iterator(); !allowRemoveAssignment && aIterator.hasNext(); )
		{
			if (AssignmentService.allowRemoveAssignment(((Assignment) aIterator.next()).getReference()))
			{
				allowRemoveAssignment = true;
			}
		}
		context.put("allowRemoveAssignment", Boolean.valueOf(allowRemoveAssignment));

		add2ndToolbarFields(data, context);

		// inform the observing courier that we just updated the page...
		// if there are pending requests to do so they can be cleared
		justDelivered(state);

		pagingInfoToContext(state, context);

		// put site object into context
		try
		{
			// get current site
			Site site = SiteService.getSite(contextString);
			context.put("site", site);
			// any group in the site?
			Collection groups = site.getGroups();
			context.put("groups", (groups != null && groups.size()>0)?Boolean.TRUE:Boolean.FALSE);

			// add active user list
			AuthzGroup realm = AuthzGroupService.getAuthzGroup(SiteService.siteReference(contextString));
			if (realm != null)
			{
				context.put("activeUserIds", realm.getUsers());
			}
		}
		catch (Exception ignore)
		{
			Log.warn("chef", this + ignore.getMessage() + " siteId= " + contextString);
		}

		boolean allowSubmit = AssignmentService.allowAddSubmission(contextString);
		context.put("allowSubmit", new Boolean(allowSubmit));
		
		// related to resubmit settings
		context.put("allowResubmitNumberProp", AssignmentSubmission.ALLOW_RESUBMIT_NUMBER);
		context.put("allowResubmitCloseTimeProp", AssignmentSubmission.ALLOW_RESUBMIT_CLOSETIME);
		
		// the type int for non-electronic submission
		context.put("typeNonElectronic", Integer.valueOf(Assignment.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION));

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_LIST_ASSIGNMENTS;

	} // build_list_assignments_context

	/**
	 * build the instructor view of creating a new assignment or editing an existing one
	 */
	protected String build_instructor_new_edit_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		// is the assignment an new assignment
		String assignmentId = (String) state.getAttribute(EDIT_ASSIGNMENT_ID);
		if (assignmentId != null)
		{
			try
			{
				Assignment a = AssignmentService.getAssignment(assignmentId);
				context.put("assignment", a);
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3") + ": " + assignmentId);
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot14") + ": " + assignmentId);
			}
		}

		// set up context variables
		setAssignmentFormContext(state, context);

		context.put("fField", state.getAttribute(NEW_ASSIGNMENT_FOCUS));

		String sortedBy = (String) state.getAttribute(SORTED_BY);
		String sortedAsc = (String) state.getAttribute(SORTED_ASC);
		context.put("sortedBy", sortedBy);
		context.put("sortedAsc", sortedAsc);

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT;

	} // build_instructor_new_assignment_context

	protected void setAssignmentFormContext(SessionState state, Context context)
	{
		// put the names and values into vm file
		
		
		context.put("name_UseReviewService", NEW_ASSIGNMENT_USE_REVIEW_SERVICE);
		context.put("name_AllowStudentView", NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW);
		
		context.put("name_title", NEW_ASSIGNMENT_TITLE);

		context.put("name_OpenMonth", NEW_ASSIGNMENT_OPENMONTH);
		context.put("name_OpenDay", NEW_ASSIGNMENT_OPENDAY);
		context.put("name_OpenYear", NEW_ASSIGNMENT_OPENYEAR);
		context.put("name_OpenHour", NEW_ASSIGNMENT_OPENHOUR);
		context.put("name_OpenMin", NEW_ASSIGNMENT_OPENMIN);
		context.put("name_OpenAMPM", NEW_ASSIGNMENT_OPENAMPM);

		context.put("name_DueMonth", NEW_ASSIGNMENT_DUEMONTH);
		context.put("name_DueDay", NEW_ASSIGNMENT_DUEDAY);
		context.put("name_DueYear", NEW_ASSIGNMENT_DUEYEAR);
		context.put("name_DueHour", NEW_ASSIGNMENT_DUEHOUR);
		context.put("name_DueMin", NEW_ASSIGNMENT_DUEMIN);
		context.put("name_DueAMPM", NEW_ASSIGNMENT_DUEAMPM);

		context.put("name_EnableCloseDate", NEW_ASSIGNMENT_ENABLECLOSEDATE);
		context.put("name_CloseMonth", NEW_ASSIGNMENT_CLOSEMONTH);
		context.put("name_CloseDay", NEW_ASSIGNMENT_CLOSEDAY);
		context.put("name_CloseYear", NEW_ASSIGNMENT_CLOSEYEAR);
		context.put("name_CloseHour", NEW_ASSIGNMENT_CLOSEHOUR);
		context.put("name_CloseMin", NEW_ASSIGNMENT_CLOSEMIN);
		context.put("name_CloseAMPM", NEW_ASSIGNMENT_CLOSEAMPM);

		context.put("name_Section", NEW_ASSIGNMENT_SECTION);
		context.put("name_SubmissionType", NEW_ASSIGNMENT_SUBMISSION_TYPE);
		context.put("name_GradeType", NEW_ASSIGNMENT_GRADE_TYPE);
		context.put("name_GradePoints", NEW_ASSIGNMENT_GRADE_POINTS);
		context.put("name_Description", NEW_ASSIGNMENT_DESCRIPTION);
		// do not show the choice when there is no Schedule tool yet
		if (state.getAttribute(CALENDAR) != null)
			context.put("name_CheckAddDueDate", ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE);
		//don't show the choice when there is no Announcement tool yet
		if (state.getAttribute(ANNOUNCEMENT_CHANNEL) != null)
			context.put("name_CheckAutoAnnounce", ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE);
		context.put("name_CheckAddHonorPledge", NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE);
		
		// number of resubmissions allowed
		context.put("name_allowResubmitNumber", AssignmentSubmission.ALLOW_RESUBMIT_NUMBER);
		
		// set the values
		context.put("value_year_from", state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_FROM));
		context.put("value_year_to", state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_TO));
		context.put("value_title", state.getAttribute(NEW_ASSIGNMENT_TITLE));
		context.put("value_OpenMonth", state.getAttribute(NEW_ASSIGNMENT_OPENMONTH));
		context.put("value_OpenDay", state.getAttribute(NEW_ASSIGNMENT_OPENDAY));
		context.put("value_OpenYear", state.getAttribute(NEW_ASSIGNMENT_OPENYEAR));
		context.put("value_OpenHour", state.getAttribute(NEW_ASSIGNMENT_OPENHOUR));
		context.put("value_OpenMin", state.getAttribute(NEW_ASSIGNMENT_OPENMIN));
		context.put("value_OpenAMPM", state.getAttribute(NEW_ASSIGNMENT_OPENAMPM));

		context.put("value_DueMonth", state.getAttribute(NEW_ASSIGNMENT_DUEMONTH));
		context.put("value_DueDay", state.getAttribute(NEW_ASSIGNMENT_DUEDAY));
		context.put("value_DueYear", state.getAttribute(NEW_ASSIGNMENT_DUEYEAR));
		context.put("value_DueHour", state.getAttribute(NEW_ASSIGNMENT_DUEHOUR));
		context.put("value_DueMin", state.getAttribute(NEW_ASSIGNMENT_DUEMIN));
		context.put("value_DueAMPM", state.getAttribute(NEW_ASSIGNMENT_DUEAMPM));

		context.put("value_EnableCloseDate", state.getAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE));
		context.put("value_CloseMonth", state.getAttribute(NEW_ASSIGNMENT_CLOSEMONTH));
		context.put("value_CloseDay", state.getAttribute(NEW_ASSIGNMENT_CLOSEDAY));
		context.put("value_CloseYear", state.getAttribute(NEW_ASSIGNMENT_CLOSEYEAR));
		context.put("value_CloseHour", state.getAttribute(NEW_ASSIGNMENT_CLOSEHOUR));
		context.put("value_CloseMin", state.getAttribute(NEW_ASSIGNMENT_CLOSEMIN));
		context.put("value_CloseAMPM", state.getAttribute(NEW_ASSIGNMENT_CLOSEAMPM));

		context.put("value_Sections", state.getAttribute(NEW_ASSIGNMENT_SECTION));
		context.put("value_SubmissionType", state.getAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE));
		context.put("value_totalSubmissionTypes", new Integer(Assignment.SUBMISSION_TYPES.length));
		context.put("value_GradeType", state.getAttribute(NEW_ASSIGNMENT_GRADE_TYPE));
		// format to show one decimal place
		String maxGrade = (String) state.getAttribute(NEW_ASSIGNMENT_GRADE_POINTS);
		context.put("value_GradePoints", displayGrade(state, maxGrade));
		context.put("value_Description", state.getAttribute(NEW_ASSIGNMENT_DESCRIPTION));
		
		
		// Keep the use review service setting
		context.put("value_UseReviewService", state.getAttribute(NEW_ASSIGNMENT_USE_REVIEW_SERVICE));
		context.put("value_AllowStudentView", state.getAttribute(NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW));
		
		// don't show the choice when there is no Schedule tool yet
		if (state.getAttribute(CALENDAR) != null)
		context.put("value_CheckAddDueDate", state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE));
		
		// don't show the choice when there is no Announcement tool yet
		if (state.getAttribute(ANNOUNCEMENT_CHANNEL) != null)
				context.put("value_CheckAutoAnnounce", state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE));
		
		String s = (String) state.getAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE);
		if (s == null) s = "1";
		context.put("value_CheckAddHonorPledge", s);
		// number of resubmissions allowed
		if (state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER) != null)
		{
			context.put("value_allowResubmitNumber", Integer.valueOf((String) state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER)));
		}
		else
		{
			// defaults to 0
			context.put("value_allowResubmitNumber", Integer.valueOf(0));
		}

		// get all available assignments from Gradebook tool except for those created from
		boolean gradebookExists = AssignmentService.isGradebookDefined();
		if (gradebookExists)
		{
			GradebookService g = (GradebookService) (org.sakaiproject.service.gradebook.shared.GradebookService) ComponentManager.get("org.sakaiproject.service.gradebook.GradebookService");
			String gradebookUid = ToolManager.getInstance().getCurrentPlacement().getContext();

			try
			{
				// get all assignments in Gradebook
				List gradebookAssignments = g.getAssignments(gradebookUid);
				List gradebookAssignmentsExceptSamigo = new Vector();
	
				// filtering out those from Samigo
				for (Iterator i=gradebookAssignments.iterator(); i.hasNext();)
				{
					org.sakaiproject.service.gradebook.shared.Assignment gAssignment = (org.sakaiproject.service.gradebook.shared.Assignment) i.next();
					if (!gAssignment.isExternallyMaintained() || gAssignment.isExternallyMaintained() && gAssignment.getExternalAppName().equals(getToolTitle()))
					{
						gradebookAssignmentsExceptSamigo.add(gAssignment);
					}
				}
				context.put("gradebookAssignments", gradebookAssignmentsExceptSamigo);
				if (StringUtil.trimToNull((String) state.getAttribute(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK)) == null)
				{
					state.setAttribute(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, AssignmentService.GRADEBOOK_INTEGRATION_NO);
				}
				
				context.put("withGradebook", Boolean.TRUE);
				
				// offer the gradebook integration choice only in the Assignments with Grading tool
				boolean withGrade = ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue();
				if (withGrade)
				{
					context.put("name_Addtogradebook", AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK);
					context.put("name_AssociateGradebookAssignment", AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
				}
				
				context.put("gradebookChoice", state.getAttribute(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK));
				context.put("gradebookChoice_no", AssignmentService.GRADEBOOK_INTEGRATION_NO);
				context.put("gradebookChoice_add", AssignmentService.GRADEBOOK_INTEGRATION_ADD);
				context.put("gradebookChoice_associate", AssignmentService.GRADEBOOK_INTEGRATION_ASSOCIATE);
				context.put("associateGradebookAssignment", state.getAttribute(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT));
			}
			catch (Exception e)
			{
				// not able to link to Gradebook
				Log.warn("chef", this + e.getMessage());
			}
		}

		context.put("monthTable", monthTable());
		context.put("gradeTypeTable", gradeTypeTable());
		context.put("submissionTypeTable", submissionTypeTable());
		context.put("hide_assignment_option_flag", state.getAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG));
		context.put("attachments", state.getAttribute(ATTACHMENTS));
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));

		String range = (String) state.getAttribute(NEW_ASSIGNMENT_RANGE);
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);

		// put site object into context
		try
		{
			// get current site
			Site site = SiteService.getSite((String) state.getAttribute(STATE_CONTEXT_STRING));
			context.put("site", site);
		}
		catch (Exception ignore)
		{
		}

		if (AssignmentService.getAllowGroupAssignments())
		{
			Collection groupsAllowAddAssignment = AssignmentService.getGroupsAllowAddAssignment(contextString);
			
			if (AssignmentService.allowAddSiteAssignment(contextString))
			{
				// default to make site selection
				context.put("range", "site");
			}
			else if (groupsAllowAddAssignment.size() > 0)
			{
				// to group otherwise
				context.put("range", "groups");
			}
			
			// group list which user can add message to
			if (groupsAllowAddAssignment.size() > 0)
			{
				String sort = (String) state.getAttribute(SORTED_BY);
				String asc = (String) state.getAttribute(SORTED_ASC);
				if (sort == null || (!sort.equals(SORTED_BY_GROUP_TITLE) && !sort.equals(SORTED_BY_GROUP_DESCRIPTION)))
				{
					sort = SORTED_BY_GROUP_TITLE;
					asc = Boolean.TRUE.toString();
					state.setAttribute(SORTED_BY, sort);
					state.setAttribute(SORTED_ASC, asc);
				}
				context.put("groups", new SortedIterator(groupsAllowAddAssignment.iterator(), new AssignmentComparator(state, sort, asc)));
				context.put("assignmentGroups", state.getAttribute(NEW_ASSIGNMENT_GROUPS));
			}
		}

		context.put("allowGroupAssignmentsInGradebook", new Boolean(AssignmentService.getAllowGroupAssignmentsInGradebook()));

		// the notification email choices
		if (state.getAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS) != null && ((Boolean) state.getAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS)).booleanValue())
		{
			context.put("name_assignment_instructor_notifications", ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS);
			if (state.getAttribute(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE) == null)
			{
				// set the notification value using site default
				state.setAttribute(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE, state.getAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DEFAULT));
			}
			context.put("value_assignment_instructor_notifications", state.getAttribute(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE));
			// the option values
			context.put("value_assignment_instructor_notifications_none", Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_NONE);
			context.put("value_assignment_instructor_notifications_each", Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_EACH);
			context.put("value_assignment_instructor_notifications_digest", Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DIGEST);
		}
	} // setAssignmentFormContext

	/**
	 * build the instructor view of create a new assignment
	 */
	protected String build_instructor_preview_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		context.put("time", TimeService.newTime());

		context.put("user", UserDirectoryService.getCurrentUser());

		context.put("value_Title", (String) state.getAttribute(NEW_ASSIGNMENT_TITLE));

		Time openTime = getOpenTime(state);
		context.put("value_OpenDate", openTime);

		// due time
		int dueMonth = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEMONTH)).intValue();
		int dueDay = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEDAY)).intValue();
		int dueYear = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEYEAR)).intValue();
		int dueHour = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEHOUR)).intValue();
		int dueMin = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEMIN)).intValue();
		String dueAMPM = (String) state.getAttribute(NEW_ASSIGNMENT_DUEAMPM);
		if ((dueAMPM.equals("PM")) && (dueHour != 12))
		{
			dueHour = dueHour + 12;
		}
		if ((dueHour == 12) && (dueAMPM.equals("AM")))
		{
			dueHour = 0;
		}
		Time dueTime = TimeService.newTimeLocal(dueYear, dueMonth, dueDay, dueHour, dueMin, 0, 0);
		context.put("value_DueDate", dueTime);

		// close time
		Time closeTime = TimeService.newTime();
		Boolean enableCloseDate = (Boolean) state.getAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE);
		context.put("value_EnableCloseDate", enableCloseDate);
		if ((enableCloseDate).booleanValue())
		{
			int closeMonth = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEMONTH)).intValue();
			int closeDay = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEDAY)).intValue();
			int closeYear = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEYEAR)).intValue();
			int closeHour = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEHOUR)).intValue();
			int closeMin = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEMIN)).intValue();
			String closeAMPM = (String) state.getAttribute(NEW_ASSIGNMENT_CLOSEAMPM);
			if ((closeAMPM.equals("PM")) && (closeHour != 12))
			{
				closeHour = closeHour + 12;
			}
			if ((closeHour == 12) && (closeAMPM.equals("AM")))
			{
				closeHour = 0;
			}
			closeTime = TimeService.newTimeLocal(closeYear, closeMonth, closeDay, closeHour, closeMin, 0, 0);
			context.put("value_CloseDate", closeTime);
		}

		context.put("value_Sections", state.getAttribute(NEW_ASSIGNMENT_SECTION));
		context.put("value_SubmissionType", state.getAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE));
		context.put("value_GradeType", state.getAttribute(NEW_ASSIGNMENT_GRADE_TYPE));
		String maxGrade = (String) state.getAttribute(NEW_ASSIGNMENT_GRADE_POINTS);
		context.put("value_GradePoints", displayGrade(state, maxGrade));
		context.put("value_Description", state.getAttribute(NEW_ASSIGNMENT_DESCRIPTION));
		context.put("value_CheckAddDueDate", state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE));
		context.put("value_CheckAutoAnnounce", state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE));
		context.put("value_CheckAddHonorPledge", state.getAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE));

		// get all available assignments from Gradebook tool except for those created from
		if (AssignmentService.isGradebookDefined())
		{
			context.put("gradebookChoice", state.getAttribute(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK));
			context.put("associateGradebookAssignment", state.getAttribute(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT));
		}

		context.put("monthTable", monthTable());
		context.put("gradeTypeTable", gradeTypeTable());
		context.put("submissionTypeTable", submissionTypeTable());
		context.put("hide_assignment_option_flag", state.getAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG));
		context.put("attachments", state.getAttribute(ATTACHMENTS));

		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));

		context.put("preview_assignment_assignment_hide_flag", state.getAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_HIDE_FLAG));
		context.put("preview_assignment_student_view_hide_flag", state.getAttribute(PREVIEW_ASSIGNMENT_STUDENT_VIEW_HIDE_FLAG));
		context.put("value_assignment_id", state.getAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_ID));
		context.put("value_assignmentcontent_id", state.getAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENTCONTENT_ID));

		context.put("currentTime", TimeService.newTime());

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_PREVIEW_ASSIGNMENT;

	} // build_instructor_preview_assignment_context

	/**
	 * build the instructor view to delete an assignment
	 */
	protected String build_instructor_delete_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		Vector assignments = new Vector();
		Vector assignmentIds = (Vector) state.getAttribute(DELETE_ASSIGNMENT_IDS);
		for (int i = 0; i < assignmentIds.size(); i++)
		{
			try
			{
				Assignment a = AssignmentService.getAssignment((String) assignmentIds.get(i));

				Iterator submissions = AssignmentService.getSubmissions(a).iterator();
				if (submissions.hasNext())
				{
					// if there is submission to the assignment, show the alert
					addAlert(state, rb.getString("areyousur") + " \"" + a.getTitle() + "\" " + rb.getString("whihassub") + "\n");
				}
				assignments.add(a);
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot14"));
			}
		}
		context.put("assignments", assignments);
		context.put("service", AssignmentService.getInstance());
		context.put("currentTime", TimeService.newTime());

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_DELETE_ASSIGNMENT;

	} // build_instructor_delete_assignment_context

	/**
	 * build the instructor view to grade an submission
	 */
	protected String build_instructor_grade_submission_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		int gradeType = -1;

		// assignment
		Assignment a = null;
		try
		{
			a = AssignmentService.getAssignment((String) state.getAttribute(GRADE_SUBMISSION_ASSIGNMENT_ID));
			context.put("assignment", a);
			gradeType = a.getContent().getTypeOfGrade();
			
			context.put("isGBEntryReleased", Boolean.valueOf(isGBEntryReleased(a)));
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}

		// assignment submission
		try
		{
			AssignmentSubmission s = AssignmentService.getSubmission((String) state.getAttribute(GRADE_SUBMISSION_SUBMISSION_ID));
			if (s != null)
			{
				context.put("submission", s);
				ResourceProperties p = s.getProperties();
				if (p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT) != null)
				{
					context.put("prevFeedbackText", p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT));
				}

				if (p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT) != null)
				{
					context.put("prevFeedbackComment", p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT));
				}
				
				if (p.getProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS) != null)
				{
					context.put("prevFeedbackAttachments", getPrevFeedbackAttachments(p));
				}
				
				// get the submission level of close date setting
				context.put("name_CloseMonth", ALLOW_RESUBMIT_CLOSEMONTH);
				context.put("name_CloseDay", ALLOW_RESUBMIT_CLOSEDAY);
				context.put("name_CloseYear", ALLOW_RESUBMIT_CLOSEYEAR);
				context.put("name_CloseHour", ALLOW_RESUBMIT_CLOSEHOUR);
				context.put("name_CloseMin", ALLOW_RESUBMIT_CLOSEMIN);
				context.put("name_CloseAMPM", ALLOW_RESUBMIT_CLOSEAMPM);
				String closeTimeString =(String) state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_CLOSETIME);
				Time time = null;
				if (closeTimeString != null)
				{
					// if there is a local setting
					time = TimeService.newTime(Long.parseLong(closeTimeString));
				}
				else if (a != null)
				{
					// if there is no local setting, default to assignment close time
					time = a.getCloseTime();
				}
				TimeBreakdown closeTime = time.breakdownLocal();
				context.put("value_CloseMonth", new Integer(closeTime.getMonth()));
				context.put("value_CloseDay", new Integer(closeTime.getDay()));
				context.put("value_CloseYear", new Integer(closeTime.getYear()));
				int closeHour = closeTime.getHour();
				if (closeHour >= 12)
				{
					context.put("value_CloseAMPM", "PM");
				}
				else
				{
					context.put("value_CloseAMPM", "AM");
				}
				if (closeHour == 0)
				{
					// for midnight point, we mark it as 12AM
					closeHour = 12;
				}
				context.put("value_CloseHour", new Integer((closeHour > 12) ? closeHour - 12 : closeHour));
				context.put("value_CloseMin", new Integer(closeTime.getMin()));
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}

		context.put("user", state.getAttribute(STATE_USER));
		context.put("submissionTypeTable", submissionTypeTable());
		context.put("gradeTypeTable", gradeTypeTable());
		context.put("instructorAttachments", state.getAttribute(ATTACHMENTS));
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("service", AssignmentService.getInstance());

		// names
		context.put("name_grade_assignment_id", GRADE_SUBMISSION_ASSIGNMENT_ID);
		context.put("name_feedback_comment", GRADE_SUBMISSION_FEEDBACK_COMMENT);
		context.put("name_feedback_text", GRADE_SUBMISSION_FEEDBACK_TEXT);
		context.put("name_feedback_attachment", GRADE_SUBMISSION_FEEDBACK_ATTACHMENT);
		context.put("name_grade", GRADE_SUBMISSION_GRADE);
		context.put("name_allowResubmitNumber", AssignmentSubmission.ALLOW_RESUBMIT_NUMBER);

		// values
		context.put("value_year_from", state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_FROM));
		context.put("value_year_to", state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_TO));
		context.put("value_grade_assignment_id", state.getAttribute(GRADE_SUBMISSION_ASSIGNMENT_ID));
		context.put("value_feedback_comment", state.getAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT));
		context.put("value_feedback_text", state.getAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT));
		context.put("value_feedback_attachment", state.getAttribute(ATTACHMENTS));
		if (state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER) != null)
		{
			context.put("value_allowResubmitNumber", Integer.valueOf((String) state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER)));
		}

		// format to show one decimal place in grade
		context.put("value_grade", (gradeType == 3) ? displayGrade(state, (String) state.getAttribute(GRADE_SUBMISSION_GRADE))
				: state.getAttribute(GRADE_SUBMISSION_GRADE));

		context.put("assignment_expand_flag", state.getAttribute(GRADE_SUBMISSION_ASSIGNMENT_EXPAND_FLAG));
		context.put("gradingAttachments", state.getAttribute(ATTACHMENTS));

		// is this a non-electronic submission type of assignment
		context.put("nonElectronic", (a!=null && a.getContent().getTypeOfSubmission() == Assignment.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)?Boolean.TRUE:Boolean.FALSE);
		
		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_GRADE_SUBMISSION;

	} // build_instructor_grade_submission_context


	/**
	 * Check if the assignment has associated gradebook entry and if yes, is this entry marked as released to student?
	 * @param a
	 */
	private boolean isGBEntryReleased(Assignment a) 
	{
		boolean gbReleased = false;
		boolean gradebookExists = AssignmentService.isGradebookDefined();
		if (gradebookExists)
		{
			GradebookService g = (GradebookService) (org.sakaiproject.service.gradebook.shared.GradebookService) ComponentManager.get("org.sakaiproject.service.gradebook.GradebookService");
			String gradebookUid = ToolManager.getInstance().getCurrentPlacement().getContext();
			
			String associateGradebookAssignment = StringUtil.trimToNull(a.getProperties().getProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT));
			if (associateGradebookAssignment != null)
			{
				// check the release grades setting
				SecurityService.pushAdvisor(new SecurityAdvisor()
					{
						public SecurityAdvice isAllowed(String userId, String function, String reference)
						{
							return SecurityAdvice.ALLOWED;
						}
					});
				
				if (g.isAssignmentDefined(gradebookUid, associateGradebookAssignment))
				{
					org.sakaiproject.service.gradebook.shared.Assignment assignmentDefinition = g.getAssignment(gradebookUid, associateGradebookAssignment);
					if (assignmentDefinition != null && assignmentDefinition.isReleased())
					{
						// the assignment is set to be releasedd
						gbReleased = true;
					}
				}
				
				// clear the permission
				SecurityService.clearAdvisors();
			}
		}
		
		return gbReleased;
	}

	private List getPrevFeedbackAttachments(ResourceProperties p) {
		String attachmentsString = p.getProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS);
		String[] attachmentsReferences = attachmentsString.split(",");
		List prevFeedbackAttachments = EntityManager.newReferenceList();
		for (int k =0; k < attachmentsReferences.length; k++)
		{
			prevFeedbackAttachments.add(EntityManager.newReference(attachmentsReferences[k]));
		}
		return prevFeedbackAttachments;
	}

	/**
	 * build the instructor preview of grading submission
	 */
	protected String build_instructor_preview_grade_submission_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{

		// assignment
		int gradeType = -1;
		try
		{
			Assignment a = AssignmentService.getAssignment((String) state.getAttribute(GRADE_SUBMISSION_ASSIGNMENT_ID));
			context.put("assignment", a);
			gradeType = a.getContent().getTypeOfGrade();
			
			// check the gradebook settting, if any
			context.put("isGBEntryReleased", Boolean.valueOf(isGBEntryReleased(a)));
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		// submission
		try
		{
			context.put("submission", AssignmentService.getSubmission((String) state.getAttribute(GRADE_SUBMISSION_SUBMISSION_ID)));
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}

		User user = (User) state.getAttribute(STATE_USER);
		context.put("user", user);
		context.put("submissionTypeTable", submissionTypeTable());
		context.put("gradeTypeTable", gradeTypeTable());
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("service", AssignmentService.getInstance());

		// filter the feedback text for the instructor comment and mark it as red
		String feedbackText = (String) state.getAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT);
		context.put("feedback_comment", state.getAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT));
		context.put("feedback_text", feedbackText);
		context.put("feedback_attachment", state.getAttribute(GRADE_SUBMISSION_FEEDBACK_ATTACHMENT));

		// format to show one decimal place
		String grade = (String) state.getAttribute(GRADE_SUBMISSION_GRADE);
		if (gradeType == 3)
		{
			grade = displayGrade(state, grade);
		}
		context.put("grade", grade);

		context.put("comment_open", COMMENT_OPEN);
		context.put("comment_close", COMMENT_CLOSE);

		context.put("allowResubmitNumber", state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER));
		String closeTimeString =(String) state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_CLOSETIME);
		if (closeTimeString != null)
		{
			// close time for resubmit
			Time time = TimeService.newTime(Long.parseLong(closeTimeString));
			context.put("allowResubmitCloseTime", time.toStringLocalFull());
		}

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION;

	} // build_instructor_preview_grade_submission_context

	/**
	 * build the instructor view to grade an assignment
	 */
	protected String build_instructor_grade_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		context.put("user", state.getAttribute(STATE_USER));

		// sorting related fields
		context.put("sortedBy", state.getAttribute(SORTED_GRADE_SUBMISSION_BY));
		context.put("sortedAsc", state.getAttribute(SORTED_GRADE_SUBMISSION_ASC));
		context.put("sort_lastName", SORTED_GRADE_SUBMISSION_BY_LASTNAME);
		context.put("sort_submitTime", SORTED_GRADE_SUBMISSION_BY_SUBMIT_TIME);
		context.put("sort_submitStatus", SORTED_GRADE_SUBMISSION_BY_STATUS);
		context.put("sort_submitGrade", SORTED_GRADE_SUBMISSION_BY_GRADE);
		context.put("sort_submitReleased", SORTED_GRADE_SUBMISSION_BY_RELEASED);
		context.put("sort_submitReview", SORTED_GRADE_SUBMISSION_CONTENTREVIEW);

		Assignment assignment = null;
		try
		{
			assignment = AssignmentService.getAssignment((String) state.getAttribute(EXPORT_ASSIGNMENT_REF));
			context.put("assignment", assignment);
			state.setAttribute(EXPORT_ASSIGNMENT_ID, assignment.getId());
			List userSubmissions = prepPage(state);
			state.setAttribute(USER_SUBMISSIONS, userSubmissions);
			context.put("userSubmissions", state.getAttribute(USER_SUBMISSIONS));
			
			// ever set the default grade for no-submissions
			String noSubmissionDefaultGrade = assignment.getProperties().getProperty(GRADE_NO_SUBMISSION_DEFAULT_GRADE);
			if (noSubmissionDefaultGrade != null)
			{
				context.put("noSubmissionDefaultGrade", noSubmissionDefaultGrade);
			}
			
			// check the gradebook setting, if any
			context.put("isGBEntryReleased", Boolean.valueOf(isGBEntryReleased(assignment)));
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.TaggingManager");
		if (taggingManager.isTaggable() && assignment != null)
		{
			context.put("producer", ComponentManager
					.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer"));
			addProviders(context, state);
			addActivity(context, assignment);
			context.put("taggable", Boolean.valueOf(true));
		}

		context.put("submissionTypeTable", submissionTypeTable());
		context.put("gradeTypeTable", gradeTypeTable());
		context.put("attachments", state.getAttribute(ATTACHMENTS));
		
		
		// Get turnitin results for instructors
		
		
		
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("service", AssignmentService.getInstance());

		context.put("assignment_expand_flag", state.getAttribute(GRADE_ASSIGNMENT_EXPAND_FLAG));
		context.put("submission_expand_flag", state.getAttribute(GRADE_SUBMISSION_EXPAND_FLAG));

		// the user directory service
		context.put("userDirectoryService", UserDirectoryService.getInstance());
		add2ndToolbarFields(data, context);

		pagingInfoToContext(state, context);

		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		context.put("accessPointUrl", (ServerConfigurationService.getAccessUrl()).concat(AssignmentService.submissionsZipReference(
				contextString, (String) state.getAttribute(EXPORT_ASSIGNMENT_REF))));

		String template = (String) getContext(data).get("template");
		
		return template + TEMPLATE_INSTRUCTOR_GRADE_ASSIGNMENT;

	} // build_instructor_grade_assignment_context

	/**
	 * build the instructor view of an assignment
	 */
	protected String build_instructor_view_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		context.put("tlang", rb);
		
		Assignment assignment = null;
		try
		{
			assignment = AssignmentService.getAssignment((String) state.getAttribute(VIEW_ASSIGNMENT_ID));
			context.put("assignment", assignment);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.TaggingManager");
		if (taggingManager.isTaggable() && assignment != null)
		{
			List<DecoratedTaggingProvider> providers = addProviders(context, state);
			List<TaggingHelperInfo> activityHelpers = new ArrayList<TaggingHelperInfo>();
			AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
					.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");
			for (DecoratedTaggingProvider provider : providers)
			{
				TaggingHelperInfo helper = provider.getProvider()
						.getActivityHelperInfo(
								assignmentActivityProducer.getActivity(
										assignment).getReference());
				if (helper != null)
				{
					activityHelpers.add(helper);
				}
			}
			addActivity(context, assignment);
			context.put("activityHelpers", activityHelpers);
			context.put("taggable", Boolean.valueOf(true));
		}

		context.put("currentTime", TimeService.newTime());
		context.put("submissionTypeTable", submissionTypeTable());
		context.put("gradeTypeTable", gradeTypeTable());
		context.put("hideAssignmentFlag", state.getAttribute(VIEW_ASSIGNMENT_HIDE_ASSIGNMENT_FLAG));
		context.put("hideStudentViewFlag", state.getAttribute(VIEW_ASSIGNMENT_HIDE_STUDENT_VIEW_FLAG));
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));

		// the user directory service
		context.put("userDirectoryService", UserDirectoryService.getInstance());

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_VIEW_ASSIGNMENT;

	} // build_instructor_view_assignment_context

	/**
	 * build the instructor view to view the list of students for an assignment
	 */
	protected String build_instructor_view_students_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);

		// get the realm and its member
		List studentMembers = new Vector();
		String realmId = SiteService.siteReference((String) state.getAttribute(STATE_CONTEXT_STRING));
		try
		{
			AuthzGroup realm = AuthzGroupService.getAuthzGroup(realmId);
			Set allowSubmitMembers = realm.getUsersIsAllowed(AssignmentService.SECURE_ADD_ASSIGNMENT_SUBMISSION);
			for (Iterator allowSubmitMembersIterator=allowSubmitMembers.iterator(); allowSubmitMembersIterator.hasNext();)
			{
				// get user
				try
				{
					String userId = (String) allowSubmitMembersIterator.next();
					User user = UserDirectoryService.getUser(userId);
					studentMembers.add(user);
				}
				catch (Exception ee)
				{
					Log.warn("chef", this + ee.getMessage());
				}
			}
		}
		catch (GroupNotDefinedException e)
		{
			Log.warn("chef", this + " IdUnusedException, not found, or not an AuthzGroup object");
			addAlert(state, rb.getString("java.realm") + realmId);
		}
		
		context.put("studentMembers", studentMembers);
		context.put("assignmentService", AssignmentService.getInstance());
		
		Hashtable showStudentAssignments = new Hashtable();
		if (state.getAttribute(STUDENT_LIST_SHOW_TABLE) != null)
		{
			Set showStudentListSet = (Set) state.getAttribute(STUDENT_LIST_SHOW_TABLE);
			context.put("studentListShowSet", showStudentListSet);
			for (Iterator showStudentListSetIterator=showStudentListSet.iterator(); showStudentListSetIterator.hasNext();)
			{
				// get user
				try
				{
					String userId = (String) showStudentListSetIterator.next();
					User user = UserDirectoryService.getUser(userId);
					showStudentAssignments.put(user, AssignmentService.getAssignmentsForContext(contextString, userId));
				}
				catch (Exception ee)
				{
					Log.warn("chef", this + ee.getMessage());
				}
			}
			
		}

		context.put("studentAssignmentsTable", showStudentAssignments);

		add2ndToolbarFields(data, context);

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT;

	} // build_instructor_view_students_assignment_context

	/**
	 * build the instructor view to report the submissions
	 */
	protected String build_instructor_report_submissions(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		context.put("submissions", prepPage(state));

		context.put("sortedBy", (String) state.getAttribute(SORTED_SUBMISSION_BY));
		context.put("sortedAsc", (String) state.getAttribute(SORTED_SUBMISSION_ASC));
		context.put("sortedBy_lastName", SORTED_SUBMISSION_BY_LASTNAME);
		context.put("sortedBy_submitTime", SORTED_SUBMISSION_BY_SUBMIT_TIME);
		context.put("sortedBy_grade", SORTED_SUBMISSION_BY_GRADE);
		context.put("sortedBy_status", SORTED_SUBMISSION_BY_STATUS);
		context.put("sortedBy_released", SORTED_SUBMISSION_BY_RELEASED);
		context.put("sortedBy_assignment", SORTED_SUBMISSION_BY_ASSIGNMENT);
		context.put("sortedBy_maxGrade", SORTED_SUBMISSION_BY_MAX_GRADE);

		add2ndToolbarFields(data, context);

		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		context.put("accessPointUrl", ServerConfigurationService.getAccessUrl()
				+ AssignmentService.gradesSpreadsheetReference(contextString, null));

		pagingInfoToContext(state, context);

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_REPORT_SUBMISSIONS;

	} // build_instructor_report_submissions
	
	/**
	 * build the instructor view to upload information from archive file
	 */
	protected String build_instructor_upload_all(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{

		context.put("hasSubmissions", state.getAttribute(UPLOAD_ALL_HAS_SUBMISSIONS));
		context.put("hasGradeFile", state.getAttribute(UPLOAD_ALL_HAS_GRADEFILE));
		context.put("hasComments", state.getAttribute(UPLOAD_ALL_HAS_COMMENTS));
		context.put("releaseGrades", state.getAttribute(UPLOAD_ALL_RELEASE_GRADES));
		
		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_UPLOAD_ALL;

	} // build_instructor_upload_all

   /**
    ** Retrieve tool title from Tool configuration file or use default
    ** (This should return i18n version of tool title if available)
    **/
   private String getToolTitle()
   {
      Tool tool = ToolManager.getTool("sakai.assignment.grades");
      String toolTitle = null;

      if (tool == null)
        toolTitle = "Assignments";
      else
        toolTitle = tool.getTitle();

      return toolTitle;
   }

	/**
	 * Go to the instructor view
	 */
	public void doView_instructor(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		state.setAttribute(SORTED_BY, SORTED_BY_DUEDATE);
		state.setAttribute(SORTED_ASC, Boolean.TRUE.toString());

	} // doView_instructor

	/**
	 * Go to the student view
	 */
	public void doView_student(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// to the student list of assignment view
		state.setAttribute(SORTED_BY, SORTED_BY_DUEDATE);
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doView_student

	/**
	 * Action is to view the content of one specific assignment submission
	 */
	public void doView_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the submission context
		resetViewSubmission(state);

		ParameterParser params = data.getParameters();
		String assignmentReference = params.getString("assignmentReference");
		state.setAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE, assignmentReference);

		User u = (User) state.getAttribute(STATE_USER);

		try
		{
			AssignmentSubmission submission = AssignmentService.getSubmission(assignmentReference, u);

			if (submission != null)
			{
				state.setAttribute(VIEW_SUBMISSION_TEXT, submission.getSubmittedText());
				state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, (new Boolean(submission.getHonorPledgeFlag())).toString());
				List v = EntityManager.newReferenceList();
				Iterator l = submission.getSubmittedAttachments().iterator();
				while (l.hasNext())
				{
					v.add(l.next());
				}
				state.setAttribute(ATTACHMENTS, v);		
			}
			else
			{
				state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, "false");
				state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());	
			}

			state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_SUBMISSION);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		} // try

	} // doView_submission

	/**
	 * Preview of the submission
	 */
	public void doPreview_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		ParameterParser params = data.getParameters();
		// String assignmentId = params.getString(assignmentId);
		state.setAttribute(PREVIEW_SUBMISSION_ASSIGNMENT_REFERENCE, state.getAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE));

		// retrieve the submission text (as formatted text)
		boolean checkForFormattingErrors = true; // the student is submitting something - so check for errors
		String text = processFormattedTextFromBrowser(state, params.getCleanString(VIEW_SUBMISSION_TEXT), checkForFormattingErrors);

		state.setAttribute(PREVIEW_SUBMISSION_TEXT, text);
		state.setAttribute(VIEW_SUBMISSION_TEXT, text);

		// assign the honor pledge attribute
		String honor_pledge_yes = params.getString(VIEW_SUBMISSION_HONOR_PLEDGE_YES);
		if (honor_pledge_yes == null)
		{
			honor_pledge_yes = "false";
		}
		state.setAttribute(PREVIEW_SUBMISSION_HONOR_PLEDGE_YES, honor_pledge_yes);
		state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, honor_pledge_yes);
		
		state.setAttribute(PREVIEW_SUBMISSION_ATTACHMENTS, state.getAttribute(ATTACHMENTS));
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_STUDENT_PREVIEW_SUBMISSION);
		}
	} // doPreview_submission

	/**
	 * Preview of the grading of submission
	 */
	public void doPreview_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// read user input
		readGradeForm(data, state, "read");

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION);
		}

	} // doPreview_grade_submission

	/**
	 * Action is to end the preview submission process
	 */
	public void doDone_preview_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_SUBMISSION);

	} // doDone_preview_submission

	/**
	 * Action is to end the view assignment process
	 */
	public void doDone_view_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doDone_view_assignments

	/**
	 * Action is to end the preview new assignment process
	 */
	public void doDone_preview_new_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the new assignment page
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT);

	} // doDone_preview_new_assignment

	/**
	 * Action is to end the preview edit assignment process
	 */
	public void doDone_preview_edit_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the edit assignment page
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT);

	} // doDone_preview_edit_assignment

	/**
	 * Action is to end the user view assignment process and redirect him to the assignment list view
	 */
	public void doCancel_student_view_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the view assignment
		state.setAttribute(VIEW_ASSIGNMENT_ID, "");

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_student_view_assignment

	/**
	 * Action is to end the show submission process
	 */
	public void doCancel_show_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the view assignment
		state.setAttribute(VIEW_ASSIGNMENT_ID, "");

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_show_submission

	/**
	 * Action is to cancel the delete assignment process
	 */
	public void doCancel_delete_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the show assignment object
		state.setAttribute(DELETE_ASSIGNMENT_IDS, new Vector());

		// back to the instructor list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_delete_assignment

	/**
	 * Action is to end the show submission process
	 */
	public void doCancel_edit_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_edit_assignment

	/**
	 * Action is to end the show submission process
	 */
	public void doCancel_new_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the assignment object
		resetAssignment(state);

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_new_assignment

	/**
	 * Action is to cancel the grade submission process
	 */
	public void doCancel_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the assignment object
		// resetAssignment (state);

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);

	} // doCancel_grade_submission

	/**
	 * Action is to cancel the preview grade process
	 */
	public void doCancel_preview_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the instructor view of grading a submission
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_SUBMISSION);

	} // doCancel_preview_grade_submission
	
	/**
	 * Action is to cancel the preview grade process
	 */
	public void doCancel_preview_to_list_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the instructor view of grading a submission
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);

	} // doCancel_preview_to_list_submission
	
	

	/**
	 * Action is to return to the view of list assignments
	 */
	public void doList_assignments(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		state.setAttribute(SORTED_BY, SORTED_BY_DUEDATE);
		state.setAttribute(SORTED_ASC, Boolean.FALSE.toString());

	} // doList_assignments

	/**
	 * Action is to cancel the student view grade process
	 */
	public void doCancel_view_grade(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the view grade submission id
		state.setAttribute(VIEW_GRADE_SUBMISSION_ID, "");

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_view_grade

	/**
	 * Action is to save the grade to submission
	 */
	public void doSave_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		readGradeForm(data, state, "save");
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			grade_submission_option(data, "save");
		}

	} // doSave_grade_submission

	/**
	 * Action is to release the grade to submission
	 */
	public void doRelease_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		readGradeForm(data, state, "release");
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			grade_submission_option(data, "release");
		}

	} // doRelease_grade_submission

	/**
	 * Action is to return submission with or without grade
	 */
	public void doReturn_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		readGradeForm(data, state, "return");
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			grade_submission_option(data, "return");
		}

	} // doReturn_grade_submission

	/**
	 * Action is to return submission with or without grade from preview
	 */
	public void doReturn_preview_grade_submission(RunData data)
	{
		grade_submission_option(data, "return");

	} // doReturn_grade_preview_submission

	/**
	 * Action is to save submission with or without grade from preview
	 */
	public void doSave_preview_grade_submission(RunData data)
	{
		grade_submission_option(data, "save");

	} // doSave_grade_preview_submission

	/**
	 * Common grading routine plus specific operation to differenciate cases when saving, releasing or returning grade.
	 */
	private void grade_submission_option(RunData data, String gradeOption)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		String sId = (String) state.getAttribute(GRADE_SUBMISSION_SUBMISSION_ID);

		try
		{
			// for points grading, one have to enter number as the points
			String grade = (String) state.getAttribute(GRADE_SUBMISSION_GRADE);

			AssignmentSubmissionEdit sEdit = AssignmentService.editSubmission(sId);
			Assignment a = sEdit.getAssignment();

			if (gradeOption.equals("release"))
			{
				sEdit.setGradeReleased(true);
				// clear the returned flag
				sEdit.setReturned(false);
				sEdit.setTimeReturned(null);
			}
			else if (gradeOption.equals("return"))
			{
				if (StringUtil.trimToNull(grade) != null)
				{
					sEdit.setGradeReleased(true);
				}
				sEdit.setReturned(true);
				sEdit.setTimeReturned(TimeService.newTime());
				sEdit.setHonorPledgeFlag(Boolean.FALSE.booleanValue());
			}
			
			boolean withGrade = state.getAttribute(WITH_GRADES) != null ? ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue(): false;
			int typeOfGrade = a.getContent().getTypeOfGrade();
			if (!withGrade)
			{
				// no grade input needed for the without-grade version of assignment tool
				sEdit.setGraded(true);
				if (gradeOption.equals("return") || gradeOption.equals("release"))
				{
					sEdit.setGradeReleased(true);
				}
			}
			else if (grade == null)
			{
				sEdit.setGrade("");
				sEdit.setGraded(false);
				sEdit.setGradeReleased(false);
			}
			else
			{
				if (typeOfGrade == 1)
				{
					sEdit.setGrade("no grade");
					sEdit.setGraded(true);
				}
				else
				{
					if (!grade.equals(""))
					{
						if (typeOfGrade == 3)
						{
							sEdit.setGrade(grade);
						}
						else
						{
							sEdit.setGrade(grade);
						}
						sEdit.setGraded(true);
					}
				}
			}

			if (state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER) != null)
			{
				// get resubmit number
				ResourcePropertiesEdit pEdit = sEdit.getPropertiesEdit();
				pEdit.addProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER, (String) state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER));
			
				if (state.getAttribute(ALLOW_RESUBMIT_CLOSEYEAR) != null)
				{
					// get resubmit time
					Time closeTime = getAllowSubmitCloseTime(state);
					pEdit.addProperty(AssignmentSubmission.ALLOW_RESUBMIT_CLOSETIME, String.valueOf(closeTime.getTime()));
				}
				else
				{
					pEdit.removeProperty(AssignmentSubmission.ALLOW_RESUBMIT_CLOSETIME);
				}
			}

			// the instructor comment
			String feedbackCommentString = StringUtil
					.trimToNull((String) state.getAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT));
			if (feedbackCommentString != null)
			{
				sEdit.setFeedbackComment(feedbackCommentString);
			}

			// the instructor inline feedback
			String feedbackTextString = (String) state.getAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT);
			if (feedbackTextString != null)
			{
				sEdit.setFeedbackText(feedbackTextString);
			}

			List v = (List) state.getAttribute(GRADE_SUBMISSION_FEEDBACK_ATTACHMENT);
			if (v != null)
			{
				
				// clear the old attachments first
				sEdit.clearFeedbackAttachments();

				for (int i = 0; i < v.size(); i++)
				{
					sEdit.addFeedbackAttachment((Reference) v.get(i));
				}
			}

			String sReference = sEdit.getReference();

			AssignmentService.commitEdit(sEdit);
		}
		catch (IdUnusedException e)
		{
		}
		catch (PermissionException e)
		{
		}
		catch (InUseException e)
		{
			addAlert(state, rb.getString("somelsis") + " " + rb.getString("submiss"));
		} // try

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);
			state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
		}

	} // grade_submission_option

	/**
	 * Action is to save the submission as a draft
	 */
	public void doSave_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		// retrieve the submission text (as formatted text)
		boolean checkForFormattingErrors = true; // the student is submitting something - so check for errors
		String text = processFormattedTextFromBrowser(state, params.getCleanString(VIEW_SUBMISSION_TEXT), checkForFormattingErrors);

		if (text == null)
		{
			text = (String) state.getAttribute(VIEW_SUBMISSION_TEXT);
		}

		String honorPledgeYes = params.getString(VIEW_SUBMISSION_HONOR_PLEDGE_YES);
		if (honorPledgeYes == null)
		{
			honorPledgeYes = "false";
		}

		String aReference = (String) state.getAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE);
		User u = (User) state.getAttribute(STATE_USER);

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			try
			{
				Assignment a = AssignmentService.getAssignment(aReference);
				String assignmentId = a.getId();

				AssignmentSubmission submission = AssignmentService.getSubmission(aReference, u);
				if (submission != null)
				{
					// the submission already exists, change the text and honor pledge value, save as draft
					try
					{
						AssignmentSubmissionEdit edit = AssignmentService.editSubmission(submission.getReference());
						edit.setSubmittedText(text);
						edit.setHonorPledgeFlag(Boolean.valueOf(honorPledgeYes).booleanValue());
						edit.setSubmitted(false);
						// edit.addSubmitter (u);
						edit.setAssignment(a);

						// add attachments
						List attachments = (List) state.getAttribute(ATTACHMENTS);
						if (attachments != null)
						{
							// clear the old attachments first
							edit.clearSubmittedAttachments();

							// add each new attachment
							Iterator it = attachments.iterator();
							while (it.hasNext())
							{
								edit.addSubmittedAttachment((Reference) it.next());
							}
						}
						AssignmentService.commitEdit(edit);
					}
					catch (IdUnusedException e)
					{
						addAlert(state, rb.getString("cannotfin2") + " " + a.getTitle());
					}
					catch (PermissionException e)
					{
						addAlert(state, rb.getString("youarenot12"));
					}
					catch (InUseException e)
					{
						addAlert(state, rb.getString("somelsis") + " " + rb.getString("submiss"));
					}
				}
				else
				{
					// new submission, save as draft
					try
					{
						AssignmentSubmissionEdit edit = AssignmentService.addSubmission((String) state
								.getAttribute(STATE_CONTEXT_STRING), assignmentId);
						edit.setSubmittedText(text);
						edit.setHonorPledgeFlag(Boolean.valueOf(honorPledgeYes).booleanValue());
						edit.setSubmitted(false);
						// edit.addSubmitter (u);
						edit.setAssignment(a);

						// add attachments
						List attachments = (List) state.getAttribute(ATTACHMENTS);
						if (attachments != null)
						{
							// add each attachment
							Iterator it = attachments.iterator();
							while (it.hasNext())
							{
								edit.addSubmittedAttachment((Reference) it.next());
							}
						}
						AssignmentService.commitEdit(edit);
					}
					catch (PermissionException e)
					{
						addAlert(state, rb.getString("youarenot4"));
					}
				}
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin5"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("not_allowed_to_view"));
			}
		}

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
			state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
		}

	} // doSave_submission

	/**
	 * Action is to post the submission
	 */
	public void doPost_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		String aReference = (String) state.getAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE);
		Assignment a = null;
		try
		{
			a = AssignmentService.getAssignment(aReference);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin2"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}
		
		if (AssignmentService.canSubmit(contextString, a))
		{
			ParameterParser params = data.getParameters();
	
			// retrieve the submission text (as formatted text)
			boolean checkForFormattingErrors = true; // the student is submitting something - so check for errors
			String text = processFormattedTextFromBrowser(state, params.getCleanString(VIEW_SUBMISSION_TEXT), checkForFormattingErrors);
	
			if (text == null)
			{
				text = (String) state.getAttribute(VIEW_SUBMISSION_TEXT);
			}
			else
			{
				state.setAttribute(VIEW_SUBMISSION_TEXT, text);
			}
	
			String honorPledgeYes = params.getString(VIEW_SUBMISSION_HONOR_PLEDGE_YES);
			if (honorPledgeYes == null)
			{
				honorPledgeYes = (String) state.getAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES);
			}
	
			if (honorPledgeYes == null)
			{
				honorPledgeYes = "false";
			}
	
			User u = (User) state.getAttribute(STATE_USER);
			String assignmentId = "";
			if (state.getAttribute(STATE_MESSAGE) == null)
			{
				assignmentId = a.getId();
	
				if (a.getContent().getHonorPledge() != 1)
				{
					if (!Boolean.valueOf(honorPledgeYes).booleanValue())
					{
						addAlert(state, rb.getString("youarenot18"));
					}
					state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, honorPledgeYes);
				}

				// check the submission inputs based on the submission type
				int submissionType = a.getContent().getTypeOfSubmission();
				if (submissionType == 1)
				{
					// for the inline only submission
					if (text.length() == 0)
					{
						addAlert(state, rb.getString("youmust7"));
					}
				}
				else if (submissionType == 2)
				{
					// for the attachment only submission
					Vector v = (Vector) state.getAttribute(ATTACHMENTS);
					if ((v == null) || (v.size() == 0))
					{
						addAlert(state, rb.getString("youmust1"));
					}
				}
				else if (submissionType == 3)
				{
					// for the inline and attachment submission
					Vector v = (Vector) state.getAttribute(ATTACHMENTS);
					if ((text.length() == 0) && ((v == null) || (v.size() == 0)))
					{
						addAlert(state, rb.getString("youmust2"));
					}
				}
			}
	
			if ((state.getAttribute(STATE_MESSAGE) == null) && (a != null))
			{
				try
				{
					AssignmentSubmission submission = AssignmentService.getSubmission(a.getReference(), u);
					if (submission != null)
					{
						// the submission already exists, change the text and honor pledge value, post it
						try
						{
							AssignmentSubmissionEdit sEdit = AssignmentService.editSubmission(submission.getReference());
							sEdit.setSubmittedText(text);
							sEdit.setHonorPledgeFlag(Boolean.valueOf(honorPledgeYes).booleanValue());
							sEdit.setTimeSubmitted(TimeService.newTime());
							sEdit.setSubmitted(true);
	
							// for resubmissions
							// when resubmit, keep the Returned flag on till the instructor grade again.
							ResourcePropertiesEdit sPropertiesEdit = sEdit.getPropertiesEdit();
							if (sEdit.getGraded())
							{
								// add the current grade into previous grade histroy
								String previousGrades = (String) sEdit.getProperties().getProperty(
										ResourceProperties.PROP_SUBMISSION_SCALED_PREVIOUS_GRADES);
								if (previousGrades == null)
								{
									previousGrades = (String) sEdit.getProperties().getProperty(
											ResourceProperties.PROP_SUBMISSION_PREVIOUS_GRADES);
									if (previousGrades != null)
									{
										int typeOfGrade = a.getContent().getTypeOfGrade();
										if (typeOfGrade == 3)
										{
											// point grade assignment type
											// some old unscaled grades, need to scale the number and remove the old property
											String[] grades = StringUtil.split(previousGrades, " ");
											String newGrades = "";
											for (int jj = 0; jj < grades.length; jj++)
											{
												String grade = grades[jj];
												if (grade.indexOf(".") == -1)
												{
													// show the grade with decimal point
													grade = grade.concat(".0");
												}
												newGrades = newGrades.concat(grade + " ");
											}
											previousGrades = newGrades;
										}
										sPropertiesEdit.removeProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_GRADES);
									}
									else
									{
										previousGrades = "";
									}
								}
								previousGrades = previousGrades.concat(sEdit.getGradeDisplay() + " ");

								sPropertiesEdit.addProperty(ResourceProperties.PROP_SUBMISSION_SCALED_PREVIOUS_GRADES,
										previousGrades);

								// clear the current grade and make the submission ungraded
								sEdit.setGraded(false);
								sEdit.setGrade("");
								sEdit.setGradeReleased(false);
	
								// keep the history of assignment feed back text
								String feedbackTextHistory = sPropertiesEdit
										.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT) != null ? sPropertiesEdit
										.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT)
										: "";
								feedbackTextHistory = sEdit.getFeedbackText() + "\n" + feedbackTextHistory;
								sPropertiesEdit.addProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT,
										feedbackTextHistory);
	
								// keep the history of assignment feed back comment
								String feedbackCommentHistory = sPropertiesEdit
										.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT) != null ? sPropertiesEdit
										.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT)
										: "";
								feedbackCommentHistory = sEdit.getFeedbackComment() + "\n" + feedbackCommentHistory;
								sPropertiesEdit.addProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT,
										feedbackCommentHistory);
								
								// keep the history of assignment feed back comment
								String feedbackAttachmentHistory = sPropertiesEdit
										.getProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS) != null ? sPropertiesEdit
										.getProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS)
										: "";
								List feedbackAttachments = sEdit.getFeedbackAttachments();
								for (int k = 0; k<feedbackAttachments.size();k++)
								{
									feedbackAttachmentHistory = ((Reference) feedbackAttachments.get(k)).getReference() + "," + feedbackAttachmentHistory;
								}
								
								sPropertiesEdit.addProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS,
										feedbackAttachmentHistory);
	
								// reset the previous grading context
								sEdit.setFeedbackText("");
								sEdit.setFeedbackComment("");
								sEdit.clearFeedbackAttachments();
	
								// decrease the allow_resubmit_number
								if (sPropertiesEdit.getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER) != null)
								{
									int number = Integer.parseInt(sPropertiesEdit.getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER));
									// minus 1 from the submit number
									if (number>=1)
									{
										sPropertiesEdit.addProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER, String.valueOf(number-1));
									}
									else if (number == -1)
									{
										sPropertiesEdit.addProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER, String.valueOf(-1));
									}
								}
							}
							// sEdit.addSubmitter (u);
							sEdit.setAssignment(a);
	
							// add attachments
							List attachments = (List) state.getAttribute(ATTACHMENTS);
							if (attachments != null)
							{
								
								//Post the attachments before clearing so that we don't sumbit duplicate attachments
								//Check if we need to post the attachments
								if (a.getContent().getAllowReviewService()) {
									if (!attachments.isEmpty()) { 
										sEdit.postAttachment((Reference) attachments.get(0));
									}
								}
																 
								// clear the old attachments first
								sEdit.clearSubmittedAttachments();
	
								// add each new attachment
								Iterator it = attachments.iterator();
								while (it.hasNext())
								{
									sEdit.addSubmittedAttachment((Reference) it.next());
								}
							}
	
							AssignmentService.commitEdit(sEdit);
						}
						catch (IdUnusedException e)
						{
							addAlert(state, rb.getString("cannotfin2") + " " + a.getTitle());
						}
						catch (PermissionException e)
						{
							addAlert(state, rb.getString("no_permissiion_to_edit_submission"));
						}
						catch (InUseException e)
						{
							addAlert(state, rb.getString("somelsis") + " " + rb.getString("submiss"));
						}
					}
					else
					{
						// new submission, post it
						try
						{
							AssignmentSubmissionEdit edit = AssignmentService.addSubmission(contextString, assignmentId);
							edit.setSubmittedText(text);
							edit.setHonorPledgeFlag(Boolean.valueOf(honorPledgeYes).booleanValue());
							edit.setTimeSubmitted(TimeService.newTime());
							edit.setSubmitted(true);
							// edit.addSubmitter (u);
							edit.setAssignment(a);
	
							// add attachments
							List attachments = (List) state.getAttribute(ATTACHMENTS);
							if (attachments != null)
							{
	 							// add each attachment
								if ((!attachments.isEmpty()) && a.getContent().getAllowReviewService()) 
									edit.postAttachment((Reference) attachments.get(0));								
								
								// add each attachment
								Iterator it = attachments.iterator();
								while (it.hasNext())
								{
									edit.addSubmittedAttachment((Reference) it.next());
								}
							}
							
							// get the assignment setting for resubmitting
							if (a.getProperties().getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER) != null)
							{
								edit.getPropertiesEdit().addProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER, a.getProperties().getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER));
							}
	
							AssignmentService.commitEdit(edit);
						}
						catch (PermissionException e)
						{
							addAlert(state, rb.getString("youarenot13"));
						}
					} // if -else
				}
				catch (IdUnusedException e)
				{
					addAlert(state, rb.getString("cannotfin5"));
				}
				catch (PermissionException e)
				{
					addAlert(state, rb.getString("not_allowed_to_view"));
				}
	
			} // if
	
			if (state.getAttribute(STATE_MESSAGE) == null)
			{
				state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_SUBMISSION_CONFIRMATION);
			}
		}	// if

	} // doPost_submission
	
	/**
	 * Action is to confirm the submission and return to list view
	 */
	public void doConfirm_assignment_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
	}
	/**
	 * Action is to show the new assignment screen
	 */
	public void doNew_assignment(RunData data, Context context)
	{
		
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		if (!alertGlobalNavigation(state, data))
		{
			if (AssignmentService.allowAddAssignment((String) state.getAttribute(STATE_CONTEXT_STRING)))
			{
				resetAssignment(state);
				
				state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
				state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT);
			}
			else
			{
				addAlert(state, rb.getString("youarenot2"));
				state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
			}
		}

	} // doNew_Assignment

	/**
	 * Action is to save the input infos for assignment fields
	 *
	 * @param validify
	 *        Need to validify the inputs or not
	 */
	protected void setNewAssignmentParameters(RunData data, boolean validify)
	{
		// read the form inputs
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		// put the input value into the state attributes
		String title = params.getString(NEW_ASSIGNMENT_TITLE);
		state.setAttribute(NEW_ASSIGNMENT_TITLE, title);

		if (title.length() == 0)
		{
			// empty assignment title
			addAlert(state, rb.getString("plespethe1"));
		}

		// open time
		int openMonth = (new Integer(params.getString(NEW_ASSIGNMENT_OPENMONTH))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_OPENMONTH, new Integer(openMonth));
		int openDay = (new Integer(params.getString(NEW_ASSIGNMENT_OPENDAY))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_OPENDAY, new Integer(openDay));
		int openYear = (new Integer(params.getString(NEW_ASSIGNMENT_OPENYEAR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_OPENYEAR, new Integer(openYear));
		int openHour = (new Integer(params.getString(NEW_ASSIGNMENT_OPENHOUR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_OPENHOUR, new Integer(openHour));
		int openMin = (new Integer(params.getString(NEW_ASSIGNMENT_OPENMIN))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_OPENMIN, new Integer(openMin));
		String openAMPM = params.getString(NEW_ASSIGNMENT_OPENAMPM);
		state.setAttribute(NEW_ASSIGNMENT_OPENAMPM, openAMPM);
		if ((openAMPM.equals("PM")) && (openHour != 12))
		{
			openHour = openHour + 12;
		}
		if ((openHour == 12) && (openAMPM.equals("AM")))
		{
			openHour = 0;
		}
		Time openTime = TimeService.newTimeLocal(openYear, openMonth, openDay, openHour, openMin, 0, 0);
		// validate date
		if (!Validator.checkDate(openDay, openMonth, openYear))
		{
			addAlert(state, rb.getString("date.invalid") + rb.getString("date.opendate") + ".");
		}

		// due time
		int dueMonth = (new Integer(params.getString(NEW_ASSIGNMENT_DUEMONTH))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_DUEMONTH, new Integer(dueMonth));
		int dueDay = (new Integer(params.getString(NEW_ASSIGNMENT_DUEDAY))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_DUEDAY, new Integer(dueDay));
		int dueYear = (new Integer(params.getString(NEW_ASSIGNMENT_DUEYEAR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_DUEYEAR, new Integer(dueYear));
		int dueHour = (new Integer(params.getString(NEW_ASSIGNMENT_DUEHOUR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_DUEHOUR, new Integer(dueHour));
		int dueMin = (new Integer(params.getString(NEW_ASSIGNMENT_DUEMIN))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_DUEMIN, new Integer(dueMin));
		String dueAMPM = params.getString(NEW_ASSIGNMENT_DUEAMPM);
		state.setAttribute(NEW_ASSIGNMENT_DUEAMPM, dueAMPM);
		if ((dueAMPM.equals("PM")) && (dueHour != 12))
		{
			dueHour = dueHour + 12;
		}
		if ((dueHour == 12) && (dueAMPM.equals("AM")))
		{
			dueHour = 0;
		}
		Time dueTime = TimeService.newTimeLocal(dueYear, dueMonth, dueDay, dueHour, dueMin, 0, 0);
		// validate date
		if (dueTime.before(TimeService.newTime()))
		{
			addAlert(state, rb.getString("assig4"));
		}
		if (!Validator.checkDate(dueDay, dueMonth, dueYear))
		{
			addAlert(state, rb.getString("date.invalid") + rb.getString("date.duedate") + ".");
		}

		state.setAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE, new Boolean(true));

		// close time
		int closeMonth = (new Integer(params.getString(NEW_ASSIGNMENT_CLOSEMONTH))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_CLOSEMONTH, new Integer(closeMonth));
		int closeDay = (new Integer(params.getString(NEW_ASSIGNMENT_CLOSEDAY))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_CLOSEDAY, new Integer(closeDay));
		int closeYear = (new Integer(params.getString(NEW_ASSIGNMENT_CLOSEYEAR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_CLOSEYEAR, new Integer(closeYear));
		int closeHour = (new Integer(params.getString(NEW_ASSIGNMENT_CLOSEHOUR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_CLOSEHOUR, new Integer(closeHour));
		int closeMin = (new Integer(params.getString(NEW_ASSIGNMENT_CLOSEMIN))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_CLOSEMIN, new Integer(closeMin));
		String closeAMPM = params.getString(NEW_ASSIGNMENT_CLOSEAMPM);
		state.setAttribute(NEW_ASSIGNMENT_CLOSEAMPM, closeAMPM);
		if ((closeAMPM.equals("PM")) && (closeHour != 12))
		{
			closeHour = closeHour + 12;
		}
		if ((closeHour == 12) && (closeAMPM.equals("AM")))
		{
			closeHour = 0;
		}
		Time closeTime = TimeService.newTimeLocal(closeYear, closeMonth, closeDay, closeHour, closeMin, 0, 0);
		// validate date
		if (!Validator.checkDate(closeDay, closeMonth, closeYear))
		{
			addAlert(state, rb.getString("date.invalid") + rb.getString("date.closedate") + ".");
		}
		if (closeTime.before(openTime))
		{
			addAlert(state, rb.getString("acesubdea3"));
		}
		if (closeTime.before(dueTime))
		{
			addAlert(state, rb.getString("acesubdea2"));
		}

		// SECTION MOD
		String sections_string = "";
		String mode = (String) state.getAttribute(STATE_MODE);
		if (mode == null) mode = "";

		state.setAttribute(NEW_ASSIGNMENT_SECTION, sections_string);
		state.setAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE, new Integer(params.getString(NEW_ASSIGNMENT_SUBMISSION_TYPE)));

		int gradeType = -1;

		// grade type and grade points
		if (state.getAttribute(WITH_GRADES) != null && ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue())
		{
			gradeType = Integer.parseInt(params.getString(NEW_ASSIGNMENT_GRADE_TYPE));
			state.setAttribute(NEW_ASSIGNMENT_GRADE_TYPE, new Integer(gradeType));
		}

		
		String r = params.getString(NEW_ASSIGNMENT_USE_REVIEW_SERVICE);
		String b;
		// set whether we use the review service or not
		if (r == null) b = Boolean.FALSE.toString();
		else b = Boolean.TRUE.toString();
		state.setAttribute(NEW_ASSIGNMENT_USE_REVIEW_SERVICE, b);
		
		//set whether students can view the review service results
		r = params.getString(NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW);
		if (r == null) b = Boolean.FALSE.toString();
		else b = Boolean.TRUE.toString();
		state.setAttribute(NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW, b);
		
		// treat the new assignment description as formatted text
		boolean checkForFormattingErrors = true; // instructor is creating a new assignment - so check for errors
		String description = processFormattedTextFromBrowser(state, params.getCleanString(NEW_ASSIGNMENT_DESCRIPTION),
				checkForFormattingErrors);
		state.setAttribute(NEW_ASSIGNMENT_DESCRIPTION, description);

		if (state.getAttribute(CALENDAR) != null)
		{
			// calendar enabled for the site
			if (params.getString(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE) != null
					&& params.getString(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE).equalsIgnoreCase(Boolean.TRUE.toString()))
			{
				state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE, Boolean.TRUE.toString());
			}
			else
			{
				state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE, Boolean.FALSE.toString());
			}
		}
		else
		{
			// no calendar yet for the site
			state.removeAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE);
		}

		if (params.getString(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE) != null
				&& params.getString(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE)
						.equalsIgnoreCase(Boolean.TRUE.toString()))
		{
			state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE, Boolean.TRUE.toString());
		}
		else
		{
			state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE, Boolean.FALSE.toString());
		}

		String s = params.getString(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE);

		// set the honor pledge to be "no honor pledge"
		if (s == null) s = "1";
		state.setAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE, s);

		String grading = params.getString(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK);
		state.setAttribute(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, grading);

		// only when choose to associate with assignment in Gradebook
		String associateAssignment = params.getString(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);

		if (grading != null)
		{
			if (grading.equals(AssignmentService.GRADEBOOK_INTEGRATION_ASSOCIATE))
			{
				state.setAttribute(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, associateAssignment);
			}
			else
			{
				state.setAttribute(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, "");
			}

			if (!grading.equals(AssignmentService.GRADEBOOK_INTEGRATION_NO))
			{
				// gradebook integration only available to point-grade assignment
				if (gradeType != Assignment.SCORE_GRADE_TYPE)
				{
					addAlert(state, rb.getString("addtogradebook.wrongGradeScale"));
				}

				// if chosen as "associate", have to choose one assignment from Gradebook
				if (grading.equals(AssignmentService.GRADEBOOK_INTEGRATION_ASSOCIATE) && StringUtil.trimToNull(associateAssignment) == null)
				{
					addAlert(state, rb.getString("grading.associate.alert"));
				}
			}
		}

		List attachments = (List) state.getAttribute(ATTACHMENTS);
		state.setAttribute(NEW_ASSIGNMENT_ATTACHMENT, attachments);

		// correct inputs
		// checks on the times
		if (validify && dueTime.before(openTime))
		{
			addAlert(state, rb.getString("assig3"));
		}

		if (validify)
		{
			if (((description == null) || (description.length() == 0)) && ((attachments == null || attachments.size() == 0)))
			{
				// if there is no description nor an attachment, show the following alert message.
				// One could ignore the message and still post the assignment
				if (state.getAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY) == null)
				{
					state.setAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY, Boolean.TRUE.toString());
				}
				else
				{
					state.removeAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY);
				}
			}
			else
			{
				state.removeAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY);
			}
		}

		if (validify && state.getAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY) != null)
		{
			addAlert(state, rb.getString("thiasshas"));
		}

		if (state.getAttribute(WITH_GRADES) != null && ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue())
		{
			// the grade point
			String gradePoints = params.getString(NEW_ASSIGNMENT_GRADE_POINTS);
			state.setAttribute(NEW_ASSIGNMENT_GRADE_POINTS, gradePoints);
			if (gradePoints != null)
			{
				if (gradeType == 3)
				{
					if ((gradePoints.length() == 0))
					{
						// in case of point grade assignment, user must specify maximum grade point
						addAlert(state, rb.getString("plespethe3"));
					}
					else
					{
						validPointGrade(state, gradePoints);
						// when scale is points, grade must be integer and less than maximum value
						if (state.getAttribute(STATE_MESSAGE) == null)
						{
							gradePoints = scalePointGrade(state, gradePoints);
						}
						state.setAttribute(NEW_ASSIGNMENT_GRADE_POINTS, gradePoints);
					}
				}
			}
		}

		// assignment range?
		String range = data.getParameters().getString("range");
		state.setAttribute(NEW_ASSIGNMENT_RANGE, range);
		if (range.equals("groups"))
		{
			String[] groupChoice = data.getParameters().getStrings("selectedGroups");
			if (groupChoice != null && groupChoice.length != 0)
			{
				state.setAttribute(NEW_ASSIGNMENT_GROUPS, new ArrayList(Arrays.asList(groupChoice)));
			}
			else
			{
				state.setAttribute(NEW_ASSIGNMENT_GROUPS, null);
				addAlert(state, rb.getString("java.alert.youchoosegroup"));
			}
		}
		else
		{
			state.removeAttribute(NEW_ASSIGNMENT_GROUPS);
		}
		
		// allow resubmission numbers
		String nString = params.getString(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER);
		if (nString != null)
		{
			state.setAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER, nString);
		}
		
		// assignment notification option
		String notiOption = params.getString(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS);
		if (notiOption != null)
		{
			state.setAttribute(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE, notiOption);
		}
	} // setNewAssignmentParameters

	/**
	 * Action is to hide the preview assignment student view
	 */
	public void doHide_submission_assignment_instruction(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_SUBMISSION_ASSIGNMENT_EXPAND_FLAG, new Boolean(false));

		// save user input
		readGradeForm(data, state, "read");

	} // doHide_preview_assignment_student_view

	/**
	 * Action is to show the preview assignment student view
	 */
	public void doShow_submission_assignment_instruction(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_SUBMISSION_ASSIGNMENT_EXPAND_FLAG, new Boolean(true));

		// save user input
		readGradeForm(data, state, "read");

	} // doShow_submission_assignment_instruction

	/**
	 * Action is to hide the preview assignment student view
	 */
	public void doHide_preview_assignment_student_view(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(PREVIEW_ASSIGNMENT_STUDENT_VIEW_HIDE_FLAG, new Boolean(true));

	} // doHide_preview_assignment_student_view

	/**
	 * Action is to show the preview assignment student view
	 */
	public void doShow_preview_assignment_student_view(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(PREVIEW_ASSIGNMENT_STUDENT_VIEW_HIDE_FLAG, new Boolean(false));

	} // doShow_preview_assignment_student_view

	/**
	 * Action is to hide the preview assignment assignment infos
	 */
	public void doHide_preview_assignment_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_HIDE_FLAG, new Boolean(true));

	} // doHide_preview_assignment_assignment

	/**
	 * Action is to show the preview assignment assignment info
	 */
	public void doShow_preview_assignment_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_HIDE_FLAG, new Boolean(false));

	} // doShow_preview_assignment_assignment

	/**
	 * Action is to hide the assignment option
	 */
	public void doHide_assignment_option(RunData data)
	{
		setNewAssignmentParameters(data, false);
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG, new Boolean(true));
		state.setAttribute(NEW_ASSIGNMENT_FOCUS, "eventSubmit_doShow_assignment_option");

	} // doHide_assignment_option

	/**
	 * Action is to show the assignment option
	 */
	public void doShow_assignment_option(RunData data)
	{
		setNewAssignmentParameters(data, false);
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG, new Boolean(false));
		state.setAttribute(NEW_ASSIGNMENT_FOCUS, NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE);

	} // doShow_assignment_option

	/**
	 * Action is to hide the assignment content in the view assignment page
	 */
	public void doHide_view_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_ASSIGNMENT_FLAG, new Boolean(true));

	} // doHide_view_assignment

	/**
	 * Action is to show the assignment content in the view assignment page
	 */
	public void doShow_view_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_ASSIGNMENT_FLAG, new Boolean(false));

	} // doShow_view_assignment

	/**
	 * Action is to hide the student view in the view assignment page
	 */
	public void doHide_view_student_view(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_STUDENT_VIEW_FLAG, new Boolean(true));

	} // doHide_view_student_view

	/**
	 * Action is to show the student view in the view assignment page
	 */
	public void doShow_view_student_view(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_STUDENT_VIEW_FLAG, new Boolean(false));

	} // doShow_view_student_view

	/**
	 * Action is to post assignment
	 */
	public void doPost_assignment(RunData data)
	{
		// post assignment
		postOrSaveAssignment(data, "post");

	} // doPost_assignment

	/**
	 * Action is to tag items via an items tagging helper
	 */
	public void doHelp_items(RunData data) {
		SessionState state = ((JetspeedRunData) data)
				.getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.TaggingManager");
		TaggingProvider provider = taggingManager.findProviderById(params
				.getString(PROVIDER_ID));

		String activityRef = params.getString(ACTIVITY_REF);

		TaggingHelperInfo helperInfo = provider
				.getItemsHelperInfo(activityRef);

		// get into helper mode with this helper tool
		startHelper(data.getRequest(), helperInfo.getHelperId());

		Map<String, ? extends Object> helperParms = helperInfo
				.getParameterMap();

		for (Iterator<String> keys = helperParms.keySet().iterator(); keys
				.hasNext();) {
			String key = keys.next();
			state.setAttribute(key, helperParms.get(key));
		}
	} // doHelp_items
	
	/**
	 * Action is to tag an individual item via an item tagging helper
	 */
	public void doHelp_item(RunData data) {
		SessionState state = ((JetspeedRunData) data)
				.getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.TaggingManager");
		TaggingProvider provider = taggingManager.findProviderById(params
				.getString(PROVIDER_ID));

		String itemRef = params.getString(ITEM_REF);

		TaggingHelperInfo helperInfo = provider
				.getItemHelperInfo(itemRef);

		// get into helper mode with this helper tool
		startHelper(data.getRequest(), helperInfo.getHelperId());

		Map<String, ? extends Object> helperParms = helperInfo
				.getParameterMap();

		for (Iterator<String> keys = helperParms.keySet().iterator(); keys
				.hasNext();) {
			String key = keys.next();
			state.setAttribute(key, helperParms.get(key));
		}
	} // doHelp_item
	
	/**
	 * Action is to tag an activity via an activity tagging helper
	 */
	public void doHelp_activity(RunData data) {
		SessionState state = ((JetspeedRunData) data)
				.getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.TaggingManager");
		TaggingProvider provider = taggingManager.findProviderById(params
				.getString(PROVIDER_ID));

		String activityRef = params.getString(ACTIVITY_REF);

		TaggingHelperInfo helperInfo = provider
				.getActivityHelperInfo(activityRef);

		// get into helper mode with this helper tool
		startHelper(data.getRequest(), helperInfo.getHelperId());

		Map<String, ? extends Object> helperParms = helperInfo
				.getParameterMap();

		for (String key : helperParms.keySet()) {
			state.setAttribute(key, helperParms.get(key));
		}
	} // doHelp_activity
	
	/**
	 * post or save assignment
	 */
	private void postOrSaveAssignment(RunData data, String postOrSave)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		
		ParameterParser params = data.getParameters();
		
		String siteId = (String) state.getAttribute(STATE_CONTEXT_STRING);
		
		boolean post = (postOrSave != null) && postOrSave.equals("post");

		// assignment old title
		String aOldTitle = null;

		// assignment old associated Gradebook entry if any
		String oAssociateGradebookAssignment = null;

		String mode = (String) state.getAttribute(STATE_MODE);
		if (!mode.equals(MODE_INSTRUCTOR_PREVIEW_ASSIGNMENT))
		{
			// read input data if the mode is not preview mode
			setNewAssignmentParameters(data, true);
		}
		
		String assignmentId = params.getString("assignmentId");
		String assignmentContentId = params.getString("assignmentContentId");

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			// AssignmentContent object
			AssignmentContentEdit ac = getAssignmentContentEdit(state, assignmentContentId);

			// Assignment
			AssignmentEdit a = getAssignmentEdit(state, assignmentId);

			// put the names and values into vm file
			String title = (String) state.getAttribute(NEW_ASSIGNMENT_TITLE);

			// open time
			Time openTime = getOpenTime(state);

			// due time
			Time dueTime = getDueTime(state);

			// close time
			Time closeTime = dueTime;
			boolean enableCloseDate = ((Boolean) state.getAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE)).booleanValue();
			if (enableCloseDate)
			{
				closeTime = getCloseTime(state);
			}

			// sections
			String s = (String) state.getAttribute(NEW_ASSIGNMENT_SECTION);

			int submissionType = ((Integer) state.getAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE)).intValue();

			int gradeType = ((Integer) state.getAttribute(NEW_ASSIGNMENT_GRADE_TYPE)).intValue();

			String gradePoints = (String) state.getAttribute(NEW_ASSIGNMENT_GRADE_POINTS);

			String description = (String) state.getAttribute(NEW_ASSIGNMENT_DESCRIPTION);

			String checkAddDueTime = state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE)!=null?(String) state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE):null;

			String checkAutoAnnounce = (String) state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE);

			String checkAddHonorPledge = (String) state.getAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE);

			String addtoGradebook = state.getAttribute(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK) != null?(String) state.getAttribute(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK):"" ;

			String associateGradebookAssignment = (String) state.getAttribute(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
			
			String allowResubmitNumber = state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER) != null?(String) state.getAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER):null;
			
			boolean useReviewService = "true".equalsIgnoreCase((String) state.getAttribute(NEW_ASSIGNMENT_USE_REVIEW_SERVICE));
			
			boolean allowStudentViewReport = "true".equalsIgnoreCase((String) state.getAttribute(NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW));
			
			// the attachments
			List attachments = (List) state.getAttribute(ATTACHMENTS);
			List attachments1 = EntityManager.newReferenceList(attachments);
			
			// set group property
			String range = (String) state.getAttribute(NEW_ASSIGNMENT_RANGE);
			Collection groups = new Vector();
			try
			{
				Site site = SiteService.getSite(siteId);
				Collection groupChoice = (Collection) state.getAttribute(NEW_ASSIGNMENT_GROUPS);
				if (range.equals(Assignment.AssignmentAccess.GROUPED) && (groupChoice == null || groupChoice.size() == 0))
				{
					// show alert if no group is selected for the group access assignment
					addAlert(state, rb.getString("java.alert.youchoosegroup"));
				}
				else if (groupChoice != null)
				{
					for (Iterator iGroups = groupChoice.iterator(); iGroups.hasNext();)
					{
						String groupId = (String) iGroups.next();
						groups.add(site.getGroup(groupId));
					}
				}
			}
			catch (Exception e)
			{
				Log.warn("chef", this + e.getMessage());
			}


			if ((state.getAttribute(STATE_MESSAGE) == null) && (ac != null) && (a != null))
			{
				aOldTitle = a.getTitle();
				// old open time
				Time oldOpenTime = a.getOpenTime();
				// old due time
				Time oldDueTime = a.getDueTime();
				
				// commit the changes to AssignmentContent object
				commitAssignmentContentEdit(state, ac, title, submissionType,useReviewService,allowStudentViewReport, gradeType, gradePoints, description, checkAddHonorPledge, attachments1);
				
				// set the Assignment Properties object
				ResourcePropertiesEdit aPropertiesEdit = a.getPropertiesEdit();
				oAssociateGradebookAssignment = aPropertiesEdit.getProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
				editAssignmentProperties(a, title, checkAddDueTime, checkAutoAnnounce, addtoGradebook, associateGradebookAssignment, allowResubmitNumber, aPropertiesEdit);
				// the notification option
				if (state.getAttribute(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE) != null)
				{
					aPropertiesEdit.addProperty(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE, (String) state.getAttribute(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE));
				}
				
				// comment the changes to Assignment object
				commitAssignmentEdit(state, post, ac, a, title, openTime, dueTime, closeTime, enableCloseDate, s, range, groups);
				
				// in case of non-electronic submission, create submissions for all students and mark it as submitted
				if (ac.getTypeOfSubmission()== Assignment.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
				{
					try
					{
						Iterator sList = AssignmentService.getSubmissions(a).iterator();
						if (sList == null || !sList.hasNext())
						{
							// add non-electronic submissions if there is none yet
							addSubmissionsForNonElectronicAssignment(state, a);
						}
					}
					catch (Exception ee)
					{
						Log.warn("chef", this + ee.getMessage());
					}
				}
	
				if (state.getAttribute(STATE_MESSAGE) == null)
				{
					state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
					state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
					resetAssignment(state);
				}

				if (post)
				{
					// only if user is posting the assignment

					// add the due date to schedule if the schedule exists
					integrateWithCalendar(state, a, title, dueTime, checkAddDueTime, oldDueTime, aPropertiesEdit);

					// the open date been announced
					integrateWithAnnouncement(state, aOldTitle, a, title, openTime, checkAutoAnnounce, oldOpenTime);

					// integrate with Gradebook
					try
					{
						integrateWithGradebook(state, siteId, aOldTitle, oAssociateGradebookAssignment, a, title, dueTime, gradeType, gradePoints, addtoGradebook, associateGradebookAssignment, range);
					}
					catch (AssignmentHasIllegalPointsException e)
					{
						addAlert(state, rb.getString("addtogradebook.illegalPoints"));
					}
	
				} // if

			} // if

		} // if

	} // postOrSaveAssignment


	/**
	 * Add submission objects if necessary for non-electronic type of assignment
	 * @param state
	 * @param a
	 */
	private void addSubmissionsForNonElectronicAssignment(SessionState state, Assignment a) 
	{
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		String authzGroupId = SiteService.siteReference(contextString);
		List allowAddSubmissionUsers = AssignmentService.allowAddSubmissionUsers(a.getReference());
		try
		{
			AuthzGroup group = AuthzGroupService.getAuthzGroup(authzGroupId);
			Set grants = group.getUsers();
			for (Iterator iUserIds = grants.iterator(); iUserIds.hasNext();)
			{
				String userId = (String) iUserIds.next();
				try
				{
					User u = UserDirectoryService.getUser(userId);
					// only include those users that can submit to this assignment
					if (u != null && allowAddSubmissionUsers.contains(u))
					{
						if (AssignmentService.getSubmission(a.getReference(), u) == null)
						{
							// construct fake submissions for grading purpose
							AssignmentSubmissionEdit submission = AssignmentService.addSubmission(contextString, a.getId());
							submission.removeSubmitter(UserDirectoryService.getCurrentUser());
							submission.addSubmitter(u);
							submission.setTimeSubmitted(TimeService.newTime());
							submission.setSubmitted(true);
							submission.setAssignment(a);
							AssignmentService.commitEdit(submission);
						}
					}
				}
				catch (Exception e)
				{
					Log.warn("chef", this + e.toString() + " here userId = " + userId);
				}
			}
		}
		catch (Exception e)
		{
			Log.warn("chef", this + e.getMessage() + " authGroupId=" + authzGroupId);
		}
	}
	
	/**
	 * Starting from Sakai 2.4, the linked assignment in GB will all be internal, instead of externally maintained.
	 * Need to run the following method to update all the previously created externally maintained assignment
	 * @param a
	 */
	private void updateExternalAssignmentInGB(Assignment a, GradebookService g, String gradebookUid)
	{
		try
		{
			ResourceProperties properties = a.getProperties();
			if (properties.getProperty(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK) != null)
			{
				// temporally allow the user update gradebook records
				SecurityService.pushAdvisor(new SecurityAdvisor()
					{
						public SecurityAdvice isAllowed(String userId, String function, String reference)
						{
							return SecurityAdvice.ALLOWED;
						}
					});
				
				String associateGradebookAssignment = StringUtil.trimToNull(properties.getProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT));
				if (associateGradebookAssignment != null)
				{
					// add or remove external grades to gradebook
					// a. if Gradebook does not exists, do nothing, 'cos setting should have been hidden
					// b. if Gradebook exists, do updates
					org.sakaiproject.service.gradebook.shared.Assignment assignmentDefinition = g.getAssignment(gradebookUid, associateGradebookAssignment);
					if (assignmentDefinition != null && assignmentDefinition.isExternallyMaintained())
					{
						// those entries are created before Sakai2.4 as externally maintained accessment in Gradebook, need to update it to internal assignment
						org.sakaiproject.service.gradebook.shared.Assignment nAssignmentDefinition = new org.sakaiproject.service.gradebook.shared.Assignment();
				 		nAssignmentDefinition.setName(assignmentDefinition.getName());
				 		nAssignmentDefinition.setPoints(assignmentDefinition.getPoints());
				 		nAssignmentDefinition.setDueDate(assignmentDefinition.getDueDate());
				 		nAssignmentDefinition.setReleased(assignmentDefinition.isReleased());
				 		nAssignmentDefinition.setCounted(assignmentDefinition.isCounted());
				 		nAssignmentDefinition.setExternallyMaintained(false);
				 		nAssignmentDefinition.setExternalAppName(null);
				 		nAssignmentDefinition.setExternalId(null);
				 		
				 		// remove the old external one and add a new internal assignment
						g.removeExternalAssessment(gradebookUid, AssignmentService.assignmentReference(a.getContext(), a.getId()));
						
				 		g.addAssignment(gradebookUid, nAssignmentDefinition);
				 		
						// bulk add all grades for assignment into gradebook
						AssignmentService.integrateGradebook(a.getReference(), associateGradebookAssignment, associateGradebookAssignment, null, null, null, -1, null, null, "update");
					}
				}
				
				// clear the permission
				SecurityService.clearAdvisors();
			}
		}
		catch (Exception e)
		{
			// log exception
			Log.warn("chef", this + e.getMessage());
		}
	}

	/**
	 * called from postOrSaveAssignment function to do integration
	 * @param state
	 * @param siteId
	 * @param aOldTitle
	 * @param oAssociateGradebookAssignment
	 * @param a
	 * @param title
	 * @param dueTime
	 * @param gradeType
	 * @param gradePoints
	 * @param addtoGradebook
	 * @param associateGradebookAssignment
	 * @param range
	 */
	private void integrateWithGradebook(SessionState state, String siteId, String aOldTitle, String oAssociateGradebookAssignment, AssignmentEdit a, String title, Time dueTime, int gradeType, String gradePoints, String addtoGradebook, String associateGradebookAssignment, String range) {
		String aReference = a.getReference();
		if (!addtoGradebook.equals(AssignmentService.GRADEBOOK_INTEGRATION_NO))
		{
			// if grouped assignment is not allowed to add into Gradebook
			if (!AssignmentService.getAllowGroupAssignmentsInGradebook() && (range.equals("groups")))
			{
				addAlert(state, rb.getString("java.alert.noGroupedAssignmentIntoGB"));
				String ref = "";
				try
				{
					ref = a.getReference();
					AssignmentEdit aEdit = AssignmentService.editAssignment(ref);
					aEdit.getPropertiesEdit().removeProperty(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK);
					aEdit.getPropertiesEdit().removeProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
					AssignmentService.commitEdit(aEdit);
				}
				catch (Exception ignore)
				{
					// ignore the exception
					Log.warn("chef", rb.getString("cannotfin2") + ref);
				}
				AssignmentService.integrateGradebook(aReference, associateGradebookAssignment, oAssociateGradebookAssignment, AssignmentService.GRADEBOOK_INTEGRATION_NO, null, null, -1, null, null, "remove");
			}
			else
			{
				if (!addtoGradebook.equals(AssignmentService.GRADEBOOK_INTEGRATION_NO) && gradeType == 3)
				{
					try
					{
						//integrate assignment with gradebook
						//add all existing grades, if any, into Gradebook
						String rv = AssignmentService.integrateGradebook(aReference, associateGradebookAssignment, oAssociateGradebookAssignment, addtoGradebook, aOldTitle, title, Integer.parseInt (gradePoints), dueTime, null, "update");
						if (rv != null && rv.length() > 0)
						{
							addAlert(state, rv);
						}
					}
					catch (NumberFormatException nE)
					{
						alertInvalidPoint(state, gradePoints);
					}
				}
				else
				{
					// otherwise, cannot do the association
				}
			}
		}
		else
		{
			// no need to do anything here, if the assignment is chosen to not hook up with GB. 
			// user can go to GB and delete the entry there manually
		}
	}

	private void integrateWithAnnouncement(SessionState state, String aOldTitle, AssignmentEdit a, String title, Time openTime, String checkAutoAnnounce, Time oldOpenTime) 
	{
		if (checkAutoAnnounce.equalsIgnoreCase(Boolean.TRUE.toString()))
		{
			AnnouncementChannel channel = (AnnouncementChannel) state.getAttribute(ANNOUNCEMENT_CHANNEL);
			if (channel != null)
			{
				String openDateAnnounced = a.getProperties().getProperty(NEW_ASSIGNMENT_OPEN_DATE_ANNOUNCED);

				// announcement channel is in place
				try
				{
					AnnouncementMessageEdit message = channel.addAnnouncementMessage();
					AnnouncementMessageHeaderEdit header = message.getAnnouncementHeaderEdit();
					header.setDraft(/* draft */false);
					header.replaceAttachments(/* attachment */EntityManager.newReferenceList());

					if (openDateAnnounced == null)
					{
						// making new announcement
						header.setSubject(/* subject */rb.getString("assig6") + " " + title);
						message.setBody(/* body */rb.getString("opedat") + " "
								+ FormattedText.convertPlaintextToFormattedText(title) + " " + rb.getString("is") + " "
								+ openTime.toStringLocalFull() + ". ");
					}
					else
					{
						// revised announcement
						header.setSubject(/* subject */rb.getString("assig5") + " " + title);
						message.setBody(/* body */rb.getString("newope") + " "
								+ FormattedText.convertPlaintextToFormattedText(title) + " " + rb.getString("is") + " "
								+ openTime.toStringLocalFull() + ". ");
					}

					// group information
					if (a.getAccess().equals(Assignment.AssignmentAccess.GROUPED))
					{
						try
						{
							// get the group ids selected
							Collection groupRefs = a.getGroups();

							// make a collection of Group objects
							Collection groups = new Vector();

							//make a collection of Group objects from the collection of group ref strings
							Site site = SiteService.getSite((String) state.getAttribute(STATE_CONTEXT_STRING));
							for (Iterator iGroupRefs = groupRefs.iterator(); iGroupRefs.hasNext();)
							{
								String groupRef = (String) iGroupRefs.next();
								groups.add(site.getGroup(groupRef));
							}

							// set access
							header.setGroupAccess(groups);
						}
						catch (Exception exception)
						{
							// log
							Log.warn("chef", exception.getMessage());
						}
					}
					else
					{
						// site announcement
						header.clearGroupAccess();
					}


					channel.commitMessage(message, NotificationService.NOTI_NONE);

					// commit related properties into Assignment object
					String ref = "";
					try
					{
						ref = a.getReference();
						AssignmentEdit aEdit = AssignmentService.editAssignment(ref);
						aEdit.getPropertiesEdit().addProperty(NEW_ASSIGNMENT_OPEN_DATE_ANNOUNCED, Boolean.TRUE.toString());
						if (message != null)
						{
							aEdit.getPropertiesEdit().addProperty(ResourceProperties.PROP_ASSIGNMENT_OPENDATE_ANNOUNCEMENT_MESSAGE_ID, message.getId());
						}
						AssignmentService.commitEdit(aEdit);
					}
					catch (Exception ignore)
					{
						// ignore the exception
						Log.warn("chef", rb.getString("cannotfin2") + ref);
					}

				}
				catch (PermissionException ee)
				{
					Log.warn("chef", rb.getString("cannotmak"));
				}
			}
		} // if
	}

	private void integrateWithCalendar(SessionState state, AssignmentEdit a, String title, Time dueTime, String checkAddDueTime, Time oldDueTime, ResourcePropertiesEdit aPropertiesEdit) 
	{
		if (state.getAttribute(CALENDAR) != null)
		{
			Calendar c = (Calendar) state.getAttribute(CALENDAR);
			String dueDateScheduled = a.getProperties().getProperty(NEW_ASSIGNMENT_DUE_DATE_SCHEDULED);
			String oldEventId = aPropertiesEdit.getProperty(ResourceProperties.PROP_ASSIGNMENT_DUEDATE_CALENDAR_EVENT_ID);
			CalendarEvent e = null;

			if (dueDateScheduled != null || oldEventId != null)
			{
				// find the old event
				boolean found = false;
				if (oldEventId != null && c != null)
				{
					try
					{
						e = c.getEvent(oldEventId);
						found = true;
					}
					catch (IdUnusedException ee)
					{
						Log.warn("chef", "The old event has been deleted: event id=" + oldEventId + ". ");
					}
					catch (PermissionException ee)
					{
						Log.warn("chef", "You do not have the permission to view the schedule event id= "
								+ oldEventId + ".");
					}
				}
				else
				{
					TimeBreakdown b = oldDueTime.breakdownLocal();
					// TODO: check- this was new Time(year...), not local! -ggolden
					Time startTime = TimeService.newTimeLocal(b.getYear(), b.getMonth(), b.getDay(), 0, 0, 0, 0);
					Time endTime = TimeService.newTimeLocal(b.getYear(), b.getMonth(), b.getDay(), 23, 59, 59, 999);
					try
					{
						Iterator events = c.getEvents(TimeService.newTimeRange(startTime, endTime), null)
								.iterator();

						while ((!found) && (events.hasNext()))
						{
							e = (CalendarEvent) events.next();
							if (((String) e.getDisplayName()).indexOf(rb.getString("assig1") + " " + title) != -1)
							{
								found = true;
							}
						}
					}
					catch (PermissionException ignore)
					{
						// ignore PermissionException
					}
				}

				if (found)
				{
					// remove the founded old event
					try
					{
						c.removeEvent(c.getEditEvent(e.getId(), CalendarService.EVENT_REMOVE_CALENDAR));
					}
					catch (PermissionException ee)
					{
						Log.warn("chef", rb.getString("cannotrem") + " " + title + ". ");
					}
					catch (InUseException ee)
					{
						Log.warn("chef", rb.getString("somelsis") + " " + rb.getString("calen"));
					}
					catch (IdUnusedException ee)
					{
						Log.warn("chef", rb.getString("cannotfin6") + e.getId());
					}
				}
			}

			if (checkAddDueTime.equalsIgnoreCase(Boolean.TRUE.toString()))
			{
				if (c != null)
				{
					// commit related properties into Assignment object
					String ref = "";
					try
					{
						ref = a.getReference();
						AssignmentEdit aEdit = AssignmentService.editAssignment(ref);

						try
						{
							e = null;
							CalendarEvent.EventAccess eAccess = CalendarEvent.EventAccess.SITE;
							Collection eGroups = new Vector();

							if (aEdit.getAccess().equals(Assignment.AssignmentAccess.GROUPED))
							{
								eAccess = CalendarEvent.EventAccess.GROUPED;
								Collection groupRefs = aEdit.getGroups();

								// make a collection of Group objects from the collection of group ref strings
								Site site = SiteService.getSite((String) state.getAttribute(STATE_CONTEXT_STRING));
								for (Iterator iGroupRefs = groupRefs.iterator(); iGroupRefs.hasNext();)
								{
									String groupRef = (String) iGroupRefs.next();
									eGroups.add(site.getGroup(groupRef));
								}
							}
							e = c.addEvent(/* TimeRange */TimeService.newTimeRange(dueTime.getTime(), /* 0 duration */0 * 60 * 1000),
									/* title */rb.getString("due") + " " + title,
									/* description */rb.getString("assig1") + " " + title + " " + "is due on "
											+ dueTime.toStringLocalFull() + ". ",
									/* type */rb.getString("deadl"),
									/* location */"",
									/* access */ eAccess,
									/* groups */ eGroups,
									/* attachments */EntityManager.newReferenceList());

							aEdit.getProperties().addProperty(NEW_ASSIGNMENT_DUE_DATE_SCHEDULED, Boolean.TRUE.toString());
							if (e != null)
							{
								aEdit.getProperties().addProperty(ResourceProperties.PROP_ASSIGNMENT_DUEDATE_CALENDAR_EVENT_ID, e.getId());
							}
						}
						catch (IdUnusedException ee)
						{
							Log.warn("chef", ee.getMessage());
						}
						catch (PermissionException ee)
						{
							Log.warn("chef", rb.getString("cannotfin1"));
						}
						catch (Exception ee)
						{
							Log.warn("chef", ee.getMessage());
						}
						// try-catch


						AssignmentService.commitEdit(aEdit);
					}
					catch (Exception ignore)
					{
						// ignore the exception
						Log.warn("chef", rb.getString("cannotfin2") + ref);
					}
				} // if
			} // if
		}
	}

	private void commitAssignmentEdit(SessionState state, boolean post, AssignmentContentEdit ac, AssignmentEdit a, String title, Time openTime, Time dueTime, Time closeTime, boolean enableCloseDate, String s, String range, Collection groups) 
	{
		a.setTitle(title);
		a.setContent(ac);
		a.setContext((String) state.getAttribute(STATE_CONTEXT_STRING));
		a.setSection(s);
		a.setOpenTime(openTime);
		a.setDueTime(dueTime);
		// set the drop dead date as the due date
		a.setDropDeadTime(dueTime);
		if (enableCloseDate)
		{
			a.setCloseTime(closeTime);
		}
		else
		{
			// if editing an old assignment with close date
			if (a.getCloseTime() != null)
			{
				a.setCloseTime(null);
			}
		}

		// post the assignment
		a.setDraft(!post);

		try
		{
			if (range.equals("site"))
			{
				a.setAccess(Assignment.AssignmentAccess.SITE);
				a.clearGroupAccess();
			}
			else if (range.equals("groups"))
			{
				a.setGroupAccess(groups);
			}
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot1"));
		}

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			// commit assignment first
			AssignmentService.commitEdit(a);
		}
	}

	private void editAssignmentProperties(AssignmentEdit a, String title, String checkAddDueTime, String checkAutoAnnounce, String addtoGradebook, String associateGradebookAssignment, String allowResubmitNumber, ResourcePropertiesEdit aPropertiesEdit) 
	{
		if (aPropertiesEdit.getProperty("newAssignment") != null)
		{
			if (aPropertiesEdit.getProperty("newAssignment").equalsIgnoreCase(Boolean.TRUE.toString()))
			{
				// not a newly created assignment, been added.
				aPropertiesEdit.addProperty("newAssignment", Boolean.FALSE.toString());
			}
		}
		else
		{
			// for newly created assignment
			aPropertiesEdit.addProperty("newAssignment", Boolean.TRUE.toString());
		}
		if (checkAddDueTime != null)
		{
			aPropertiesEdit.addProperty(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE, checkAddDueTime);
		}
		else
		{
			aPropertiesEdit.removeProperty(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE);
		}
		aPropertiesEdit.addProperty(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE, checkAutoAnnounce);
		aPropertiesEdit.addProperty(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, addtoGradebook);
		aPropertiesEdit.addProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, associateGradebookAssignment);

		if (addtoGradebook.equals(AssignmentService.GRADEBOOK_INTEGRATION_ADD))
		{
			// if the choice is to add an entry into Gradebook, let just mark it as associated with such new entry then
			aPropertiesEdit.addProperty(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, AssignmentService.GRADEBOOK_INTEGRATION_ASSOCIATE);
			aPropertiesEdit.addProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, title);

		}
		
		// allow resubmit number
		if (allowResubmitNumber != null)
		{
			aPropertiesEdit.addProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER, allowResubmitNumber);
		}
	}

	private void commitAssignmentContentEdit(SessionState state, AssignmentContentEdit ac, String title, int submissionType,boolean useReviewService, boolean allowStudentViewReport, int gradeType, String gradePoints, String description, String checkAddHonorPledge, List attachments1) 
	{
		ac.setTitle(title);
		ac.setInstructions(description);
		ac.setHonorPledge(Integer.parseInt(checkAddHonorPledge));
		ac.setTypeOfSubmission(submissionType);
		ac.setAllowReviewService(useReviewService);
		ac.setAllowStudentViewReport(allowStudentViewReport);
		ac.setTypeOfGrade(gradeType);
		if (gradeType == 3)
		{
			try
			{
				ac.setMaxGradePoint(Integer.parseInt(gradePoints));
			}
			catch (NumberFormatException e)
			{
				alertInvalidPoint(state, gradePoints);
			}
		}
		ac.setGroupProject(true);
		ac.setIndividuallyGraded(false);

		if (submissionType != 1)
		{
			ac.setAllowAttachments(true);
		}
		else
		{
			ac.setAllowAttachments(false);
		}

		// clear attachments
		ac.clearAttachments();

		// add each attachment
		Iterator it = EntityManager.newReferenceList(attachments1).iterator();
		while (it.hasNext())
		{
			Reference r = (Reference) it.next();
			ac.addAttachment(r);
		}
		state.setAttribute(ATTACHMENTS_MODIFIED, new Boolean(false));

		// commit the changes
		AssignmentService.commitEdit(ac);
	}

	private AssignmentEdit getAssignmentEdit(SessionState state, String assignmentId) 
	{
		AssignmentEdit a = null;
		if (assignmentId.length() == 0)
		{
			// create a new assignment
			try
			{
				a = AssignmentService.addAssignment((String) state.getAttribute(STATE_CONTEXT_STRING));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot1"));
			}
		}
		else
		{
			try
			{
				// edit assignment
				a = AssignmentService.editAssignment(assignmentId);
			}
			catch (InUseException e)
			{
				addAlert(state, rb.getString("theassicon"));
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot14"));
			} // try-catch
		} // if-else
		return a;
	}

	private AssignmentContentEdit getAssignmentContentEdit(SessionState state, String assignmentContentId) 
	{
		AssignmentContentEdit ac = null;
		if (assignmentContentId.length() == 0)
		{
			// new assignment
			try
			{
				ac = AssignmentService.addAssignmentContent((String) state.getAttribute(STATE_CONTEXT_STRING));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot3"));
			}
		}
		else
		{
			try
			{
				// edit assignment
				ac = AssignmentService.editAssignmentContent(assignmentContentId);
			}
			catch (InUseException e)
			{
				addAlert(state, rb.getString("theassicon"));
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin4"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot15"));
			}

		}
		return ac;
	}

	private Time getOpenTime(SessionState state) 
	{
		int openMonth = ((Integer) state.getAttribute(NEW_ASSIGNMENT_OPENMONTH)).intValue();
		int openDay = ((Integer) state.getAttribute(NEW_ASSIGNMENT_OPENDAY)).intValue();
		int openYear = ((Integer) state.getAttribute(NEW_ASSIGNMENT_OPENYEAR)).intValue();
		int openHour = ((Integer) state.getAttribute(NEW_ASSIGNMENT_OPENHOUR)).intValue();
		int openMin = ((Integer) state.getAttribute(NEW_ASSIGNMENT_OPENMIN)).intValue();
		String openAMPM = (String) state.getAttribute(NEW_ASSIGNMENT_OPENAMPM);
		if ((openAMPM.equals("PM")) && (openHour != 12))
		{
			openHour = openHour + 12;
		}
		if ((openHour == 12) && (openAMPM.equals("AM")))
		{
			openHour = 0;
		}
		Time openTime = TimeService.newTimeLocal(openYear, openMonth, openDay, openHour, openMin, 0, 0);
		return openTime;
	}

	private Time getCloseTime(SessionState state) 
	{
		Time closeTime;
		int closeMonth = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEMONTH)).intValue();
		int closeDay = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEDAY)).intValue();
		int closeYear = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEYEAR)).intValue();
		int closeHour = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEHOUR)).intValue();
		int closeMin = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEMIN)).intValue();
		String closeAMPM = (String) state.getAttribute(NEW_ASSIGNMENT_CLOSEAMPM);
		if ((closeAMPM.equals("PM")) && (closeHour != 12))
		{
			closeHour = closeHour + 12;
		}
		if ((closeHour == 12) && (closeAMPM.equals("AM")))
		{
			closeHour = 0;
		}
		closeTime = TimeService.newTimeLocal(closeYear, closeMonth, closeDay, closeHour, closeMin, 0, 0);
		return closeTime;
	}

	private Time getDueTime(SessionState state) 
	{
		int dueMonth = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEMONTH)).intValue();
		int dueDay = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEDAY)).intValue();
		int dueYear = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEYEAR)).intValue();
		int dueHour = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEHOUR)).intValue();
		int dueMin = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEMIN)).intValue();
		String dueAMPM = (String) state.getAttribute(NEW_ASSIGNMENT_DUEAMPM);
		if ((dueAMPM.equals("PM")) && (dueHour != 12))
		{
			dueHour = dueHour + 12;
		}
		if ((dueHour == 12) && (dueAMPM.equals("AM")))
		{
			dueHour = 0;
		}
		Time dueTime = TimeService.newTimeLocal(dueYear, dueMonth, dueDay, dueHour, dueMin, 0, 0);
		return dueTime;
	}
	
	private Time getAllowSubmitCloseTime(SessionState state) 
	{
		int closeMonth = ((Integer) state.getAttribute(ALLOW_RESUBMIT_CLOSEMONTH)).intValue();
		int closeDay = ((Integer) state.getAttribute(ALLOW_RESUBMIT_CLOSEDAY)).intValue();
		int closeYear = ((Integer) state.getAttribute(ALLOW_RESUBMIT_CLOSEYEAR)).intValue();
		int closeHour = ((Integer) state.getAttribute(ALLOW_RESUBMIT_CLOSEHOUR)).intValue();
		int closeMin = ((Integer) state.getAttribute(ALLOW_RESUBMIT_CLOSEMIN)).intValue();
		String closeAMPM = (String) state.getAttribute(ALLOW_RESUBMIT_CLOSEAMPM);
		if ((closeAMPM.equals("PM")) && (closeHour != 12))
		{
			closeHour = closeHour + 12;
		}
		if ((closeHour == 12) && (closeAMPM.equals("AM")))
		{
			closeHour = 0;
		}
		Time closeTime = TimeService.newTimeLocal(closeYear, closeMonth, closeDay, closeHour, closeMin, 0, 0);
		return closeTime;
	}

	/**
	 * Action is to post new assignment
	 */
	public void doSave_assignment(RunData data)
	{
		postOrSaveAssignment(data, "save");

	} // doSave_assignment

	/**
	 * Action is to preview the selected assignment
	 */
	public void doPreview_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		setNewAssignmentParameters(data, false);

		String assignmentId = data.getParameters().getString("assignmentId");
		state.setAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_ID, assignmentId);

		String assignmentContentId = data.getParameters().getString("assignmentContentId");
		state.setAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENTCONTENT_ID, assignmentContentId);

		state.setAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_HIDE_FLAG, new Boolean(false));
		state.setAttribute(PREVIEW_ASSIGNMENT_STUDENT_VIEW_HIDE_FLAG, new Boolean(true));
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_PREVIEW_ASSIGNMENT);
		}

	} // doPreview_assignment

	/**
	 * Action is to view the selected assignment
	 */
	public void doView_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		// show the assignment portion
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_ASSIGNMENT_FLAG, new Boolean(false));
		// show the student view portion
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_STUDENT_VIEW_FLAG, new Boolean(true));

		String assignmentId = params.getString("assignmentId");
		state.setAttribute(VIEW_ASSIGNMENT_ID, assignmentId);

		try
		{
			Assignment a = AssignmentService.getAssignment(assignmentId);
			EventTrackingService.post(EventTrackingService.newEvent(AssignmentService.SECURE_ACCESS_ASSIGNMENT, a.getReference(), false));
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_VIEW_ASSIGNMENT);
		}

	} // doView_Assignment

	/**
	 * Action is for student to view one assignment content
	 */
	public void doView_assignment_as_student(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		String assignmentId = params.getString("assignmentId");
		state.setAttribute(VIEW_ASSIGNMENT_ID, assignmentId);

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_ASSIGNMENT);
		}

	} // doView_assignment_as_student

	/**
	 * Action is to show the edit assignment screen
	 */
	public void doEdit_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		String assignmentId = StringUtil.trimToNull(params.getString("assignmentId"));
		// whether the user can modify the assignment
		state.setAttribute(EDIT_ASSIGNMENT_ID, assignmentId);

		try
		{
			Assignment a = AssignmentService.getAssignment(assignmentId);
			// for the non_electronice assignment, submissions are auto-generated by the time that assignment is created;
			// don't need to go through the following checkings.
			if (a.getContent().getTypeOfSubmission() != Assignment.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
			{
				Iterator submissions = AssignmentService.getSubmissions(a).iterator();
				if (submissions.hasNext())
				{
					// any submitted?
					boolean anySubmitted = false;
					for (;submissions.hasNext() && !anySubmitted;)
					{
						AssignmentSubmission s = (AssignmentSubmission) submissions.next();
						if (s.getSubmitted() && s.getTimeSubmitted() != null)
						{
							anySubmitted = true;
						}
					}
					
					// any draft submission
					boolean anyDraft = false;
					for (;submissions.hasNext() && !anyDraft;)
					{
						AssignmentSubmission s = (AssignmentSubmission) submissions.next();
						if (!s.getSubmitted())
						{
							anyDraft = true;
						}
					}
					if (anySubmitted)
					{
						// if there is any submitted submission to this assignment, show alert
						addAlert(state, rb.getString("assig1") + " " + a.getTitle() + " " + rb.getString("hassum"));
					}
					
					if (anyDraft)
					{
						// otherwise, show alert about someone has started working on the assignment, not necessarily submitted
						addAlert(state, rb.getString("hasDraftSum"));
					}
				}
			}

			// SECTION MOD
			state.setAttribute(STATE_SECTION_STRING, a.getSection());

			// put the names and values into vm file
			state.setAttribute(NEW_ASSIGNMENT_TITLE, a.getTitle());
			TimeBreakdown openTime = a.getOpenTime().breakdownLocal();
			state.setAttribute(NEW_ASSIGNMENT_OPENMONTH, new Integer(openTime.getMonth()));
			state.setAttribute(NEW_ASSIGNMENT_OPENDAY, new Integer(openTime.getDay()));
			state.setAttribute(NEW_ASSIGNMENT_OPENYEAR, new Integer(openTime.getYear()));
			int openHour = openTime.getHour();
			if (openHour >= 12)
			{
				state.setAttribute(NEW_ASSIGNMENT_OPENAMPM, "PM");
			}
			else
			{
				state.setAttribute(NEW_ASSIGNMENT_OPENAMPM, "AM");
			}
			if (openHour == 0)
			{
				// for midnight point, we mark it as 12AM
				openHour = 12;
			}
			state.setAttribute(NEW_ASSIGNMENT_OPENHOUR, new Integer((openHour > 12) ? openHour - 12 : openHour));
			state.setAttribute(NEW_ASSIGNMENT_OPENMIN, new Integer(openTime.getMin()));

			TimeBreakdown dueTime = a.getDueTime().breakdownLocal();
			state.setAttribute(NEW_ASSIGNMENT_DUEMONTH, new Integer(dueTime.getMonth()));
			state.setAttribute(NEW_ASSIGNMENT_DUEDAY, new Integer(dueTime.getDay()));
			state.setAttribute(NEW_ASSIGNMENT_DUEYEAR, new Integer(dueTime.getYear()));
			int dueHour = dueTime.getHour();
			if (dueHour >= 12)
			{
				state.setAttribute(NEW_ASSIGNMENT_DUEAMPM, "PM");
			}
			else
			{
				state.setAttribute(NEW_ASSIGNMENT_DUEAMPM, "AM");
			}
			if (dueHour == 0)
			{
				// for midnight point, we mark it as 12AM
				dueHour = 12;
			}
			state.setAttribute(NEW_ASSIGNMENT_DUEHOUR, new Integer((dueHour > 12) ? dueHour - 12 : dueHour));
			state.setAttribute(NEW_ASSIGNMENT_DUEMIN, new Integer(dueTime.getMin()));
			// generate alert when editing an assignment past due date
			if (a.getDueTime().before(TimeService.newTime()))
			{
				addAlert(state, rb.getString("youarenot17"));
			}

			if (a.getCloseTime() != null)
			{
				state.setAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE, new Boolean(true));
				TimeBreakdown closeTime = a.getCloseTime().breakdownLocal();
				state.setAttribute(NEW_ASSIGNMENT_CLOSEMONTH, new Integer(closeTime.getMonth()));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEDAY, new Integer(closeTime.getDay()));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEYEAR, new Integer(closeTime.getYear()));
				int closeHour = closeTime.getHour();
				if (closeHour >= 12)
				{
					state.setAttribute(NEW_ASSIGNMENT_CLOSEAMPM, "PM");
				}
				else
				{
					state.setAttribute(NEW_ASSIGNMENT_CLOSEAMPM, "AM");
				}
				if (closeHour == 0)
				{
					// for the midnight point, we mark it as 12 AM
					closeHour = 12;
				}
				state.setAttribute(NEW_ASSIGNMENT_CLOSEHOUR, new Integer((closeHour > 12) ? closeHour - 12 : closeHour));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEMIN, new Integer(closeTime.getMin()));
			}
			else
			{
				state.setAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE, new Boolean(false));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEMONTH, state.getAttribute(NEW_ASSIGNMENT_DUEMONTH));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEDAY, state.getAttribute(NEW_ASSIGNMENT_DUEDAY));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEYEAR, state.getAttribute(NEW_ASSIGNMENT_DUEYEAR));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEHOUR, state.getAttribute(NEW_ASSIGNMENT_DUEHOUR));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEMIN, state.getAttribute(NEW_ASSIGNMENT_DUEMIN));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEAMPM, state.getAttribute(NEW_ASSIGNMENT_DUEAMPM));
			}
			state.setAttribute(NEW_ASSIGNMENT_SECTION, a.getSection());

			state.setAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE, new Integer(a.getContent().getTypeOfSubmission()));
			int typeOfGrade = a.getContent().getTypeOfGrade();
			state.setAttribute(NEW_ASSIGNMENT_GRADE_TYPE, new Integer(typeOfGrade));
			if (typeOfGrade == 3)
			{
				state.setAttribute(NEW_ASSIGNMENT_GRADE_POINTS, a.getContent().getMaxGradePointDisplay());
			}
			state.setAttribute(NEW_ASSIGNMENT_DESCRIPTION, a.getContent().getInstructions());
			
			ResourceProperties properties = a.getProperties();
			state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE, properties.getProperty(
					ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE));
			state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE, properties.getProperty(
					ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE));
			state.setAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE, Integer.toString(a.getContent().getHonorPledge()));
			state.setAttribute(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, properties.getProperty(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK));
			state.setAttribute(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, properties.getProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT));
			state.setAttribute(ATTACHMENTS, a.getContent().getAttachments());
			
			// notification option
			if (properties.getProperty(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE) != null)
			{
				state.setAttribute(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE, properties.getProperty(Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE));
			}

			// group setting
			if (a.getAccess().equals(Assignment.AssignmentAccess.SITE))
			{
				state.setAttribute(NEW_ASSIGNMENT_RANGE, "site");
			}
			else
			{
				state.setAttribute(NEW_ASSIGNMENT_RANGE, "groups");
			}
				
			state.setAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER, properties.getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER) != null?properties.getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER):"0");
			
			// set whether we use the review service or not
			state.setAttribute(NEW_ASSIGNMENT_USE_REVIEW_SERVICE, new Boolean(a.getContent().getAllowReviewService()).toString());
			
			//set whether students can view the review service results
			state.setAttribute(NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW, new Boolean(a.getContent().getAllowStudentViewReport()).toString());
			
			
			state.setAttribute(NEW_ASSIGNMENT_GROUPS, a.getGroups());
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT);

	} // doEdit_Assignment

	/**
	 * Action is to show the delete assigment confirmation screen
	 */
	public void doDelete_confirm_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		String[] assignmentIds = params.getStrings("selectedAssignments");

		if (assignmentIds != null)
		{
			Vector ids = new Vector();
			for (int i = 0; i < assignmentIds.length; i++)
			{
				String id = (String) assignmentIds[i];
				if (!AssignmentService.allowRemoveAssignment(id))
				{
					addAlert(state, rb.getString("youarenot9") + " " + id + ". ");
				}
				ids.add(id);
			}

			if (state.getAttribute(STATE_MESSAGE) == null)
			{
				// can remove all the selected assignments
				state.setAttribute(DELETE_ASSIGNMENT_IDS, ids);
				state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_DELETE_ASSIGNMENT);
			}
		}
		else
		{
			addAlert(state, rb.getString("youmust6"));
		}

	} // doDelete_confirm_Assignment

	/**
	 * Action is to delete the confirmed assignments
	 */
	public void doDelete_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// get the delete assignment ids
		Vector ids = (Vector) state.getAttribute(DELETE_ASSIGNMENT_IDS);
		for (int i = 0; i < ids.size(); i++)
		{

			String assignmentId = (String) ids.get(i);
			try
			{
				AssignmentEdit aEdit = AssignmentService.editAssignment(assignmentId);

				ResourcePropertiesEdit pEdit = aEdit.getPropertiesEdit();

				String associateGradebookAssignment = pEdit.getProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);

				String title = aEdit.getTitle();

				// remove releted event if there is one
				String isThereEvent = pEdit.getProperty(NEW_ASSIGNMENT_DUE_DATE_SCHEDULED);
				if (isThereEvent != null && isThereEvent.equals(Boolean.TRUE.toString()))
				{
					removeCalendarEvent(state, aEdit, pEdit, title);
				} // if-else

				if (!AssignmentService.getSubmissions(aEdit).iterator().hasNext())
				{
					// there is no submission to this assignment yet, delete the assignment record completely
					try
					{
						TaggingManager taggingManager = (TaggingManager) ComponentManager
								.get("org.sakaiproject.assignment.taggable.api.TaggingManager");

						AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
								.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");

						if (taggingManager.isTaggable()) {
							for (TaggingProvider provider : taggingManager
									.getProviders()) {
								provider.removeTags(assignmentActivityProducer
										.getActivity(aEdit));
							}
						}
						
						AssignmentService.removeAssignment(aEdit);
					}
					catch (PermissionException e)
					{
						addAlert(state, rb.getString("youarenot11") + " " + aEdit.getTitle() + ". ");
					}
				}
				else
				{
					// remove the assignment by marking the remove status property true
					pEdit.addProperty(ResourceProperties.PROP_ASSIGNMENT_DELETED, Boolean.TRUE.toString());

					AssignmentService.commitEdit(aEdit);
				}

				// remove from Gradebook
				AssignmentService.integrateGradebook((String) ids.get (i), associateGradebookAssignment, null, AssignmentService.GRADEBOOK_INTEGRATION_NO, null, null, -1, null, null, "remove");
			}
			catch (InUseException e)
			{
				addAlert(state, rb.getString("somelsis") + " " + rb.getString("assig2"));
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot6"));
			}
		} // for

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(DELETE_ASSIGNMENT_IDS, new Vector());

			state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		}

	} // doDelete_Assignment

	private void removeCalendarEvent(SessionState state, AssignmentEdit aEdit, ResourcePropertiesEdit pEdit, String title) throws PermissionException 
	{
		// remove the associated calender event
		Calendar c = (Calendar) state.getAttribute(CALENDAR);
		if (c != null)
		{
			// already has calendar object
			// get the old event
			CalendarEvent e = null;
			boolean found = false;
			String oldEventId = pEdit.getProperty(ResourceProperties.PROP_ASSIGNMENT_DUEDATE_CALENDAR_EVENT_ID);
			if (oldEventId != null)
			{
				try
				{
					e = c.getEvent(oldEventId);
					found = true;
				}
				catch (IdUnusedException ee)
				{
					// no action needed for this condition
				}
				catch (PermissionException ee)
				{
				}
			}
			else
			{
				TimeBreakdown b = aEdit.getDueTime().breakdownLocal();
				// TODO: check- this was new Time(year...), not local! -ggolden
				Time startTime = TimeService.newTimeLocal(b.getYear(), b.getMonth(), b.getDay(), 0, 0, 0, 0);
				Time endTime = TimeService.newTimeLocal(b.getYear(), b.getMonth(), b.getDay(), 23, 59, 59, 999);
				Iterator events = c.getEvents(TimeService.newTimeRange(startTime, endTime), null).iterator();
				while ((!found) && (events.hasNext()))
				{
					e = (CalendarEvent) events.next();
					if (((String) e.getDisplayName()).indexOf(rb.getString("assig1") + " " + title) != -1)
					{
						found = true;
					}
				}
			}
			// remove the founded old event
			if (found)
			{
				// found the old event delete it
				try
				{
					c.removeEvent(c.getEditEvent(e.getId(), CalendarService.EVENT_REMOVE_CALENDAR));
					pEdit.removeProperty(NEW_ASSIGNMENT_DUE_DATE_SCHEDULED);
					pEdit.removeProperty(ResourceProperties.PROP_ASSIGNMENT_DUEDATE_CALENDAR_EVENT_ID);
				}
				catch (PermissionException ee)
				{
					Log.warn("chef", rb.getString("cannotrem") + " " + title + ". ");
				}
				catch (InUseException ee)
				{
					Log.warn("chef", rb.getString("somelsis") + " " + rb.getString("calen"));
				}
				catch (IdUnusedException ee)
				{
					Log.warn("chef", rb.getString("cannotfin6") + e.getId());
				}
			}
		}
	}

	/**
	 * Action is to delete the assignment and also the related AssignmentSubmission
	 */
	public void doDeep_delete_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// get the delete assignment ids
		Vector ids = (Vector) state.getAttribute(DELETE_ASSIGNMENT_IDS);
		for (int i = 0; i < ids.size(); i++)
		{
			String currentId = (String) ids.get(i);
			try
			{
				AssignmentEdit a = AssignmentService.editAssignment(currentId);
				try
				{
					TaggingManager taggingManager = (TaggingManager) ComponentManager
							.get("org.sakaiproject.assignment.taggable.api.TaggingManager");

					AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
					.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");

					if (taggingManager.isTaggable()) {
						for (TaggingProvider provider : taggingManager
								.getProviders()) {
							provider.removeTags(assignmentActivityProducer
									.getActivity(a));
						}
					}
			
					AssignmentService.removeAssignment(a);
				}
				catch (PermissionException e)
				{
					addAlert(state, rb.getString("youarenot11") + " " + a.getTitle() + ". ");
				}
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot14"));
			}
			catch (InUseException e)
			{
				addAlert(state, rb.getString("somelsis") + " " +  rb.getString("assig2"));
			}
		}
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(DELETE_ASSIGNMENT_IDS, new Vector());
			state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		}

	} // doDeep_delete_Assignment

	/**
	 * Action is to show the duplicate assignment screen
	 */
	public void doDuplicate_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// we are changing the view, so start with first page again.
		resetPaging(state);

		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		ParameterParser params = data.getParameters();
		String assignmentId = StringUtil.trimToNull(params.getString("assignmentId"));

		if (assignmentId != null)
		{
			try
			{
				AssignmentEdit aEdit = AssignmentService.addDuplicateAssignment(contextString, assignmentId);

				// clean the duplicate's property
				ResourcePropertiesEdit aPropertiesEdit = aEdit.getPropertiesEdit();
				aPropertiesEdit.removeProperty(NEW_ASSIGNMENT_DUE_DATE_SCHEDULED);
				aPropertiesEdit.removeProperty(ResourceProperties.PROP_ASSIGNMENT_DUEDATE_CALENDAR_EVENT_ID);
				aPropertiesEdit.removeProperty(NEW_ASSIGNMENT_OPEN_DATE_ANNOUNCED);
				aPropertiesEdit.removeProperty(ResourceProperties.PROP_ASSIGNMENT_OPENDATE_ANNOUNCEMENT_MESSAGE_ID);

				AssignmentService.commitEdit(aEdit);
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot5"));
			}
			catch (IdInvalidException e)
			{
				addAlert(state, rb.getString("theassiid") + " " + assignmentId + " " + rb.getString("isnotval"));
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("theassiid") + " " + assignmentId + " " + rb.getString("hasnotbee"));
			}
			catch (Exception e)
			{
			}

		}

	} // doDuplicate_Assignment

	/**
	 * Action is to show the grade submission screen
	 */
	public void doGrade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the submission context
		resetViewSubmission(state);
		
		ParameterParser params = data.getParameters();

		// reset the grade assignment id
		state.setAttribute(GRADE_SUBMISSION_ASSIGNMENT_ID, params.getString("assignmentId"));
		state.setAttribute(GRADE_SUBMISSION_SUBMISSION_ID, params.getString("submissionId"));
		
		// allow resubmit number
		String allowResubmitNumber = "0";
		Assignment a = null;
		try
		{
			a = AssignmentService.getAssignment((String) state.getAttribute(GRADE_SUBMISSION_ASSIGNMENT_ID));
			if (a.getProperties().getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER) != null)
			{
				allowResubmitNumber= a.getProperties().getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER);
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}

		try
		{
			AssignmentSubmission s = AssignmentService.getSubmission((String) state.getAttribute(GRADE_SUBMISSION_SUBMISSION_ID));

			if ((s.getFeedbackText() == null) || (s.getFeedbackText().length() == 0))
			{
				state.setAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT, s.getSubmittedText());
			}
			else
			{
				state.setAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT, s.getFeedbackText());
			}
			state.setAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT, s.getFeedbackComment());

			List v = EntityManager.newReferenceList();
			Iterator attachments = s.getFeedbackAttachments().iterator();
			while (attachments.hasNext())
			{
				v.add(attachments.next());
			}
			state.setAttribute(ATTACHMENTS, v);

			state.setAttribute(GRADE_SUBMISSION_GRADE, s.getGrade());
			
			ResourceProperties p = s.getProperties();
			if (p.getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER) != null)
			{
				allowResubmitNumber = p.getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER);
			}
			else if (p.getProperty(GRADE_SUBMISSION_ALLOW_RESUBMIT) != null)
			{
				// if there is any legacy setting for generally allow resubmit, set the allow resubmit number to be 1, and remove the legacy property
				allowResubmitNumber = "1";
			}
			
			state.setAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER, allowResubmitNumber);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(GRADE_SUBMISSION_ASSIGNMENT_EXPAND_FLAG, new Boolean(false));
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_SUBMISSION);
		}

	} // doGrade_submission

	/**
	 * Action is to release all the grades of the submission
	 */
	public void doRelease_grades(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		ParameterParser params = data.getParameters();

		try
		{
			// get the assignment
			Assignment a = AssignmentService.getAssignment(params.getString("assignmentId"));

			String aReference = a.getReference();

			Iterator submissions = AssignmentService.getSubmissions(a).iterator();
			while (submissions.hasNext())
			{
				AssignmentSubmission s = (AssignmentSubmission) submissions.next();
				if (s.getGraded())
				{
					AssignmentSubmissionEdit sEdit = AssignmentService.editSubmission(s.getReference());
					String grade = s.getGrade();
					
					boolean withGrade = state.getAttribute(WITH_GRADES) != null ? ((Boolean) state.getAttribute(WITH_GRADES))
							.booleanValue() : false;
					if (withGrade)
					{
						// for the assignment tool with grade option, a valide grade is needed
						if (grade != null && !grade.equals(""))
						{
							sEdit.setGradeReleased(true);
						}
					}
					else
					{
						// for the assignment tool without grade option, no grade is needed
						sEdit.setGradeReleased(true);
					}
					
					// also set the return status
					sEdit.setReturned(true);
					sEdit.setTimeReturned(TimeService.newTime());
					sEdit.setHonorPledgeFlag(Boolean.FALSE.booleanValue());
					
					AssignmentService.commitEdit(sEdit);
				}

			} // while

			// add grades into Gradebook
			String integrateWithGradebook = a.getProperties().getProperty(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK);
			if (integrateWithGradebook != null && !integrateWithGradebook.equals(AssignmentService.GRADEBOOK_INTEGRATION_NO))
			{
				// integrate with Gradebook
				String associateGradebookAssignment = StringUtil.trimToNull(a.getProperties().getProperty(AssignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT));
				AssignmentService.integrateGradebook(aReference, associateGradebookAssignment, null, null, a.getTitle(), a.getTitle(), -1, null, null, "update");
				// set the gradebook assignment to be released to student
				AssignmentService.releaseGradebookAssignment(associateGradebookAssignment, true);
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}
		catch (InUseException e)
		{
			addAlert(state, rb.getString("somelsis") + " " + rb.getString("submiss"));
		}

	} // doRelease_grades

	/**
	 * Action is to show the assignment in grading page
	 */
	public void doExpand_grade_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_ASSIGNMENT_EXPAND_FLAG, new Boolean(true));

	} // doExpand_grade_assignment

	/**
	 * Action is to hide the assignment in grading page
	 */
	public void doCollapse_grade_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_ASSIGNMENT_EXPAND_FLAG, new Boolean(false));

	} // doCollapse_grade_assignment

	/**
	 * Action is to show the submissions in grading page
	 */
	public void doExpand_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_SUBMISSION_EXPAND_FLAG, new Boolean(true));

	} // doExpand_grade_submission

	/**
	 * Action is to hide the submissions in grading page
	 */
	public void doCollapse_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_SUBMISSION_EXPAND_FLAG, new Boolean(false));

	} // doCollapse_grade_submission

	/**
	 * Action is to show the grade assignment
	 */
	public void doGrade_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		// clean state attribute
		state.removeAttribute(USER_SUBMISSIONS);
		
		state.setAttribute(EXPORT_ASSIGNMENT_REF, params.getString("assignmentId"));

		try
		{
			Assignment a = AssignmentService.getAssignment((String) state.getAttribute(EXPORT_ASSIGNMENT_REF));
			state.setAttribute(EXPORT_ASSIGNMENT_ID, a.getId());
			state.setAttribute(GRADE_ASSIGNMENT_EXPAND_FLAG, new Boolean(false));
			state.setAttribute(GRADE_SUBMISSION_EXPAND_FLAG, new Boolean(true));
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);

			// we are changing the view, so start with first page again.
			resetPaging(state);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}
	} // doGrade_assignment

	/**
	 * Action is to show the View Students assignment screen
	 */
	public void doView_students_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT);

	} // doView_students_Assignment

	/**
	 * Action is to show the student submissions
	 */
	public void doShow_student_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		Set t = (Set) state.getAttribute(STUDENT_LIST_SHOW_TABLE);
		ParameterParser params = data.getParameters();

		String id = params.getString("studentId");
		// add the student id into the table
		t.add(id);

		state.setAttribute(STUDENT_LIST_SHOW_TABLE, t);

	} // doShow_student_submission

	/**
	 * Action is to hide the student submissions
	 */
	public void doHide_student_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		Set t = (Set) state.getAttribute(STUDENT_LIST_SHOW_TABLE);
		ParameterParser params = data.getParameters();

		String id = params.getString("studentId");
		// remove the student id from the table
		t.remove(id);

		state.setAttribute(STUDENT_LIST_SHOW_TABLE, t);

	} // doHide_student_submission

	/**
	 * Action is to show the graded assignment submission
	 */
	public void doView_grade(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		ParameterParser params = data.getParameters();

		state.setAttribute(VIEW_GRADE_SUBMISSION_ID, params.getString("submissionId"));

		state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_GRADE);

	} // doView_grade

	/**
	 * Action is to show the student submissions
	 */
	public void doReport_submissions(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_REPORT_SUBMISSIONS);
		state.setAttribute(SORTED_BY, SORTED_SUBMISSION_BY_LASTNAME);
		state.setAttribute(SORTED_ASC, Boolean.TRUE.toString());

	} // doReport_submissions

	/**
	 *
	 *
	 */
	public void doAssignment_form(RunData data)
	{
		ParameterParser params = data.getParameters();

		String option = (String) params.getString("option");
		if (option != null)
		{
			if (option.equals("post"))
			{
				// post assignment
				doPost_assignment(data);
			}
			else if (option.equals("save"))
			{
				// save assignment
				doSave_assignment(data);
			}
			else if (option.equals("preview"))
			{
				// preview assignment
				doPreview_assignment(data);
			}
			else if (option.equals("cancel"))
			{
				// cancel creating assignment
				doCancel_new_assignment(data);
			}
			else if (option.equals("canceledit"))
			{
				// cancel editing assignment
				doCancel_edit_assignment(data);
			}
			else if (option.equals("attach"))
			{
				// attachments
				doAttachments(data);
			}
			else if (option.equals("view"))
			{
				// view
				doView(data);
			}
			else if (option.equals("permissions"))
			{
				// permissions
				doPermissions(data);
			}
			else if (option.equals("returngrade"))
			{
				// return grading
				doReturn_grade_submission(data);
			}
			else if (option.equals("savegrade"))
			{
				// save grading
				doSave_grade_submission(data);
			}
			else if (option.equals("previewgrade"))
			{
				// preview grading
				doPreview_grade_submission(data);
			}
			else if (option.equals("cancelgrade"))
			{
				// cancel grading
				doCancel_grade_submission(data);
			}
			else if (option.equals("sortbygrouptitle"))
			{
				// read input data
				setNewAssignmentParameters(data, true);

				// sort by group title
				doSortbygrouptitle(data);
			}
			else if (option.equals("sortbygroupdescription"))
			{
				// read input data
				setNewAssignmentParameters(data, true);

				// sort group by description
				doSortbygroupdescription(data);
			}
			else if (option.equals("hide_instruction"))
			{
				// hide the assignment instruction
				doHide_submission_assignment_instruction(data);
			}
			else if (option.equals("show_instruction"))
			{
				// show the assignment instruction
				doShow_submission_assignment_instruction(data);
			}
			else if (option.equals("sortbygroupdescription"))
			{
				// show the assignment instruction
				doShow_submission_assignment_instruction(data);
			}


		}
	}

	/**
	 * Action is to use when doAattchmentsadding requested, corresponding to chef_Assignments-new "eventSubmit_doAattchmentsadding" when "add attachments" is clicked
	 */
	public void doAttachments(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		String mode = (String) state.getAttribute(STATE_MODE);
		if (mode.equals(MODE_STUDENT_VIEW_SUBMISSION))
		{
			// retrieve the submission text (as formatted text)
			boolean checkForFormattingErrors = true; // the student is submitting something - so check for errors
			String text = processFormattedTextFromBrowser(state, params.getCleanString(VIEW_SUBMISSION_TEXT),
					checkForFormattingErrors);

			state.setAttribute(VIEW_SUBMISSION_TEXT, text);
			if (params.getString(VIEW_SUBMISSION_HONOR_PLEDGE_YES) != null)
			{
				state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, "true");
			}
			// TODO: file picker to save in dropbox? -ggolden
			// User[] users = { UserDirectoryService.getCurrentUser() };
			// state.setAttribute(ResourcesAction.STATE_SAVE_ATTACHMENT_IN_DROPBOX, users);
		}
		else if (mode.equals(MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT))
		{
			setNewAssignmentParameters(data, false);
		}
		else if (mode.equals(MODE_INSTRUCTOR_GRADE_SUBMISSION))
		{
			readGradeForm(data, state, "read");
		}

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			// get into helper mode with this helper tool
			startHelper(data.getRequest(), "sakai.filepicker");

			state.setAttribute(FilePickerHelper.FILE_PICKER_TITLE_TEXT, rb.getString("gen.addatttoassig"));
			state.setAttribute(FilePickerHelper.FILE_PICKER_INSTRUCTION_TEXT, rb.getString("gen.addatttoassiginstr"));

			// use the real attachment list
			state.setAttribute(FilePickerHelper.FILE_PICKER_ATTACHMENTS, state.getAttribute(ATTACHMENTS));
		}
	}

	/**
	 * readGradeForm
	 */
	public void readGradeForm(RunData data, SessionState state, String gradeOption)
	{

		ParameterParser params = data.getParameters();

		boolean withGrade = state.getAttribute(WITH_GRADES) != null ? ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue()
				: false;

		boolean checkForFormattingErrors = false; // so that grading isn't held up by formatting errors
		String feedbackComment = processFormattedTextFromBrowser(state, params.getCleanString(GRADE_SUBMISSION_FEEDBACK_COMMENT),
				checkForFormattingErrors);
		if (feedbackComment != null)
		{
			state.setAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT, feedbackComment);
		}

		String feedbackText = processAssignmentFeedbackFromBrowser(state, params.getCleanString(GRADE_SUBMISSION_FEEDBACK_TEXT));
		if (feedbackText != null)
		{
			state.setAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT, feedbackText);
		}
		
		state.setAttribute(GRADE_SUBMISSION_FEEDBACK_ATTACHMENT, state.getAttribute(ATTACHMENTS));

		String g = params.getCleanString(GRADE_SUBMISSION_GRADE);
		if (g != null)
		{
			state.setAttribute(GRADE_SUBMISSION_GRADE, g);
		}

		String sId = (String) state.getAttribute(GRADE_SUBMISSION_SUBMISSION_ID);

		try
		{
			// for points grading, one have to enter number as the points
			String grade = (String) state.getAttribute(GRADE_SUBMISSION_GRADE);

			Assignment a = AssignmentService.getSubmission(sId).getAssignment();
			int typeOfGrade = a.getContent().getTypeOfGrade();

			if (withGrade)
			{
				// do grade validation only for Assignment with Grade tool
				if (typeOfGrade == 3)
				{
					if ((grade.length() == 0))
					{
						if (gradeOption.equals("release") || gradeOption.equals("return"))
						{
							// in case of releasing grade, user must specify a grade
							addAlert(state, rb.getString("plespethe2"));
						}
					}
					else
					{
						// the preview grade process might already scaled up the grade by 10
						if (!((String) state.getAttribute(STATE_MODE)).equals(MODE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION))
						{
							validPointGrade(state, grade);
							
							if (state.getAttribute(STATE_MESSAGE) == null)
							{
								int maxGrade = a.getContent().getMaxGradePoint();
								try
								{
									if (Integer.parseInt(scalePointGrade(state, grade)) > maxGrade)
									{
										if (state.getAttribute(GRADE_GREATER_THAN_MAX_ALERT) == null)
										{
											// alert user first when he enters grade bigger than max scale
											addAlert(state, rb.getString("grad2"));
											state.setAttribute(GRADE_GREATER_THAN_MAX_ALERT, Boolean.TRUE);
										}
										else
										{
											// remove the alert once user confirms he wants to give student higher grade
											state.removeAttribute(GRADE_GREATER_THAN_MAX_ALERT);
										}
									}
								}
								catch (NumberFormatException e)
								{
									alertInvalidPoint(state, grade);
								}
							}
							
							if (state.getAttribute(STATE_MESSAGE) == null)
							{
								grade = scalePointGrade(state, grade);
							}
							state.setAttribute(GRADE_SUBMISSION_GRADE, grade);
						}
					}
				}

				// if ungraded and grade type is not "ungraded" type
				if ((grade == null || grade.equals("ungraded")) && (typeOfGrade != 1) && gradeOption.equals("release"))
				{
					addAlert(state, rb.getString("plespethe2"));
				}
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}
		
		// allow resubmit number and due time
		if (params.getString(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER) != null)
		{
			String allowResubmitNumberString = params.getString(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER);
			state.setAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER, params.getString(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER));
		
			if (Integer.parseInt(allowResubmitNumberString) != 0)
			{
				int closeMonth = (new Integer(params.getString(ALLOW_RESUBMIT_CLOSEMONTH))).intValue();
				state.setAttribute(ALLOW_RESUBMIT_CLOSEMONTH, new Integer(closeMonth));
				int closeDay = (new Integer(params.getString(ALLOW_RESUBMIT_CLOSEDAY))).intValue();
				state.setAttribute(ALLOW_RESUBMIT_CLOSEDAY, new Integer(closeDay));
				int closeYear = (new Integer(params.getString(ALLOW_RESUBMIT_CLOSEYEAR))).intValue();
				state.setAttribute(ALLOW_RESUBMIT_CLOSEYEAR, new Integer(closeYear));
				int closeHour = (new Integer(params.getString(ALLOW_RESUBMIT_CLOSEHOUR))).intValue();
				state.setAttribute(ALLOW_RESUBMIT_CLOSEHOUR, new Integer(closeHour));
				int closeMin = (new Integer(params.getString(ALLOW_RESUBMIT_CLOSEMIN))).intValue();
				state.setAttribute(ALLOW_RESUBMIT_CLOSEMIN, new Integer(closeMin));
				String closeAMPM = params.getString(ALLOW_RESUBMIT_CLOSEAMPM);
				state.setAttribute(ALLOW_RESUBMIT_CLOSEAMPM, closeAMPM);
				if ((closeAMPM.equals("PM")) && (closeHour != 12))
				{
					closeHour = closeHour + 12;
				}
				if ((closeHour == 12) && (closeAMPM.equals("AM")))
				{
					closeHour = 0;
				}
				Time closeTime = TimeService.newTimeLocal(closeYear, closeMonth, closeDay, closeHour, closeMin, 0, 0);
				state.setAttribute(AssignmentSubmission.ALLOW_RESUBMIT_CLOSETIME, String.valueOf(closeTime.getTime()));
				// validate date
				if (closeTime.before(TimeService.newTime()))
				{
					addAlert(state, rb.getString("assig4"));
				}
				if (!Validator.checkDate(closeDay, closeMonth, closeYear))
				{
					addAlert(state, rb.getString("date.invalid") + rb.getString("date.closedate") + ".");
				}
			}
			else
			{
				// reset the state attributes
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEMONTH);
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEDAY);
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEYEAR);
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEHOUR);
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEMIN);
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEAMPM);
				state.removeAttribute(AssignmentSubmission.ALLOW_RESUBMIT_CLOSETIME);
			}
		}
	}

	/**
	 * Populate the state object, if needed - override to do something!
	 */
	protected void initState(SessionState state, VelocityPortlet portlet, JetspeedRunData data)
	{
		super.initState(state, portlet, data);

		String siteId = ToolManager.getCurrentPlacement().getContext();

		// show the list of assignment view first
		if (state.getAttribute(STATE_SELECTED_VIEW) == null)
		{
			state.setAttribute(STATE_SELECTED_VIEW, MODE_LIST_ASSIGNMENTS);
		}

		if (state.getAttribute(STATE_USER) == null)
		{
			state.setAttribute(STATE_USER, UserDirectoryService.getCurrentUser());
		}

		/** The content type image lookup service in the State. */
		ContentTypeImageService iService = (ContentTypeImageService) state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE);
		if (iService == null)
		{
			iService = org.sakaiproject.content.cover.ContentTypeImageService.getInstance();
			state.setAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE, iService);
		} // if

		/** The calendar service in the State. */
		CalendarService cService = (CalendarService) state.getAttribute(STATE_CALENDAR_SERVICE);
		if (cService == null)
		{
			cService = org.sakaiproject.calendar.cover.CalendarService.getInstance();
			state.setAttribute(STATE_CALENDAR_SERVICE, cService);

			String calendarId = ServerConfigurationService.getString("calendar", null);
			if (calendarId == null)
			{
				calendarId = cService.calendarReference(siteId, SiteService.MAIN_CONTAINER);
				try
				{
					state.setAttribute(CALENDAR, cService.getCalendar(calendarId));
				}
				catch (IdUnusedException e)
				{
					Log.info("chef", "No calendar found for site " + siteId);
					state.removeAttribute(CALENDAR);
				}
				catch (PermissionException e)
				{
					Log.info("chef", "No permission to get the calender. ");
					state.removeAttribute(CALENDAR);
				}
				catch (Exception ex)
				{
					Log.info("chef", "Assignment : Action : init state : calendar exception : " + ex);
					state.removeAttribute(CALENDAR);
				}
			}
		} // if

		/** The announcement service in the State. */
		AnnouncementService aService = (AnnouncementService) state.getAttribute(STATE_ANNOUNCEMENT_SERVICE);
		if (aService == null)
		{
			aService = org.sakaiproject.announcement.cover.AnnouncementService.getInstance();
			state.setAttribute(STATE_ANNOUNCEMENT_SERVICE, aService);

			String channelId = ServerConfigurationService.getString("channel", null);
			if (channelId == null)
			{
				channelId = aService.channelReference(siteId, SiteService.MAIN_CONTAINER);
				try
				{
					state.setAttribute(ANNOUNCEMENT_CHANNEL, aService.getAnnouncementChannel(channelId));
				}
				catch (IdUnusedException e)
				{
					Log.warn("chef", "No announcement channel found. ");
					state.removeAttribute(ANNOUNCEMENT_CHANNEL);
				}
				catch (PermissionException e)
				{
					Log.warn("chef", "No permission to annoucement channel. ");
				}
				catch (Exception ex)
				{
					Log.warn("chef", "Assignment : Action : init state : calendar exception : " + ex);
				}
			}

		} // if

		if (state.getAttribute(STATE_CONTEXT_STRING) == null)
		{
			state.setAttribute(STATE_CONTEXT_STRING, siteId);
		} // if context string is null

		if (state.getAttribute(SORTED_BY) == null)
		{
			state.setAttribute(SORTED_BY, SORTED_BY_DUEDATE);
		}

		if (state.getAttribute(SORTED_ASC) == null)
		{
			state.setAttribute(SORTED_ASC, Boolean.FALSE.toString());
		}

		if (state.getAttribute(SORTED_GRADE_SUBMISSION_BY) == null)
		{
			state.setAttribute(SORTED_GRADE_SUBMISSION_BY, SORTED_GRADE_SUBMISSION_BY_LASTNAME);
		}

		if (state.getAttribute(SORTED_GRADE_SUBMISSION_ASC) == null)
		{
			state.setAttribute(SORTED_GRADE_SUBMISSION_ASC, Boolean.TRUE.toString());
		}

		if (state.getAttribute(SORTED_SUBMISSION_BY) == null)
		{
			state.setAttribute(SORTED_SUBMISSION_BY, SORTED_SUBMISSION_BY_LASTNAME);
		}

		if (state.getAttribute(SORTED_SUBMISSION_ASC) == null)
		{
			state.setAttribute(SORTED_SUBMISSION_ASC, Boolean.TRUE.toString());
		}

		if (state.getAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG) == null)
		{
			resetAssignment(state);
		}

		if (state.getAttribute(STUDENT_LIST_SHOW_TABLE) == null)
		{
			state.setAttribute(STUDENT_LIST_SHOW_TABLE, new HashSet());
		}

		if (state.getAttribute(ATTACHMENTS_MODIFIED) == null)
		{
			state.setAttribute(ATTACHMENTS_MODIFIED, new Boolean(false));
		}

		// SECTION MOD
		if (state.getAttribute(STATE_SECTION_STRING) == null)
		{
			
			state.setAttribute(STATE_SECTION_STRING, "001");
		}

		// // setup the observer to notify the Main panel
		// if (state.getAttribute(STATE_OBSERVER) == null)
		// {
		// // the delivery location for this tool
		// String deliveryId = clientWindowId(state, portlet.getID());
		//
		// // the html element to update on delivery
		// String elementId = mainPanelUpdateId(portlet.getID());
		//
		// // the event resource reference pattern to watch for
		// String pattern = AssignmentService.assignmentReference((String) state.getAttribute (STATE_CONTEXT_STRING), "");
		//
		// state.setAttribute(STATE_OBSERVER, new MultipleEventsObservingCourier(deliveryId, elementId, pattern));
		// }

		if (state.getAttribute(STATE_MODE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		}

		if (state.getAttribute(STATE_TOP_PAGE_MESSAGE) == null)
		{
			state.setAttribute(STATE_TOP_PAGE_MESSAGE, new Integer(0));
		}

		if (state.getAttribute(WITH_GRADES) == null)
		{
			PortletConfig config = portlet.getPortletConfig();
			String withGrades = StringUtil.trimToNull(config.getInitParameter("withGrades"));
			if (withGrades == null)
			{
				withGrades = Boolean.FALSE.toString();
			}
			state.setAttribute(WITH_GRADES, new Boolean(withGrades));
		}
		
		// whether the choice of emails instructor submission notification is available in the installation
		if (state.getAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS) == null)
		{
			state.setAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS, Boolean.valueOf(ServerConfigurationService.getBoolean(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS, true)));
		}
		
		// whether or how the instructor receive submission notification emails, none(default)|each|digest
		if (state.getAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DEFAULT) == null)
		{
			state.setAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DEFAULT, ServerConfigurationService.getString(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DEFAULT, Assignment.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_NONE));
		}
		
		if (state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_FROM) == null)
		{
			state.setAttribute(NEW_ASSIGNMENT_YEAR_RANGE_FROM, new Integer(2002));
		}
		
		if (state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_TO) == null)
		{
			state.setAttribute(NEW_ASSIGNMENT_YEAR_RANGE_TO, new Integer(2012));
		}
	} // initState

	/**
	 * reset the attributes for view submission
	 */
	private void resetViewSubmission(SessionState state)
	{
		state.removeAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE);
		state.removeAttribute(VIEW_SUBMISSION_TEXT);
		state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, "false");
		state.removeAttribute(GRADE_GREATER_THAN_MAX_ALERT);

	} // resetViewSubmission

	/**
	 * reset the attributes for view submission
	 */
	private void resetAssignment(SessionState state)
	{
		// put the input value into the state attributes
		state.setAttribute(NEW_ASSIGNMENT_TITLE, "");

		// get current time
		Time t = TimeService.newTime();
		TimeBreakdown tB = t.breakdownLocal();
		int month = tB.getMonth();
		int day = tB.getDay();
		int year = tB.getYear();

		// set the open time to be 12:00 PM
		state.setAttribute(NEW_ASSIGNMENT_OPENMONTH, new Integer(month));
		state.setAttribute(NEW_ASSIGNMENT_OPENDAY, new Integer(day));
		state.setAttribute(NEW_ASSIGNMENT_OPENYEAR, new Integer(year));
		state.setAttribute(NEW_ASSIGNMENT_OPENHOUR, new Integer(12));
		state.setAttribute(NEW_ASSIGNMENT_OPENMIN, new Integer(0));
		state.setAttribute(NEW_ASSIGNMENT_OPENAMPM, "PM");

		// due date is shifted forward by 7 days
		t.setTime(t.getTime() + 7 * 24 * 60 * 60 * 1000);
		tB = t.breakdownLocal();
		month = tB.getMonth();
		day = tB.getDay();
		year = tB.getYear();

		// set the due time to be 5:00pm
		state.setAttribute(NEW_ASSIGNMENT_DUEMONTH, new Integer(month));
		state.setAttribute(NEW_ASSIGNMENT_DUEDAY, new Integer(day));
		state.setAttribute(NEW_ASSIGNMENT_DUEYEAR, new Integer(year));
		state.setAttribute(NEW_ASSIGNMENT_DUEHOUR, new Integer(5));
		state.setAttribute(NEW_ASSIGNMENT_DUEMIN, new Integer(0));
		state.setAttribute(NEW_ASSIGNMENT_DUEAMPM, "PM");

		// enable the close date by default
		state.setAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE, new Boolean(true));
		// set the close time to be 5:00 pm, same as the due time by default
		state.setAttribute(NEW_ASSIGNMENT_CLOSEMONTH, new Integer(month));
		state.setAttribute(NEW_ASSIGNMENT_CLOSEDAY, new Integer(day));
		state.setAttribute(NEW_ASSIGNMENT_CLOSEYEAR, new Integer(year));
		state.setAttribute(NEW_ASSIGNMENT_CLOSEHOUR, new Integer(5));
		state.setAttribute(NEW_ASSIGNMENT_CLOSEMIN, new Integer(0));
		state.setAttribute(NEW_ASSIGNMENT_CLOSEAMPM, "PM");

		state.setAttribute(NEW_ASSIGNMENT_SECTION, "001");
		state.setAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE, new Integer(Assignment.TEXT_AND_ATTACHMENT_ASSIGNMENT_SUBMISSION));
		state.setAttribute(NEW_ASSIGNMENT_GRADE_TYPE, new Integer(Assignment.UNGRADED_GRADE_TYPE));
		state.setAttribute(NEW_ASSIGNMENT_GRADE_POINTS, "");
		state.setAttribute(NEW_ASSIGNMENT_DESCRIPTION, "");
		state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE, Boolean.FALSE.toString());
		state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE, Boolean.FALSE.toString());
		// make the honor pledge not include as the default
		state.setAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE, (new Integer(Assignment.HONOR_PLEDGE_NONE)).toString());

		state.setAttribute(AssignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, AssignmentService.GRADEBOOK_INTEGRATION_NO);

		state.setAttribute(NEW_ASSIGNMENT_ATTACHMENT, EntityManager.newReferenceList());

		state.setAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG, new Boolean(false));

		state.setAttribute(NEW_ASSIGNMENT_FOCUS, NEW_ASSIGNMENT_TITLE);

		state.removeAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY);

		// reset the global navigaion alert flag
		if (state.getAttribute(ALERT_GLOBAL_NAVIGATION) != null)
		{
			state.removeAttribute(ALERT_GLOBAL_NAVIGATION);
		}

		state.setAttribute(NEW_ASSIGNMENT_RANGE, "site");
		state.removeAttribute(NEW_ASSIGNMENT_GROUPS);

		// remove the edit assignment id if any
		state.removeAttribute(EDIT_ASSIGNMENT_ID);
		
		// remove the resubmit number
		state.removeAttribute(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER);

	} // resetNewAssignment

	/**
	 * construct a Hashtable using integer as the key and three character string of the month as the value
	 */
	private Hashtable monthTable()
	{
		Hashtable n = new Hashtable();
		n.put(new Integer(1), rb.getString("jan"));
		n.put(new Integer(2), rb.getString("feb"));
		n.put(new Integer(3), rb.getString("mar"));
		n.put(new Integer(4), rb.getString("apr"));
		n.put(new Integer(5), rb.getString("may"));
		n.put(new Integer(6), rb.getString("jun"));
		n.put(new Integer(7), rb.getString("jul"));
		n.put(new Integer(8), rb.getString("aug"));
		n.put(new Integer(9), rb.getString("sep"));
		n.put(new Integer(10), rb.getString("oct"));
		n.put(new Integer(11), rb.getString("nov"));
		n.put(new Integer(12), rb.getString("dec"));
		return n;

	} // monthTable

	/**
	 * construct a Hashtable using the integer as the key and grade type String as the value
	 */
	private Hashtable gradeTypeTable()
	{
		Hashtable n = new Hashtable();
		n.put(new Integer(2), rb.getString("letter"));
		n.put(new Integer(3), rb.getString("points"));
		n.put(new Integer(4), rb.getString("pass"));
		n.put(new Integer(5), rb.getString("check"));
		n.put(new Integer(1), rb.getString("ungra"));
		return n;

	} // gradeTypeTable

	/**
	 * construct a Hashtable using the integer as the key and submission type String as the value
	 */
	private Hashtable submissionTypeTable()
	{
		Hashtable n = new Hashtable();
		n.put(new Integer(1), rb.getString("inlin"));
		n.put(new Integer(2), rb.getString("attaonly"));
		n.put(new Integer(3), rb.getString("inlinatt"));
		n.put(new Integer(4), rb.getString("nonelec"));
		return n;

	} // submissionTypeTable

	/**
	 * Sort based on the given property
	 */
	public void doSort(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// we are changing the sort, so start from the first page again
		resetPaging(state);

		setupSort(data, data.getParameters().getString("criteria"));
	}

	/**
	 * setup sorting parameters
	 *
	 * @param criteria
	 *        String for sortedBy
	 */
	private void setupSort(RunData data, String criteria)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// current sorting sequence
		String asc = "";
		if (!criteria.equals(state.getAttribute(SORTED_BY)))
		{
			state.setAttribute(SORTED_BY, criteria);
			asc = Boolean.TRUE.toString();
			state.setAttribute(SORTED_ASC, asc);
		}
		else
		{
			// current sorting sequence
			asc = (String) state.getAttribute(SORTED_ASC);

			// toggle between the ascending and descending sequence
			if (asc.equals(Boolean.TRUE.toString()))
			{
				asc = Boolean.FALSE.toString();
			}
			else
			{
				asc = Boolean.TRUE.toString();
			}
			state.setAttribute(SORTED_ASC, asc);
		}

	} // doSort

	/**
	 * Do sort by group title
	 */
	public void doSortbygrouptitle(RunData data)
	{
		setupSort(data, SORTED_BY_GROUP_TITLE);

	} // doSortbygrouptitle

	/**
	 * Do sort by group description
	 */
	public void doSortbygroupdescription(RunData data)
	{
		setupSort(data, SORTED_BY_GROUP_DESCRIPTION);

	} // doSortbygroupdescription

	/**
	 * Sort submission based on the given property
	 */
	public void doSort_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// we are changing the sort, so start from the first page again
		resetPaging(state);

		// get the ParameterParser from RunData
		ParameterParser params = data.getParameters();

		String criteria = params.getString("criteria");

		// current sorting sequence
		String asc = "";

		if (!criteria.equals(state.getAttribute(SORTED_SUBMISSION_BY)))
		{
			state.setAttribute(SORTED_SUBMISSION_BY, criteria);
			asc = Boolean.TRUE.toString();
			state.setAttribute(SORTED_SUBMISSION_ASC, asc);
		}
		else
		{
			// current sorting sequence
			state.setAttribute(SORTED_SUBMISSION_BY, criteria);
			asc = (String) state.getAttribute(SORTED_SUBMISSION_ASC);

			// toggle between the ascending and descending sequence
			if (asc.equals(Boolean.TRUE.toString()))
			{
				asc = Boolean.FALSE.toString();
			}
			else
			{
				asc = Boolean.TRUE.toString();
			}
			state.setAttribute(SORTED_SUBMISSION_ASC, asc);
		}
	} // doSort_submission


	
	
	/**
	 * Sort submission based on the given property in instructor grade view
	 */
	public void doSort_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// we are changing the sort, so start from the first page again
		resetPaging(state);

		// get the ParameterParser from RunData
		ParameterParser params = data.getParameters();

		String criteria = params.getString("criteria");

		// current sorting sequence
		String asc = "";

		if (!criteria.equals(state.getAttribute(SORTED_GRADE_SUBMISSION_BY)))
		{
			state.setAttribute(SORTED_GRADE_SUBMISSION_BY, criteria);
			asc = Boolean.TRUE.toString();
			state.setAttribute(SORTED_GRADE_SUBMISSION_ASC, asc);
		}
		else
		{
			// current sorting sequence
			state.setAttribute(SORTED_GRADE_SUBMISSION_BY, criteria);
			asc = (String) state.getAttribute(SORTED_GRADE_SUBMISSION_ASC);

			// toggle between the ascending and descending sequence
			if (asc.equals(Boolean.TRUE.toString()))
			{
				asc = Boolean.FALSE.toString();
			}
			else
			{
				asc = Boolean.TRUE.toString();
			}
			state.setAttribute(SORTED_GRADE_SUBMISSION_ASC, asc);
		}
	} // doSort_grade_submission

	public void doSort_tags(RunData data) {
		SessionState state = ((JetspeedRunData) data)
				.getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		ParameterParser params = data.getParameters();

		String criteria = params.getString("criteria");
		String providerId = params.getString(PROVIDER_ID);
		
		String savedText = params.getString("savedText");		
		state.setAttribute(VIEW_SUBMISSION_TEXT, savedText);

		String mode = (String) state.getAttribute(STATE_MODE);
		
		List<DecoratedTaggingProvider> providers = (List) state
				.getAttribute(mode + PROVIDER_LIST);

		for (DecoratedTaggingProvider dtp : providers) {
			if (dtp.getProvider().getId().equals(providerId)) {
				Sort sort = dtp.getSort();
				if (sort.getSort().equals(criteria)) {
					sort.setAscending(sort.isAscending() ? false : true);
				} else {
					sort.setSort(criteria);
					sort.setAscending(true);
				}
				break;
			}
		}
	}
	
	public void doPage_tags(RunData data) {
		SessionState state = ((JetspeedRunData) data)
				.getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		ParameterParser params = data.getParameters();

		String page = params.getString("page");
		String pageSize = params.getString("pageSize");
		String providerId = params.getString(PROVIDER_ID);

		String savedText = params.getString("savedText");		
		state.setAttribute(VIEW_SUBMISSION_TEXT, savedText);

		String mode = (String) state.getAttribute(STATE_MODE);
		
		List<DecoratedTaggingProvider> providers = (List) state
				.getAttribute(mode + PROVIDER_LIST);

		for (DecoratedTaggingProvider dtp : providers) {
			if (dtp.getProvider().getId().equals(providerId)) {
				Pager pager = dtp.getPager();
				pager.setPageSize(Integer.valueOf(pageSize));
				if (Pager.FIRST.equals(page)) {
					pager.setFirstItem(0);
				} else if (Pager.PREVIOUS.equals(page)) {
					pager.setFirstItem(pager.getFirstItem()
							- pager.getPageSize());
				} else if (Pager.NEXT.equals(page)) {
					pager.setFirstItem(pager.getFirstItem()
							+ pager.getPageSize());
				} else if (Pager.LAST.equals(page)) {
					pager.setFirstItem((pager.getTotalItems() / pager
							.getPageSize())
							* pager.getPageSize());
				}
				break;			
			}
		}
	}
	
	/**
	 * the UserSubmission clas
	 */
	public class UserSubmission
	{
		/**
		 * the User object
		 */
		User m_user = null;

		/**
		 * the AssignmentSubmission object
		 */
		AssignmentSubmission m_submission = null;

		public UserSubmission(User u, AssignmentSubmission s)
		{
			m_user = u;
			m_submission = s;
		}

		/**
		 * Returns the AssignmentSubmission object
		 */
		public AssignmentSubmission getSubmission()
		{
			return m_submission;
		}

		/**
		 * Returns the User object
		 */
		public User getUser()
		{
			return m_user;
		}
	}

	/**
	 * the AssignmentComparator clas
	 */
	private class AssignmentComparator implements Comparator
	{
		Collator collator = Collator.getInstance();
		
		/**
		 * the SessionState object
		 */
		SessionState m_state = null;

		/**
		 * the criteria
		 */
		String m_criteria = null;

		/**
		 * the criteria
		 */
		String m_asc = null;

		/**
		 * the user
		 */
		User m_user = null;

		/**
		 * constructor
		 *
		 * @param state
		 *        The state object
		 * @param criteria
		 *        The sort criteria string
		 * @param asc
		 *        The sort order string. TRUE_STRING if ascending; "false" otherwise.
		 */
		public AssignmentComparator(SessionState state, String criteria, String asc)
		{
			m_state = state;
			m_criteria = criteria;
			m_asc = asc;

		} // constructor

		/**
		 * constructor
		 *
		 * @param state
		 *        The state object
		 * @param criteria
		 *        The sort criteria string
		 * @param asc
		 *        The sort order string. TRUE_STRING if ascending; "false" otherwise.
		 * @param user
		 *        The user object
		 */
		public AssignmentComparator(SessionState state, String criteria, String asc, User user)
		{
			m_state = state;
			m_criteria = criteria;
			m_asc = asc;
			m_user = user;
		} // constructor

		/**
		 * caculate the range string for an assignment
		 */
		private String getAssignmentRange(Assignment a)
		{
			String rv = "";
			if (a.getAccess().equals(Assignment.AssignmentAccess.SITE))
			{
				// site assignment
				rv = rb.getString("range.allgroups");
			}
			else
			{
				try
				{
					// get current site
					Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
					for (Iterator k = a.getGroups().iterator(); k.hasNext();)
					{
						// announcement by group
						rv = rv.concat(site.getGroup((String) k.next()).getTitle());
					}
				}
				catch (Exception ignore)
				{
				}
			}

			return rv;

		} // getAssignmentRange

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
			if (m_criteria.equals(SORTED_BY_TITLE))
			{
				// sorted by the assignment title
				String s1 = ((Assignment) o1).getTitle();
				String s2 = ((Assignment) o2).getTitle();
				result = compareString(s1, s2);
			}
			else if (m_criteria.equals(SORTED_BY_SECTION))
			{
				// sorted by the assignment section
				String s1 = ((Assignment) o1).getSection();
				String s2 = ((Assignment) o2).getSection();
				result = compareString(s1, s2);
			}
			else if (m_criteria.equals(SORTED_BY_DUEDATE))
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
			else if (m_criteria.equals(SORTED_BY_OPENDATE))
			{
				// sorted by the assignment open
				Time t1 = ((Assignment) o1).getOpenTime();
				Time t2 = ((Assignment) o2).getOpenTime();

				if (t1 == null)
				{
					result = -1;
				}
				else if (t2 == null)
				{
					result = 1;
				}
				if (t1.before(t2))
				{
					result = -1;
				}
				else
				{
					result = 1;
				}
			}
			else if (m_criteria.equals(SORTED_BY_ASSIGNMENT_STATUS))
			{
				String s1 = getAssignmentStatus((Assignment) o1);
				String s2 = getAssignmentStatus((Assignment) o2);
				result = compareString(s1, s2);
			}
			else if (m_criteria.equals(SORTED_BY_NUM_SUBMISSIONS))
			{
				// sort by numbers of submissions

				// initialize
				int subNum1 = 0;
				int subNum2 = 0;

				Iterator submissions1 = AssignmentService.getSubmissions((Assignment) o1).iterator();
				while (submissions1.hasNext())
				{
					AssignmentSubmission submission1 = (AssignmentSubmission) submissions1.next();
					if (submission1.getSubmitted()) subNum1++;
				}

				Iterator submissions2 = AssignmentService.getSubmissions((Assignment) o2).iterator();
				while (submissions2.hasNext())
				{
					AssignmentSubmission submission2 = (AssignmentSubmission) submissions2.next();
					if (submission2.getSubmitted()) subNum2++;
				}

				result = (subNum1 > subNum2) ? 1 : -1;

			}
			else if (m_criteria.equals(SORTED_BY_NUM_UNGRADED))
			{
				// sort by numbers of ungraded submissions

				// initialize
				int ungraded1 = 0;
				int ungraded2 = 0;

				Iterator submissions1 = AssignmentService.getSubmissions((Assignment) o1).iterator();
				while (submissions1.hasNext())
				{
					AssignmentSubmission submission1 = (AssignmentSubmission) submissions1.next();
					if (submission1.getSubmitted() && !submission1.getGraded()) ungraded1++;
				}

				Iterator submissions2 = AssignmentService.getSubmissions((Assignment) o2).iterator();
				while (submissions2.hasNext())
				{
					AssignmentSubmission submission2 = (AssignmentSubmission) submissions2.next();
					if (submission2.getSubmitted() && !submission2.getGraded()) ungraded2++;
				}

				result = (ungraded1 > ungraded2) ? 1 : -1;

			}
			else if (m_criteria.equals(SORTED_BY_SUBMISSION_STATUS))
			{
				try
				{
					AssignmentSubmission submission1 = AssignmentService.getSubmission(((Assignment) o1).getId(), m_user);
					String status1 = getSubmissionStatus(submission1, (Assignment) o1);

					AssignmentSubmission submission2 = AssignmentService.getSubmission(((Assignment) o2).getId(), m_user);
					String status2 = getSubmissionStatus(submission2, (Assignment) o2);

					result = compareString(status1, status2);
				}
				catch (IdUnusedException e)
				{
					return 1;
				}
				catch (PermissionException e)
				{
					return 1;
				}
			}
			else if (m_criteria.equals(SORTED_BY_GRADE))
			{
				try
				{
					AssignmentSubmission submission1 = AssignmentService.getSubmission(((Assignment) o1).getId(), m_user);
					String grade1 = " ";
					if (submission1 != null && submission1.getGraded() && submission1.getGradeReleased())
					{
						grade1 = submission1.getGrade();
					}

					AssignmentSubmission submission2 = AssignmentService.getSubmission(((Assignment) o2).getId(), m_user);
					String grade2 = " ";
					if (submission2 != null && submission2.getGraded() && submission2.getGradeReleased())
					{
						grade2 = submission2.getGrade();
					}

					result = compareString(grade1, grade2);
				}
				catch (IdUnusedException e)
				{
					return 1;
				}
				catch (PermissionException e)
				{
					return 1;
				}
			}
			else if (m_criteria.equals(SORTED_BY_MAX_GRADE))
			{
				String maxGrade1 = maxGrade(((Assignment) o1).getContent().getTypeOfGrade(), (Assignment) o1);
				String maxGrade2 = maxGrade(((Assignment) o2).getContent().getTypeOfGrade(), (Assignment) o2);

				try
				{
					// do integer comparation inside point grade type
					int max1 = Integer.parseInt(maxGrade1);
					int max2 = Integer.parseInt(maxGrade2);
					result = (max1 < max2) ? -1 : 1;
				}
				catch (NumberFormatException e)
				{
					// otherwise do an alpha-compare
					result = compareString(maxGrade1, maxGrade2);
				}
			}
			// group related sorting
			else if (m_criteria.equals(SORTED_BY_FOR))
			{
				// sorted by the public view attribute
				String factor1 = getAssignmentRange((Assignment) o1);
				String factor2 = getAssignmentRange((Assignment) o2);
				result = compareString(factor1, factor2);
			}
			else if (m_criteria.equals(SORTED_BY_GROUP_TITLE))
			{
				// sorted by the group title
				String factor1 = ((Group) o1).getTitle();
				String factor2 = ((Group) o2).getTitle();
				result = compareString(factor1, factor2);
			}
			else if (m_criteria.equals(SORTED_BY_GROUP_DESCRIPTION))
			{
				// sorted by the group description
				String factor1 = ((Group) o1).getDescription();
				String factor2 = ((Group) o2).getDescription();
				if (factor1 == null)
				{
					factor1 = "";
				}
				if (factor2 == null)
				{
					factor2 = "";
				}
				result = compareString(factor1, factor2);
			}
			/** ***************** for sorting submissions in instructor grade assignment view ************* */
			else if(m_criteria.equals(SORTED_GRADE_SUBMISSION_CONTENTREVIEW))
			{
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;
				if (u1 == null || u2 == null || u1.getUser() == null || u2.getUser() == null )
				{
					result = 1;
				}
				else
				{	
					AssignmentSubmission s1 = u1.getSubmission();
					AssignmentSubmission s2 = u2.getSubmission();


					if (s1 == null)
					{
						result = -1;
					}
					else if (s2 == null )
					{
						result = 1;
					} 
					else
					{
						int score1 = u1.getSubmission().getReviewScore();
						int score2 = u2.getSubmission().getReviewScore();
						result = (new Integer(score1)).intValue() > (new Integer(score2)).intValue() ? 1 : -1;
					}
				}
				
			}
			else if (m_criteria.equals(SORTED_GRADE_SUBMISSION_BY_LASTNAME))
			{
				// sorted by the submitters sort name
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;

				if (u1 == null || u2 == null || u1.getUser() == null || u2.getUser() == null )
				{
					result = 1;
				}
				else
				{
					String lName1 = u1.getUser().getSortName();
					String lName2 = u2.getUser().getSortName();
					result = compareString(lName1, lName2);
				}
			}
			else if (m_criteria.equals(SORTED_GRADE_SUBMISSION_BY_SUBMIT_TIME))
			{
				// sorted by submission time
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;

				if (u1 == null || u2 == null)
				{
					result = -1;
				}
				else
				{
					AssignmentSubmission s1 = u1.getSubmission();
					AssignmentSubmission s2 = u2.getSubmission();


					if (s1 == null || s1.getTimeSubmitted() == null)
					{
						result = -1;
					}
					else if (s2 == null || s2.getTimeSubmitted() == null)
					{
						result = 1;
					}
					else if (s1.getTimeSubmitted().before(s2.getTimeSubmitted()))
					{
						result = -1;
					}
					else
					{
						result = 1;
					}
				}
			}
			else if (m_criteria.equals(SORTED_GRADE_SUBMISSION_BY_STATUS))
			{
				// sort by submission status
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;

				String status1 = "";
				String status2 = "";
				
				if (u1 == null)
				{
					status1 = rb.getString("listsub.nosub");
				}
				else
				{
					AssignmentSubmission s1 = u1.getSubmission();
					if (s1 == null)
					{
						status1 = rb.getString("listsub.nosub");
					}
					else
					{
						status1 = getSubmissionStatus(m_state, (AssignmentSubmission) s1);
					}
				}
				
				if (u2 == null)
				{
					status2 = rb.getString("listsub.nosub");
				}
				else
				{
					AssignmentSubmission s2 = u2.getSubmission();
					if (s2 == null)
					{
						status2 = rb.getString("listsub.nosub");
					}
					else
					{
						status2 = getSubmissionStatus(m_state, (AssignmentSubmission) s2);
					}
				}
				
				result = compareString(status1, status2);
			}
			else if (m_criteria.equals(SORTED_GRADE_SUBMISSION_BY_GRADE))
			{
				// sort by submission status
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;

				if (u1 == null || u2 == null)
				{
					result = -1;
				}
				else
				{
					AssignmentSubmission s1 = u1.getSubmission();
					AssignmentSubmission s2 = u2.getSubmission();

					//sort by submission grade
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
						String grade1 = s1.getGrade();
						String grade2 = s2.getGrade();
						if (grade1 == null)
						{
							grade1 = "";
						}
						if (grade2 == null)
						{
							grade2 = "";
						}

						// if scale is points
						if ((s1.getAssignment().getContent().getTypeOfGrade() == 3)
								&& ((s2.getAssignment().getContent().getTypeOfGrade() == 3)))
						{
							if (grade1.equals(""))
							{
								result = -1;
							}
							else if (grade2.equals(""))
							{
								result = 1;
							}
							else
							{
								result = (new Double(grade1)).doubleValue() > (new Double(grade2)).doubleValue() ? 1 : -1;

							}
						}
						else
						{
							result = compareString(grade1, grade2);
						}
					}
				}
			}
			else if (m_criteria.equals(SORTED_GRADE_SUBMISSION_BY_RELEASED))
			{
				// sort by submission status
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;

				if (u1 == null || u2 == null)
				{
					result = -1;
				}
				else
				{
					AssignmentSubmission s1 = u1.getSubmission();
					AssignmentSubmission s2 = u2.getSubmission();

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
						// sort by submission released
						String released1 = (new Boolean(s1.getGradeReleased())).toString();
						String released2 = (new Boolean(s2.getGradeReleased())).toString();

						result = compareString(released1, released2);
					}
				}
			}
			/****** for other sort on submissions **/
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_LASTNAME))
			{
				// sorted by the submitters sort name
				User[] u1 = ((AssignmentSubmission) o1).getSubmitters();
				User[] u2 = ((AssignmentSubmission) o2).getSubmitters();

				if (u1 == null || u2 == null)
				{
					return 1;
				}
				else
				{
					String submitters1 = "";
					String submitters2 = "";

					for (int j = 0; j < u1.length; j++)
					{
						if (u1[j] != null && u1[j].getLastName() != null)
						{
							if (j > 0)
							{
								submitters1 = submitters1.concat("; ");
							}
							submitters1 = submitters1.concat("" + u1[j].getLastName());
						}
					}

					for (int j = 0; j < u2.length; j++)
					{
						if (u2[j] != null && u2[j].getLastName() != null)
						{
							if (j > 0)
							{
								submitters2 = submitters2.concat("; ");
							}
							submitters2 = submitters2.concat(u2[j].getLastName());
						}
					}
					result = compareString(submitters1, submitters2);
				}
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_SUBMIT_TIME))
			{
				// sorted by submission time
				Time t1 = ((AssignmentSubmission) o1).getTimeSubmitted();
				Time t2 = ((AssignmentSubmission) o2).getTimeSubmitted();

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
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_STATUS))
			{
				// sort by submission status
				String status1 = getSubmissionStatus(m_state, (AssignmentSubmission) o1);
				String status2 = getSubmissionStatus(m_state, (AssignmentSubmission) o2);

				result = compareString(status1, status2);
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_GRADE))
			{
				// sort by submission grade
				String grade1 = ((AssignmentSubmission) o1).getGrade();
				String grade2 = ((AssignmentSubmission) o2).getGrade();
				if (grade1 == null)
				{
					grade1 = "";
				}
				if (grade2 == null)
				{
					grade2 = "";
				}

				// if scale is points
				if ((((AssignmentSubmission) o1).getAssignment().getContent().getTypeOfGrade() == 3)
						&& ((((AssignmentSubmission) o2).getAssignment().getContent().getTypeOfGrade() == 3)))
				{
					if (grade1.equals(""))
					{
						result = -1;
					}
					else if (grade2.equals(""))
					{
						result = 1;
					}
					else
					{
						result = (new Double(grade1)).doubleValue() > (new Double(grade2)).doubleValue() ? 1 : -1;

					}
				}
				else
				{
					result = compareString(grade1, grade2);
				}
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_GRADE))
			{
				// sort by submission grade
				String grade1 = ((AssignmentSubmission) o1).getGrade();
				String grade2 = ((AssignmentSubmission) o2).getGrade();
				if (grade1 == null)
				{
					grade1 = "";
				}
				if (grade2 == null)
				{
					grade2 = "";
				}

				// if scale is points
				if ((((AssignmentSubmission) o1).getAssignment().getContent().getTypeOfGrade() == 3)
						&& ((((AssignmentSubmission) o2).getAssignment().getContent().getTypeOfGrade() == 3)))
				{
					if (grade1.equals(""))
					{
						result = -1;
					}
					else if (grade2.equals(""))
					{
						result = 1;
					}
					else
					{
						result = (new Double(grade1)).doubleValue() > (new Double(grade2)).doubleValue() ? 1 : -1;

					}
				}
				else
				{
					result = compareString(grade1, grade2);
				}
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_MAX_GRADE))
			{
				Assignment a1 = ((AssignmentSubmission) o1).getAssignment();
				Assignment a2 = ((AssignmentSubmission) o2).getAssignment();
				String maxGrade1 = maxGrade(a1.getContent().getTypeOfGrade(), a1);
				String maxGrade2 = maxGrade(a2.getContent().getTypeOfGrade(), a2);

				try
				{
					// do integer comparation inside point grade type
					int max1 = Integer.parseInt(maxGrade1);
					int max2 = Integer.parseInt(maxGrade2);
					result = (max1 < max2) ? -1 : 1;
				}
				catch (NumberFormatException e)
				{
					// otherwise do an alpha-compare
					result = maxGrade1.compareTo(maxGrade2);
				}
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_RELEASED))
			{
				// sort by submission released
				String released1 = (new Boolean(((AssignmentSubmission) o1).getGradeReleased())).toString();
				String released2 = (new Boolean(((AssignmentSubmission) o2).getGradeReleased())).toString();

				result = compareString(released1, released2);
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_ASSIGNMENT))
			{
				// sort by submission's assignment
				String title1 = ((AssignmentSubmission) o1).getAssignment().getContent().getTitle();
				String title2 = ((AssignmentSubmission) o2).getAssignment().getContent().getTitle();

				result = compareString(title1, title2);
			}

			// sort ascending or descending
			if (m_asc.equals(Boolean.FALSE.toString()))
			{
				result = -result;
			}
			return result;
		} // compare

		private int compareString(String s1, String s2) 
		{
			int result;
			if (s1 == null && s2 == null) {
				result = 0;
			} else if (s2 == null) {
				result = 1;
			} else if (s1 == null) {
				result = -1;
			} else {
				result = collator.compare(s1.toLowerCase(), s2.toLowerCase());
			}
			return result;
		}
		
		/**
		 * get the submissin status
		 */
		private String getSubmissionStatus(SessionState state, AssignmentSubmission s)
		{
			String status = "";
			if (s.getReturned())
			{
				if (s.getTimeReturned() != null && s.getTimeSubmitted() != null && s.getTimeReturned().before(s.getTimeSubmitted()))
				{
					status = rb.getString("listsub.resubmi");
				}
				else
				{
					status = rb.getString("gen.returned");
				}
			}
			else if (s.getGraded())
			{
				if (state.getAttribute(WITH_GRADES) != null && ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue())
				{
					status = rb.getString("grad3");
				}
				else
				{
					status = rb.getString("gen.commented");
				}
			}
			else
			{
				status = rb.getString("gen.ung1");
			}
			
			return status;

		} // getSubmissionStatus

		/**
		 * get the status string of assignment
		 */
		private String getAssignmentStatus(Assignment a)
		{
			String status = "";
			Time currentTime = TimeService.newTime();

			if (a.getDraft())
				status = rb.getString("draft2");
			else if (a.getOpenTime().after(currentTime))
				status = rb.getString("notope");
			else if (a.getDueTime().after(currentTime))
				status = rb.getString("ope");
			else if ((a.getCloseTime() != null) && (a.getCloseTime().before(currentTime)))
				status = rb.getString("clos");
			else
				status = rb.getString("due2");
			return status;
		} // getAssignmentStatus

		/**
		 * get submission status
		 */
		private String getSubmissionStatus(AssignmentSubmission submission, Assignment assignment)
		{
			String status = "";

			if (submission != null)
				if (submission.getSubmitted())
					if (submission.getGraded() && submission.getGradeReleased())
						status = rb.getString("grad3");
					else if (submission.getReturned())
						status = rb.getString("return") + " " + submission.getTimeReturned().toStringLocalFull();
					else
					{
						status = rb.getString("submitt") + submission.getTimeSubmitted().toStringLocalFull();
						if (submission.getTimeSubmitted().after(assignment.getDueTime())) status = status + rb.getString("late");
					}
				else
					status = rb.getString("inpro");
			else
				status = rb.getString("notsta");

			return status;

		} // getSubmissionStatus

		/**
		 * get assignment maximun grade available based on the assignment grade type
		 *
		 * @param gradeType
		 *        The int value of grade type
		 * @param a
		 *        The assignment object
		 * @return The max grade String
		 */
		private String maxGrade(int gradeType, Assignment a)
		{
			String maxGrade = "";

			if (gradeType == -1)
			{
				// Grade type not set
				maxGrade = rb.getString("granotset");
			}
			else if (gradeType == 1)
			{
				// Ungraded grade type
				maxGrade = rb.getString("nogra");
			}
			else if (gradeType == 2)
			{
				// Letter grade type
				maxGrade = "A";
			}
			else if (gradeType == 3)
			{
				// Score based grade type
				maxGrade = Integer.toString(a.getContent().getMaxGradePoint());
			}
			else if (gradeType == 4)
			{
				// Pass/fail grade type
				maxGrade = rb.getString("pass2");
			}
			else if (gradeType == 5)
			{
				// Grade type that only requires a check
				maxGrade = rb.getString("check2");
			}

			return maxGrade;

		} // maxGrade

	} // DiscussionComparator

	/**
	 * Fire up the permissions editor
	 */
	public void doPermissions(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		if (!alertGlobalNavigation(state, data))
		{
			// we are changing the view, so start with first page again.
			resetPaging(state);

			// clear search form
			doSearch_clear(data, null);

			if (SiteService.allowUpdateSite((String) state.getAttribute(STATE_CONTEXT_STRING)))
			{
				// get into helper mode with this helper tool
				startHelper(data.getRequest(), "sakai.permissions.helper");

				String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
				String siteRef = SiteService.siteReference(contextString);

				// setup for editing the permissions of the site for this tool, using the roles of this site, too
				state.setAttribute(PermissionsHelper.TARGET_REF, siteRef);

				// ... with this description
				state.setAttribute(PermissionsHelper.DESCRIPTION, rb.getString("setperfor") + " "
						+ SiteService.getSiteDisplay(contextString));

				// ... showing only locks that are prpefixed with this
				state.setAttribute(PermissionsHelper.PREFIX, "asn.");

				// disable auto-updates while leaving the list view
				justDelivered(state);
			}

			// reset the global navigaion alert flag
			if (state.getAttribute(ALERT_GLOBAL_NAVIGATION) != null)
			{
				state.removeAttribute(ALERT_GLOBAL_NAVIGATION);
			}

			// switching back to assignment list view
			state.setAttribute(STATE_SELECTED_VIEW, MODE_LIST_ASSIGNMENTS);
			doList_assignments(data);
		}

	} // doPermissions

	/**
	 * transforms the Iterator to Vector
	 */
	private Vector iterator_to_vector(Iterator l)
	{
		Vector v = new Vector();
		while (l.hasNext())
		{
			v.add(l.next());
		}
		return v;
	} // iterator_to_vector

	/**
	 * Implement this to return alist of all the resources that there are to page. Sort them as appropriate.
	 */
	protected List readResourcesPage(SessionState state, int first, int last)
	{

		List returnResources = (List) state.getAttribute(STATE_PAGEING_TOTAL_ITEMS);

		PagingPosition page = new PagingPosition(first, last);
		page.validate(returnResources.size());
		returnResources = returnResources.subList(page.getFirst() - 1, page.getLast());

		return returnResources;

	} // readAllResources

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sakaiproject.cheftool.PagedResourceActionII#sizeResources(org.sakaiproject.service.framework.session.SessionState)
	 */
	protected int sizeResources(SessionState state)
	{
		String mode = (String) state.getAttribute(STATE_MODE);
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		// all the resources for paging
		List returnResources = new Vector();

		boolean allowAddAssignment = AssignmentService.allowAddAssignment(contextString);
		if (mode.equalsIgnoreCase(MODE_LIST_ASSIGNMENTS))
		{
			String view = "";
			if (state.getAttribute(STATE_SELECTED_VIEW) != null)
			{
				view = (String) state.getAttribute(STATE_SELECTED_VIEW);
			}

			if (allowAddAssignment && view.equals(MODE_LIST_ASSIGNMENTS))
			{
				// read all Assignments
				returnResources = AssignmentService.getListAssignmentsForContext((String) state
						.getAttribute(STATE_CONTEXT_STRING));
			}
			else if (allowAddAssignment && view.equals(MODE_STUDENT_VIEW)
					|| !allowAddAssignment)
			{
				// in the student list view of assignments
				Iterator assignments = AssignmentService
						.getAssignmentsForContext(contextString);
				Time currentTime = TimeService.newTime();
				while (assignments.hasNext())
				{
					Assignment a = (Assignment) assignments.next();
					try
					{
						String deleted = a.getProperties().getProperty(ResourceProperties.PROP_ASSIGNMENT_DELETED);
						if (deleted == null || deleted.equals(""))
						{
							// show not deleted assignments
							Time openTime = a.getOpenTime();
							if (openTime != null && currentTime.after(openTime) && !a.getDraft())
							{
								returnResources.add(a);
							}
						}
						else if (deleted.equalsIgnoreCase(Boolean.TRUE.toString()) && AssignmentService.getSubmission(a.getReference(), (User) state
								.getAttribute(STATE_USER)) != null)
						{
							// and those deleted assignments but the user has made submissions to them
							returnResources.add(a);
						}
					}
					catch (IdUnusedException e)
					{
						addAlert(state, rb.getString("cannotfin3"));
					}
					catch (PermissionException e)
					{
						addAlert(state, rb.getString("youarenot14"));
					}
				}
			}
		}
		else if (mode.equalsIgnoreCase(MODE_INSTRUCTOR_REPORT_SUBMISSIONS))
		{
			Vector submissions = new Vector();

			Vector assignments = iterator_to_vector(AssignmentService.getAssignmentsForContext((String) state
					.getAttribute(STATE_CONTEXT_STRING)));
			if (assignments.size() > 0)
			{
				// users = AssignmentService.allowAddSubmissionUsers (((Assignment)assignments.get(0)).getReference ());
			}

			for (int j = 0; j < assignments.size(); j++)
			{
				Assignment a = (Assignment) assignments.get(j);
				String deleted = a.getProperties().getProperty(ResourceProperties.PROP_ASSIGNMENT_DELETED);
				if ((deleted == null || deleted.equals("")) && (!a.getDraft()))
				{
					try
					{
						List assignmentSubmissions = AssignmentService.getSubmissions(a);
						for (int k = 0; k < assignmentSubmissions.size(); k++)
						{
							AssignmentSubmission s = (AssignmentSubmission) assignmentSubmissions.get(k);
							if (s != null && (s.getSubmitted() || (s.getReturned() && (s.getTimeLastModified().before(s
												.getTimeReturned())))))
							{
								// has been subitted or has been returned and not work on it yet
								submissions.add(s);
							} // if-else
						}
					}
					catch (Exception e)
					{
					}
				}
			}

			returnResources = submissions;
		}
		else if (mode.equalsIgnoreCase(MODE_INSTRUCTOR_GRADE_ASSIGNMENT))
		{
			try
			{
				Assignment a = AssignmentService.getAssignment((String) state.getAttribute(EXPORT_ASSIGNMENT_REF));
				Iterator submissionsIterator = AssignmentService.getSubmissions(a).iterator();
				List submissions = new Vector();
				while (submissionsIterator.hasNext())
				{
					submissions.add(submissionsIterator.next());
				}

				//	get all active site users
				String authzGroupId = SiteService.siteReference(contextString);

				// all users that can submit
				List allowAddSubmissionUsers = AssignmentService.allowAddSubmissionUsers((String) state.getAttribute(EXPORT_ASSIGNMENT_REF));

				try
				{
					AuthzGroup group = AuthzGroupService.getAuthzGroup(authzGroupId);
					Set grants = group.getUsers();
					for (Iterator iUserIds = grants.iterator(); iUserIds.hasNext();)
					{
						String userId = (String) iUserIds.next();
						try
						{
							User u = UserDirectoryService.getUser(userId);
							// only include those users that can submit to this assignment
							if (u != null && allowAddSubmissionUsers.contains(u))
							{
								boolean found = false;
								for (int i = 0; !found && i<submissions.size();i++)
								{
									AssignmentSubmission s = (AssignmentSubmission) submissions.get(i);
									if (s.getSubmitterIds().contains(userId))
									{
										returnResources.add(new UserSubmission(u, s));
										found = true;
									}
								}

								// add those users who haven't made any submissions
								if (!found)
								{
									// construct fake submissions for grading purpose
									
									AssignmentSubmissionEdit s = AssignmentService.addSubmission(contextString, a.getId());
									s.removeSubmitter(UserDirectoryService.getCurrentUser());
									s.addSubmitter(u);
									s.setSubmitted(true);
									s.setAssignment(a);
									AssignmentService.commitEdit(s);
									
									// update the UserSubmission list by adding newly created Submission object
									AssignmentSubmission sub = AssignmentService.getSubmission(s.getReference());
									returnResources.add(new UserSubmission(u, sub));
								}
							}
						}
						catch (Exception e)
						{
							Log.warn("chef", this + e.toString() + " here userId = " + userId);
						}
					}
				}
				catch (Exception e)
				{
					Log.warn("chef", e.getMessage() + " authGroupId=" + authzGroupId);
				}

			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot14"));
			}

		}

		// sort them all
		String ascending = "true";
		String sort = "";
		ascending = (String) state.getAttribute(SORTED_ASC);
		sort = (String) state.getAttribute(SORTED_BY);
		if (mode.equalsIgnoreCase(MODE_INSTRUCTOR_GRADE_ASSIGNMENT) && !sort.startsWith("sorted_grade_submission_by"))
		{
			ascending = (String) state.getAttribute(SORTED_GRADE_SUBMISSION_ASC);
			sort = (String) state.getAttribute(SORTED_GRADE_SUBMISSION_BY);
		}
		else if (mode.equalsIgnoreCase(MODE_INSTRUCTOR_REPORT_SUBMISSIONS) && sort.startsWith("sorted_submission_by"))
		{
			ascending = (String) state.getAttribute(SORTED_SUBMISSION_ASC);
			sort = (String) state.getAttribute(SORTED_SUBMISSION_BY);
		}
		else
		{
			ascending = (String) state.getAttribute(SORTED_ASC);
			sort = (String) state.getAttribute(SORTED_BY);
		}
		
		if ((returnResources.size() > 1) && !mode.equalsIgnoreCase(MODE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT))
		{
			Collections.sort(returnResources, new AssignmentComparator(state, sort, ascending));
		}

		// record the total item number
		state.setAttribute(STATE_PAGEING_TOTAL_ITEMS, returnResources);
		
		return returnResources.size();
	}

	public void doView(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		if (!alertGlobalNavigation(state, data))
		{
			// we are changing the view, so start with first page again.
			resetPaging(state);

			// clear search form
			doSearch_clear(data, null);

			String viewMode = data.getParameters().getString("view");
			state.setAttribute(STATE_SELECTED_VIEW, viewMode);

			if (viewMode.equals(MODE_LIST_ASSIGNMENTS))
			{
				doList_assignments(data);
			}
			else if (viewMode.equals(MODE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT))
			{
				doView_students_assignment(data);
			}
			else if (viewMode.equals(MODE_INSTRUCTOR_REPORT_SUBMISSIONS))
			{
				doReport_submissions(data);
			}
			else if (viewMode.equals(MODE_STUDENT_VIEW))
			{
				doView_student(data);
			}

			// reset the global navigaion alert flag
			if (state.getAttribute(ALERT_GLOBAL_NAVIGATION) != null)
			{
				state.removeAttribute(ALERT_GLOBAL_NAVIGATION);
			}
		}

	} // doView

	/**
	 * put those variables related to 2ndToolbar into context
	 */
	private void add2ndToolbarFields(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		context.put("totalPageNumber", new Integer(totalPageNumber(state)));
		context.put("form_search", FORM_SEARCH);
		context.put("formPageNumber", FORM_PAGE_NUMBER);
		context.put("prev_page_exists", state.getAttribute(STATE_PREV_PAGE_EXISTS));
		context.put("next_page_exists", state.getAttribute(STATE_NEXT_PAGE_EXISTS));
		context.put("current_page", state.getAttribute(STATE_CURRENT_PAGE));
		context.put("selectedView", state.getAttribute(STATE_MODE));

	} // add2ndToolbarFields

	/**
	 * valid grade?
	 */
	private void validPointGrade(SessionState state, String grade)
	{
		if (grade != null && !grade.equals(""))
		{
			if (grade.startsWith("-"))
			{
				// check for negative sign
				addAlert(state, rb.getString("plesuse3"));
			}
			else
			{
				int index = grade.indexOf(".");
				if (index != -1)
				{
					// when there is decimal points inside the grade, scale the number by 10
					// but only one decimal place is supported
					// for example, change 100.0 to 1000
					if (!grade.equals("."))
					{
						if (grade.length() > index + 2)
						{
							// if there are more than one decimal point
							addAlert(state, rb.getString("plesuse2"));
						}
						else
						{
							// decimal points is the only allowed character inside grade
							// replace it with '1', and try to parse the new String into int
							String gradeString = (grade.endsWith(".")) ? grade.substring(0, index).concat("0") : grade.substring(0,
									index).concat(grade.substring(index + 1));
							try
							{
								Integer.parseInt(gradeString);
							}
							catch (NumberFormatException e)
							{
								alertInvalidPoint(state, gradeString);
							}
						}
					}
					else
					{
						// grade is "."
						addAlert(state, rb.getString("plesuse1"));
					}
				}
				else
				{
					// There is no decimal point; should be int number
					String gradeString = grade + "0";
					try
					{
						Integer.parseInt(gradeString);
					}
					catch (NumberFormatException e)
					{
						alertInvalidPoint(state, gradeString);
					}
				}
			}
		}

	} // validPointGrade

	private void alertInvalidPoint(SessionState state, String grade)
	{
		String VALID_CHARS_FOR_INT = "-01234567890";

		boolean invalid = false;
		// case 1: contains invalid char for int
		for (int i = 0; i < grade.length() && !invalid; i++)
		{
			char c = grade.charAt(i);
			if (VALID_CHARS_FOR_INT.indexOf(c) == -1)
			{
				invalid = true;
			}
		}
		if (invalid)
		{
			addAlert(state, rb.getString("plesuse1"));
		}
		else
		{
			int maxInt = Integer.MAX_VALUE / 10;
			int maxDec = Integer.MAX_VALUE - maxInt * 10;
			// case 2: Due to our internal scaling, input String is larger than Integer.MAX_VALUE/10
			addAlert(state, rb.getString("plesuse4") + maxInt + "." + maxDec + ".");
		}
	}

	/**
	 * display grade properly
	 */
	private String displayGrade(SessionState state, String grade)
	{
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			if (grade != null && (grade.length() >= 1))
			{
				if (grade.indexOf(".") != -1)
				{
					if (grade.startsWith("."))
					{
						grade = "0".concat(grade);
					}
					else if (grade.endsWith("."))
					{
						grade = grade.concat("0");
					}
				}
				else
				{
					try
					{
						Integer.parseInt(grade);
						grade = grade.substring(0, grade.length() - 1) + "." + grade.substring(grade.length() - 1);
					}
					catch (NumberFormatException e)
					{
						alertInvalidPoint(state, grade);
					}
				}
			}
			else
			{
				grade = "";
			}
		}
		return grade;

	} // displayGrade

	/**
	 * scale the point value by 10 if there is a valid point grade
	 */
	private String scalePointGrade(SessionState state, String point)
	{
		validPointGrade(state, point);
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			if (point != null && (point.length() >= 1))
			{
				// when there is decimal points inside the grade, scale the number by 10
				// but only one decimal place is supported
				// for example, change 100.0 to 1000
				int index = point.indexOf(".");
				if (index != -1)
				{
					if (index == 0)
					{
						// if the point is the first char, add a 0 for the integer part
						point = "0".concat(point.substring(1));
					}
					else if (index < point.length() - 1)
					{
						// use scale integer for gradePoint
						point = point.substring(0, index) + point.substring(index + 1);
					}
					else
					{
						// decimal point is the last char
						point = point.substring(0, index) + "0";
					}
				}
				else
				{
					// if there is no decimal place, scale up the integer by 10
					point = point + "0";
				}

				// filter out the "zero grade"
				if (point.equals("00"))
				{
					point = "0";
				}
			}
		}
		return point;

	} // scalePointGrade

	/**
	 * Processes formatted text that is coming back from the browser (from the formatted text editing widget).
	 *
	 * @param state
	 *        Used to pass in any user-visible alerts or errors when processing the text
	 * @param strFromBrowser
	 *        The string from the browser
	 * @param checkForFormattingErrors
	 *        Whether to check for formatted text errors - if true, look for errors in the formatted text. If false, accept the formatted text without looking for errors.
	 * @return The formatted text
	 */
	private String processFormattedTextFromBrowser(SessionState state, String strFromBrowser, boolean checkForFormattingErrors)
	{
		StringBuffer alertMsg = new StringBuffer();
		try
		{
			boolean replaceWhitespaceTags = true;
			String text = FormattedText.processFormattedText(strFromBrowser, alertMsg, checkForFormattingErrors,
					replaceWhitespaceTags);
			if (alertMsg.length() > 0) addAlert(state, alertMsg.toString());
			return text;
		}
		catch (Exception e)
		{
			Log.warn("chef", this + ": ", e);
			return strFromBrowser;
		}
	}

	/**
	 * Processes the given assignmnent feedback text, as returned from the user's browser. Makes sure that the Chef-style markup {{like this}} is properly balanced.
	 */
	private String processAssignmentFeedbackFromBrowser(SessionState state, String strFromBrowser)
	{
		if (strFromBrowser == null || strFromBrowser.length() == 0) return strFromBrowser;

		StringBuffer buf = new StringBuffer(strFromBrowser);
		int pos = -1;
		int numopentags = 0;

		while ((pos = buf.indexOf("{{")) != -1)
		{
			buf.replace(pos, pos + "{{".length(), "<ins>");
			numopentags++;
		}

		while ((pos = buf.indexOf("}}")) != -1)
		{
			buf.replace(pos, pos + "}}".length(), "</ins>");
			numopentags--;
		}

		while (numopentags > 0)
		{
			buf.append("</ins>");
			numopentags--;
		}

		boolean checkForFormattingErrors = false; // so that grading isn't held up by formatting errors
		buf = new StringBuffer(processFormattedTextFromBrowser(state, buf.toString(), checkForFormattingErrors));

		while ((pos = buf.indexOf("<ins>")) != -1)
		{
			buf.replace(pos, pos + "<ins>".length(), "{{");
		}

		while ((pos = buf.indexOf("</ins>")) != -1)
		{
			buf.replace(pos, pos + "</ins>".length(), "}}");
		}

		return buf.toString();
	}

	/**
	 * Called to deal with old Chef-style assignment feedback annotation, {{like this}}.
	 *
	 * @param value
	 *        A formatted text string that may contain {{}} style markup
	 * @return HTML ready to for display on a browser
	 */
	public static String escapeAssignmentFeedback(String value)
	{
		if (value == null || value.length() == 0) return value;

		value = fixAssignmentFeedback(value);

		StringBuffer buf = new StringBuffer(value);
		int pos = -1;

		while ((pos = buf.indexOf("{{")) != -1)
		{
			buf.replace(pos, pos + "{{".length(), "<span class='highlight'>");
		}

		while ((pos = buf.indexOf("}}")) != -1)
		{
			buf.replace(pos, pos + "}}".length(), "</span>");
		}

		return FormattedText.escapeHtmlFormattedText(buf.toString());
	}

	/**
	 * Escapes the given assignment feedback text, to be edited as formatted text (perhaps using the formatted text widget)
	 */
	public static String escapeAssignmentFeedbackTextarea(String value)
	{
		if (value == null || value.length() == 0) return value;

		value = fixAssignmentFeedback(value);

		return FormattedText.escapeHtmlFormattedTextarea(value);
	}

	/**
	 * Apply the fix to pre 1.1.05 assignments submissions feedback.
	 */
	private static String fixAssignmentFeedback(String value)
	{
		if (value == null || value.length() == 0) return value;

		StringBuffer buf = new StringBuffer(value);
		int pos = -1;

		// <br/> -> \n
		while ((pos = buf.indexOf("<br/>")) != -1)
		{
			buf.replace(pos, pos + "<br/>".length(), "\n");
		}

		// <span class='chefAlert'>( -> {{
		while ((pos = buf.indexOf("<span class='chefAlert'>(")) != -1)
		{
			buf.replace(pos, pos + "<span class='chefAlert'>(".length(), "{{");
		}

		// )</span> -> }}
		while ((pos = buf.indexOf(")</span>")) != -1)
		{
			buf.replace(pos, pos + ")</span>".length(), "}}");
		}

		while ((pos = buf.indexOf("<ins>")) != -1)
		{
			buf.replace(pos, pos + "<ins>".length(), "{{");
		}

		while ((pos = buf.indexOf("</ins>")) != -1)
		{
			buf.replace(pos, pos + "</ins>".length(), "}}");
		}

		return buf.toString();

	} // fixAssignmentFeedback

	/**
	 * Apply the fix to pre 1.1.05 assignments submissions feedback.
	 */
	public static String showPrevFeedback(String value)
	{
		if (value == null || value.length() == 0) return value;

		StringBuffer buf = new StringBuffer(value);
		int pos = -1;

		// <br/> -> \n
		while ((pos = buf.indexOf("\n")) != -1)
		{
			buf.replace(pos, pos + "\n".length(), "<br />");
		}

		return buf.toString();

	} // showPrevFeedback

	private boolean alertGlobalNavigation(SessionState state, RunData data)
	{
		String mode = (String) state.getAttribute(STATE_MODE);
		ParameterParser params = data.getParameters();

		if (mode.equals(MODE_STUDENT_VIEW_SUBMISSION) || mode.equals(MODE_STUDENT_PREVIEW_SUBMISSION)
				|| mode.equals(MODE_STUDENT_VIEW_GRADE) || mode.equals(MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT)
				|| mode.equals(MODE_INSTRUCTOR_DELETE_ASSIGNMENT) || mode.equals(MODE_INSTRUCTOR_GRADE_SUBMISSION)
				|| mode.equals(MODE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION) || mode.equals(MODE_INSTRUCTOR_PREVIEW_ASSIGNMENT)
				|| mode.equals(MODE_INSTRUCTOR_VIEW_ASSIGNMENT))
		{
			if (state.getAttribute(ALERT_GLOBAL_NAVIGATION) == null)
			{
				addAlert(state, rb.getString("alert.globalNavi"));
				state.setAttribute(ALERT_GLOBAL_NAVIGATION, Boolean.TRUE);

				if (mode.equals(MODE_STUDENT_VIEW_SUBMISSION))
				{
					// retrieve the submission text (as formatted text)
					boolean checkForFormattingErrors = true; // the student is submitting something - so check for errors
					String text = processFormattedTextFromBrowser(state, params.getCleanString(VIEW_SUBMISSION_TEXT),
							checkForFormattingErrors);

					state.setAttribute(VIEW_SUBMISSION_TEXT, text);
					if (params.getString(VIEW_SUBMISSION_HONOR_PLEDGE_YES) != null)
					{
						state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, "true");
					}
					state.setAttribute(FilePickerHelper.FILE_PICKER_TITLE_TEXT, rb.getString("gen.addatt"));

					// TODO: file picker to save in dropbox? -ggolden
					// User[] users = { UserDirectoryService.getCurrentUser() };
					// state.setAttribute(ResourcesAction.STATE_SAVE_ATTACHMENT_IN_DROPBOX, users);
				}
				else if (mode.equals(MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT))
				{
					setNewAssignmentParameters(data, false);
				}
				else if (mode.equals(MODE_INSTRUCTOR_GRADE_SUBMISSION))
				{
					readGradeForm(data, state, "read");
				}

				return true;
			}
		}

		return false;

	} // alertGlobalNavigation

	/**
	 * Dispatch function inside add submission page
	 */
	public void doRead_add_submission_form(RunData data)
	{
		String option = data.getParameters().getString("option");
		if (option.equals("cancel"))
		{
			// cancel
			doCancel_show_submission(data);
		}
		else if (option.equals("preview"))
		{
			// preview
			doPreview_submission(data);
		}
		else if (option.equals("save"))
		{
			// save draft
			doSave_submission(data);
		}
		else if (option.equals("post"))
		{
			// post
			doPost_submission(data);
		}
		else if (option.equals("revise"))
		{
			// done preview
			doDone_preview_submission(data);
		}
		else if (option.equals("attach"))
		{
			// attach
			doAttachments(data);
		}
	}
	
	/**
	 * 
	 */
	public void doSet_defaultNoSubmissionScore(RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ()); 
		ParameterParser params = data.getParameters();
		
		String grade = StringUtil.trimToNull(params.getString("defaultGrade"));
		if (grade == null)
		{
			addAlert(state, rb.getString("plespethe2"));
		}
		
		String assignmentId = (String) state.getAttribute(EXPORT_ASSIGNMENT_REF);
		try
		{
			// record the default grade setting for no-submission
			AssignmentEdit aEdit = AssignmentService.editAssignment(assignmentId); 
			aEdit.getPropertiesEdit().addProperty(GRADE_NO_SUBMISSION_DEFAULT_GRADE, grade);
			AssignmentService.commitEdit(aEdit);
			
			Assignment a = AssignmentService.getAssignment(assignmentId);
			if (a.getContent().getTypeOfGrade() == Assignment.SCORE_GRADE_TYPE)
			{
				//for point-based grades
				validPointGrade(state, grade);
				
				if (state.getAttribute(STATE_MESSAGE) == null)
				{
					int maxGrade = a.getContent().getMaxGradePoint();
					try
					{
						if (Integer.parseInt(scalePointGrade(state, grade)) > maxGrade)
						{
							if (state.getAttribute(GRADE_GREATER_THAN_MAX_ALERT) == null)
							{
								// alert user first when he enters grade bigger than max scale
								addAlert(state, rb.getString("grad2"));
								state.setAttribute(GRADE_GREATER_THAN_MAX_ALERT, Boolean.TRUE);
							}
							else
							{
								// remove the alert once user confirms he wants to give student higher grade
								state.removeAttribute(GRADE_GREATER_THAN_MAX_ALERT);
							}
						}
					}
					catch (NumberFormatException e)
					{
						alertInvalidPoint(state, grade);
					}
				}
				
				if (state.getAttribute(STATE_MESSAGE) == null)
				{
					grade = scalePointGrade(state, grade);
				}
			}
			
			
			if (grade != null && state.getAttribute(STATE_MESSAGE) == null)
			{
				// get the user list
				List userSubmissions = new Vector();
				if (state.getAttribute(USER_SUBMISSIONS) != null)
				{
					userSubmissions = (List) state.getAttribute(USER_SUBMISSIONS);
				}
				
				// constructor a new UserSubmissions list
				List userSubmissionsNew = new Vector();
				
				for (int i = 0; i<userSubmissions.size(); i++)
				{
					// get the UserSubmission object
					UserSubmission us = (UserSubmission) userSubmissions.get(i);
					
					User u = us.getUser();
					AssignmentSubmission submission = us.getSubmission();
					
					// check whether there is a submission associated
					if (submission == null)
					{
						AssignmentSubmissionEdit s = AssignmentService.addSubmission((String) state.getAttribute(STATE_CONTEXT_STRING), assignmentId);
						s.removeSubmitter(UserDirectoryService.getCurrentUser());
						s.addSubmitter(u);
						// submitted by without submit time
						s.setSubmitted(true);
						s.setGrade(grade);
						s.setGraded(true);
						s.setAssignment(a);
						AssignmentService.commitEdit(s);
						
						// update the UserSubmission list by adding newly created Submission object
						AssignmentSubmission sub = AssignmentService.getSubmission(s.getReference());
						userSubmissionsNew.add(new UserSubmission(u, sub));
					}
					else if (submission.getTimeSubmitted() == null)
					{
						// update the grades for those existing non-submissions
						AssignmentSubmissionEdit sEdit = AssignmentService.editSubmission(submission.getReference());
						sEdit.setGrade(grade);
						sEdit.setSubmitted(true);
						sEdit.setGraded(true);
						sEdit.setAssignment(a);
						AssignmentService.commitEdit(sEdit);
						
						userSubmissionsNew.add(new UserSubmission(u, AssignmentService.getSubmission(sEdit.getReference())));
					}
					else
					{
						// no change for this user
						userSubmissionsNew.add(us);
					}
				}
				
				state.setAttribute(USER_SUBMISSIONS, userSubmissionsNew);
			}
			
		}
		catch (Exception e)
		{
			Log.warn("chef", e.toString());
		
		}
		
		
	}
	
	/**
	 * 
	 * @return
	 */
	public void doUpload_all(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();
		String flow = params.getString("flow");
		if (flow.equals("upload"))
		{
			// upload
			doUpload_all_upload(data);
		}
		else if (flow.equals("cancel"))
		{
			// cancel
			doCancel_upload_all(data);
		}
	}
	
	public void doUpload_all_upload(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();
		
		String contextString = ToolManager.getCurrentPlacement().getContext();
		String toolTitle = ToolManager.getTool("sakai.assignment").getTitle();
		
		boolean hasSubmissions = false;
		boolean hasGradeFile = false;
		boolean hasComments = false;
		boolean releaseGrades = false;
		
		// check against the content elements selection
		if (params.getString("studentSubmission") != null)
		{
			// should contain student submission information
			hasSubmissions = true;
		}
		if (params.getString("gradeFile") != null)
		{
			// should contain grade file
			hasGradeFile = true;	
		}
		if (params.getString("instructorComments") != null)
		{
			// comments.xml should be available
			hasComments = true;
		}
		if (params.getString("release") != null)
		{
			// comments.xml should be available
			releaseGrades = params.getBoolean("release");
		}
		state.setAttribute(UPLOAD_ALL_HAS_SUBMISSIONS, Boolean.valueOf(hasSubmissions));
		state.setAttribute(UPLOAD_ALL_HAS_GRADEFILE, Boolean.valueOf(hasGradeFile));
		state.setAttribute(UPLOAD_ALL_HAS_COMMENTS, Boolean.valueOf(hasComments));
		state.setAttribute(UPLOAD_ALL_RELEASE_GRADES, Boolean.valueOf(releaseGrades));
		
		if (!hasSubmissions && !hasGradeFile && !hasComments)
		{
			// has to choose one upload feature
			addAlert(state, rb.getString("uploadall.alert.choose.element"));
		}
		else
		{
			// constructor the hashtable for all submission objects
			Hashtable submissionTable = new Hashtable();
			Assignment assignment = null;
			try
			{
				assignment = AssignmentService.getAssignment((String) state.getAttribute(EXPORT_ASSIGNMENT_REF));
				
				Iterator sIterator = AssignmentService.getSubmissions(assignment).iterator();
				while (sIterator.hasNext())
				{
					AssignmentSubmission s = (AssignmentSubmission) sIterator.next();
					User[] users = s.getSubmitters();
					String uName = users[0].getSortName();
					submissionTable.put(uName, new UploadGradeWrapper("", "", "", new Vector()));
				}
			}
			catch (Exception e)
			{
				Log.warn("chef", e.toString());
			}
			
			// see if the user uploaded a file
		    FileItem fileFromUpload = null;
		    String fileName = null;
		    fileFromUpload = params.getFileItem("file");
			    
		    String max_file_size_mb = ServerConfigurationService.getString("content.upload.max", "1");
		    int max_bytes = 1024 * 1024;
		    try
		    {
		    		max_bytes = Integer.parseInt(max_file_size_mb) * 1024 * 1024;
		    	}
			catch(Exception e)
			{
				// if unable to parse an integer from the value
				// in the properties file, use 1 MB as a default
				max_file_size_mb = "1";
				max_bytes = 1024 * 1024;
			}
			
			if(fileFromUpload == null)
			{
				// "The user submitted a file to upload but it was too big!"
				addAlert(state, rb.getString("uploadall.size") + " " + max_file_size_mb + "MB " + rb.getString("uploadall.exceeded"));
			}
			else if (fileFromUpload.getFileName() == null || fileFromUpload.getFileName().length() == 0)
			{
				// no file
				addAlert(state, rb.getString("uploadall.alert.zipFile"));
			}
			else
			{
				byte[] fileData = fileFromUpload.get();
				    
				if(fileData.length >= max_bytes)
				{
					addAlert(state, rb.getString("uploadall.size") + " " + max_file_size_mb + "MB " + rb.getString("uploadall.exceeded"));
				}
				else if(fileData.length > 0)
				{	
					ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(fileData));
					ZipEntry entry;
					
					try
					{
						while ((entry=zin.getNextEntry()) != null)
						{
							if (!entry.isDirectory())
							{
								String entryName = entry.getName();
								if (entryName.endsWith("grades.csv"))
								{
									if (hasGradeFile)
									{
										// read grades.cvs from zip
								        String result = StringUtil.trimToZero(readIntoString(zin));
								        String[] lines=null;
								        if (result.indexOf("\r") != -1)
								        		lines = result.split("\r");
								        else if (result.indexOf("\n") != -1)
							        			lines = result.split("\n");
								        for (int i = 3; i<lines.length; i++)
								        {
								        		// escape the first three header lines
								        		String[] items = lines[i].split(",");
								        		if (items.length > 3)
								        		{
								        			// has grade information
									        		try
									        		{
									        			User u = UserDirectoryService.getUserByEid(items[0]/*user id*/);
									        			UploadGradeWrapper w = (UploadGradeWrapper) submissionTable.get(u.getSortName());
									        			if (w != null)
									        			{
									        				w.setGrade(assignment.getContent().getTypeOfGrade() == Assignment.SCORE_GRADE_TYPE?scalePointGrade(state, items[3]):items[3]);
									        				submissionTable.put(u.getSortName(), w);
									        			}
									        		}
									        		catch (Exception e )
									        		{
									        			Log.warn("chef", e.toString());
									        		}
								        		}
								        }
									}
								}
								else 
								{
									// get user sort name
									String userName = "";
									if (entryName.indexOf("/") != -1)
									{
										userName = entryName.substring(0, entryName.lastIndexOf("/"));
										if (userName.indexOf("/") != -1)
										{
											userName = userName.substring(userName.lastIndexOf("/")+1, userName.length());
										}
									}
									if (hasComments && entryName.endsWith("comments.txt"))
									{
										// read the comments file
								        String comment = StringUtil.trimToNull(readIntoString(zin));
								        if (submissionTable.containsKey(userName) && comment != null)
								        {
								        		UploadGradeWrapper r = (UploadGradeWrapper) submissionTable.get(userName);
								        		r.setComment(comment);
								        		submissionTable.put(userName, r);
								        }
									}
									else if (hasSubmissions)
									{
										if (entryName.endsWith("_submissionText.html"))
										{
											// upload the student submission text along with the feedback text
											String text = StringUtil.trimToNull(readIntoString(zin));
											if (submissionTable.containsKey(userName) && text != null)
									        {
									        		UploadGradeWrapper r = (UploadGradeWrapper) submissionTable.get(userName);
									        		r.setText(text);
									        		submissionTable.put(userName, r);
									        }
										}
										else
										{
											// upload all the files as instuctor attachments to the submission for grading purpose
											String fName = entryName.substring(entryName.lastIndexOf("/") + 1, entryName.length());
											ContentTypeImageService iService = (ContentTypeImageService) state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE);
											try
											{
												if (submissionTable.containsKey(userName))
										        {
													// add the file as attachment
													ResourceProperties properties = ContentHostingService.newResourceProperties();
													properties.addProperty(ResourceProperties.PROP_DISPLAY_NAME, fName);
													ContentResource attachment = ContentHostingService.addAttachmentResource(
																				fName, 
																				contextString, 
																				toolTitle, 
																				iService.getContentType(fName.substring(fName.lastIndexOf(".") + 1)),
																				readIntoString(zin).getBytes(), 
																				properties);
										        		UploadGradeWrapper r = (UploadGradeWrapper) submissionTable.get(userName);
										        		List attachments = r.getAttachments();
										        		attachments.add(EntityManager.newReference(attachment.getReference()));
										        		r.setAttachments(attachments);
										        }
											}
											catch (Exception ee)
											{
												Log.warn("chef", ee.toString());
											}
										}
									}
								}
							}
						}
					}
					catch (IOException e) 
					{
						// uploaded file is not a valid archive
						addAlert(state, rb.getString("uploadall.alert.zipFile"));
						
					}
				}
			}
			
			if (state.getAttribute(STATE_MESSAGE) == null)
			{
				// update related submissions
				if (assignment != null)
				{
					Iterator sIterator = AssignmentService.getSubmissions(assignment).iterator();
					while (sIterator.hasNext())
					{
						AssignmentSubmission s = (AssignmentSubmission) sIterator.next();
						User[] users = s.getSubmitters();
						String uName = users[0].getSortName();
						if (submissionTable.containsKey(uName))
						{
							// update the AssignmetnSubmission record
							try
							{
								AssignmentSubmissionEdit sEdit = AssignmentService.editSubmission(s.getReference());
								
								UploadGradeWrapper w = (UploadGradeWrapper) submissionTable.get(uName);
								// add all attachment
								if (hasSubmissions)
								{
									
									sEdit.clearFeedbackAttachments();
									for (Iterator attachments = w.getAttachments().iterator(); attachments.hasNext();)
									{
										sEdit.addFeedbackAttachment((Reference) attachments.next());
									}
									sEdit.setFeedbackText(w.getText());
								}
								
								if (hasComments)
								{
									// add comment
									sEdit.setFeedbackComment(w.getComment());
								}
								
								if (hasGradeFile)
								{
									// set grade
									sEdit.setGrade(w.getGrade());
									sEdit.setGraded(true);
								}
								
								// release or not
								sEdit.setGradeReleased(releaseGrades);
								sEdit.setReturned(releaseGrades);
								
								// commit
								AssignmentService.commitEdit(sEdit);
							}
							catch (Exception ee)
							{
								Log.debug("chef", ee.toString());
							}
						}	
					}
				}
			}
		}
		
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			// go back to the list of submissions view
			cleanUploadAllContext(state);
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);
		}
	}

	private String readIntoString(ZipInputStream zin) throws IOException {
		byte[] buf = new byte[1024];
		int len;
		StringBuffer b = new StringBuffer();
		while ((len = zin.read(buf)) > 0) {
		    b.append(new String(buf));
		}
		return b.toString();
	}
	
	/**
	 * 
	 * @return
	 */
	public void doCancel_upload_all(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);
		cleanUploadAllContext(state);
	}
	
	/**
	 * clean the state variabled used by upload all process
	 */
	private void cleanUploadAllContext(SessionState state)
	{
		state.removeAttribute(UPLOAD_ALL_HAS_SUBMISSIONS);
		state.removeAttribute(UPLOAD_ALL_HAS_GRADEFILE);
		state.removeAttribute(UPLOAD_ALL_HAS_COMMENTS);
		state.removeAttribute(UPLOAD_ALL_RELEASE_GRADES);
		
	}
	

	/**
	 * Action is to preparing to go to the upload files
	 */
	public void doPrep_upload_all(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_UPLOAD_ALL);

	} // doPrep_upload_all
	
	/**
	 * the UploadGradeWrapper class to be used for the "upload all" feature
	 */
	public class UploadGradeWrapper
	{
		/**
		 * the grade 
		 */
		String m_grade = null;
		
		/**
		 * the text
		 */
		String m_text = null;
		
		/**
		 * the comment
		 */
		String m_comment = "";
		
		/**
		 * the attachment list
		 */
		List m_attachments = EntityManager.newReferenceList();

		public UploadGradeWrapper(String grade, String text, String comment, List attachments)
		{
			m_grade = grade;
			m_text = text;
			m_comment = comment;
			m_attachments = attachments;
		}

		/**
		 * Returns grade string
		 */
		public String getGrade()
		{
			return m_grade;
		}
		
		/**
		 * Returns the text
		 */
		public String getText()
		{
			return m_text;
		}

		/**
		 * Returns the comment string
		 */
		public String getComment()
		{
			return m_comment;
		}
		
		/**
		 * Returns the attachment list
		 */
		public List getAttachments()
		{
			return m_attachments;
		}
		
		/**
		 * set the grade string
		 */
		public void setGrade(String grade)
		{
			m_grade = grade;
		}
		
		/**
		 * set the text
		 */
		public void setText(String text)
		{
			m_text = text;
		}
		
		/**
		 * set the comment string
		 */
		public void setComment(String comment)
		{
			m_comment = comment;
		}
		
		/**
		 * set the attachment list
		 */
		public void setAttachments(List attachments)
		{
			m_attachments = attachments;
		}
	}
	
	private List<DecoratedTaggingProvider> initDecoratedProviders() {
		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.TaggingManager");
		List<DecoratedTaggingProvider> providers = new ArrayList<DecoratedTaggingProvider>();
		for (TaggingProvider provider : taggingManager.getProviders())
		{
			providers.add(new DecoratedTaggingProvider(provider));
		}
		return providers;
	}
	
	private List<DecoratedTaggingProvider> addProviders(Context context, SessionState state)
	{
		String mode = (String) state.getAttribute(STATE_MODE);
		List<DecoratedTaggingProvider> providers = (List) state
				.getAttribute(mode + PROVIDER_LIST);
		if (providers == null)
		{
			providers = initDecoratedProviders();
			state.setAttribute(mode + PROVIDER_LIST, providers);
		}
		context.put("providers", providers);
		return providers;
	}
	
	private void addActivity(Context context, Assignment assignment)
	{
		AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");
		context.put("activity", assignmentActivityProducer
				.getActivity(assignment));
	}
	
	private void addItem(Context context, AssignmentSubmission submission, String userId)
	{
		AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");
		context.put("item", assignmentActivityProducer
				.getItem(submission, userId));
	}
	
	private ContentReviewService contentReviewService;
	public String getReportURL(Long score) {
		if (contentReviewService == null)
		{
			contentReviewService = (ContentReviewService) ComponentManager.get(ContentReviewService.class.getName());
		}
		return contentReviewService.getIconUrlforScore(score);
	}

}
