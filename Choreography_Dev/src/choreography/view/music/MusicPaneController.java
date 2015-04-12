/**
 * Sample Skeleton for "MusicPane.fxml" Controller Class
 * You can copy and paste this code into your favorite IDE
 **/

package choreography.view.music;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import SimpleJavaFXPlayer.AudioWaveformCreator;
import SimpleJavaFXPlayer.Music;
import choreography.Main;
import choreography.io.FilePayload;
import choreography.view.ChoreographyController;
import choreography.view.sim.FountainSimController;
import choreography.view.sliders.SlidersController;
import choreography.view.timeline.TimelineController;

/**
 *
 * @author elementsking
 */
public class MusicPaneController {

	private int timeFactor;

	private static MusicPaneController instance;

	private MediaPlayer mediaPlayer;
	private double time, roundedTime;
	private Duration duration;
	Music music2;
	private boolean notFirst = false;
	final DecimalFormat f = new DecimalFormat("0.0");
	public static final int H_PIXEL_SIZE = 15;
	public static final int V_PIXEL_SIZE = 15;
	public static int SONG_TIME = 0;

	@FXML
	// ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML
	// URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML
	// fx:id="colorPicker"
	private ColorPicker colorPicker; // Value injected by FXMLLoader

	@FXML
	// fx:id="musicPane"
	private VBox musicPane; // Value injected by FXMLLoader

	@FXML
	// fx:id="songName"
	private Label songName; // Value injected by FXMLLoader

	@FXML
	// fx:id="songProgress"
	private Label songProgress; // Value injected by FXMLLoader

	@FXML
	// fx:id="volume"
	private Slider volume, timeSlider; // Value injected by FXMLLoader

	@FXML
	private ScrollPane waterTimeline, timeLabel;

	@SuppressWarnings("rawtypes")
	@FXML
	private LineChart labelChart;

	@FXML
	private NumberAxis labelAxis, numberLine;

	@FXML
	private Button playButton, resetButton;

	/**
	 * Returns the instance of MusicPaneController
	 */
	public static MusicPaneController getInstance() {
		if (instance == null)
			instance = new MusicPaneController();
		return instance;
	}

	public MusicPaneController() {
		this.timeFactor = 100;
	}

	@FXML
	void resetSong(ActionEvent event) {
		resetAll();
	}

	/**
	 * Pauses all of the necessary elements when the pause button is pressed
	 * 
	 * @param event
	 */
	@FXML
	void pauseSong(ActionEvent event) {

		mediaPlayer.pause();
		ChoreographyController.getInstance().stopTimelineTimer();
		ChoreographyController.getInstance().stopSliderTimer();
		TimelineController.getInstance().fireSliderChangeEvent();
		FountainSimController.getInstance().pauseLeftSweep();
		FountainSimController.getInstance().pauseRightSweep();
	}

	/**
	 * Resumes music playback and resumes the simulation
	 * 
	 * @param event
	 */
	@FXML
	void playSong(ActionEvent event) {
		// Makes sure the music is not already playing
		if (mediaPlayer.statusProperty().getValue() == Status.PAUSED || mediaPlayer.statusProperty().getValue() == Status.STOPPED || mediaPlayer.statusProperty().getValue() == Status.READY) {
			mediaPlayer.play();
			FountainSimController.getInstance().playLeftSweep();
			FountainSimController.getInstance().playRightSweep();
			playButton.setText("Pause");
			ChoreographyController.getInstance().startPollingTimeSliderAlgorithm();
			ChoreographyController.getInstance().startPollingSimAlgorithm();
			ChoreographyController.getInstance().startPollingColorAlgorithm();
			SlidersController.getInstance().resetAllSliders();
		}
		// Calls the pause method and updates the button text
		if (mediaPlayer.statusProperty().getValue() == Status.PLAYING) {
			pauseSong(event);
			playButton.setText("Play");
		}
	}

	/**
	 * Stops music and simulation
	 * 
	 * @param event
	 */
	@FXML
	private void stopSong(ActionEvent event) {
		stopMusic();
	}

	private void stopMusic() {
		mediaPlayer.stop();
		mediaPlayer.seek(Duration.ZERO);
		timeSlider.setValue(0.0);
		TimelineController.getInstance().fireSimClearEvent();
		SlidersController.getInstance().resetAllSliders();
		FountainSimController.getInstance().clearSweeps();
		playButton.setText("Play");
	}

	/**
	 * Gets needed information from the music
	 * 
	 * @param fileChosen
	 */
	private void getAllMusic(File fileChosen) {
		music2.setName(fileChosen.getName());
		music2.setDirectoryFile(fileChosen.getAbsolutePath());
		songName.setText(music2.getName());
	}

