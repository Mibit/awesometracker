/*
 * Awesome Time Tracker project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.att.ui;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.converter.DateTimeStringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.zlogic.att.data.ConfigurationElement;
import org.zlogic.att.data.converters.Exporter;
import org.zlogic.att.data.converters.GrindstoneImporter;
import org.zlogic.att.data.converters.Importer;
import org.zlogic.att.data.converters.XmlExporter;
import org.zlogic.att.data.converters.XmlImporter;
import org.zlogic.att.ui.adapters.CustomFieldAdapter;
import org.zlogic.att.ui.adapters.CustomFieldValueAdapter;
import org.zlogic.att.ui.adapters.DataManager;
import org.zlogic.att.ui.adapters.DurationFormatter;
import org.zlogic.att.ui.adapters.TaskAdapter;
import org.zlogic.att.ui.adapters.TimeSegmentAdapter;

/**
 * Controller for the main window
 *
 * @author Dmitry Zolotukhin <a
 * href="mailto:zlogic@gmail.com">zlogic@gmail.com</a>
 */
public class MainWindowController {

	/**
	 * The logger
	 */
	private final static Logger log = Logger.getLogger(MainWindowController.class.getName());
	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/att/ui/messages");
	/**
	 * DataManager reference
	 */
	private DataManager dataManager;
	/**
	 * Last opened directory
	 */
	private ObjectProperty<File> lastDirectory = new SimpleObjectProperty<>();
	/**
	 * Easy access to preference storage
	 */
	/**
	 * The background task thread (used for synchronization & clean termination)
	 */
	protected Thread backgroundThread;
	/**
	 * The background task
	 */
	protected Task<Void> backgroundTask;
	/**
	 * Task to be performed before shutdown/exit
	 */
	private Runnable shutdownProcedure;
	/**
	 * Custom field editor stage
	 */
	private Stage customFieldEditorStage;
	/**
	 * Custom field editor controller
	 */
	private CustomFieldEditorController customFieldEditorController;
	/**
	 * Report stage
	 */
	private Stage reportStage;
	/**
	 * Report controller
	 */
	private ReportController reportController;
	/**
	 * Current task notification controller
	 */
	private CurrentTaskNotificationController currentTaskNotificationController;
	/**
	 * Inactivity prompt dialog controller
	 */
	private InactivityDialogController inactivityDialogController;
	/**
	 * Confirmation prompt dialog controller
	 */
	private ConfirmationDialogController confirmationDialogController;
	/**
	 * Filters stage
	 */
	private Stage filterEditorStage;
	/**
	 * Report controller
	 */
	private FilterEditorController filterEditorController;
	/**
	 * About stage
	 */
	private Stage aboutStage;
	/**
	 * Root pane
	 */
	@FXML
	private VBox rootPane;
	/**
	 * Task editor controller
	 */
	@FXML
	private TaskEditorController taskEditorController;
	/**
	 * Time graph controller
	 */
	@FXML
	private TimeGraphController timeGraphController;
	/**
	 * Tasks list table
	 */
	@FXML
	private TableView<TaskAdapter> taskList;
	/**
	 * Task name column
	 */
	@FXML
	private TableColumn<TaskAdapter, String> columnTaskName;
	/**
	 * Task total time column
	 */
	@FXML
	private TableColumn<TaskAdapter, String> columnTotalTime;
	/**
	 * Task first time column
	 */
	@FXML
	private TableColumn<TaskAdapter, Date> columnFirstTime;
	/**
	 * Task last time column
	 */
	@FXML
	private TableColumn<TaskAdapter, Date> columnLastTime;
	/**
	 * Task completed column
	 */
	@FXML
	private TableColumn<TaskAdapter, Boolean> columnTaskCompleted;
	/**
	 * Label to indicate the active task
	 */
	@FXML
	private Label activeTaskLabel;
	/**
	 * Duplicate task button
	 */
	@FXML
	private Button duplicateTaskButton;
	/**
	 * Delete task button
	 */
	@FXML
	private Button deleteTaskButton;
	/**
	 * Active task pane (can be hidden)
	 */
	@FXML
	private HBox activeTaskPane;
	/**
	 * Status pane
	 */
	@FXML
	private HBox statusPane;
	/**
	 * Total filtered time field
	 */
	@FXML
	private TextField totalTimeField;
	/**
	 * Progress indicator
	 */
	@FXML
	private ProgressIndicator progressIndicator;
	/**
	 * Progress indicator label
	 */
	@FXML
	private Label progressLabel;
	/**
	 * Cleanup DB menu item
	 */
	@FXML
	private MenuItem menuItemCleanupDB;
	/**
	 * Import Awesome Time Tracker XML data menu item
	 */
	@FXML
	private MenuItem menuItemImportAwesomeTimeTrackerXml;
	/**
	 * Export Awesome Time Tracker XML data menu item
	 */
	@FXML
	private MenuItem menuItemExportAwesomeTimeTrackerXml;
	/**
	 * Property to store the number of selected tasks
	 */
	@FXML
	private IntegerProperty taskSelectionSize = new SimpleIntegerProperty(0);
	/**
	 * Graphical representation tab
	 */
	@FXML
	private Tab tabGraphical;

