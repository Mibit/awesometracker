/*
 * Awesome Time Tracker project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.att.ui.adapters;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javax.persistence.EntityManager;
import org.zlogic.att.data.ApplicationShuttingDownException;
import org.zlogic.att.data.Task;
import org.zlogic.att.data.TimeSegment;
import org.zlogic.att.data.TransactedChange;
import org.zlogic.att.ui.TimerRapidFiringDetector;

/**
 * Adapter to interface JPA with Java FX observable properties for TimeSegment
 * classes.
 *
 * @author Dmitry Zolotukhin <a
 * href="mailto:zlogic@gmail.com">zlogic@gmail.com</a>
 */
public class TimeSegmentAdapter {

	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/att/ui/adapters/messages");
	/**
	 * Assigned entity
	 */
	private TimeSegment segment;
	/*
	 * Java FX
	 */
	/**
	 * Description property
	 */
	private StringProperty description = new SimpleStringProperty();
	/**
	 * Last (latest) time assigned property (generated)
	 */
	private ReadOnlyStringWrapper duration = new ReadOnlyStringWrapper();
	/**
	 * Full description, including the task name, description and elapsed time
	 */
	private ReadOnlyStringWrapper fullDescription = new ReadOnlyStringWrapper();
	/**
	 * Start time property
	 */
	private ObjectProperty<Date> start = new SimpleObjectProperty<>(this, "");
	/**
	 * End time property
	 */
	private ObjectProperty<Date> end = new SimpleObjectProperty<>(this, "");
	/**
	 * Owner task property
	 */
	private ObjectProperty<TaskAdapter> ownerTask = new SimpleObjectProperty<>();
	/**
	 * DataManager reference
	 */
	private DataManager dataManager;
	/**
	 * Timer for automatically updating the time
	 */
	private Timer timer;
	/**
	 * Property to indicate if the segment is currently timing
	 */
	private BooleanProperty timingProperty = new SimpleBooleanProperty(false);
	/*
	 * Change listeners
	 */
	/**
	 * Description change listener
	 */
	private ChangeListener<String> descriptionChangeListener = new ChangeListener<String>() {
		@Override
		public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
			oldValue = oldValue == null ? "" : oldValue; //NOI18N
			newValue = newValue == null ? "" : newValue; //NOI18N
			if (!oldValue.equals(newValue) && getDataManager() != null) {
				getDataManager().getPersistenceHelper().performTransactedChange(new TransactedChange() {
					private String newValue;

					public TransactedChange setNewValue(String newValue) {
						this.newValue = newValue;
						return this;
					}

					@Override
					public void performChange(EntityManager entityManager) {
						setTimeSegment(entityManager.find(TimeSegment.class, getTimeSegment().getId()));
						getTimeSegment().setDescription(newValue);
					}
				}.setNewValue(newValue));
				updateFxProperties();
				getDataManager().signalTaskUpdate();
			}
		}
	};
	/**
	 * Start time change listener
	 */
	private ChangeListener<Date> startChangeListener = new ChangeListener<Date>() {
		@Override
		public void changed(ObservableValue<? extends Date> observableValue, Date oldValue, Date newValue) {
			if (!oldValue.equals(newValue) && getDataManager() != null) {
				//TODO: catch exceptions & revert
				getDataManager().getPersistenceHelper().performTransactedChange(new TransactedChange() {
					private Date newValue;

					public TransactedChange setNewValue(Date newValue) {
						this.newValue = newValue;
						return this;
					}

					@Override
					public void performChange(EntityManager entityManager) {
						setTimeSegment(entityManager.find(TimeSegment.class, getTimeSegment().getId()));
						getTimeSegment().setStartTime(newValue);
					}
				}.setNewValue(newValue));
				ownerTaskProperty().get().updateFromDatabase();
				updateFxProperties();
				getDataManager().signalTaskUpdate();
				//Update total time
				Instant oldValueDateTime = oldValue.toInstant();
				Instant newValueDateTime = newValue.toInstant();
				if (!newValueDateTime.equals(oldValueDateTime)) {
					Duration deltaTime = Duration.between(oldValueDateTime, newValueDateTime);
					getDataManager().addFilteredTotalTime(deltaTime);
				}
			}
		}
	};
	/**
	 * End time change listener
	 */
	private ChangeListener<Date> endChangeListener = new ChangeListener<Date>() {
		@Override
		public void changed(ObservableValue<? extends Date> observableValue, Date oldValue, Date newValue) {
			if (!oldValue.equals(newValue) && getDataManager() != null) {
				//TODO: catch exceptions & revert
				try {
					getDataManager().getPersistenceHelper().performTransactedChange(new TransactedChange() {
						private Date newValue;

						public TransactedChange setNewValue(Date newValue) {
							this.newValue = newValue;
							return this;
						}

						@Override
						public void performChange(EntityManager entityManager) {
							setTimeSegment(entityManager.find(TimeSegment.class, getTimeSegment().getId()));
							getTimeSegment().setEndTime(newValue);
						}
					}.setNewValue(newValue));
					ownerTaskProperty().get().updateFromDatabase();
					updateFxProperties();
					getDataManager().signalTaskUpdate();
					//Update total time
					Instant oldValueDateTime = oldValue.toInstant();
					Instant newValueDateTime = newValue.toInstant();
					if (!newValueDateTime.equals(oldValueDateTime)) {
						Duration deltaTime = Duration.between(oldValueDateTime, newValueDateTime);
						getDataManager().addFilteredTotalTime(deltaTime);
					}
				} catch (ApplicationShuttingDownException ex) {
					//Rethrow exception if we are not shutting down
					if (!dataManager.getPersistenceHelper().isShuttingDown())
						throw ex;
				}
			}
		}
	};
	/**
	 * Owner task change listener
	 */
	private ChangeListener<TaskAdapter> ownerTaskChangeListener = new ChangeListener<TaskAdapter>() {
		@Override
		public void changed(ObservableValue<? extends TaskAdapter> ov, TaskAdapter oldValue, TaskAdapter newValue) {
			if (!oldValue.equals(newValue) && getDataManager() != null) {
				getDataManager().getPersistenceHelper().performTransactedChange(new TransactedChange() {
					private TaskAdapter newValue;

					public TransactedChange setNewValue(TaskAdapter newValue) {
						this.newValue = newValue;
						return this;
					}

					@Override
					public void performChange(EntityManager entityManager) {
						setTimeSegment(entityManager.find(TimeSegment.class, getTimeSegment().getId()));
						Task newTask = entityManager.find(Task.class, newValue.getTask().getId());
						getTimeSegment().setOwner(newTask);
					}
				}.setNewValue(newValue));
				if (isTimingProperty().get()) {
					oldValue.isTimingProperty().unbind();
					oldValue.isTimingProperty().set(false);
					newValue.isTimingProperty().bind(isTimingProperty());
				}
				oldValue.updateFromDatabase();
				newValue.updateFromDatabase();
				oldValue.nameProperty().removeListener(ownerTaskNameListener);
				newValue.nameProperty().addListener(ownerTaskNameListener);
				updateFxProperties();
			}
		}
	};
	/**
	 * Owner task name listener
	 */
	private ChangeListener<String> ownerTaskNameListener = new ChangeListener<String>() {
		@Override
		public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
			if (newValue != null)
				updateFxProperties();
		}
	};

	/**
	 * Creates a TimeSegmentAdapter instance
	 *
	 * @param segment the associated entity
	 * @param ownerTask the owner TaskAdapter reference
	 * @param dataManager the DataManager reference
	 */
	public TimeSegmentAdapter(TimeSegment segment, TaskAdapter ownerTask, DataManager dataManager) {
		this.segment = segment;
		this.dataManager = dataManager;
		this.ownerTask.setValue(ownerTask);

		updateFxProperties();

		//Set change listeners
		this.ownerTask.addListener(ownerTaskChangeListener);
		this.ownerTask.get().nameProperty().addListener(ownerTaskNameListener);
	}

	/**
	 * Sets the start/end time in a single call to prevent race conditions
	 *
	 * @param startTime the new start time
	 * @param endTime the new end time
	 */
	public void setStartEndTime(Date startTime, Date endTime) {
		Duration previousTime = segment.getDuration();
		dataManager.getPersistenceHelper().performTransactedChange(new TransactedChange() {
			private Date startTime, endTime;

			public TransactedChange setParameters(Date startTime, Date endTime) {
				this.startTime = startTime;
				this.endTime = endTime;
				return this;
			}

			@Override
			public void performChange(EntityManager entityManager) {
				setTimeSegment(entityManager.find(TimeSegment.class, getTimeSegment().getId()));
				getTimeSegment().setStartEndTime(startTime, endTime);
			}
		}.setParameters(startTime, endTime));
		getDataManager().addFilteredTotalTime(segment.getDuration().minus(previousTime));
		updateFxProperties();
	}

	/**
	 * Starts timing this segment
	 */
	public void startTiming() {
		ownerTask.get().isTimingProperty().bind(timingProperty);
		timingProperty.set(true);
		if (timer != null)
			timer.cancel();
		//Start the timer
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask() {
			private Runnable task = new Runnable() {
				private TimerRapidFiringDetector timerMissedEventConsumer = new TimerRapidFiringDetector(500);

				@Override
				public void run() {
					if (timerMissedEventConsumer.isRapidFiring())
						return;
					if (isTimingProperty().get())
						endProperty.setValue(new Date());
				}
			};
			private ObjectProperty<Date> endProperty;

			public TimerTask setEndProperty(ObjectProperty<Date> endProperty) {
				this.endProperty = endProperty;
				return this;
			}

			@Override
			public void run() {
				Platform.runLater(task);
			}
		}.setEndProperty(endProperty()), 0, 1000);
	}

	/**
	 * Stops timing this segment
	 */
	public void stopTiming() {
		timer.cancel();
		timer = null;
		endProperty().setValue(new Date());
		timingProperty.set(false);
		dataManager.timingSegmentProperty().set(null);
		ownerTask.get().isTimingProperty().unbind();
	}

	/*
	 * JavaFX properties
	 */
	/**
	 * Start time property
	 *
	 * @return the start time property
	 */
	public ObjectProperty<Date> startProperty() {
		return start;
	}

	/**
	 * End time property
	 *
	 * @return the end time property
	 */
	public ObjectProperty<Date> endProperty() {
		return end;
	}

	/**
	 * Duration property
	 *
	 * @return the duration property
	 */
	public ReadOnlyStringProperty durationProperty() {
		return duration.getReadOnlyProperty();
	}

	/**
	 * Description property
	 *
	 * @return the description property
	 */
	public StringProperty descriptionProperty() {
		return description;
	}

	/**
	 * Full description property
	 *
	 * @return the full description property
	 */
	public ReadOnlyStringProperty fullDescriptionProperty() {
		return fullDescription.getReadOnlyProperty();
	}

	/**
	 * The time segment currently timing state property (true if is timing,
	 * false if timer is not running)
	 *
	 * @return the time segment timing state property
	 */
	public BooleanProperty isTimingProperty() {
		return timingProperty;
	}

	/**
	 * The owner task property
	 *
	 * @return the owner task property
	 */
	public ObjectProperty<TaskAdapter> ownerTaskProperty() {
		return ownerTask;
	}

	/*
	 * Getters/setters
	 */
	/**
	 * Returns the associated TimeSegment entity
	 *
	 * @return the associated TimeSegment entity
	 */
	public TimeSegment getTimeSegment() {
		return segment;
	}

	/**
	 * Changes the associated TimeSegment entity
	 *
	 * @param segment the new (or updated) segment
	 */
	private void setTimeSegment(TimeSegment segment) {
		this.segment = segment;
	}
	//TODO: other Getters/setters

	/*
	 * Internal methods
	 */
	/**
	 * Returns the DataManager reference
	 *
	 * @return the DataManager reference
	 */
	private DataManager getDataManager() {
		return dataManager;
	}

	/**
	 * Updates Java FX properties from the associated entity
	 */
	private void updateFxProperties() {
		//Remove listeners since the update is initiated by us
		description.removeListener(descriptionChangeListener);
		start.removeListener(startChangeListener);
		end.removeListener(endChangeListener);
		//Perform update
		if (description.get() == null || !description.get().equals(segment.getDescription()))
			description.setValue(segment.getDescription());
		if (start.get() == null || !start.get().equals(segment.getStartTime()))
			start.setValue(segment.getStartTime());
		if (end.get() == null || !end.get().equals(segment.getEndTime()))
			end.setValue(segment.getEndTime());
		duration.setValue(DurationFormatter.formatDuration(segment.getDuration()));
		fullDescription.setValue(MessageFormat.format(messages.getString("FULL_DESCRIPTION"),
				new Object[]{
					((ownerTask != null && ownerTask.get().nameProperty().get() != null) ? ownerTask.get().nameProperty().get() : messages.getString("NULL_OWNER_TASK")),
					description.get(),
					duration.get()}));
		//Restore listener
		description.addListener(descriptionChangeListener);
		start.addListener(startChangeListener);
		end.addListener(endChangeListener);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TimeSegmentAdapter)
			return ((TimeSegmentAdapter) obj).getTimeSegment().equals(segment);
		else
			return obj == null && segment == null;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 47 * hash + Objects.hashCode(this.segment.hashCode());
		return hash;
	}
}
