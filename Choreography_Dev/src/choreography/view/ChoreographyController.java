/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package choreography.view;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog.Actions;
import org.controlsfx.dialog.Dialogs;

import choreography.Main;
import choreography.io.CtlLib;
import choreography.io.FCWLib;
import choreography.io.FilePayload;
import choreography.io.GhmfLibrary;
import choreography.io.LagTimeLibrary;
import choreography.io.MapLib;
import choreography.io.MarkLib;
import choreography.model.color.ColorPaletteModel;
import choreography.model.fcw.FCW;
import choreography.view.colorPalette.ColorPaletteController;
import choreography.view.customChannel.CustomChannel;
import choreography.view.lagtime.LagTimeGUIController;
import choreography.view.music.MusicPaneController;
import choreography.view.sim.FountainSimController;
import choreography.view.sliders.SlidersController;
import choreography.view.specialOperations.SpecialoperationsController;
import choreography.view.timeline.TimelineController;
import choreography.io.CTLUpdater;

/**
 * FXML Controller class
 *
 * @author elementsking
 */
public class ChoreographyController implements Initializable {

	public static final String WORKINGDIRECTORY = "";

	private static ChoreographyController cc;
	private ConcurrentSkipListMap<Integer, ArrayList<FCW>> events;
	private File saveLocation;
	private boolean isSaved, isAdvanced, isSelected = false, lookUp = true, toggleSimulation = true, shiftPressed = false;
	private int time;
	Timer timelineTimer = new Timer("progressTimer", true);
	Timer sliderTimer = new Timer("progressTimer", true);

	@FXML
	private VBox csGUI, vboxParent;
	@FXML
	private Label fcwOutput;
	@FXML
	private Menu fileMenu, openRecentMenuItemItem;
	@FXML
	private MenuItem newItemMenuItem, openMusicMenuItem, closeMenuItem, saveCTLMenuItem, saveMenuItem, saveAsMenuItem,
			revertMenuItem, advancedCheckMenuItem, quitMenuItem, addChannelsMenuItem, undoMenuItem, redoMenuItem,
			cutMenuItem, copyMenuItem, pasteMenuItem, deleteMenuItem, selectAllMenuItem, unselectAllMenuItem,
			aboutMenuItem, setLagTimesMenuItem, openGhmfMenuItem, splitSimulationMenuItem, showSimulationMenuItem,
			updateCtlMenuItem;
	@FXML
	private MenuItem openCTLMenuItem;
	@FXML
	private Menu editMenu, helpMenu;
	@FXML
	private ToggleButton selectionButton;
	@FXML
	private Pane simPane;
	
