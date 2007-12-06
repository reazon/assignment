/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/entity/trunk/entity-api/api/src/java/org/sakaiproject/entity/api/serialize/EntityParseException.java $
 * $Id: EntityParseException.java 34790 2007-09-07 22:57:54Z ian@caret.cam.ac.uk $
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

package org.sakaiproject.entity.api.serialize;

/**
 * Thrown when there is an issue with Parsing an Entity from storage or saving the entity to storage
 * @author ieb
 *
 */
public class EntityParseException extends Exception
{

	/**
	 * 
	 */
	public EntityParseException()
	{
	}

	/**
	 * @param arg0
	 */
	public EntityParseException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public EntityParseException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public EntityParseException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}