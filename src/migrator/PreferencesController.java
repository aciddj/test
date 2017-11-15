package migrator;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import migrator.helpers.Dialogs;
import migrator.helpers.IModalWindow;
import migrator.helpers.IWindow;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Walter Ego on 21.04.2017.
 */
public class PreferencesController implements IModalWindow {
    private Stage stage;
    private FileChooser fileChooser;
    private boolean modalResult = false;

    @FXML
    private AnchorPane apRoot;

    @FXML
    private TextField tfSVNPath;

    @FXML
    private TextField tfMySQLDumpPath;

    @Override
    public void setStage(Stage stage, Stage parent) {
        this.stage = stage;
        this.stage.setTitle("Preferences");
        this.stage.setResizable(false);
        this.stage.setScene(new Scene(apRoot, apRoot.getPrefWidth() - 10, apRoot.getPrefHeight() - 10));
        init();
    }

    @Override
    public boolean getModalResult() {
        return modalResult;
    }

    private void init() {
        Config cfg;
        try {
            cfg = Config.getInstance();
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error loading settings", e.getMessage());
            return;
        }

        tfSVNPath.setText(cfg.getPreferences().getSvnExecutablePath());
        tfMySQLDumpPath.setText(cfg.getPreferences().getMySQLDumpPath());
        fileChooser = new FileChooser();
    }

    @FXML
    private void svnBrowseClick() {
        fileChooser.setTitle("Choose svn.exe");
        Path svnPath = Paths.get(tfSVNPath.getText());
        if (svnPath != null && svnPath.getParent() != null && svnPath.getParent().toFile().exists()) {
            fileChooser.setInitialDirectory(svnPath.getParent().toFile());
            fileChooser.setInitialFileName(svnPath.toString());
        }
        File chosenFile = fileChooser.showOpenDialog(stage);
        if (chosenFile != null)
            tfSVNPath.setText(chosenFile.getAbsolutePath());
    }

    @FXML
    private void mySQLDumpBrowseClick() {
        fileChooser.setTitle("Choose mysqldump.exe");
        Path mySQLDumpPath = Paths.get(tfMySQLDumpPath.getText());
        if (mySQLDumpPath != null && mySQLDumpPath.getParent() != null && mySQLDumpPath.getParent().toFile().exists()) {
            fileChooser.setInitialDirectory(mySQLDumpPath.getParent().toFile());
            fileChooser.setInitialFileName(mySQLDumpPath.toString());
        }
        File chosenFile = fileChooser.showOpenDialog(stage);
        if (chosenFile != null)
            tfMySQLDumpPath.setText(chosenFile.getAbsolutePath());
    }

    private void checkInput() throws Exception {
        if (tfMySQLDumpPath.getText().trim().isEmpty())
            throw new Exception("MySQLDump path cannot be empty.");
        Path mySQLDumpPath = Paths.get(tfMySQLDumpPath.getText());
        if (mySQLDumpPath == null)
            throw new Exception("MySQLDump path is incorrect.");
        if (!mySQLDumpPath.toFile().exists())
            throw new Exception("Specified MySQLDump file does not exist.");
        if (tfSVNPath.getText().trim().isEmpty())
            throw new Exception("SVN executable path cannot be empty.");
        Path svnPath = Paths.get(tfSVNPath.getText());
        if (svnPath == null)
            throw new Exception("SVN path is incorrect.");
        if (!svnPath.toFile().exists())
            throw new Exception("Specified SVN file does not exist.");
    }

    @FXML
    private void bCancelClick(){
        modalResult = false;
        close();
    }

    @FXML
    private void close(){
        stage.close();
    }

    @FXML
    private void bOkClick() {
        try {
            checkInput();
        } catch (Exception e) {
            Dialogs.ErrorMessage("Input error", e.getMessage());
            return;
        }

        Config cfg;
        try {
            cfg = Config.getInstance();
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error loading settings", e.getMessage());
            return;
        }

        Preferences prefs = cfg.getPreferences();
        prefs.setMySQLDumpPath(tfMySQLDumpPath.getText());
        prefs.setSvnExecutablePath(tfSVNPath.getText());
        try {
            cfg.savePreferences(prefs);
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error saving settings", e.getMessage());
            return;
        }

        modalResult = true;
        close();
    }
}
