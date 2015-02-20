package choreography;

import choreography.io.CtlLib;
import choreography.model.fountain.Fountain;
import javafx.application.Application;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;


/**
 * Created by ik1nk_000 on 2/20/2015.
 */
public class TEST extends Application {
    private static Fountain fountain;
    private VBox root;
    private static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }
    public static Fountain getFountain() {
        return fountain;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        CtlLib.getInstance().commentCtlFile();
    }
}

