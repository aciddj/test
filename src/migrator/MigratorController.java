package migrator;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import migrator.helpers.Dialogs;
import migrator.helpers.IWindow;
import migrator.helpers.Window;

/**
 * Created by Walter Ego on 21.04.2017.
 */
enum NodeIconType {DATABASE, CONNECTED_DATABASE, REVISION, APPLIED_REVISION};

public class MigratorController implements IWindow, Initializable {
    private Stage stage;
    private MigratorModel model;
    private String selectedTreeObjectId;

    @FXML
    private AnchorPane apRoot;

    @FXML
    private TreeView tvDatabases;

    @FXML
    private Button bRemoveDatabase;

    @FXML
    private Button bAddDatabase;

    @FXML
    private Button bEditDatabase;

    @FXML
    private TextArea taScript;

    @FXML
    private Button bGetRevisions;

    @FXML
    private Button bApplyRevision;

    @FXML
    private Button bCommitRevision;

    @FXML
    private ContextMenu cmTreeContextMenu;

    @Override
    public void setStage(Stage stage, Stage parent) {
        this.stage = stage;
        this.stage.setTitle("MySQL Migrator");
        this.stage.setMinWidth(400);
        this.stage.setMinHeight(300);
        this.stage.setScene(new Scene(apRoot, apRoot.getPrefWidth(), apRoot.getPrefHeight()));

        this.stage.setOnCloseRequest(event -> {
            try {
                model.resetFirstLaunch();
            } catch (Exception e) {}
        });

        this.stage.setOnShown(e -> ready());
        cmTreeContextMenu.setOnShowing(event -> TreeContextMenuShow());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources){
        model = MigratorModel.getInstance();

        SetupTree();
    }

    private void ready() {
        boolean firstLaunch = false;

        try {
            firstLaunch = model.isFirstLaunch();
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error loading settings", e.getMessage());
        }

        if (firstLaunch) {
            Dialogs.InformationMessage("Preferences are not defined", "The program is launched first time. Please specify preferences.");
            try {
                preferencesOpen();
            } catch (Exception e) {
                Dialogs.ErrorMessage("Error loading program resource", e.getMessage());
            }
        }
    }

    @FXML
    private void exit(){
        stage.close();
    }

    @FXML
    private void preferencesOpen() throws IOException {
        Window.ShowFXMLWindow(getClass().getResource("./views/preferences/preferences.fxml"), stage, true, null);
    }

    @FXML
    private void addDatabaseClick() {
        try {
            model.checkSVN();
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error checking SVN", e.getMessage());
            try {
                Window.ShowFXMLWindow(getClass().getResource("./views/preferences/preferences.fxml"), stage, true, null);
            } catch (IOException ioe) {}
            return;
        }

        boolean result = false;
        try {
            result = Window.ShowFXMLWindow(getClass().getResource("./views/database_properties/database_properties.fxml"), stage, true, null);
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error loading view", e.getMessage());
            return;
        }

        if(result)
            updateTree();
    }

    @FXML
    private void editDatabaseClick() {
        TreeItem selectedItem = (TreeItem)tvDatabases.getSelectionModel().selectedItemProperty().getValue();
        Object selectedObject = selectedItem.getValue();

        if (selectedObject instanceof Database) {
            Boolean result = false;
            try {
                result = Window.ShowFXMLWindow(getClass().getResource("./views/database_properties/database_properties.fxml"), stage, true, controller ->
                  ((DatabasePropertiesController) controller).setCurrentDB((Database)selectedObject));
            } catch (Exception e) {
                Dialogs.ErrorMessage("Error loading view", e.getMessage());
                return;
            }

            if (result)
                updateDBNode(selectedItem, (Database)selectedObject, true);
        }
    }

    @FXML
    private void removeDatabaseClick() {
        TreeItem selectedItem = (TreeItem)tvDatabases.getSelectionModel().selectedItemProperty().getValue();
        Object selectedObject = selectedItem.getValue();

        if (selectedObject instanceof Database) {
            if (!Dialogs.YesNoQuestion("Confirmation","Confirm removing database",
                    "Are you sure you want to remove the '" + ((Database) selectedObject).getName() + "' database? All uncommitted changes will be lost!"))
                return;
            try {
                model.removeDatabase((Database)selectedObject);
            } catch (Exception e) {
                Dialogs.ErrorMessage("Error removing database", e.getMessage());
                return;
            }
        } else {
            Dialogs.ErrorMessage("Error removing database", "Selected object is invalid.");
            return;
        }

        updateTree();
    }