	/**
	 * Initializes the controller
	 *
	 * @param url initialization URL
	 * @param resourceBundle supplied resources
	 */
	@FXML
	public void initialize() {
		//Default sort order
		taskList.getSortOrder().add(columnLastTime);
		columnLastTime.setSortType(TableColumn.SortType.DESCENDING);
		//Task comparator
		Comparator<Date> TaskComparator = new Comparator<Date>() {
			@Override
			public int compare(Date o1, Date o2) {
				if (o1 != null && o2 != null)
					return o1.compareTo(o2);
				if (o1 == null && o2 != null)
					return 1;
				if (o1 != null && o2 == null)
					return -1;
				else
					return 0;
			}
		};
		columnLastTime.setComparator(TaskComparator);
		//Create the data manager
		dataManager = new DataManager();
		//Total time field
		dataManager.filteredTotalTimeProperty().addListener(new ChangeListener<Duration>() {
			@Override
			public void changed(ObservableValue<? extends Duration> ov, Duration oldValue, Duration newValue) {
				if (newValue != null)
					totalTimeField.setText(DurationFormatter.formatDuration(newValue));
				else
					totalTimeField.setText(""); //NOI18N
			}
		});
		//Task list
		setItems(dataManager.getTasks());
		reloadTasks();

		//Auto update sort order
		dataManager.addTasksUpdatedListener(new EventHandler() {
			@Override
			public void handle(Event event) {
				updateSortOrder();
			}
		});

		//Update the selection size property
		taskList.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<TaskAdapter>() {
			@Override
			public void onChanged(Change<? extends TaskAdapter> change) {
				taskEditorController.setEditedTaskList(taskList.getSelectionModel().getSelectedItems());
				taskSelectionSize.set(change.getList().size());
			}
		});

		taskList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		//Bind the current task panel
		activeTaskPane.managedProperty().bind(activeTaskPane.visibleProperty());
		activeTaskPane.visibleProperty().bind(dataManager.timingSegmentProperty().isNotNull());
		dataManager.timingSegmentProperty().addListener(new ChangeListener<TimeSegmentAdapter>() {
			private ChangeListener<TaskAdapter> taskChangedListener = new ChangeListener<TaskAdapter>() {
				@Override
				public void changed(ObservableValue<? extends TaskAdapter> ov, TaskAdapter oldValue, TaskAdapter newValue) {
					activeTaskLabel.textProperty().unbind();
					activeTaskLabel.textProperty().bind(newValue.nameProperty());
				}
			};

			@Override
			public void changed(ObservableValue<? extends TimeSegmentAdapter> ov, TimeSegmentAdapter oldValue, TimeSegmentAdapter newValue) {
				if (newValue != null && newValue.equals(oldValue))
					return;
				if (oldValue != null)
					oldValue.ownerTaskProperty().removeListener(taskChangedListener);
				if (newValue != null) {
					activeTaskLabel.textProperty().unbind();
					activeTaskLabel.textProperty().bind(newValue.ownerTaskProperty().get().nameProperty());
					newValue.ownerTaskProperty().addListener(taskChangedListener);
				}
			}
		});

