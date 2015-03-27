package resources;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by ik1nk_000 on 3/25/2015.
 */
public class CTLUpdater extends Application{
    @Override
    public void start(Stage primaryStage) throws Exception {
        findOutdatedOpCodes();
        updateCTL();
        System.out.println("Yup");
        System.exit(0);
    }

    public static void updateCTL(){
        ArrayList<ArrayList<String>> outdated = findOutdatedOpCodes();
        FileChooser fc = new FileChooser();
        fc.setTitle("Open CTL File");
        fc.setInitialDirectory(new File(System.getProperty("user.dir")));
        fc.getExtensionFilters().add(new ExtensionFilter("CTL Files", "*.ctl"));
        File f = fc.showOpenDialog(null);

        String oldName = f.getName();
        String oldPath = f.getPath() + "\\";

        String newName = renameOldCTL(f,oldName);
        //Scan the old file line by line and place the updated opcodes into a string
        f = new File(newName);
        String ctlString = "";

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                ctlString += removeOutdatedOpCodes(line,outdated) + '\n';
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            createNewCTL(oldPath,ctlString);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


    }
    public static String removeOutdatedOpCodes(String line,ArrayList<ArrayList<String>> outdated){
        String time = line.substring(0,7);
        String[] tempfcws = line.substring(7).split(" ");
        ArrayList<String> fcws = new ArrayList<>();
        for(String s: tempfcws){
            fcws.add(s);
        }

        for(int j = 0; j < fcws.size(); j++){
            String s = fcws.get(j);
            System.err.println("s= " + s);
            if(!s.equals("")) {
                for (ArrayList<String> al : outdated) {
                    String data = s.substring(3);
                    if (s.substring(0, 3).equals(al.get(0))) {
                        fcws.remove(s);
                        for (int i = 1; i < al.size(); i++) {
                            fcws.add(al.get(i) + data);
                            System.out.println(al.get(0) + " fffff");
                        }
                    }
                }
            }
        }

        String newline = time + " ";
        for(String s: fcws){
            newline += s + " ";
        }

        return newline;
    }


    /*
     * Returns true only if the file was successfully renamed
     */
    public static String renameOldCTL(File f,String fileName){
        String newName = f.getParent()+"\\" + fileName.substring(0,fileName.length()-4) + "_OLD.ctl";
        //rename the old file
        if(f.renameTo(new File(newName))){

            return newName;
        }
        return fileName;
    }


    /*
     * Returns true only if the file was successfully created
     */
    public static void createNewCTL(String fileName, String contents) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(fileName,"UTF-8");
        writer.print(contents);
        writer.close();
    }

    public static ArrayList<ArrayList<String>> findOutdatedOpCodes(){
        File f = new File("Choreography_Dev\\src\\resources\\outdated_op_codes.txt");
        ArrayList<ArrayList<String>> opList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                String[] opCodes = line.split(" ");

                opList.add(count,new ArrayList<String>());
                for(String s : opCodes){
                    if(!s.equals("-")){
                        opList.get(count).add(s);
                    }
                }
                count++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return opList;
    }
}
