/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/entity/trunk/entity-api/api/src/java/org/sakaiproject/entity/api/serialize/DataStreamEntitySerializer.java $
 * $Id: DataStreamEntitySerializer.java 34790 2007-09-07 22:57:54Z ian@caret.cam.ac.uk $
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

import java.io.DataInputStream;
import java.io.DataOutputStream;


/**
 * @author ieb
 *
 */
public interface DataStreamEntitySerializer
{

	/**
	 * @param se
	 * @param ds
	 * @throws EntityParseException
	 */
	void parse(SerializableEntity se, DataInputStream ds) throws EntityParseException;

	/**
	 * @param se
	 * @param ds
	 * @throws EntityParseException
	 */
	void serialize(SerializableEntity se, DataOutputStream ds) throws EntityParseException;

}