		deleteTaskButton.disableProperty().bind(taskSelectionSize.lessThanOrEqualTo(0));
		duplicateTaskButton.disableProperty().bind(taskSelectionSize.lessThanOrEqualTo(0));
		//Restore settings
		lastDirectory.addListener(new ChangeListener<File>() {
			@Override
			public void changed(ObservableValue<? extends File> ov, File oldFile, File newFile) {
				ConfigurationElement element = new ConfigurationElement("lastDirectory", newFile); //NOI18N
				dataManager.getPersistenceHelper().mergeEntity(element);
			}
		});
		ConfigurationElement lastDirectoryConfigurationElement = dataManager.getPersistenceHelper().getConfigurationElement("lastDirectory"); //NOI18N
		lastDirectory.set(lastDirectoryConfigurationElement == null ? null : ((File) lastDirectoryConfigurationElement.getValue()));
		//Row properties
		taskList.setRowFactory(new Callback<TableView<TaskAdapter>, TableRow<TaskAdapter>>() {
			@Override
			public TableRow<TaskAdapter> call(TableView<TaskAdapter> p) {
				TableRow<TaskAdapter> row = new TableRow<>();
				row.itemProperty().addListener(new ChangeListener<TaskAdapter>() {
					private TableRow<TaskAdapter> row;
					private ChangeListener timingChangeListener = new ChangeListener<Boolean>() {
						@Override
						public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
							if (newValue != null && newValue)
								row.getStyleClass().add("timing-segment"); //NOI18N
							else
								row.getStyleClass().remove("timing-segment"); //NOI18N
						}
					};
					private ChangeListener completedChangeListener = new ChangeListener<Boolean>() {
						@Override
						public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
							row.getStyleClass().removeAll("item-completed", "item-not-completed");//FIXME: remove this once fixed in Java FX //NOI18N
							if (newValue != null && newValue)
								row.getStyleClass().add("item-completed"); //NOI18N
							else
								row.getStyleClass().add("item-not-completed"); //NOI18N
						}
					};

					public ChangeListener<TaskAdapter> setRow(TableRow<TaskAdapter> row) {
						this.row = row;
						return this;
					}

					@Override
					public void changed(ObservableValue<? extends TaskAdapter> ov, TaskAdapter oldValue, TaskAdapter newValue) {
						if (oldValue != null) {
							oldValue.isTimingProperty().removeListener(timingChangeListener);
							oldValue.completedProperty().removeListener(completedChangeListener);
						}
						if (newValue != null) {
							newValue.isTimingProperty().addListener(timingChangeListener);
							newValue.completedProperty().addListener(completedChangeListener);
						}
						timingChangeListener.changed(
								newValue != null ? newValue.isTimingProperty() : null,
								oldValue != null ? oldValue.isTimingProperty().get() : false,
								newValue != null ? newValue.isTimingProperty().get() : false);
						completedChangeListener.changed(
								newValue != null ? newValue.completedProperty() : null,
								oldValue != null ? oldValue.completedProperty().get() : false,
								newValue != null ? newValue.completedProperty().get() : false);
					}
				}.setRow(row));
				//Drag'n'drop support
				//TODO: create a separate class
				row.setOnDragEntered(new EventHandler<DragEvent>() {
					private TableRow<TaskAdapter> row;

					public EventHandler<DragEvent> setRow(TableRow<TaskAdapter> row) {
						this.row = row;
						return this;
					}

					@Override
					public void handle(DragEvent event) {
						if (event.getGestureSource() instanceof TableView && taskEditorController.getDragSource(event.getGestureSource()) != null && row.getItem() != null) {
							row.getStyleClass().add("drag-accept-task"); //NOI18N
							event.consume();
						}
					}
				}.setRow(row));
				row.setOnDragExited(new EventHandler<DragEvent>() {
					private TableRow<TaskAdapter> row;

					public EventHandler<DragEvent> setRow(TableRow<TaskAdapter> row) {
						this.row = row;
						return this;
					}

					@Override
					public void handle(DragEvent event) {
						row.getStyleClass().remove("drag-accept-task"); //NOI18N
						event.consume();
					}
				}.setRow(row));
				row.setOnDragOver(new EventHandler<DragEvent>() {
					@Override
					public void handle(DragEvent event) {
						if (event.getGestureSource() instanceof TableView && taskEditorController.getDragSource(event.getGestureSource()) != null)
							event.acceptTransferModes(TransferMode.MOVE);
						event.consume();
					}
				});
				row.setOnDragDropped(new EventHandler<DragEvent>() {
					private TableRow<TaskAdapter> row;

					public EventHandler<DragEvent> setRow(TableRow<TaskAdapter> row) {
						this.row = row;
						return this;
					}

					@Override
					public void handle(DragEvent event) {
						boolean success = false;
						if (event.getGestureSource() instanceof TableView) {
							TimeSegmentAdapter selectedItem = taskEditorController.getDragSource(event.getGestureSource());
							if (selectedItem != null && row.getItem() != null) {
								event.setDropCompleted(success);
								((TimeSegmentAdapter) selectedItem).ownerTaskProperty().set(row.getItem());
							}
						}
						event.setDropCompleted(success);
						event.consume();
					}
				}.setRow(row));
				return row;
			}
		;
		});
		//Cell editors
		columnTaskName.setCellFactory(new Callback<TableColumn<TaskAdapter, String>, TableCell<TaskAdapter, String>>() {
			@Override
			public TableCell<TaskAdapter, String> call(TableColumn<TaskAdapter, String> p) {
				TextFieldTableCell<TaskAdapter, String> cell = new TextFieldTableCell<>();
				cell.setConverter(new DefaultStringConverter());
				//Shift + space causes all kids of problems ("rarely occurring discarded edits of time segments which are currently being timed")
				//So it's best to intercept it and not allow TableView to ruin an edit in progress
				cell.setOnKeyPressed(new TableCellBadShortcutsInterceptor(cell.editingProperty()));
				return cell;
			}
		});
		columnTaskCompleted.setCellFactory(new Callback<TableColumn<TaskAdapter, Boolean>, TableCell<TaskAdapter, Boolean>>() {
			@Override
			public TableCell<TaskAdapter, Boolean> call(TableColumn<TaskAdapter, Boolean> taskAdapterBooleanTableColumn) {
				CheckBoxTableCell<TaskAdapter, Boolean> cell = new CheckBoxTableCell<>();
				cell.setAlignment(Pos.CENTER);
				return cell;
			}
		});
		columnTotalTime.setCellFactory(new Callback<TableColumn<TaskAdapter, String>, TableCell<TaskAdapter, String>>() {
			@Override
			public TableCell<TaskAdapter, String> call(TableColumn<TaskAdapter, String> p) {
				TextFieldTableCell<TaskAdapter, String> cell = new TextFieldTableCell<>();
				cell.setConverter(new DefaultStringConverter());
				cell.setAlignment(Pos.CENTER_RIGHT);
				return cell;
			}
		});
		columnFirstTime.setCellFactory(new Callback<TableColumn<TaskAdapter, Date>, TableCell<TaskAdapter, Date>>() {
			@Override
			public TableCell<TaskAdapter, Date> call(TableColumn<TaskAdapter, Date> p) {
				TextFieldTableCell<TaskAdapter, Date> cell = new TextFieldTableCell<>();
				cell.setConverter(new DateTimeStringConverter());
				cell.setAlignment(Pos.CENTER_RIGHT);
				return cell;
			}
		});
		columnLastTime.setCellFactory(new Callback<TableColumn<TaskAdapter, Date>, TableCell<TaskAdapter, Date>>() {
			@Override
			public TableCell<TaskAdapter, Date> call(TableColumn<TaskAdapter, Date> p) {
				TextFieldTableCell<TaskAdapter, Date> cell = new TextFieldTableCell<>();
				cell.setConverter(new DateTimeStringConverter());
				cell.setAlignment(Pos.CENTER_RIGHT);
				return cell;
			}
		});

		//Set column sizing policy
		taskList.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

		//Menu items and status pane
		menuItemCleanupDB.disableProperty().bind(statusPane.visibleProperty());
		menuItemImportAwesomeTimeTrackerXml.disableProperty().bind(statusPane.visibleProperty());
		menuItemExportAwesomeTimeTrackerXml.disableProperty().bind(statusPane.visibleProperty());
		statusPane.managedProperty().bind(statusPane.visibleProperty());
		statusPane.setVisible(false);

		//Set the window close handler
		//setCloseHandler();
		//Load other windows
		loadWindowCustomFieldEditor();
		loadWindowReport();
		loadWindowFilterEditor();
		loadCurrentTaskNotification();
		loadInactivityDialog();
		loadWindowAbout();
		confirmationDialogController = ConfirmationDialogController.createInstance();
		taskEditorController.setDataManager(dataManager);
		timeGraphController.setDataManager(dataManager);

		timeGraphController.visibleProperty().bind(tabGraphical.selectedProperty());//TODO: maybe always visible is actually OK?
		timeGraphController.visibleProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
				if (newValue)
					timeGraphController.setSelectedTimeSegments(taskEditorController.getSelectedTimeSegments());
			}
		});
	}

	/**
	 * Assigns the shutdown procedure to be performed before quitting Java FX
	 * application
	 *
	 * @param shutdownProcedure the shutdown procedure
	 */
	public void setShutdownProcedure(Runnable shutdownProcedure) {
		this.shutdownProcedure = shutdownProcedure;
	}

	/**
	 * Returns the DataManager reference
	 *
	 * @return the DataManager reference
	 */
	public DataManager getDataManager() {
		return dataManager;
	}

	/**
	 * Replaces the "close" action with a "hide" action. If rootPane has no
	 * Scene or Window associated, will monitor these fields and perform
	 * replacement once they are assigned.
	 */
	private void setCloseHandler() {
		ChangeListener<Window> windowListener = new ChangeListener<Window>() {
			@Override
			public void changed(ObservableValue<? extends Window> ov, Window oldWindow, Window newWindow) {
				if (newWindow == null)
					return;
				newWindow.setOnCloseRequest(new EventHandler<WindowEvent>() {
					private Window window;

					public EventHandler<WindowEvent> setWindow(Window window) {
						this.window = window;
						return this;
					}

					@Override
					public void handle(WindowEvent event) {
						event.consume();
						if (window instanceof Stage)
							((Stage) window).setIconified(true);
					}
				}.setWindow(newWindow));
				rootPane.getScene().windowProperty().removeListener(this);
			}
		};
		ChangeListener<Scene> sceneListener = new ChangeListener<Scene>() {
			private ChangeListener<Window> windowListener;

			public ChangeListener<Scene> setWindowListener(ChangeListener<Window> windowListener) {
				this.windowListener = windowListener;
				return this;
			}

			@Override
			public void changed(ObservableValue<? extends Scene> ov, Scene oldScene, Scene newScene) {
				if (newScene.getWindow() != null) {
					windowListener.changed(null, null, newScene.getWindow());
					rootPane.sceneProperty().removeListener(this);
				} else {
					newScene.windowProperty().addListener(windowListener);
				}
			}
		}.setWindowListener(windowListener);
		rootPane.sceneProperty().addListener(sceneListener);
	}

	/**
	 * Sets the window icons
	 *
	 * @param icons the icons to be set
	 */
	public void setWindowIcons(ObservableList<Image> icons) {
		ExceptionLogger.getInstance().setWindowIcons(icons);
		currentTaskNotificationController.setWindowIcons(icons);
		inactivityDialogController.setWindowIcons(icons);
		taskEditorController.setWindowIcons(icons);
		confirmationDialogController.setWindowIcons(icons);
		customFieldEditorController.setWindowIcons(icons);
	}

	/**
	 * Loads the custom field editor FXML
	 */
	private void loadWindowCustomFieldEditor() {
		//Load FXML
		customFieldEditorStage = new Stage();
		customFieldEditorStage.initModality(Modality.NONE);
		Parent root = null;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("CustomFieldEditor.fxml"), messages); //NOI18N
		loader.setLocation(getClass().getResource("CustomFieldEditor.fxml")); //NOI18N
		try {
			root = loader.load();
		} catch (IOException ex) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, messages.getString("ERROR_LOADING_FXML"), ex);
			ExceptionLogger.getInstance().showException(messages.getString("ERROR_LOADING_FXML"), ex);
		}
		//Initialize the scene properties
		if (root != null) {
			Scene scene = new Scene(root);
			customFieldEditorStage.setTitle(messages.getString("CUSTOM_FIELD_EDITOR"));
			customFieldEditorStage.setScene(scene);
		}
		//Set the data manager
		customFieldEditorController = loader.getController();
		customFieldEditorController.setDataManager(dataManager);
	}

	/**
	 * Loads the report FXML
	 */
	private void loadWindowReport() {
		//Load FXML
		reportStage = new Stage();
		reportStage.initModality(Modality.NONE);
		Parent root = null;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("Report.fxml"), messages); //NOI18N
		loader.setLocation(getClass().getResource("Report.fxml")); //NOI18N
		try {
			root = loader.load();
		} catch (IOException ex) {
			log.log(Level.SEVERE, messages.getString("ERROR_LOADING_FXML"), ex);
			ExceptionLogger.getInstance().showException(messages.getString("ERROR_LOADING_FXML"), ex);
		}
		//Initialize the scene properties
		if (root != null) {
			Scene scene = new Scene(root);
			reportStage.setTitle(messages.getString("REPORT"));
			reportStage.setScene(scene);
		}
		//Set the data manager
		reportController = loader.getController();
		reportController.setDataManager(dataManager);
		reportController.setLastDirectory(lastDirectory);
	}

	/**
	 * Loads the filter editor FXML
	 */
	private void loadWindowFilterEditor() {
		//Load FXML
		filterEditorStage = new Stage();
		filterEditorStage.initModality(Modality.NONE);
		Parent root = null;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("FilterEditor.fxml"), messages); //NOI18N
		loader.setLocation(getClass().getResource("FilterEditor.fxml")); //NOI18N
		try {
			root = loader.load();
		} catch (IOException ex) {
			log.log(Level.SEVERE, messages.getString("ERROR_LOADING_FXML"), ex);
			ExceptionLogger.getInstance().showException(messages.getString("ERROR_LOADING_FXML"), ex);
		}
		//Initialize the scene properties
		if (root != null) {
			Scene scene = new Scene(root);
			filterEditorStage.setTitle(messages.getString("FILTERS"));
			filterEditorStage.setScene(scene);
		}
		//Set the data manager
		filterEditorController = loader.getController();
		filterEditorController.setDataManager(dataManager);
	}

	/**
	 * Loads the current task notification FXML
	 */
	private void loadCurrentTaskNotification() {
		//Load FXML
		FXMLLoader loader = new FXMLLoader(getClass().getResource("CurrentTaskNotification.fxml"), messages); //NOI18N
		loader.setLocation(getClass().getResource("CurrentTaskNotification.fxml")); //NOI18N
		try {
			loader.load();
		} catch (IOException ex) {
			log.log(Level.SEVERE, messages.getString("ERROR_LOADING_FXML"), ex);
			ExceptionLogger.getInstance().showException(messages.getString("ERROR_LOADING_FXML"), ex);
		}
		//Set the data manager
		currentTaskNotificationController = loader.getController();
		currentTaskNotificationController.setDataManager(dataManager);
	}

	/**
	 * Loads the inactivity prompt dialog FXML
	 */
	private void loadInactivityDialog() {
		//Load FXML
		FXMLLoader loader = new FXMLLoader(getClass().getResource("InactivityDialog.fxml"), messages); //NOI18N
		loader.setLocation(getClass().getResource("InactivityDialog.fxml")); //NOI18N
		try {
			loader.load();
		} catch (IOException ex) {
			log.log(Level.SEVERE, messages.getString("ERROR_LOADING_FXML"), ex);
			ExceptionLogger.getInstance().showException(messages.getString("ERROR_LOADING_FXML"), ex);
		}
		//Set the data manager
		inactivityDialogController = loader.getController();
		inactivityDialogController.setDataManager(dataManager);
	}

	/**
	 * Loads the about window FXML
	 */
	private void loadWindowAbout() {
		//Load FXML
		aboutStage = new Stage();
		aboutStage.initModality(Modality.NONE);
		aboutStage.setResizable(false);
		Parent root = null;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("About.fxml"), messages); //NOI18N
		loader.setLocation(getClass().getResource("About.fxml")); //NOI18N
		try {
			root = loader.load();
		} catch (IOException ex) {
			log.log(Level.SEVERE, messages.getString("ERROR_LOADING_FXML"), ex);
			ExceptionLogger.getInstance().showException(messages.getString("ERROR_LOADING_FXML"), ex);
		}
		//Initialize the scene properties
		if (root != null) {
			Scene scene = new Scene(root);
			aboutStage.setTitle(messages.getString("ABOUT_TITLE"));
			aboutStage.setScene(scene);
		}
	}

	/**
	 * Reloads tasks
	 */
	protected void reloadTasks() {
		dataManager.reloadTasks();
		taskEditorController.setEditedTaskList(taskList.getSelectionModel().getSelectedItems());
		updateSortOrder();
	}

	/**
	 * Sets the taskList items while keeping the sort order preferences
	 *
	 * @param items the items to assign to taskList's items
	 */
	protected void setItems(ObservableList<TaskAdapter> items) {
		List<TableColumn<TaskAdapter, ?>> sortOrder = new ArrayList(taskList.getSortOrder());
		taskList.setItems(items);
		taskList.getSortOrder().setAll(sortOrder);
	}

	/**
	 * Updates the tasks sort order
	 */
	private void updateSortOrder() {
		if (taskList.getEditingCell() != null && taskList.getEditingCell().getRow() >= 0)
			return;

		TaskAdapter focusedAdapter = taskList.getFocusModel().getFocusedItem();
		taskList.getSortPolicy().call(taskList);
		//Restore lost focus
		if (focusedAdapter != null && taskList.getFocusModel().getFocusedItem() == null)
			taskList.getFocusModel().focus(taskList.getItems().indexOf(focusedAdapter));
	}

	/*
	 * Background task processing
	 */
	/**
	 * Starts the background task (disables task-related components, show the
	 * progress pane)
	 */
	private void beginBackgroundTask() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				statusPane.setVisible(true);
			}
		});
	}

	/**
	 * Ends the background task (enables task-related components, hides the
	 * progress pane)
	 */
	private void endBackgroundTask() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				statusPane.setVisible(false);
			}
		});
	}

	/**
	 * Starts a task in a background thread
	 *
	 * @param task the task to be started
	 */
	protected void startTaskThread(Task<Void> task) {
		synchronized (this) {
			//Wait for previous task to compete
			completeTaskThread();
			backgroundTask = task;

			progressIndicator.progressProperty().bind(task.progressProperty());
			progressLabel.textProperty().bind(task.messageProperty());

			//Automatically run beginTask/endTask before the actual task is processed
			backgroundThread = new Thread(new Runnable() {
				protected Task<Void> task;

				public Runnable setTask(Task<Void> task) {
					this.task = task;
					return this;
				}

				@Override
				public void run() {
					beginBackgroundTask();
					task.run();
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (task.getException() != null)
								ExceptionLogger.getInstance().uncaughtException(backgroundThread, task.getException());
						}
					});
					endBackgroundTask();
				}
			}.setTask(task));
			backgroundThread.setDaemon(true);
			backgroundThread.start();
		}
	}

	/**
	 * Waits for the current task to complete
	 */
	protected void completeTaskThread() {
		synchronized (this) {
			if (backgroundThread != null) {
				try {
					backgroundThread.join();
					backgroundThread = null;
				} catch (InterruptedException ex) {
					Logger.getLogger(MainWindowController.class.getName()).log(Level.SEVERE, null, ex);
					ExceptionLogger.getInstance().showException(null, ex);
				}
			}
			if (backgroundTask != null) {
				progressIndicator.progressProperty().unbind();
				progressLabel.textProperty().unbind();
				backgroundTask = null;
			}
		}
	}

	/*
	 * Callbacks
	 */
	/**
	 * Create a new task
	 */
	@FXML
	private void createNewTask() {
		TaskAdapter newTask = dataManager.createTask();
		taskList.getSelectionModel().clearSelection();
		taskList.getSelectionModel().select(newTask);
	}

	/**
	 * Delete all selected tasks
	 */
	@FXML
	private void deleteSelectedTasks() {
		StringBuilder tasksToDelete = new StringBuilder();
		List<TaskAdapter> tasksToDeleteList = new LinkedList(taskList.getSelectionModel().getSelectedItems());
		for (TaskAdapter taskToDelete : tasksToDeleteList)
			tasksToDelete.append(tasksToDelete.length() > 0 ? "\n" : "").append(taskToDelete.nameProperty().get()); //NOI18N
		ConfirmationDialogController.Result result = confirmationDialogController.showDialog(
				messages.getString("CONFIRM_TASK_DELETION"),
				MessageFormat.format(messages.getString("ARE_YOU_SURE_YOU_WANT_TO_DELETE_THE_FOLLOWING_TASKS"), tasksToDelete)
		);
		if (result == ConfirmationDialogController.Result.CONFIRMED)
			for (TaskAdapter taskToDelete : tasksToDeleteList)
				dataManager.deleteTask(taskToDelete);
	}

	/**
	 * Duplicate all selected tasks
	 */
	@FXML
	private void duplicateSelectedTasks() {
		for (TaskAdapter selectedTask : taskList.getSelectionModel().getSelectedItems()) {
			TaskAdapter newTask = dataManager.createTask();
			newTask.nameProperty().set(selectedTask.nameProperty().get());
			newTask.descriptionProperty().set(selectedTask.descriptionProperty().get());
			for (CustomFieldAdapter customField : dataManager.getCustomFields()) {
				CustomFieldValueAdapter customFieldValue = new CustomFieldValueAdapter(customField, dataManager);
				customFieldValue.setTask(newTask);
				customFieldValue.valueProperty().set(selectedTask.getTask().getCustomField(customField.getCustomField()));
			}
		}
	}

	/**
	 * Show the custom field editor window
	 */
	@FXML
	private void showCustomFieldEditor() {
		customFieldEditorStage.getIcons().setAll(((Stage) rootPane.getScene().getWindow()).getIcons());
		customFieldEditorStage.show();
	}

	/**
	 * Show the custom report window
	 */
	@FXML
	private void showReportWindow() {
		reportStage.getIcons().setAll(((Stage) rootPane.getScene().getWindow()).getIcons());
		reportStage.show();
	}

	/**
	 * Show the filter editor
	 */
	@FXML
	private void showFiltersEditor() {
		filterEditorStage.getIcons().setAll(((Stage) rootPane.getScene().getWindow()).getIcons());
		filterEditorStage.show();
	}

	/**
	 * Show the about window
	 */
	@FXML
	private void showAboutWindow() {
		aboutStage.getIcons().setAll(((Stage) rootPane.getScene().getWindow()).getIcons());
		aboutStage.show();
	}

	/**
	 * Exit the application
	 */
	@FXML
	private void exit() {
		if (shutdownProcedure != null) {
			shutdownProcedure.run();
		} else {
			//Default shutdown procedure
			completeTaskThread();
			dataManager.shutdown();
			Platform.exit();
		}
	}

	/**
	 * Import Grindstone data
	 */
	@FXML
	private void importGrindstoneData() {
		//Prepare file chooser dialog
		FileChooser fileChooser = new FileChooser();
		if (lastDirectory.get() != null && lastDirectory.get().exists())
			fileChooser.setInitialDirectory(lastDirectory.get());
		fileChooser.setTitle(messages.getString("CHOOSE_FILE_TO_IMPORT"));
		//Prepare file chooser filter
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(messages.getString("EXPORTED_GRINDSTONE_XML_FILES"), "*.xml")); //NOI18N

		//Show the dialog
		File selectedFile;
		if ((selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow())) != null) {
			lastDirectory.set(selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile());

			//Choose the importer based on the file extension
			Importer importer = null;
			String extension = selectedFile.isFile() ? selectedFile.getName().substring(selectedFile.getName().lastIndexOf(".")) : null; //NOI18N
			if (extension != null && extension.equals(".xml")) { //NOI18N
				log.fine(messages.getString("EXTENSION_MATCHED"));
				importer = new GrindstoneImporter(selectedFile);
			}
			//Import data
			if (importer != null)
				dataManager.getPersistenceHelper().importData(importer);
			else
				log.fine(messages.getString("EXTENSION_NOT_RECOGNIZED"));

			dataManager.reloadCustomFields();
			reloadTasks();
		}
	}

	/**
	 * Import XML data
	 */
	@FXML
	private void importXmlData() {
		//Prepare file chooser dialog
		FileChooser fileChooser = new FileChooser();
		if (lastDirectory.get() != null && lastDirectory.get().exists())
			fileChooser.setInitialDirectory(lastDirectory.get());
		fileChooser.setTitle(messages.getString("CHOOSE_FILE_TO_IMPORT"));
		//Prepare file chooser filter
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(messages.getString("XML_FILES"), "*.xml")); //NOI18N

		//Show the dialog
		File selectedFile;
		if ((selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow())) != null) {
			lastDirectory.set(selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile());

			//Choose the importer based on the file extension
			Importer importer = null;
			String extension = selectedFile.isFile() ? selectedFile.getName().substring(selectedFile.getName().lastIndexOf(".")) : null; //NOI18N
			if (extension != null && extension.equals(".xml")) { //NOI18N
				log.fine(messages.getString("EXTENSION_MATCHED"));
				importer = new XmlImporter(selectedFile);
			}
			//Prepare the task
			Task<Void> task = new Task<Void>() {
				private Importer importer;

				public Task<Void> setImporter(Importer importer) {
					this.importer = importer;
					return this;
				}

				@Override
				protected Void call() throws Exception {
					updateMessage(messages.getString("IMPORTING_DATA"));
					updateProgress(-1, 1);

					//Import data
					if (importer != null)
						dataManager.getPersistenceHelper().importData(importer);
					else
						log.fine(messages.getString("EXTENSION_NOT_RECOGNIZED"));

					updateProgress(1, 1);
					updateMessage(""); //NOI18N

					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							dataManager.reloadCustomFields();
							reloadTasks();
							updateSortOrder();
						}
					});
					return null;
				}
			}.setImporter(importer);
			//Run the task
			startTaskThread(task);
		}
	}

	/**
	 * Export XML data
	 */
	@FXML
	private void exportXmlData() {
		// Prepare file chooser dialog
		FileChooser fileChooser = new FileChooser();
		if (lastDirectory.get() != null && lastDirectory.get().exists())
			fileChooser.setInitialDirectory(lastDirectory.get());
		fileChooser.setTitle(messages.getString("CHOOSE_WHERE_TO_EXPORT"));
		//Prepare file chooser filter
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(messages.getString("XML_FILES"), "*.xml")); //NOI18N

		//Show the dialog
		File selectedFile;
		if ((selectedFile = fileChooser.showSaveDialog(rootPane.getScene().getWindow())) != null) {
			lastDirectory.set(selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile());

			//Append extension if needed
			String extension = selectedFile.getName().lastIndexOf(".") >= 0 ? selectedFile.getName().substring(selectedFile.getName().lastIndexOf(".")) : null; //NOI18N
			if (extension == null || extension.isEmpty())
				selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".xml"); //NOI18N

			Exporter exporter = new XmlExporter(selectedFile);
			//Prepare the task
			Task<Void> task = new Task<Void>() {
				private Exporter exporter;

				public Task<Void> setExporter(Exporter exporter) {
					this.exporter = exporter;
					return this;
				}

				@Override
				protected Void call() throws Exception {
					updateMessage(messages.getString("EXPORTING_DATA"));
					updateProgress(-1, 1);

					//Export data
					if (exporter != null)
						exporter.exportData(dataManager.getPersistenceHelper());
					else
						log.fine(messages.getString("EXTENSION_NOT_RECOGNIZED"));

					updateProgress(1, 1);
					updateMessage(""); //NOI18N
					return null;
				}
			}.setExporter(exporter);
			//Run the task
			startTaskThread(task);
		}
	}

	/**
	 * Stop timing the currently timing task
	 */
	@FXML
	private void stopTimingTask() {
		dataManager.stopTiming();
	}

	/**
	 * Perform DB cleanup functions
	 */
	@FXML
	private void cleanupDB() {
		//Prepare the task
		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				updateMessage(messages.getString("CLEANING_UP_DB"));
				updateProgress(-1, 1);

				dataManager.getPersistenceHelper().cleanupDB();

				updateProgress(1, 1);
				updateMessage(""); //NOI18N
				return null;
			}
		};
		//Run the task
		startTaskThread(task);
	}
}