    private void SetupTree() {
        tvDatabases.setRoot(new TreeItem<Object>(null, null));
        tvDatabases.getRoot().setExpanded(true);
        tvDatabases.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            nodeChanged((TreeItem) oldValue, (TreeItem) newValue);
        });
        updateTree();
    }

    private void updateTree() {
        saveSelection();
        ArrayList<Database> databases = new ArrayList<>();
        try {
            databases = model.getDatabases();
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error adding databases", "Error loading database list");
        }

        tvDatabases.getRoot().getChildren().clear();
        for (Database db : databases){
            TreeItem<Database> item = new TreeItem<>(db, null);
            item.expandedProperty().addListener((observable, oldValue, newValue) -> {
                dbNodeExpandedChanged(item, newValue);
            });
            tvDatabases.getRoot().getChildren().add(item);

            boolean isConnected = false;
            try {
                isConnected = db.isConnected();
            }  catch (Exception e) {
                Dialogs.ErrorMessage("Error checking connection.", e.getMessage());
            }

            if (isConnected) {
                item.setGraphic(getNodeIcon(NodeIconType.CONNECTED_DATABASE));
                updateDBNode(item, db, false);
            }
            else {
                item.setGraphic(getNodeIcon(NodeIconType.DATABASE));
                addDummyItem(item);
            }
        }
        restoreSelection();
    }

    private void saveSelection() {
        TreeItem selectedItem = (TreeItem)tvDatabases.getSelectionModel().selectedItemProperty().getValue();
        if (selectedItem == null)
            return;
        Object selectedTreeObject = selectedItem.getValue();
        if (selectedTreeObject instanceof Database)
            selectedTreeObjectId = ((Database)selectedTreeObject).getStorageId();
        if (selectedTreeObject instanceof Revision)
            selectedTreeObjectId = ((Revision)selectedTreeObject).getId();
    }

    private void restoreSelection() {
        restoreSelectionRecursive(tvDatabases.getRoot());
    }

    private void restoreSelectionRecursive(TreeItem root) {
        if (selectedTreeObjectId == null || selectedTreeObjectId.equals(""))
            return;
        if (root == null)
            return;

        boolean shouldSelect = false;
        for (Object node : root.getChildren()){
            Object selectedTreeObject = ((TreeItem)node).getValue();
            if (selectedTreeObject == null)
                return;
            if (selectedTreeObject instanceof Database)
                if (selectedTreeObjectId.equals(((Database)selectedTreeObject).getStorageId()))
                    shouldSelect = true;
            if (selectedTreeObject instanceof Revision)
                if (selectedTreeObjectId.equals(((Revision)selectedTreeObject).getId()))
                    shouldSelect = true;
            if (shouldSelect) {
                MultipleSelectionModel msm = tvDatabases.getSelectionModel();
                int row = tvDatabases.getRow((TreeItem) node);
                msm.select(row);
                return;
            }
            restoreSelectionRecursive((TreeItem) node);
        }
    }

    private void updateDBNode(TreeItem dbItem, Database db, boolean restoreSelection){
        if (restoreSelection)
            saveSelection();

        dbItem.getChildren().clear();
        dbItem.setExpanded(true);
        ArrayList<Revision> revisions = null;
        try {
            revisions = db.getRevisionsWithRefresh();
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error getting revisions.", e.getMessage());
        }
        for (Revision rev : revisions){
            TreeItem<Revision> item;
            if (rev.canBeApplied())
                item = new TreeItem<>(rev, getNodeIcon(NodeIconType.REVISION));
            else
                item = new TreeItem<>(rev, getNodeIcon(NodeIconType.APPLIED_REVISION));
            dbItem.getChildren().add(item);
        }

        if (restoreSelection)
            restoreSelection();
    }

    private void nodeChanged(TreeItem oldItem, TreeItem newItem){
        updateActionsState(newItem);

        if (newItem == null)
            return;

        Object selectedObject = newItem.getValue();
        if (selectedObject instanceof Revision) {
            Revision currentRevision = (Revision)selectedObject;
            String sql;

            try {
                sql = currentRevision.getSQL();
            } catch (Exception e) {
                Dialogs.ErrorMessage("Error getting script", "Cannot get script for the selected revision. " + e.getMessage());
                return;
            }

            taScript.setVisible(true);
            taScript.setText(sql);
        } else {
            taScript.setVisible(false);
            taScript.setText(""); // TODO: set database info ??
        }
    }

    private void updateActionsState(TreeItem selectedItem){
        bRemoveDatabase.setDisable(true);
        bEditDatabase.setDisable(true);
        bGetRevisions.setDisable(true);
        bApplyRevision.setDisable(true);
        bCommitRevision.setDisable(true);

        if (selectedItem != null){
            if (selectedItem.getValue() instanceof Database){ // database node chosen
                bRemoveDatabase.setDisable(false);
                bEditDatabase.setDisable(false);
                Database db = (Database) selectedItem.getValue();
                try {
                    if (db.isConnected())
                        bGetRevisions.setDisable(false);
                } catch (Exception e) {};
            } else { // revision node chosen
                bGetRevisions.setDisable(false);
                Revision revision = (Revision)selectedItem.getValue();
                if (revision.canBeApplied()){
                    bApplyRevision.setDisable(false);
                }
                if (revision.canBeCommited())
                    bCommitRevision.setDisable(false);
            }
        }
    }

    private void addDummyItem(TreeItem node){
        TreeItem<Object> item = new TreeItem<>(null, null);
        node.getChildren().add(item);
    }

    private void removeDummyItem(TreeItem node){
        for (Object item : node.getChildren()){
            if (((TreeItem)item).getValue() == null){
                node.getChildren().remove(item);
                return;
            }
        }
    }

    private void dbNodeExpandedChanged(TreeItem<Database> item, boolean expanded){
        if(expanded){
            boolean createInitialRevision = false;
            Database db = item.getValue();
            boolean isConnected = false;
            try {
                isConnected = db.isConnected();
            } catch (Exception e) {
                Dialogs.ErrorMessage("Error checking connection.", e.getMessage());
                return;
            }

            boolean firstConnectMade = false;
            if (!isConnected) {
                if(!db.isInitialized()) {
                    try {
                        model.checkMySQLDump();
                    } catch (Exception e) {
                        Dialogs.ErrorMessage("Error checking MySQLDump", e.getMessage());
                        try {
                            Window.ShowFXMLWindow(getClass().getResource("./views/preferences/preferences.fxml"), stage, true, null);
                        } catch (IOException ioe) {}
                        return;
                    }

                    Dialogs.InformationMessage("Database folder initialization",
                            "Database does not have revisions. Preparing folders may take some time.");

                    try {
                        db.initFolder(true);
                    } catch (Exception e){
                        item.setExpanded(false);
                        Dialogs.ErrorMessage("Error preparing database folder", e.getMessage());
                        return;
                    }

                    firstConnectMade = true;
                }

                if ((!db.hasRevisions()) && (!db.isTraced()))
                    createInitialRevision = Dialogs.YesNoQuestion("Question", "There are no database revisions found",
                            "There are no database revisions, and you have not enabled tracing for your database changes. Would you like to make your current database dump as initial revision?");
                try {
                    db.connect(createInitialRevision);
                } catch (Exception e){
                    item.setExpanded(false);
                    Dialogs.ErrorMessage("Error connecting to database.", e.getMessage());
                    return;
                }

                if (firstConnectMade && db.isTraced()) {
                    Dialogs.Warning("Tracing was enabled", "Please reconnect all your MySQL clients to make their changes visible for the program.");
                }

                item.setGraphic(getNodeIcon(NodeIconType.CONNECTED_DATABASE));
                updateDBNode(item, db, false);
                removeDummyItem(item);
            }
            updateActionsState(item);
        }
    }

    private ImageView getNodeIcon(NodeIconType nit){
        switch (nit){
            case DATABASE:
                return new ImageView(new Image(getClass().getResourceAsStream("./views/migrator/database.png")));
            case CONNECTED_DATABASE:
                return new ImageView(new Image(getClass().getResourceAsStream("./views/migrator/database_connected.png")));
            case REVISION:
                return new ImageView(new Image(getClass().getResourceAsStream("./views/migrator/revision.png")));
            case APPLIED_REVISION:
                return new ImageView(new Image(getClass().getResourceAsStream("./views/migrator/applied_revision.png")));
        }
        return new ImageView(new Image(getClass().getResourceAsStream("./views/migrator/database.png")));
    }

    @FXML
    private void commitRevisionClick() {
        TreeItem selectedItem = (TreeItem)tvDatabases.getSelectionModel().selectedItemProperty().getValue();
        if (selectedItem == null)
            return;

        if (taScript.getText().equals("")) {
            Dialogs.ErrorMessage("Unable to commit changes", "There are no current database changes.");
            return;
        }

        Dialogs.Warning("Get ready to commit your changes", "Please disconnect all your MySQL clients before committing changes.");

        Object selectedObject = selectedItem.getValue();
        if (selectedObject instanceof Revision) {
            Boolean result = false;
            try {
                result = Window.ShowFXMLWindow(getClass().getResource("./views/commit/commit.fxml"), stage, true, controller ->
                    ((CommitController) controller).setRevision((Revision) selectedObject)
            );} catch (Exception e) {
                Dialogs.ErrorMessage("Error loading view", e.getMessage());
            }

            if (result)
                updateDBNode(selectedItem.getParent(), ((Revision)selectedObject).getDatabase(), true);
        }
    }

    @FXML
    private void getRevisionsClick() {
        TreeItem selectedItem = (TreeItem)tvDatabases.getSelectionModel().selectedItemProperty().getValue();
        if (selectedItem == null)
            return;
        Object selectedObject = selectedItem.getValue();
        if (selectedObject == null)
            return;

        if (selectedObject instanceof Database) {
            updateDBNode(selectedItem, (Database)selectedObject, true);
        } else {
            Database db = ((Revision)selectedObject).getDatabase();
            updateDBNode(selectedItem.getParent(), db, true);
        }
    }

    @FXML
    private void applyRevisionClick() {
        TreeItem selectedItem = (TreeItem)tvDatabases.getSelectionModel().selectedItemProperty().getValue();
        if (selectedItem == null)
            return;
        Object selectedObject = selectedItem.getValue();
        if (selectedObject == null)
            return;

        Revision revision = (Revision)selectedObject;

        if (model.hasPreviousUnappliedRevisions(revision)) {
            Dialogs.ErrorMessage("Unable to apply current revision", "There are previously created revisions which have not been applied. Please apply those revisions first.");
            return;
        }

        boolean applied = false;
        try {
            revision.apply();
            applied = true;
         } catch (Exception applyException) {
            if (Dialogs.YesNoQuestion("Revision cannot be applied", "Error: \"" + applyException.getMessage() + "\"", "Revision cannot be applied. Do you want to mark revision as applied?")) {
                try {
                    revision.markAsApplied();
                } catch (Exception markException) {
                    Dialogs.ErrorMessage("Error marking revision", "There was an error when marking revision as applied. " + markException.getMessage());
                    return;
                }
            }
        }

        if (applied)
            Dialogs.InformationMessage("Information", "Revision applied successfully.");

        updateDBNode(selectedItem.getParent(), revision.getDatabase(), true);
    }

    private void TreeContextMenuShow() {
        TreeItem selectedItem = (TreeItem)tvDatabases.getSelectionModel().selectedItemProperty().getValue();

        ArrayList<MenuItem> items = (selectedItem == null ? getMenuItemsByObjectType(null) : getMenuItemsByObjectType(selectedItem.getValue()));
        cmTreeContextMenu.getItems().clear();
        for (MenuItem item : items)
            cmTreeContextMenu.getItems().add(item);
    }

    private ArrayList<MenuItem> getMenuItemsByObjectType(Object object) {
        ArrayList<MenuItem> items = new ArrayList<>();

        if (object == null) {
            items.add(getMenuItem("Add Database", event -> addDatabaseClick()));
            return items;
        }

        if (object instanceof Database) {
            items.add(getMenuItem("Add Database", event -> addDatabaseClick()));
            items.add(getMenuItem("Edit Database", event -> editDatabaseClick()));
            items.add(getMenuItem("Remove Database", event -> removeDatabaseClick()));
            items.add(new SeparatorMenuItem());
            items.add(getMenuItem("Update Revisions", event -> getRevisionsClick()));
        }

        if (object instanceof Revision) {
            items.add(getMenuItem("Add Database", event -> addDatabaseClick()));
            items.add(new SeparatorMenuItem());
            if(((Revision)object).canBeApplied())
                items.add(getMenuItem("Apply Revision", event -> applyRevisionClick()));
            if(((Revision)object).canBeCommited())
                items.add(getMenuItem("Commit Revision", event -> commitRevisionClick()));

            if (items.get(items.size() - 1) instanceof SeparatorMenuItem)
                items.remove(items.size() - 1);

            items.add(new SeparatorMenuItem());
            items.add(getMenuItem("Update Revisions", event -> getRevisionsClick()));
        }

        return items;
    }

    private MenuItem getMenuItem(String caption, EventHandler<ActionEvent> handler) {
        MenuItem item = new MenuItem(caption);
        item.setOnAction(handler);
        return item;
    }
}
