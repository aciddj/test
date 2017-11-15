package migrator.helpers;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import java.util.Optional;

/**
 * Created by Walter Ego on 29.04.2017.
 */
public class Dialogs{
	public static boolean YesNoQuestion(String title, String headerText, String contentText){
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setHeaderText(headerText);
		alert.setContentText(contentText);

		Optional<ButtonType> result = alert.showAndWait();
		return (result.isPresent() && (result.get() == ButtonType.OK));
	}

	public static void ErrorMessage(String headerText, String contentText){
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(headerText);
		alert.setContentText(contentText);

		alert.showAndWait();
	}

	public static void InformationMessage(String headerText, String contentText){
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText(headerText);
		alert.setContentText(contentText);

		alert.showAndWait();
	}

	public static void Warning(String headerText, String contentText){
		Alert alert = new Alert(AlertType.WARNING);
		alert.setTitle("Warning");
		alert.setHeaderText(headerText);
		alert.setContentText(contentText);

		alert.showAndWait();
	}
}