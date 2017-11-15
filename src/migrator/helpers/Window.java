package migrator.helpers;

import javafx.fxml.FXMLLoader;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Created by Walter Ego on 21.04.2017.
 */
public class Window {
    public static Boolean ShowFXMLWindow(URL location, Stage parentStage, boolean showModal, Consumer<Object> beforeShowCallback) throws ClassCastException, IOException {
        Stage stage = new Stage();
        return ShowFXMLWindow(location, parentStage, showModal, beforeShowCallback, stage);
    }

    public static Boolean ShowFXMLWindow(URL location, Stage parentStage, boolean showModal, Consumer<Object> beforeShowCallback, Stage stage) throws ClassCastException, IOException {
        FXMLLoader loader = new FXMLLoader(location);
        loader.load();
        IWindow controller = loader.getController();

        Stage localStage = (stage != null) ? stage : (new Stage());
        if (parentStage != null)
            localStage.initOwner(parentStage);

        controller.setStage(localStage, parentStage);

        if (beforeShowCallback != null){
            beforeShowCallback.accept(controller);
        }

        if (showModal) {
            localStage.initModality(Modality.APPLICATION_MODAL);
            localStage.showAndWait();
            return ((IModalWindow)controller).getModalResult();
        } else
            localStage.show();

        return false;
    }
}
