/*
 * Awesome Time Tracker project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.att.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import net.sf.dynamicreports.report.exception.DRException;
import org.zlogic.att.data.reporting.DateTools;
import org.zlogic.att.ui.adapters.DataManager;
import org.zlogic.att.ui.report.Report;

/**
 * Controller for the report window
 *
 * @author Dmitry Zolotukhin <a
 * href="mailto:zlogic@gmail.com">zlogic@gmail.com</a>
 */
public class ReportController {

	/**
	 * The logger
	 */
	private final static Logger log = Logger.getLogger(ReportController.class.getName());
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
	private ObjectProperty<File> lastDirectory;
	/**
	 * Report preview browser
	 */
	@FXML
	private WebView viewer;
	/**
	 * Start date field
	 */
	@FXML
	private DatePicker startDate;
	/**
	 * End date field
	 */
	@FXML
	private DatePicker endDate;
	/**
	 * Status pane
	 */
	@FXML
	private HBox statusPane;
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
	 * Build report button
	 */
	@FXML
	private Button buildReportButton;
	/**
	 * Preview report label pane
	 */
	@FXML
	private HBox previewReportPane;
	/**
	 * Save report button pane
	 */
	@FXML
	private HBox savePane;
	/**
	 * Save report button
	 */
	@FXML
	private Button saveReportButton;
	/**
	 * Report generation task thread
	 */
	private ObjectProperty<Thread> reportTaskThread = new SimpleObjectProperty<>();
	/**
	 * Generated report
	 */
	private ObjectProperty<Report> generatedReport = new SimpleObjectProperty<>();//TODO: destroy on close

	/**
	 * Initializes the controller
	 *
	 * @param url initialization URL
	 * @param resourceBundle supplied resources
	 */
	@FXML
	public void initialize() {
		//Configure background task
		statusPane.managedProperty().bind(statusPane.visibleProperty());
		statusPane.visibleProperty().bind(reportTaskThread.isNotNull());
		buildReportButton.disableProperty().bind(reportTaskThread.isNotNull());

		//Configure preview & save panes
		previewReportPane.managedProperty().bind(previewReportPane.visibleProperty());
		previewReportPane.visibleProperty().bind(generatedReport.isNotNull());
		savePane.managedProperty().bind(savePane.visibleProperty());
		savePane.visibleProperty().bind(generatedReport.isNotNull());
		saveReportButton.disableProperty().bind(generatedReport.isNull());

		//Configure dates
		startDate.setValue(DateTools.getInstance().convertDateToStartOfMonth(LocalDate.now()));
		endDate.setValue(DateTools.getInstance().convertDateToEndOfMonth(LocalDate.now()));
	}

	/**
	 * Sets the DataManager reference
	 *
	 * @param dataManager the DataManager reference
	 */
	public void setDataManager(DataManager dataManager) {
		this.dataManager = dataManager;
		//Destroy report on stage close
		viewer.getScene().getWindow().showingProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
				if (oldValue && !newValue) {
					generatedReport.set(null);
					viewer.getEngine().load("about:blank"); //NOI18N
				}
			}
		});
	}

	/**
	 * Sets the last directory property
	 *
	 * @param lastDirectory the last directory property
	 */
	public void setLastDirectory(ObjectProperty<File> lastDirectory) {
		this.lastDirectory = lastDirectory;
	}

	/**
	 * Builds the report
	 */
	@FXML
	private void buildReport() {
		if (reportTaskThread.getValue() != null)
			return;
		//TODO: ExecutorService?
		Task reportTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				updateMessage(messages.getString("GENERATING_REPORT..."));
				Report report = new Report(dataManager);
				report.setStartDate(startDate.getValue());
				report.setEndDate(endDate.getValue());
				report.progressProperty().addListener(new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
						updateProgress(newValue.doubleValue(), 1);
					}
				});
				report.buildReport();
				Platform.runLater(new Runnable() {
					private Report report;

					public Runnable setReport(Report report) {
						this.report = report;
						return this;
					}

					@Override
					public void run() {
						generatedReport.set(report);
						viewer.getEngine().loadContent(generatedReport.get().getReportHTML());
					}
				}.setReport(report));
				//updateProgress(-1, 1);
				return null;
			}
		};

		progressIndicator.progressProperty().bind(reportTask.progressProperty());
		progressLabel.textProperty().bind(reportTask.messageProperty());
		reportTask.runningProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
				if (!newValue && newValue != oldValue) {
					try {
						reportTaskThread.get().join();
						reportTaskThread.set(null);
					} catch (InterruptedException ex) {
						log.log(Level.SEVERE, null, ex);
						ExceptionLogger.getInstance().showException(null, ex);
					}
				}
			}
		});
		reportTaskThread.set(new Thread(reportTask));
		reportTaskThread.get().setDaemon(true);
		reportTaskThread.get().start();
	}

	/**
	 * Saves the report
	 */
	@FXML
	void saveReport() {
		if (generatedReport.get() == null)
			return;
		// Prepare file chooser dialog
		FileChooser fileChooser = new FileChooser();
		if (lastDirectory.get() != null && lastDirectory.get().exists())
			fileChooser.setInitialDirectory(lastDirectory.get());
		fileChooser.setTitle(messages.getString("CHOOSE_WHERE_TO_SAVE_THE_REPORT"));
		//Prepare file chooser filter
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(messages.getString("PDF_FILES"), "*.pdf")); //NOI18N

		//Show the dialog
		File selectedFile;
		if ((selectedFile = fileChooser.showSaveDialog(savePane.getScene().getWindow())) != null) {
			lastDirectory.set(selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile());

			//Set extension if necessary
			String extension = selectedFile.getName().contains(".") ? selectedFile.getName().substring(selectedFile.getName().lastIndexOf(".")) : null; //NOI18N
			if (extension == null || extension.isEmpty())
				selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".pdf"); //NOI18N
			try {
				generatedReport.get().savePdfReport(selectedFile);
			} catch (FileNotFoundException | DRException ex) {
				log.log(Level.SEVERE, null, ex);
				ExceptionLogger.getInstance().showException(null, ex);
			}
		}
	}
}
