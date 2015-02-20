package choreography.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import choreography.model.fcw.FCW;
import choreography.view.ChoreographyController;
import choreography.view.music.MusicPaneController;

/**
 *
 * This class is responsible for reading and writing CTL files in the format
 * <Time Signature><FCW>...<FCW> 
 * 
 * @author Frank Madrid
 */
public class CtlLib {

	private static CtlLib instance;

	/**
	 * Returns an instance of CtlLib. Controls instantiation and access to the
	 * class. The class is intended to be a of singleton design.
	 * 
	 * @return CtlLib
	 */
	public static synchronized CtlLib getInstance() {
		if (instance == null)
			instance = new CtlLib();
		instance.setTimeCompensated(true);
		return instance;
	}

	// Used to flag if the time is compensated within the CTL file or not
	private boolean isTimeCompensated;

	private CtlLib() {

	}

	/**
	 * Method throws up a Open File dialog to select a CTL file.
	 * Calls the openCtl method if a file was chosen.
	 * 
	 * @throws java.io.IOException
	 */
	
	
	public synchronized void openCtl() throws IOException {
		FileChooser fc = new FileChooser();
		fc.setTitle("Open CTL File");
		fc.setInitialDirectory(new File(System.getProperty("user.dir")));
		fc.getExtensionFilters().add(new ExtensionFilter("CTL Files", "*.ctl"));
		File ctlFile = fc.showOpenDialog(null);
		if (ctlFile != null) {
			openCtl(ctlFile);
		}
	}

	/**
	 * Wraps the CTL file in a buffered reader and sets the event timeline
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void openCtl(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		ChoreographyController.getInstance().setEventTimeline(parseCTL(readFile(reader)));
	}

	/**
	 * Wraps the incoming zipEntryInputStream in a buffered reader and sets
	 * event timeline
	 * 
	 * @param is
	 * @throws IOException
	 */
	public void openCtl(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		ChoreographyController.getInstance().setEventTimeline(parseCTL(readFile(reader)));
	}

	/**
	 * Reads the CTL contents from the file into memory and returns a 
	 * string for the parseCTL method to take as a parameter.
	 * 
	 * @param reader
	 *            A reader wrapped around the ctl data
	 * @return the contents of the CTL file
	 */
	public synchronized String readFile(BufferedReader reader) throws IOException {
		StringBuilder stringBuffer = new StringBuilder();

		// The first line will indicate whether or not it is a legacy file.
		// Legacy files will not have "GHMF" as the first line. If the file
		// does have "GHMF" as the first line, it will allow the ctl file
		// to be saved with time compensated (particularly water commands).
		try {
			String version = reader.readLine();
			switch (version) {

			// Time compensated file
			case "GHMF":
				isTimeCompensated = true;
				ChoreographyController.getInstance().getSaveCTLMenuItem().setDisable(false);
				break;

			// Non Time compensated file, Legacy File
			default:
				isTimeCompensated = false;
				ChoreographyController.getInstance().getSaveCTLMenuItem().setDisable(true);
				break;
			}
			String text = null;

			while ((text = reader.readLine()) != null) {
				stringBuffer.append(text);
				stringBuffer.append(System.getProperty("line.separator"));
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			reader.close();
		}

		return stringBuffer.toString();
	}

	/**
	 * Parses the input into timeIndex and an ArrayList of FCW commands.
	 * 
	 * @param input
	 *            contents of the CTL file
	 * @return A map containing <timeIndex, ArrayList<FCW>>
	 */
	public synchronized ConcurrentSkipListMap<Integer, ArrayList<FCW>> parseCTL(String input) {
		// Split file into tokens of lines
		String[] lines = input.split(System.getProperty("line.separator"));
		// Create a list to hold all events
		ConcurrentSkipListMap<Integer, ArrayList<FCW>> events = new ConcurrentSkipListMap<Integer, ArrayList<FCW>>();
		try {
			// For each line,
			for (String line : lines) {
				// Get the time signature
				String totalTime = line.substring(0, 7);
				// Get the minutes
				int minutes = (Integer.parseInt(totalTime.substring(0, 2)));
				// get the seconds
				int seconds = Integer.parseInt(totalTime.substring(3, 5));
				// get the tenths
				int tenths = Integer.parseInt(totalTime.substring(6, 7));
				// find the total time in seconds
				int totalTimeinTenthSecs = (minutes * 600) + (seconds * 10) + tenths;
				// get the commands section on the line
				String commands = line.substring(7, line.length());
				// break the commands into tokens
				String[] commandTokens = commands.split(" ");
				// create a new FCW for the command token
				FCW fcw = null;
				ArrayList<FCW> fcws = new ArrayList<>();
				for (String command : commandTokens) {
					String[] tokens = command.split("-");
					fcw = new FCW(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));

					fcws.add(fcw);
				}
				events.put(totalTimeinTenthSecs, fcws);
			}
			// checks to see if time is to be compensated based on checking the first line in the file.
			// If it is, call reversePostDate to "reverse" the modification by moving up the water
			// commands by the desired amount.
			if (isTimeCompensated) {
				events = (ConcurrentSkipListMap<Integer, ArrayList<FCW>>) reversePostDate(events);
			}
		} catch (StringIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Your CTL file may be corrupted..." + " Check the manual.");
		}
		return events;
	}