	/**
	 * Initializes the controller class.
	 * 
	 * @param url
	 * @param rb
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/**
		 * Detaches simulator from main view, displays it in a new window. 
		 */
		splitSimulationMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {

				if (lookUp) {
					lookUp();
					lookUp = false;
				}
				removeSimulation();

				Stage simStage = new Stage();
				simStage.setTitle("Simulation");
				simStage.setScene(new Scene(simPane, 1320, 250));
				simStage.show();
				splitSimulationMenuItem.setDisable(true);
				showSimulationMenuItem.setDisable(true);

				simStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
					public void handle(WindowEvent we) {
						splitSimulationMenuItem.setDisable(false);
						showSimulationMenuItem.setDisable(false);
						addSimulation();
					}
				});
			}
		});

        /**
         * Updates the CTL file according to outdated_op_codes.txt
         */
		updateCtlMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				updateCtlMenuItem.setDisable(false);
				System.out.println("got the update ctl menu Item");
                /*CTLUpdater app2 = CTLUpdater.newInstance();
                Stage anotherStage = new Stage();
                app2.start(anotherStage);
                */
                CTLUpdater.updateCTL();
			}
		});

		/**
		 * Menu option to hide or display the simulator on the main view. 
		 */
		showSimulationMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {

				if (lookUp) {
					lookUp();
					lookUp = false;
				}

				if (toggleSimulation) {
					removeSimulation();

				} else {
					addSimulation();
				}
			}

		});

        /**
         * Loads a music file selected from file chooser.
         * Initializes timeline for selected music file, then loads
         * ctl file with same name as music file.
         *
         * If ctl file does not exist, creates new ctl file.
         */
        openMusicMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                fcwOutput.setText("Loading music file ...");
				FileChooser fc = new FileChooser();
				fc.setTitle("Open Music");
				fc.setInitialDirectory(new File(System.getProperty("user.dir")));
				fc.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Music Files", "*.wav"));
				File file2 = fc.showOpenDialog(null);
				MusicPaneController.getInstance().selectMusic(file2);
                TimelineController.getInstance().disposeTimeline();
				TimelineController.getInstance().initializeTimelines();

                String ctl = (file2.getPath().substring(0, (file2.getPath().length() - 3))) + "ctl";
                File file3 = new File(ctl);
                if(!file3.exists()){
                    try {
                        file3.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                fcwOutput.setText("Loading CTL file ...");
                try {
                    loadDefaultMap();
                    CtlLib.getInstance().openCtl(file3);
                    cc.setfcwOutput("CTL file has loaded!");
                    SpecialoperationsController.getInstance().initializeSweepSpeedSelectors();
                } catch (IOException ex) {
                    Logger.getLogger(ChoreographyController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NullPointerException e) {

                } finally {
                    fcwOutput.setText("Choreographer has loaded!") ;
                    SlidersController.getInstance().enableAllSliders();
                    MusicPaneController.getInstance().getTimeSlider().setDisable(false);
                }
            }
        });

		selectionButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (isSelected) {
					isSelected = false;
					TimelineController.getInstance().clearAllAL();
					TimelineController.getInstance().disableCopyPaste();
				} else {
					isSelected = true;
				}

			}

		});

		/**
		 * Turns on advanced features. 
		 * TODO Password for access to these features not yet implemented 
		 */
		advancedCheckMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				Integer[] advancedOnlyLightNames = FCWLib.getInstance().getAdvancedLightNames();
				TimelineController.getInstance().setLabelGridPane(advancedOnlyLightNames);
				TimelineController.getInstance().setTimelineGridPane();
				TimelineController.getInstance().rePaintLightTimeline();
			}
		});

		/**
		 * When quit is clicked, displays confirmation dialog, then prompts user to save. 
		 */
		quitMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				Action result = Dialogs.create().title("Quit?").masthead("").message("Are you sure you want to quit?").showConfirm();
				if (result != Actions.YES) {
				} else {
					if (isSaved) {
						Platform.exit();
					} else {
						Action saveResult = Dialogs.create().title("Save?").masthead("You haven't saved before exiting.").message("Would you like to save before quiting?").showConfirm();
						if (saveResult == Actions.YES) {
							saveAsMenuItem.getOnAction().handle(t);
						} else if (saveResult == Actions.NO) {
							Platform.exit();
						}
					}
				}
			}
		});

        /**
         * Saves the CTL file
         */
		saveCTLMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				FileChooser fc = new FileChooser();
				fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("ctl", "*.ctl"));
				fc.setInitialDirectory(new File(System.getProperty("user.dir")));
				saveLocation = fc.showSaveDialog(null);
				if (saveLocation != null) {
					String filePath = saveLocation.getAbsolutePath();
					// Check if save file has .ctl extension to prevent extra .ctl from being appened to file name
					if(filePath.contains(".ctl")){
						saveLocation = new File(saveLocation.getAbsoluteFile() + "");
					} else {
						saveLocation = new File(saveLocation.getAbsoluteFile() + ".ctl");
					}
					isSaved = true;
				} else {
					return; // User closed file chooser without saving. Prevent a null pointer exception being thrown on next line
				}
				CtlLib.getInstance().saveFile(saveLocation, TimelineController.getInstance().getTimeline().getTimeline());
			}
		});

        /**
         * If the project is saved, attempts to save as a GHMF zip file
         * else calls save as
         */
		saveMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				if (isSaved) {
					saveGhmfZipFile();
				} else {
					saveAsMenuItem.getOnAction().handle(t);
				}
			}
		});

        /**
         * Saves the project as a GHMF zip file at location of user's choice
         */
		saveAsMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				saveLocation = selectSaveLocation();
				saveGhmfZipFile();

			}
		});
		
		/**
		 * TODO
		 * 
		 * Not yet implemented
		 */
		// setLagTimesMenuItem.setOnAction(new EventHandler<ActionEvent>() {
		//
		// @Override
		// public void handle(ActionEvent t) {
		//// openLagTimeDialog();
		// }
		// });

		/**
		 * When the New menu option is clicked, it launches a new window of the
		 * choreographer. 
		 * 
		 * TODO Optionally, we can set the currently opened window to close, by calling
		 * the File->Quit code, then calling System.exit(). 
		 */
		newItemMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				StringBuilder cmd = new StringBuilder();
				cmd.append(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java ");
				for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
					cmd.append(jvmArg + " ");
				}
				cmd.append("-cp ").append(ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");
				cmd.append(Main.class.getName()).append(" ");

				try {
					Runtime.getRuntime().exec(cmd.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}


				// System.exit(0); If we don't want previous window to remain open, call this line.
			}

		});

		/**
		 * Event handler for shift key. While shift key is pressed, the
		 * boolean flag shiftPressed is set to true
		 */
		csGUI.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent e) {
				if (e.getCode() == KeyCode.SHIFT) {
					shiftPressed = true;
				}
			}
		});

		/**
		 * Event handler for shift key. While shift key is not pressed, the
		 * boolean flag shiftPressed is set to false
		 */
		csGUI.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent e) {
				if (e.getCode() == KeyCode.SHIFT) {
					shiftPressed = false;
				}
			}
		});

		events = new ConcurrentSkipListMap<>();
		fcwOutput.setText("Choreographer has loaded!");
		cc = this;
	}

	/**
	 * Called to display/hide simulation window
	 */
	private void lookUp() {
		Scene scene = Main.getPrimaryStage().getScene();
		simPane = (Pane) scene.lookup("#simPane");
		vboxParent = (VBox) scene.lookup("#vboxParent");
	}

	/**
	 * Called by split simulation menu option. Detaches the simulation display
	 * from the main choreography window. 
	 */
	private void removeSimulation() {
		vboxParent.getChildren().remove(simPane);
		toggleSimulation = false;
		vboxParent.setPrefHeight(500);
		Main.getPrimaryStage().setHeight(580);
		showSimulationMenuItem.setText("Show Simulation");
	}

	/**
	 * Called by menu option, re-attaches the simulation window to the main
	 * choreography window. 
	 */
	private void addSimulation() {
		vboxParent.getChildren().add(0, simPane);
		toggleSimulation = true;
		vboxParent.setPrefHeight(770);
		Main.getPrimaryStage().setHeight(840);
		showSimulationMenuItem.setText("Hide Simulation");
	}

	/**
	 * Opens the initial default color palate when launched
	 */
	public void loadDefaultMap() {
		boolean isMap = MapLib.isMapLoaded();
		if (!isMap) {
			MapLib.openMap(getClass().getResourceAsStream("/resources/default.map"));
		}
	}

	/**
	 * If a legacy ctl file is loaded, it should be read only. 
	 * This method will disable editing. 
	 * 
	 * TODO Untested. 
	 */
	public void killFeaturesOnLegacy() {
		SpecialoperationsController.getInstance().killSpecialOpsPane();
		SlidersController.getInstance().killSlidersPane();
		// TODO ColorPaletteModel.getInstance().setClassicColors(true);
		ColorPaletteController.getInstance().rePaint();
	}

	/**
	 * Save as a zipped file
	 * 
	 * TODO Not fully working yet.
	 */
	private void saveGhmfZipFile() {
		try {
			// if(ColorPaletteModel.getInstance().isClassicColors()) {
			// Dialogs.create()
			// .message("It is currently impossible to save legacy files.")
			// .title("Cannot Save Legacy CTL")
			// .showError();
			// }
			FilePayload ctl = CtlLib.getInstance().createFilePayload(TimelineController.getInstance().getTimeline().getTimeline());
			FilePayload map = MapLib.createFilePayload();
			FilePayload music = MusicPaneController.getInstance().createFilePayload();
			//FilePayload marks = MarkLib.createFilePayload();
			isSaved = GhmfLibrary.writeGhmfZip(saveLocation, ctl, map, music);
		} catch (IOException ex) {
			Logger.getLogger(ChoreographyController.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private File selectSaveLocation() {
		FileChooser fc = new FileChooser();
		fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("GHMF", "*.ghmf"));
		fc.setInitialDirectory(new File(System.getProperty("user.dir")));
		saveLocation = fc.showSaveDialog(null);
		if (saveLocation != null) {
			saveLocation = new File(saveLocation.getAbsoluteFile() + ".ghmf");
			isSaved = true;
			return saveLocation;
		}
		return null;
	}

	/**
	 * TODO
	 * Not yet implemented
	 */
	public boolean openLagTimeDialog() {
		try {
			// Load the fxml file and create a new stage for the popup
			FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/lagtime/LagTimeGUI.fxml"));
			GridPane page = (GridPane) loader.load();
			Stage dialogStage = new Stage();
			dialogStage.setTitle("Edit Lag Times");
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initOwner(Main.getPrimaryStage());
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// Set the lagtimes into the controller
			LagTimeGUIController controller = loader.getController();
			controller.setDialogStage(dialogStage);
			controller.setDelays(LagTimeLibrary.getInstance().getLagTimes());

			// Show the dialog and wait until the user closes it
			dialogStage.showAndWait();
			return true;

		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Updates fcWOutput label on view 
	 */
	public void setfcwOutput(String s) {
		fcwOutput.setText(s);
	}

	/**
	 * When Add Channels menu option clicked, displays window
	 * to select channels to add. 
	 * 
	 * TODO The CustomChannel class
	 * needs to be worked on for this to function properly. 
	 */
	public void addChannels() {
		Stage primaryStage = new Stage();
		CustomChannel.start(primaryStage);
	}

	/**
	 * Returns the current instance of this class. Called by other
	 * classes so they may access this class's methods. 
	 */
	public static ChoreographyController getInstance() {
		return cc;
	}

	/**
	 * Sets the event Timeline 
	 * 
	 * @param parsedCTL
	 */
	public void setEventTimeline(ConcurrentSkipListMap<Integer, ArrayList<FCW>> parsedCTL) {
		events.putAll(parsedCTL);
		TimelineController.getInstance().setTimeline(parsedCTL);
		TimelineController.getInstance().setLabelGridPaneWithCtl();
		TimelineController.getInstance().rePaint();
	}

	/**
	 * Returns the event Timeline
	 */
	public SortedMap<Integer, ArrayList<FCW>> getEventTimeline() {
		return events;
	}

	/**
	 * Called to set access to advanced user settings 
	 */
	public void setAdvanced(boolean b) {
		isAdvanced = b;
	}

	/**
	 * Returns true if advanced user settings have been enabled 
	 */
	public boolean getAdvanced() {
		return isAdvanced;
	}

	/**
	 * Updates timeSlider in MusicPaneController every 1/8th of a second
	 */
	public void startPollingTimeSliderAlgorithm() {

		sliderTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				Platform.runLater(() -> {
					MusicPaneController.getInstance().updateProgress();
				});
			}
		}, 0l, 125l);
	}

	/**
	 * Draws SIM and sets sliders every 10th of a second
	 */
	public void startPollingSlidersAlgorithm() {

		timelineTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				Platform.runLater(() -> {
					//TimelineController.getInstance().fireSubmapToSim();
					//TimelineController.getInstance().fireSliderChangeEvent();
				});
			}
		}, 0l, 20000l);
	}

	public void startPollingSimAlgorithm() {

		timelineTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				Platform.runLater(() -> {
					// TimelineController.getInstance().fireSubmapToSim();
					TimelineController.getInstance().fireSimChangeEvent();
					// FountainSimController.getInstance().drawSim(MusicPaneController.getInstance().getTenthsTime());
				});
			}
		}, 0l, 100l);
	}

	public void startPollingColorAlgorithm() {

		timelineTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				Platform.runLater(() -> {

                    //Update update colors with a second parameter. This parameter should be time in seconds which is already kept treack of in this class.
                    // The time passed will allow
					TimelineController.getInstance().updateColors(MusicPaneController.getInstance().getTenthsTime());
				});
			}
		}, 0l, 100l);
	}

	/**
	 * Returns true if select button is pressed
	 */
	public boolean getIsSelected() {
		return isSelected;
	}

	/**
	 * Returns true if shift key is pressed
	 */
	public boolean getShiftPressed() {
		return shiftPressed;
	}

	/**
	 * Pauses the timeline when called from other classes
	 */
	public void stopTimelineTimer() {
		timelineTimer.purge();
	}

	/**
	 * Pauses sliders when called from other classes
	 */
	public void stopSliderTimer() {
		sliderTimer.purge();
	}

	/**
	 * Helper method for opening a custom color map
	 */
	public void openMapFileMenuItemHandler() {
		try {
			MapLib.openMap();
		} catch (FileNotFoundException ex) {
			Dialogs.create().title("Invalid MAP file").message("You've selected an invalid MAP file. " + "Please try again.").showError();
			Logger.getLogger(ChoreographyController.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * For loading a ghmf file
	 * 
	 * TODO Not yet tested
	 */
	@FXML
	public void openGhmfFile(ActionEvent event) {
		GhmfLibrary.openGhmfFile();
	}

	@FXML
	/**
	 * Displays information dialog about the software 
	 */
	public void aboutDialogueBox() {
		Dialogs.create()
				.title("About GHMF Choreography Studio")
				.message("Grand Valley State University Senior Project" + System.lineSeparator() + "Information Systems, Winter 2014" + System.lineSeparator() + "Computer Science, Fall 2014" + System.lineSeparator() + System.lineSeparator() + "This software is used to create light shows for the Grand Haven "
								+ "Musical Fountain located in Grand Haven, Michigan.  ").masthead("About").showInformation();
	}

	@FXML
	/**
	 * Creates the view for the user manual page in 
	 * a browser 
	 */
	public void userManual() {
		Stage stage = new Stage();
		Scene scene;
		
		// Names the browser window
		stage.setTitle("Help - User Manual");

		MyBrowser myBrowser = new MyBrowser();
		scene = new Scene(myBrowser, 800, 600);

		stage.setScene(scene);
		
		// Opens the browser
		stage.show();
	}

	/**
	 * Displays the user manual page 
	 */
	class MyBrowser extends Region {

		final String userManualHtml = "User_Manual_v10.htm";

		WebView webView = new WebView();
		WebEngine webEngine = webView.getEngine();

		public MyBrowser() {
			// Points to the location of the htm file for the manual
			URL urlHello = getClass().getResource("/resources/User_Manual_v10.htm");
			webEngine.load(urlHello.toExternalForm());
			// Adds the browser to the scene
			getChildren().add(webView);
		}
	}

	public MenuItem getSaveCTLMenuItem() {
		return saveCTLMenuItem;
	}

	public void setSaveCTLMenuItem(MenuItem saveCTLMenuItem) {
		this.saveCTLMenuItem = saveCTLMenuItem;
	}
}