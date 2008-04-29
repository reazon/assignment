/**********************************************************************************
 * $URL:  $
 * $Id:  $
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

package org.sakaiproject.assignment.model;

import org.sakaiproject.assignment.model.AssignmentSubmissionVersion;
import org.sakaiproject.assignment.model.SubmissionAttachmentBase;

/**
 * the attachment used for feedback purpose
 * @author zqian
 *
 */
public class FeedbackAttachment extends SubmissionAttachmentBase {
	
	public FeedbackAttachment() {
		
	}
	
	public FeedbackAttachment(AssignmentSubmissionVersion submissionVersion, String attachmentReference) {
		this.submissionVersion = submissionVersion;
		this.attachmentReference = attachmentReference;
	}

}