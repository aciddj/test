package migrator;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import migrator.helpers.Dialogs;
import migrator.helpers.IModalWindow;

/**
 * Created by Walter Ego on 27.05.2017.
 */
public class CommitController implements IModalWindow {
    private Stage stage;
    private Boolean modalResult = false;
    private Revision revision;

    @FXML
    private AnchorPane apRoot;

    @FXML
    private TextArea taSQL;

    @FXML
    private TextArea taComment;

    @Override
    public void setStage(Stage stage, Stage parent) {
        this.stage = stage;
        this.stage.setTitle("Commit database revision");
        this.stage.setMinHeight(400);
        this.stage.setMinWidth(350);
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
        if (revision == null) {
            Dialogs.ErrorMessage("Internal error", "Current revision was not set.");
            return;
        }

        try {
            checkInput();
        } catch (Exception e) {
            Dialogs.ErrorMessage("Input error", e.getMessage());
            return;
        }

        try {
            revision.commit(taComment.getText().trim());
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error committing changes", e.getMessage());
            return;
        }

        modalResult = true;
        stage.close();
    }

    public void setRevision(Revision revision) {
        this.revision = revision;
        initValues();
    }

    private void checkInput() throws Exception {
        if (taSQL.getText().trim().isEmpty())
            throw new Exception("SQL cannot be empty.");

        if (taComment.getText().trim().isEmpty())
            throw new Exception("Commit comment cannot be empty.");
    }

    private void initValues() {
        try {
            taSQL.setText(revision.getSQL());
        } catch (Exception e) {
            Dialogs.ErrorMessage("Unable to get SQL of current revision", e.getMessage());
        }
    }
}
