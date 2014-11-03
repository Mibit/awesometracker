/*
 * Awesome Time Tracker project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.att.data;

import java.io.Serializable;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Entity class for a tracked task. Each task's time is tracked with
 * TimeSegments.
 *
 * @author Dmitry Zolotukhin <a
 * href="mailto:zlogic@gmail.com">zlogic@gmail.com</a>
 */
@Entity
public class Task implements Serializable {

	/**
	 * JPA ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private long id;
	/**
	 * The task description
	 */
	private String description;
	/**
	 * The task name
	 */
	private String name;
	/**
	 * The list of this task's time segments
	 */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<TimeSegment> timeSegments;
	/**
	 * Values of custom fields
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	private Map<CustomField, String> customFields;
	/**
	 * Boolean setting indicating the task is completed
	 */
	private Boolean completed;

	/**
	 * Default constructor
	 */
	protected Task() {
		id = -1;
		description = ""; //NOI18N
		name = ""; //NOI18N
		completed = false;
		timeSegments = new TreeSet<>();
		customFields = new TreeMap<>();
	}

	/**
	 * Creates a new TimeSegment for this task
	 *
	 * @return the created TimeSegment
	 */
	public TimeSegment createSegment() {
		TimeSegment segment = new TimeSegment();
		segment.setOwner(this);
		return segment;
	}

	/**
	 * Adds a time segment to this task
	 *
	 * @param segment the new time segment
	 */
	public void addSegment(TimeSegment segment) {
		timeSegments.add(segment);
	}

	/**
	 * Removes a time segment from this task
	 *
	 * @param segment the time segment to be removed
	 */
	public void removeSegment(TimeSegment segment) {
		timeSegments.remove(segment);
	}

	/**
	 * Calculates the total time for all time segments associated with this task
	 *
	 * @return the total time for this task
	 */
	public Duration getTotalTime() {
		Duration totalTime = Duration.ZERO;
		for (TimeSegment segment : timeSegments)
			totalTime = totalTime.plus(segment.getDuration());
		return totalTime;
	}

	/**
	 * Calculates the total time for all time segments associated with this
	 * task, clipped to fit into the specified time period
	 *
	 * @param clipStartTime start time of clip period
	 * @param clipEndTime end time of clip period
	 * @return the total time for this task
	 */
	public Duration getTotalTime(Date clipStartTime, Date clipEndTime) {
		Duration totalTime = Duration.ZERO;
		for (TimeSegment segment : timeSegments)
			totalTime = totalTime.plus(segment.getClippedDuration(clipStartTime, clipEndTime));
		return totalTime;
	}

	/**
	 * Returns the description
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description
	 *
	 * @param description the new description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Returns the name
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the task's completion state
	 *
	 * @return the task's completion state
	 */
	public boolean getCompleted() {
		return completed;
	}

	/**
	 * Sets the task's completion state
	 *
	 * @param complete the task's new completion state
	 */
	public void setCompleted(Boolean complete) {
		this.completed = complete;
	}

	/**
	 * Returns the task's custom field value
	 *
	 * @param field the field to be retrieved
	 * @return the task's custom field value (or null if it's not set)
	 */
	public String getCustomField(CustomField field) {
		return customFields.get(field);
	}

	/**
	 * Sets the task's custom field value
	 *
	 * @param customField the custom field to be set
	 * @param value the new value for customField
	 */
	public void setCustomField(CustomField customField, String value) {
		if (value == null)
			customFields.remove(customField);
		else
			customFields.put(customField, value);
	}

	/**
	 * Returns all time segments for this task
	 *
	 * @return the list of all time segments for this task
	 */
	public Set<TimeSegment> getTimeSegments() {
		return timeSegments;
	}

	/**
	 * Returns the JPA ID
	 *
	 * @return the JPA ID
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns this class instance (return this); required for some reflection
	 * functions
	 *
	 * @return this Task instance
	 */
	public Task getTask() {
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Task && id == ((Task) obj).id;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 37 * hash + (int) (this.id ^ (this.id >>> 32));
		return hash;
	}
}
