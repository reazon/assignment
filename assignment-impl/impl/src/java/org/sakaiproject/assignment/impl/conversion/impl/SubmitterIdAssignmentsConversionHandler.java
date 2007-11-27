/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/assignment/branches/post-2-4/assignment-impl/impl/src/java/org/sakaiproject/assignment/impl/conversion/impl/SubmitterIdAssignmentsConversionHandler.java $
 * $Id: SubmitterIdAssignmentsConversionHandler.java 36811 2007-10-13 00:52:59Z jimeng@umich.edu $
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

package org.sakaiproject.assignment.impl.conversion.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.impl.conversion.api.SchemaConversionHandler;

/**
 * Performs just the file size conversion for quota calculations
 * 
 * @author ieb
 */
public class SubmitterIdAssignmentsConversionHandler implements SchemaConversionHandler
{

	private static final Log log = LogFactory
			.getLog(SubmitterIdAssignmentsConversionHandler.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.content.impl.serialize.impl.SchemaConversionHandler#getSource(java.lang.String,
	 *      java.sql.ResultSet)
	 */
	public Object getSource(String id, ResultSet rs) throws SQLException
	{
		return rs.getString(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.content.impl.serialize.impl.SchemaConversionHandler#convertSource(java.lang.String,
	 *      java.lang.Object, java.sql.PreparedStatement)
	 */
	public boolean convertSource(String id, Object source, PreparedStatement updateRecord)
			throws SQLException
	{

		String xml = (String) source;

		AssignmentSubmissionAccess sax = new AssignmentSubmissionAccess();
		try
		{
			sax.parse(xml);
		}
		catch (Exception e1)
		{
			log.warn("Failed to parse " + id + "[" + xml + "]", e1);
			return false;
		}

		try
		{
			List<String> submitters = sax.getSubmitters();
			if(submitters == null || submitters.isEmpty())
			{
				updateRecord.setString(1, null);
			}
			else
			{
				updateRecord.setString(1, submitters.get(0));
			}
			String dateSubmitted = sax.getDatesubmitted();
			if(dateSubmitted == null || dateSubmitted.trim().equals(""))
			{
				updateRecord.setString(2, null);
			}
			else
			{
				updateRecord.setString(2, dateSubmitted);
			}
			updateRecord.setString(3, sax.getSubmitted());
			updateRecord.setString(4, sax.getGraded());
			updateRecord.setString(5, id);
			return true;
		}
		catch (Exception e)
		{
			log.warn("Failed to process record " + id, e);
		}
		return false;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.content.impl.serialize.impl.conversion.SchemaConversionHandler#validate(java.lang.String,
	 *      java.lang.Object, java.lang.Object)
	 */
	public void validate(String id, Object source, Object result) throws Exception
	{
		// this conversion did not modify source data.
	}
	/* (non-Javadoc)
	 * @see org.sakaiproject.content.impl.serialize.impl.conversion.SchemaConversionHandler#getValidateSource(java.lang.String, java.sql.ResultSet)
	 */
	public Object getValidateSource(String id, ResultSet rs) throws SQLException
	{
		return rs.getString(1);
	}


}