	/**
	 * Writes the CTL data from memory into a file.
	 * 
	 * @param file
	 * @param content
	 * @return
	 */
	public synchronized boolean saveFile(File file, SortedMap<Integer, ArrayList<FCW>> content) {
		if (isTimeCompensated) {
			try (FileWriter fileWriter = new FileWriter(file)) {
				// actual call to the creation of the content to write to file is here
				StringBuilder commandsOutput = createCtlData(content);
				fileWriter.write(commandsOutput.toString());
				return true;
			} catch (IOException ex) {
				ex.printStackTrace();
				return false;
			}
		}
		return false;

	}

	/**
	 * Puts a version header at the top of the file. Iterates through the
	 * timeline and builds lines of <MM:SS.T000-000 111-111 222-222>
	 * 
	 * @param content
	 *            the timeline you want to save
	 * @return a string holding that data in the ctl format
	 * @throws IOException
	 */
	private StringBuilder createCtlData(SortedMap<Integer, ArrayList<FCW>> content) throws IOException {
		StringBuilder commandsOutput = new StringBuilder();
		//note that saving is disabled for legacy files, so saving will always need the
		//GHMF line at the top to indicate that the lag time will be compensated.
		commandsOutput.append("GHMF");
		commandsOutput.append(System.lineSeparator());
		for (Integer timeIndex : content.keySet()) {
			Iterator<FCW> it = content.get(timeIndex).iterator();
			while (it.hasNext()) {
				FCW f = it.next();
				// if it is a water command, save it with a "lag time" using postDateSingelFcw
				if (f.getIsWater()) {
					if (postDateSingleFcw(f, content, timeIndex)) {
						it.remove();
					}
				}
				if (content.get(timeIndex).isEmpty()) {
					content.remove(timeIndex);
				}
			}
		}

		//this part converts the data into actual data that can be saved.
		//i.e. MM:SS.sADR-DDD ADR-DDD.......ADR-DDD
		for (Iterator<Integer> it = content.keySet().iterator(); it.hasNext();) {
			Integer i = it.next();
			String totTime = "";
			int timeIndex = i;
			if (i < 0) {
				totTime = "-";
				timeIndex = Math.abs(i);
			}
			int tenths = Math.abs(timeIndex % 10);
			int seconds = Math.abs(timeIndex / 10 % 60);
			int minutes = Math.abs(((timeIndex / 10) - seconds) / 60);
			totTime += String.format("%1$02d:%2$02d.%3$01d", minutes, seconds, tenths);
			commandsOutput.append(totTime);
			for (FCW f : content.get(i)) {
				commandsOutput.append(f);
				commandsOutput.append(" ");
			}
			commandsOutput.append(System.lineSeparator());
		}
		return commandsOutput;
	}
	
	/**
	 * post dates a single FCW by lag time
	 * 
	 * @param f
	 *            the FCW
	 * @param content
	 *            the timeline
	 * @param timeIndex
	 *            the point at which the fcw currently exists
	 * @return whether the fcw was moved or not
	 */
	
