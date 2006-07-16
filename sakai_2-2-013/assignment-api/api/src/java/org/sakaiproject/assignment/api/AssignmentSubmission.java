/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
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

package org.sakaiproject.assignment.api;

import java.util.List;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.user.api.User;

/**
 * <p>
 * AssignmentSubmission is the an interface for the Sakai assignments module. It represents student submissions for assignments.
 * </p>
 */
public interface AssignmentSubmission extends Entity
{
	/**
	 * Access the context at the time of creation.
	 * 
	 * @return String - the context string.
	 */
	public String getContext();

	/**
	 * Access the Assignment for this Submission
	 * 
	 * @return the Assignment
	 */
	public Assignment getAssignment();

	/**
	 * Access the ID for the Assignment for this Submission
	 * 
	 * @return String - the Assignment id
	 */
	public String getAssignmentId();

	/**
	 * Access the list of Users who submitted this response to the Assignment.
	 * 
	 * @return Array of user objects.
	 */
	public User[] getSubmitters();

	/**
	 * Access the list of Users who submitted this response to the Assignment.
	 * 
	 * @return List of user ids
	 */
	public List getSubmitterIds();

	/**
	 * Get whether this is a final submission.
	 * 
	 * @return True if a final submission, false if still a draft.
	 */
	public boolean getSubmitted();

	/**
	 * Set the time at which this response was submitted; null signifies the response is unsubmitted.
	 * 
	 * @return Time of submission.
	 */
	public Time getTimeSubmitted();

	/**
	 * Text submitted in response to the Assignment.
	 * 
	 * @return The text of the submission.
	 */
	public String getSubmittedText();

	/**
	 * Access the list of attachments to this response to the Assignment.
	 * 
	 * @return List of the list of attachments as Reference objects;
	 */
	public List getSubmittedAttachments();

	/**
	 * Get the general comments by the grader
	 * 
	 * @return The text of the grader's comments; may be null.
	 */
	public String getFeedbackComment();

	/**
	 * Access the text part of the instructors feedback; usually an annotated copy of the submittedText
	 * 
	 * @return The text of the grader's feedback.
	 */
	public String getFeedbackText();

	/**
	 * Access the list of attachments returned to the students in the process of grading this assignment; usually a modified or annotated version of the attachment submitted.
	 * 
	 * @return List of the Resource objects pointing to the attachments.
	 */
	public List getFeedbackAttachments();

	/**
	 * Get whether this Submission was rejected by the grader.
	 * 
	 * @return True if this response was rejected by the grader, false otherwise.
	 */
	public boolean getReturned();

	/**
	 * Get whether this Submission has been graded.
	 * 
	 * @return True if the submission has been graded, false otherwise.
	 */
	public boolean getGraded();

	/**
	 * Get whether the grade has been released.
	 * 
	 * @return True if the Submissions's grade has been released, false otherwise.
	 */
	public boolean getGradeReleased();

	/**
	 * Access the grade recieved.
	 * 
	 * @return The Submission's grade..
	 */
	public String getGrade();

	/**
	 * Access the grade recieved. When points-type, format it to one decimal place
	 * 
	 * @return The Submission's grade..
	 */
	public String getGradeDisplay();

	/**
	 * Get the time of last modification;
	 * 
	 * @return The time of last modification.
	 */
	public Time getTimeLastModified();

	/**
	 * Get the time at which the graded submission was returned; null means the response is not yet graded.
	 * 
	 * @return the time (may be null)
	 */
	public Time getTimeReturned();

	/**
	 * Access the checked status of the honor pledge flag.
	 * 
	 * @return True if the honor pledge is checked, false otherwise.
	 */
	public boolean getHonorPledgeFlag();

	/**
	 * Returns the status of the submission : Not Started, submitted, returned or graded.
	 * 
	 * @return The Submission's status.
	 */
	public String getStatus();
}
