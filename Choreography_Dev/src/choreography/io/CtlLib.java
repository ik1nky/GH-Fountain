package choreography.io;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Map;
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

                //Regex selects any string between two parentheses. Includes the parentheses in the selection
                String[] commentTokens = commands.split("\\((.*?)\\)");
                for(String s : commentTokens)
                {
                    System.out.println("Comment: " + s);
                }

                //Remove comments so they are not included in the fcws. FCWs and comments will still be linked via time.
                commands.replaceAll("\\((.*?)\\)","");
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
     *  0 --> Invalid Ctl file provided
     */

    public int commentCtlFile(){
        String myFCWs = "";
        String[] lines;

        try {
             myFCWs = openCtlForComment();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        lines = myFCWs.split(System.getProperty("line.separator"));

        //i is equal to 1 when the ctl is legacy and 0 when it is modern
        for(int i = 1; i < lines.length;i++)
        {
            lines[i] += " " + buildComment(lines[i]);
        }

        try (PrintWriter out = new PrintWriter("auto-comment.ctl")) {
            for(String s : lines) {
                out.println(s);
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for(String s: lines){
            System.out.println(s);
        }
        return 1;
    }

    //i.e. MM:SS.sADR-DDD ADR-DDD.......ADR-DDD
    public String buildComment(String fcw)
    {
        String comment = "(";
        Map<String,String> opCodes = makeFCWHash();
		Map<String, String> data = makeDataLookupHash();

        //Strip the time signature MM:SS.s (First 7 characters)
        //comment += "Minutes: " + fcw.substring(0,2) + " Seconds: " + fcw.substring(3,5) + "." + fcw.charAt(6);
        fcw = fcw.substring(7);

        //Find how many FCWs are in the line
        int count = countFCWs(fcw);
        if(count == 0)
            return comment + " No commands provided)";
        else if(count > 10)
            return comment + " More than ten commands provided: " + count + ")";

        //Get 1 to 10 commands
        //Strip each command  and trailing space after it is added to the comment
        for(int i = 0; i < count; i++)
        {
            comment += "\t\t" + opCodes.get(fcw.substring(0,3));
            //System.out.println("Opcode: " + fcw.substring(0,3) + "\t\tMessage: " + opCodes.get(fcw.substring(0,3)));

            if(fcw.length() > 7)
                fcw = fcw.substring(8);
            else
                return comment + ")";
        }
        return comment + ")";
    }

	public String buildDataComment(String data, String table)
	{
		String comment = "(";
		Map<String, String> dataMap;

		int num = Integer.parseInt(data);

		int[] dataNums;

		switch (table) {
			case "A1": dataMap = makeA1Hash();
				break;
			case "A2": dataMap = makeA2Hash();
				break;
			case "B": dataMap = makeBHash();
				break;
			case "C1": dataMap = makeC1Hash();
				break;
			case "C2": dataMap = makeC2Hash();
				break;
			case "D1": dataMap = makeD1Hash();
				break;
			case "D2": dataMap = makeD2Hash();
				break;
			case "D3": dataMap = makeD3Hash();
				break;
			case "E": dataMap = makeEHash();
				break;
			case "F": dataMap = makeFHash();
				break;
			case "G": dataMap = makeGHash();
				break;
			case "H": dataMap = makeHHash();
				break;
			case "I": dataMap = makeIHash();
				break;
			case "J": dataMap = makeJHash();
				break;
		}

		return comment;
	}

    public int countFCWs(String myFCW)
    {
        int count = 0;
        String staticMyFCW = myFCW;


        //Increments by seven as an FCW is seven characters
        for(int i = 0; i < myFCW.length(); i+=7) {
            //FCWs are separated by a blank space
            if(myFCW.charAt(i) == ' ') {
                i++;
            }
            //Check ADR
            for(int j = 0; j <= 2; j++) {
                if(!(myFCW.charAt(j) >= '0' && myFCW.charAt(j) <= '9')) {
                    return count;
                }
            }

            //Check for '-' between ADR and DDD
            if(myFCW.charAt(3) != '-')
                return count;

            //Check DDD
            for(int j = 4; j <= 6; j++) {
                if(!(myFCW.charAt(j) >= '0' && myFCW.charAt(j) <= '9')) {
                    return count;
                }
            }
            count++;
            myFCW = staticMyFCW.substring(i);
        }
        return count;
    }
   public synchronized String openCtlForComment() throws IOException {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open CTL File");
        fc.setInitialDirectory(new File(System.getProperty("user.dir")));
        fc.getExtensionFilters().add(new ExtensionFilter("CTL Files", "*.ctl"));
        File ctlFile = fc.showOpenDialog(null);
        //Create buffer --> read the file --> parse the file
        return readFileForComment(createCtlCommentBuffer(ctlFile));
    }

    public BufferedReader createCtlCommentBuffer(File file) throws IOException {
        return  new BufferedReader(new FileReader(file));
    }

    public Map<String, String> makeFCWHash()
    {
        Map<String, String> opCodes = new HashMap<String, String>();


        opCodes.put("001", "Ring 1");
        opCodes.put("002", "Ring 2");
        opCodes.put("003", "Ring 3");
        opCodes.put("004", "Ring 4");
        opCodes.put("005", "Ring 5");
        opCodes.put("006", "Sweep");
        opCodes.put("007", "A = Bazooka, B = Spout");
        opCodes.put("008", "Candelabra");
        opCodes.put("009", "A = Front B = back Curtain & Peacock POSSIBLE ISSUE");
        opCodes.put("016", "Selected Lights from the table");
        opCodes.put("017", "Module #1 lights");
        opCodes.put("018", "Module #2 lights");
        opCodes.put("019", "Module #3 lights");
        opCodes.put("020", "Module #4 lights");
        opCodes.put("021", "Module #5 lights");
        opCodes.put("022", "Module #6 lights");
        opCodes.put("023", "Module #7 lights");
        opCodes.put("024", "Back courtain Lights (Green and Yello only)");
        opCodes.put("025", "Peacock Light Group A");
        opCodes.put("026", "Peacock Light Group B");
        opCodes.put("027", "Peacock Light Group A + B");
        opCodes.put("033", "Sweep Together");
        opCodes.put("034", "Sweep Opposite");
        opCodes.put("035", "Sweep to Limit Left and Right");
        opCodes.put("036", "Sweep to Limit Left");
        opCodes.put("037", "Sweep to Limit Right");
        opCodes.put("038", "Sweep Left Speed");
        opCodes.put("039", "Sweep Right Speed");
        opCodes.put("040", "Sweep Left and Right Speed");
        opCodes.put("042", "Sweep type");
        opCodes.put("047", "Mutlicake");
        opCodes.put("048", "Water Modules W1-W6 and Wedding Cake Formation");
        opCodes.put("049", "Module A lights (ODD 1,3,5,7)");
        opCodes.put("050", "Module B lights (Even 2,4,6)");
        opCodes.put("051", "Module A and B lights");
        opCodes.put("054", "Voice");
        opCodes.put("055", "Center spout (voice)");
        opCodes.put("056", "Front courtain - excludes black center module spots");
        opCodes.put("057", "Back Curtain");
        opCodes.put("069", "Repeat JUMP water level (Pulse) Sweep Water @0.5 seconds");
        opCodes.put("080", "Interchange A and B module formations of water and light");
        opCodes.put("085", "Shit or rotate module 1 - 7 lights");
        opCodes.put("086", "Set shift timer interval");
        opCodes.put("099", "Off");

        opCodes.put("117", "Module 1 (017) All Leds - fade up or down");
        opCodes.put("118", "Module 2 (018) All Leds - fade up or down");
        opCodes.put("119", "Module 3 (019) All Leds - fade up or down");
        opCodes.put("120", "Module 4 (020) All Leds - fade up or down");
        opCodes.put("121", "Module 5 (021) All Leds - fade up or down");
        opCodes.put("122", "Module 6 (022) All Leds - fade up or down");
        opCodes.put("123", "Module 7 (023) All Leds - fade up or down");

        opCodes.put("127", "Peacock LED group A & B (027) - fade up or down");
        opCodes.put("149", "Peacock A LEDS (049) - fade up or down");
        opCodes.put("150", "Modules B LEDS (050) - fade up or down");
        opCodes.put("155", "Center spout (voice / 055) - fade up or down");
        opCodes.put("156", "Front curtain (056) - fade up or down");
        opCodes.put("157", "Back curtain (057) - fade up or down)");

//Legacy
        opCodes.put("041", "Peacock Light Group B");
        opCodes.put("052", "Module A and B lights");
        return opCodes;
    }
    
    public Map<String, String> makeDataLookupHash()
    {
    	Map<String, String> dataTable = new HashMap<String, String>();

		dataTable.put("001", "A1");
		dataTable.put("002", "A1");
		dataTable.put("003", "A1");
		dataTable.put("004", "A1");
		dataTable.put("005", "A1");
		dataTable.put("006", "A1");
		dataTable.put("007", "A1");
		dataTable.put("008", "A1");
		dataTable.put("009", "B");
		dataTable.put("016", "E");
		dataTable.put("017", "C1/K");
		dataTable.put("018", "C1/K");
		dataTable.put("019", "C1/K");
		dataTable.put("020", "C1/K");
		dataTable.put("021", "C1/K");
		dataTable.put("022", "C1/K");
		dataTable.put("023", "C1/K");
		dataTable.put("024", "C2");
		dataTable.put("025", "C1/K");
		dataTable.put("026", "C1/K");
		dataTable.put("027", "C1/K");
		dataTable.put("033", "D1");
		dataTable.put("034", "D1");
		dataTable.put("035", "J");
		dataTable.put("036", "J");
		dataTable.put("037", "J");
		dataTable.put("038", "D2");
		dataTable.put("039", "D2");
		dataTable.put("040", "D2");
		dataTable.put("042", "D3");
		dataTable.put("047", "A2");
		dataTable.put("048", "A1");
		dataTable.put("049", "C1/K");
		dataTable.put("050", "C1/K");
		dataTable.put("051", "C1/K");
		dataTable.put("054", "F");
		dataTable.put("055", "K");
		dataTable.put("056", "K");
		dataTable.put("057", "K");
		dataTable.put("069", "H");
		dataTable.put("080", "I");
		dataTable.put("085", "G");
		dataTable.put("086", "Time");
		dataTable.put("099", "M");

		dataTable.put("117", "L");
		dataTable.put("118", "L");
		dataTable.put("119", "L");
		dataTable.put("120", "L");
		dataTable.put("121", "L");
		dataTable.put("122", "L");
		dataTable.put("123", "L");

		dataTable.put("127", "L");
		dataTable.put("149", "L");
		dataTable.put("150", "L");
		dataTable.put("155", "L");
		dataTable.put("156", "L");
		dataTable.put("157", "L");

		//Legacy
		dataTable.put("041", "C1/K");
		dataTable.put("052", "C1/K");
		dataTable.put("053", "C1/K");

	return dataTable;
    }
    
    public Map<String, String> makeA1Hash()
    {
    	Map<String, String> A1 = new HashMap<String, String>();
		A1.put("000", "Water level off and bypass valve closed");
		A1.put("001", "Water level 1");
		A1.put("002", "Water level 2");
		A1.put("003", "Water level 3");
		A1.put("004", "Water level 4");
		A1.put("005", "Water level 5");
		A1.put("006", "Wedding cake");
		A1.put("016", "Module A water valves (Odd numberd)");
		A1.put("032", "Module B water valves (Even numbered)");
		A1.put("064", "Connect A to B through bypass valve");
    
    	return A1;
    }
    
    public Map<String, String> makeA2Hash()
    {
    	Map<String, String> A2 = new HashMap<String, String>();
		A2.put("000", "Water level off and bypass valve closed");
		A2.put("001", "Water level 1");
		A2.put("002", "Water level 2");
		A2.put("003", "Water level 3");
		A2.put("004", "Water level 4");
		A2.put("005", "Water level 5");
		A2.put("006", "Wedding cake");
		A2.put("016", "Module A water valves (Odd numberd)");
		A2.put("032", "Module B water valves (Even numbered)");
		A2.put("064", "Connect A to B through bypass valve");
	return A2;
    }
    
    //Courtains and peacock water level
    public Map<String, String> makeBHash()
    {
		Map<String, String> B = new HashMap<String, String>();
		B.put("000", "Peacock water level off and peacock valve closed");
		B.put("001", "water level 1");
		B.put("002", "water level 2");
		B.put("003", "water level 3");
		B.put("004", "water level 4");
		B.put("005", "water level 5");
		B.put("016", "Front curtain water valves");
		B.put("032", "Back curtain water valves");
		B.put("096", "Peacock on / back curtain off - bypass valve");

		return B;
    }
    
    public Map<String, String> makeC1Hash()
    {
    	Map<String, String> C1 = new HashMap<String, String>();
		C1.put("000", "All colors off");
		C1.put("001", "Red");
		C1.put("002", "Blue");
		C1.put("004", "Amber");
		C1.put("008", "White");

		return C1;
    }
    
    public Map<String, String> makeC2Hash() 
    {
    	Map<String, String> C2 = new HashMap<String, String>();
		C2.put("000", "All colors off");
		C2.put("016", "Green back curtain");
		C2.put("032", "Yellow back curtain");
    	
    	return C2;
    }
    
    public Map<String, String> makeD1Hash() 
    {
    	Map<String, String> D1 = new HashMap<String, String>();
		D1.put("000", "All stop and return to center");
		D1.put("001", "Short sweep");
		D1.put("002", "Long sweep");
		D1.put("008", "Sweep pause");
		D1.put("016", "Largo");
		D1.put("032", "Adagio");
		D1.put("048", "Andante");
		D1.put("064", "Moderato");
		D1.put("080", "Allegretto");
		D1.put("096", "Allegro");
		D1.put("102", "Presto");
	
		return D1;
    }
    
    public Map<String, String> makeD2Hash() 
    {
    	Map<String, String> D2 = new HashMap<String, String>();
		D2.put("000", "All stop and return to center");
		D2.put("008", "Sweep pause");
		D2.put("016", "Largo");
		D2.put("032", "Adagio");
		D2.put("048", "Andante");
		D2.put("064", "Moderato");
		D2.put("080", "Allegretto");
		D2.put("096", "Allegro");
		D2.put("102", "Presto");
    	
    	return D2;
    }
    
    public Map<String, String> makeD3Hash() 
    {
    	Map<String, String> D3 = new HashMap<String, String>();
		D3.put("000", "Sweep independent");
		D3.put("001", "Sweep left and right together");
		D3.put("002", "Sweep left and right Opposed");
	
		return D3;
    }
    
    public Map<String, String> makeEHash() 
    {
    	Map<String, String> E = new HashMap<String, String>();
		E.put("000", "All off");
		E.put("002", "Top of hill - cross/star/anchor - highlight");
		E.put("004", "White beacons on end of fountain apron");
		E.put("008", "Superceded white modules");
	
		return E;
    }
    
    public Map<String, String> makeFHash() 
    {
    	Map<String, String> F = new HashMap<String, String>();
		F.put("000", "All off");
		F.put("002", "Voice of the fountain water and lights on");
	
		return F;
    }
    
    public Map<String, String> makeGHash() 
    {
    	Map<String, String> G = new HashMap<String, String>();
		G.put("000", "Stop all shifting and reset");
		G.put("001", "Motion to the right");
		G.put("002", "Motion to the left");
		G.put("016", "Shift without end-carry");
		G.put("032", "Shift light with end-carry");
		G.put("064", "Repeat shifting at timed interval");
	
		return G;
    }
    
    public Map<String, String> makeHHash()
    {
    	Map<String, String> H = new HashMap<String, String>();
		H.put("000", "Stop jumping and return to preset");
		H.put("006", "Address the sweep water formation");
		H.put("016", "Jump A module water level");
		H.put("032", "Jump B module water level");
		H.put("064", "Jump 0 phase or 1 phase of cycle timers");

		return H;
    }
    
    public Map<String, String> makeIHash()
    {
    	Map<String, String> I = new HashMap<String, String>();
		I.put("000", "Stop motion");
		I.put("001", "Effect the water setting");
		I.put("002", "Effect the light setting");
		I.put("016", "Place A configurations into B");
		I.put("032", "Place B configurations into C");

		return I;
    }
    
    public Map<String, String> makeJHash()
    {
    	Map<String, String> J = new HashMap<String, String>();
		J.put("000", "Hold at center");
		J.put("017", "Right long to right very short");
		J.put("019", "Hold at right long");
		J.put("020", "Right long to center");
		J.put("021", "Right long to left very short");
		J.put("034", "Hold at Right short");
		J.put("051", "Hold at right very short");
		J.put("068", "Hold at center");
		J.put("085", "Hold at left very short");
		J.put("102", "Hold at left short");
		J.put("119", "Hold at left long");
		J.put("018", "Oscillate at limit at right long");
		J.put("035", "Oscillate at limit at right short");
		J.put("052", "Oscillate at limit at right very long");
		J.put("053", "Oscillate at limit at center");
		J.put("069", "Oscillate at limit at left very short");
		J.put("086", "Oscillate at limit at left short");
		J.put("103", "Oscillate at limit at left long");
		J.put("022", "Right long to left short");
		J.put("023", "Right long to left long");
		J.put("036", "Right short to center");
		J.put("037", "Right short to left very short");
		J.put("038", "Right short to left short");
		J.put("039", "Right short to left long");
		J.put("070", "Center to left short");
		J.put("071", "Center to left long");
		J.put("087", "Left very short to left long");

		return J;
    }

    public  String readFileForComment(BufferedReader reader) throws IOException {
        StringBuilder stringBuffer = new StringBuilder();

        try {
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
}
