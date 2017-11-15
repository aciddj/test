package migrator;

import javafx.application.Application;
import javafx.stage.Stage;
import migrator.helpers.Window;

/**
 * Created by Walter Ego on 21.04.2017.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Window.ShowFXMLWindow(getClass().getResource("./views/migrator/migrator.fxml"), null, false, null, primaryStage);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