	/**
	 * Getters for MediaPlayer, WaterTimeline and LabelPane
	 */
	public MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}

	public ScrollPane getWaterPane() {
		return waterTimeline;
	}

	public ScrollPane getLabelPane() {
		return timeLabel;
	}

	/**
	 * Opens a music file and sets up the screen
	 * 
	 * @param file
	 */
	public void selectMusic(File file) {
		// Checks to make sure music has not already been loaded
		if (notFirst) {
			mediaPlayer.dispose();
		}

		if (file != null) {
			openMusicFile(file);
			playButton.setDisable(false);
			resetButton.setDisable(false);
		} else
			songName.setText("No File Selected");
	}

    /**
     * Methods used for resetting the music pane controller
     */
	public void resetSongName() {
		songName.setText("No File Selected");
	}

	public void resetSongProgress() {
		songProgress.setText("0:00.0");
	}

	public void resetTimeLabel() {
		timeLabel.setContent(null);
	}

    /**
     * Disables the play and reset buttons
     */
	public void disablePlaybackButtons() {
		playButton.setDisable(true);
		resetButton.setDisable(true);
	}

	/**
	 * Takes in a music file and loads it into the program. It then calculates
	 * times and sets up the light timeline
	 */
	public void loadMusicFile(File file2) {
		URL url = null;
		try {
			url = file2.toURI().toURL();
		} catch (MalformedURLException ec) {
			ec.printStackTrace();
		}

		try {
			AudioWaveformCreator awc = new AudioWaveformCreator(url, "out.png");
			setTime(awc.getTime());
			DecimalFormat f = new DecimalFormat("0.0");

			roundedTime = Double.parseDouble(f.format(getTime()));
			setTime(getTimeFactor() * Double.parseDouble(f.format(getTime())));
			SONG_TIME = (int) getTime();
            System.err.println("song time"+getTime());
			TimelineController.getInstance().getTimeline().setTime(SONG_TIME);
			TimelineController.getInstance().setTimelineGridPane();
			TimelineController.getInstance().setWaterGridPane();
			//ChoreographyController.getInstance().setBeatMarkGridPane();

            numberLine.setMinWidth(SONG_TIME * 26);
            numberLine.setPrefWidth(SONG_TIME * 26);

			numberLine.setUpperBound(roundedTime);
			numberLine.setVisible(true);
            songProgress.setText("0:00.0/" + roundedTime);
		} catch (Exception ex) {

			ex.printStackTrace();
		}
		notFirst = true;
	}

    /**
     * Opens a music file given by file2
     */
	public void openMusicFile(File file2) {
		music2 = new Music();
		if (file2 != null) {
			getAllMusic(file2);
			music2.setDirectoryFile(file2.getAbsolutePath());
		}
		loadMusicFile(file2);

		String source = new File(music2.getDirectoryFile()).toURI().toString();
		Media media = new Media(source);
		mediaPlayer = new MediaPlayer(media);
		songName.setText(music2.getName());
		String delims = "[.]+";
		String[] tokens = music2.getName().split(delims);
		Main.getPrimaryStage().setTitle("GHMF Choreography Studio  -  " + tokens[0] + ".wav");
		mediaPlayer.play();
		mediaPlayer.pause();
	}

	/**
     *
     */
	public void updateProgress() {
		final DecimalFormat f = new DecimalFormat("0.0");
		try {

			//The next couple of lines are necessary because JavaFX does not return a time in min and second format so
			//we had to write a 'hack' to create it

            String timeInFormat = "";
			String finalTimeInFormat;
			String inSeconds;
			int finalInMinutes = 0;
            int inMinutes = 0;

            for(int i = 0; i < (int) mediaPlayer.getCurrentTime().toMinutes(); i++) {
                inMinutes++;
            }
			inSeconds = f.format((mediaPlayer.getCurrentTime().toSeconds() - inMinutes * 60));
			if(inSeconds.length() == 3) {
				inSeconds = "0" + inSeconds;
			}
			timeInFormat = inMinutes + ":" + inSeconds;

            for(int i = 0; i < (int) mediaPlayer.getTotalDuration().toMinutes(); i++) {
                finalInMinutes++;
            }
			String finalInSeconds = f.format((mediaPlayer.getTotalDuration().toSeconds() - finalInMinutes * 60));
			if(finalInSeconds.length() == 3) {
				finalInSeconds = "0" + finalInSeconds;
			}
			finalTimeInFormat = finalInMinutes + ":" + finalInSeconds;


			songProgress.setText(timeInFormat + " / " + finalTimeInFormat);
			duration = mediaPlayer.getMedia().getDuration();

			double totalTime = mediaPlayer.getTotalDuration().toSeconds();
			double currentTime = mediaPlayer.getCurrentTime().toSeconds();
			double percentComplete = currentTime / totalTime * 100;

            Double hValue = percentComplete;

			TimelineController.getInstance().getScrollPane().setHvalue(hValue);
			timeSlider.setValue(hValue);
			waterTimeline.setHvalue(hValue);
			//ChoreographyController.getInstance().getBeatMarkScrollPane().setHvalue(hValue);
			timeLabel.setHvalue(hValue);
		} catch (Exception e) {
			System.out.println("Error updating song progress " + e);
		}

	}

    /**
     * This method is called by the FXMLLoader when initialization is complete
     */
	@FXML
	void initialize() {
		assert colorPicker != null : "fx:id=\"colorPicker\" was not injected: check your FXML file 'MusicPane.fxml'.";
		assert musicPane != null : "fx:id=\"musicPane\" was not injected: check your FXML file 'MusicPane.fxml'.";
		assert songName != null : "fx:id=\"songName\" was not injected: check your FXML file 'MusicPane.fxml'.";
		assert songProgress != null : "fx:id=\"songProgress\" was not injected: check your FXML file 'MusicPane.fxml'.";

		timeSlider.valueProperty().addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable ov) {
				if (timeSlider.isValueChanging()) {
					// multiply duration by percentage calculated by slider
					// position
					mediaPlayer.pause();
					playButton.setText("Play");
					mediaPlayer.seek(duration.multiply(timeSlider.getValue() / 100.0));
				}
			}
		});
        timeSlider.setDisable(true);
		this.timeFactor = 10;
		instance = this;
	}

	/**
	 * @return the timeFactor
	 */
	public int getTimeFactor() {
		return timeFactor;
	}

	/**
	 * @param timeFactor
	 *            the timeFactor to set
	 */
	public void setTimeFactor(int timeFactor) {
		this.timeFactor = timeFactor;
	}

	/**
	 * @return the time
	 */
	public double getTime() {
		return time;
	}

    /**
     * @param time
     *          the time to set
     */
	public void setTime(double time) {
		this.time = time;
	}

    /**
     * @return time in tenths of a second
     */
	public int getTenthsTime() {
		double wholeTime = timeSlider.getValue() / 100 * time;
		int tenths = (int) wholeTime;
		return tenths;
	}

    /**
     * @return the timeslider
     */
	public Slider getTimeSlider() {
		return timeSlider;
	}

    /**
     * @return the name of the music file
     */
	public String getMusicName() {
		return music2.getName().substring(0, music2.getName().length() - 4).replaceAll("\\d*$", "");
	}

	/**
	 * Packages the music into a file for it to be saved into a .ghmf/zip file
	 * The only thing it does to the music is compresses it and renames it to
	 * the same name as all of the other files contained in the archive
	 * 
	 * @return
	 */
	public FilePayload createFilePayload() {
		try {
			File musicFile = new File(music2.getDirectoryFile());
			FileInputStream input = new FileInputStream(musicFile);
			int length = (int) musicFile.length();
			byte[] musicFileBytes = new byte[length];
			input.read(musicFileBytes);
			input.close();
			return new FilePayload(music2.getName(), musicFileBytes);
		} catch (FileNotFoundException ex) {
			Logger.getLogger(MusicPaneController.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(MusicPaneController.class.getName()).log(Level.SEVERE, null, ex);
		}
		throw new IllegalArgumentException("Unable to create music FilePayload");
	}

	public void disposeMusic() {
		mediaPlayer.dispose();
		notFirst = false;
	}

    /**
     * Resets the music pane
     * Stops the music and resets everything
     */
	public void resetAll() {
		stopMusic();
		timeSlider.setValue(0);

		// Clears the simulation screen
		FountainSimController.getInstance().clearSweeps();
		FountainSimController.getInstance().resetAll();
	}

    /**
     * @return the play/pause button
     */
	public Button getPlayButton() {
		return playButton;
	}

    /**
     * @param playButton
     *          The play/pause button
     */
	public void setPlayButton(Button playButton) {
		this.playButton = playButton;
	}

    /**
     * @return the reset button
     */
	public Button getResetButton() {
		return resetButton;
	}

    /**
     * @param resetButton
     *          The button used to reset the music pane
     */
	public void setResetButton(Button resetButton) {
		this.resetButton = resetButton;
	}
}
