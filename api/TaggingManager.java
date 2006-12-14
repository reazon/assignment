/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
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

package org.sakaiproject.assignment.taggable.api;

import java.util.List;

/**
 * The TaggingManager provides access to available
 * {@link TaggableActivityProducer TaggableActivityProducers} and
 * {@link TaggingProvider TaggingProviders} and the various activities and tags
 * they provide.
 * 
 * @author The Sakai Foundation.
 */
public interface TaggingManager {

	/**
	 * Method to get a taggable activity producer by unique service type.
	 * 
	 * @param type
	 *            A unique service type identifier.
	 * @return A taggable activity producer.
	 */
	public TaggableActivityProducer findProducerByType(String type);

	/**
	 * Method to get the taggable activity producer that handles this reference.
	 * 
	 * @param ref
	 *            A reference to an object provided by a taggable activity
	 *            producer.
	 * @return A taggable activity producer.
	 */
	public TaggableActivityProducer findProducerByRef(String ref);

	/**
	 * Method to get a tagging provider by unique service type.
	 * 
	 * @param type
	 *            A unique service type identifier.
	 * @return A tagging provider.
	 */
	public TaggingProvider findProviderByType(String type);

	/**
	 * Method for a taggable activity producer to register itself with the
	 * tagging manager.
	 * 
	 * @param producer
	 *            A taggable activity producer.
	 */
	public void registerProducer(TaggableActivityProducer producer);

	/**
	 * Method for a tagging provider to register itself with this tagging
	 * manager.
	 * 
	 * @param provider
	 *            A tagging provider.
	 */
	public void registerProvider(TaggingProvider provider);

	/**
	 * Method to get the context of the object represented by this reference.
	 * 
	 * @param ref
	 *            A reference for an object produced by a taggable activity
	 *            producer.
	 * @return The context of the referenced object.
	 */
	public String getContext(String ref);

	/**
	 * Method to get a list of all taggable activities within the given context.
	 * 
	 * @param context
	 *            The context to search.
	 * @return A list of all taggable activities within the given context.
	 */
	public List<TaggableActivity> getActivities(String context);

	/**
	 * Method to get a taggable activity by reference string.
	 * 
	 * @param activityRef
	 *            A reference for the taggable activity.
	 * @return The taggable activity, or null if no such activity exists.
	 */
	public TaggableActivity getActivity(String activityRef);

	/**
	 * Method to get a list of the currently registered taggable activity
	 * producers.
	 * 
	 * @return List of taggable activity producers.
	 */
	public List<TaggableActivityProducer> getProducers();

	/**
	 * Method to get a list of the currently registered tagging providers.
	 * 
	 * @return List of tagging providers.
	 */
	public List<TaggingProvider> getProviders();

	/**
	 * Method to get a taggable item by reference string.
	 * 
	 * @param itemRef
	 *            The reference for the taggable item.
	 * @return The taggable item, or null if no such item exists.
	 */
	public TaggableItem getItem(String itemRef);

	/**
	 * Method to get a list of taggable items for the activity identified by the
	 * given activity reference.
	 * 
	 * @param activityRef
	 *            The reference for the taggable activity.
	 * @return A list of taggable items.
	 */
	public List<TaggableItem> getItems(String activityRef);

	/**
	 * Method to determine if there are any tagging providers available.
	 * 
	 * @return True if a tagging provider is available, false otherwise.
	 */
	public boolean isTaggable();
}
