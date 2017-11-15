package migrator;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import migrator.helpers.Dialogs;
import migrator.helpers.IModalWindow;

/**
 * Created by Walter Ego on 21.04.2017.
 */
public class DatabasePropertiesController implements IModalWindow {
    private Stage stage;
    private Boolean modalResult = false;

    @FXML
    private AnchorPane apRoot;

    @FXML
    private TextField tfPort;

    @FXML
    private TextField tfDatabaseName;

    @FXML
    private TextField tfSVNURL;

    @FXML
    private TextField tfUserName;

    @FXML
    private TextField tfPassword;

    @FXML
    private TextField tfSVNUserName;

    @FXML
    private TextField tfSVNPassword;

    @FXML
    private CheckBox chTraceChanges;

    private Database currentDB = null;

    @Override
    public void setStage(Stage stage, Stage parent) {
        this.stage = stage;
        this.stage.setTitle("Database Properties");
        this.stage.setResizable(false);
        this.stage.setScene(new Scene(apRoot, apRoot.getPrefWidth() - 10, apRoot.getPrefHeight() - 10));
    }

    @Override
    public boolean getModalResult() {
        return modalResult;
    }

    @FXML
    private void bCancelClick(){
        modalResult = false;
        stage.close();
    }

    @FXML
    private void bOkClick() throws Exception {
        try {
            checkInput();
        } catch (Exception e) {
            Dialogs.ErrorMessage("Input error", e.getMessage());
            return;
        }

        MigratorModel model = MigratorModel.getInstance();

        if (isEditMode()) {
            try {
                model.updateDatabase(currentDB, tfPort.textProperty().getValue(), tfDatabaseName.textProperty().getValue(), tfUserName.textProperty().getValue(),
                    tfPassword.textProperty().getValue(), tfSVNURL.textProperty().getValue(), tfSVNUserName.textProperty().getValue(), tfSVNPassword.textProperty().getValue(),
                    chTraceChanges.isSelected());
            } catch (Exception e) {
                Dialogs.ErrorMessage("Error updating database", e.getMessage());
                return;
            }
        } else {
            try {
                model.addDatabase(tfPort.textProperty().getValue(), tfDatabaseName.textProperty().getValue(), tfUserName.textProperty().getValue(),
                    tfPassword.textProperty().getValue(), tfSVNURL.textProperty().getValue(), tfSVNUserName.textProperty().getValue(), tfSVNPassword.textProperty().getValue(),
                    chTraceChanges.isSelected());
            } catch (Exception e) {
                Dialogs.ErrorMessage("Error adding database", e.getMessage());
                return;
            }
        }

        modalResult = true;
        stage.close();
    }

    private void checkInput() throws Exception {
        if (tfPort.textProperty().getValue().equals(""))
            throw new Exception("Port value cannot be empty.");

        try {
            int port = Integer.parseInt(tfPort.textProperty().getValue());
            if (port <= 0)
                throw new Exception("Port value should be positive.");
        } catch (Exception e) {
            throw new Exception("Port value should be numeric.");
        }

        if (tfDatabaseName.textProperty().getValue().trim().equals(""))
            throw new Exception("Database name cannot be empty.");

        if (tfSVNURL.textProperty().getValue().trim().equals(""))
            throw new Exception("SVN URL cannot be empty.");
    }

    public void setCurrentDB(Database currentDB) {
        this.currentDB = currentDB;
        initValues();
        setAvailability();
    }

    private boolean isEditMode(){
        return (currentDB != null);
    }

    private void initValues(){
        if (isEditMode()) {
            tfPort.textProperty().setValue(currentDB.getPort());
            tfDatabaseName.textProperty().setValue(currentDB.getName());
            tfSVNURL.textProperty().setValue(currentDB.getSvnUrl());
            tfSVNUserName.textProperty().setValue(currentDB.getSvnUserName());
            tfSVNPassword.textProperty().setValue(currentDB.getSVNPassword());
            tfUserName.textProperty().setValue(currentDB.getUserName());
            tfPassword.textProperty().setValue(currentDB.getPassword());
            chTraceChanges.selectedProperty().setValue(currentDB.isTraced());
        }
    }

    private void setAvailability(){
        if (isEditMode()) {
            tfPort.setEditable(false);
            tfDatabaseName.setEditable(false);
            tfSVNURL.setEditable(false);
        }
    }
}