	//NOTE - DOES NOT WORK CURRENTLY AS IT DOES NOT GET THE CORRECT LAG TIME IN TENTHS.
	//       THE ERROR IS IN LAGTIMELIBRARY
	public synchronized boolean postDateSingleFcw(FCW f, SortedMap<Integer, ArrayList<FCW>> content, Integer timeIndex) {
		int lag = LagTimeLibrary.getInstance().getLagTimeInTenths(f);
		int adjustedTime = timeIndex - lag;
		if (adjustedTime >= 0) {
			if (lag != 0) {
				if (content.containsKey(timeIndex - lag)) {
					content.get(timeIndex - lag).add(f);
				} else {
					content.put(timeIndex - lag, new ArrayList<FCW>());
					content.get(timeIndex - lag).add(f);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * moves FCWs forward when opening a file.
	 * 
	 * 
	 * @param content
	 *            the timeline to adjust
	 * @return a lag time - modified version of the timeline
	 */
	//NOTE - DOES NOT WORK CURRENTLY AS IT DOES NOT GET THE CORRECT LAG TIME IN TENTHS.
	//       THE ERROR IS IN LAGTIMELIBRARY
	private SortedMap<Integer, ArrayList<FCW>> reversePostDate(SortedMap<Integer, ArrayList<FCW>> content) {

		SortedMap<Integer, ArrayList<FCW>> results = new ConcurrentSkipListMap<Integer, ArrayList<FCW>>();
		for (Integer timeIndex : content.keySet()) {
			Iterator<FCW> it = content.get(timeIndex).iterator();
			while (it.hasNext()) {
				FCW f = it.next();
				// if the FCW is a water event, it needs to be adjusted before added
				// to results.
				if (f.getIsWater()) {
					int lag = LagTimeLibrary.getInstance().getLagTimeInTenths(f);

					if (lag != 0) {
						if (results.containsKey(timeIndex + lag)) {
							results.get(timeIndex + lag).add(f);
						} else {
							results.put(timeIndex + lag, new ArrayList<FCW>());
							results.get(timeIndex + lag).add(f);
						}
					}
				// otherwise, just add it to results (creating an arraylist if necessary)
				} else {
					if (results.containsKey(timeIndex)) {
						results.get(timeIndex).add(f);
					} else {
						results.put(timeIndex, new ArrayList<FCW>());
						results.get(timeIndex).add(f);
					}
				}
			}
		}
		return results;
	}

	/**
	 * @return whether or not the time is compensated for the current ctl file
	 */
	public boolean isTimeCompensated() {
		return isTimeCompensated;
	}

	/**
	 * 
	 * @param isTimeCompensated whether time should be compensated (non-legacy files)
	 */
	public void setTimeCompensated(boolean isTimeCompensated) {
		this.isTimeCompensated = isTimeCompensated;
	}

	/**
	 * Creates a FilePayload for output as a ZipEntry
	 * 
	 * @param timeline
	 * @return FilePayload(songName, ctlData) containing the CTL data
	 * @throws IOException
	 */
	public FilePayload createFilePayload(SortedMap<Integer, ArrayList<FCW>> timeline) throws IOException {
		StringBuilder sb = createCtlData(timeline);
		return new FilePayload(MusicPaneController.getInstance().getMusicName() + ".ctl", sb.toString().getBytes());
	}

    /**
     * Creates a new file from a ctl file that is commented after each line. The comments describe what the fountain
     * should be doing in english. The comments are surrounded by parentheses (such as this) .
     *
     *
     * Returns:
     *  1 --> Commented file created successfully
     *  0 -->
     */

    public int commentCtlFile(){
        try {
            ConcurrentSkipListMap<Integer, ArrayList<FCW>> myFCWS = openCtlForComment();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 1;
    }

    public synchronized ConcurrentSkipListMap<Integer, ArrayList<FCW>> openCtlForComment() throws IOException {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open CTL File");
        fc.setInitialDirectory(new File(System.getProperty("user.dir")));
        fc.getExtensionFilters().add(new ExtensionFilter("CTL Files", "*.ctl"));
        File ctlFile = fc.showOpenDialog(null);
        //Create buffer --> read the file --> parse the file
        if (ctlFile != null) return parseCTL(readFile(createCtlCommentBuffer(ctlFile)));

        return null;
    }

    public BufferedReader createCtlCommentBuffer(File file) throws IOException {
        return  new BufferedReader(new FileReader(file));
    }
}
