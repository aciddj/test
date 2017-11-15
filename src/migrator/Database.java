package migrator;

import migrator.helpers.Dialogs;
import migrator.helpers.Encryptor;
import javax.xml.bind.annotation.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by Walter Ego on 22.04.2017.
 */
@XmlRootElement(name = "databases")
@XmlAccessorType(XmlAccessType.FIELD)
public class Database {

    @XmlElement(name = "id")
    private String storageId;

    private String name;
    private String port;

    @XmlElement(name = "svn_url")
    private String svnUrl;

    @XmlElement(name = "svn_user_name")
    private String svnUserName;

    @XmlElement(name = "encrypted_svn_password")
    private String encryptedSVNPassword;

    private ArrayList<Revision> revisions;

    @XmlElement(name = "trace_changes")
    private boolean traceChanges;

    @XmlElement(name = "user_name")
    private String userName;

    @XmlElement(name = "encrypted_password")
    private String encryptedPassword;

    @XmlElement(name = "applied_revision")
    private ArrayList<String> appliedRevisionIds;

    @XmlElement(name = "connection_id")
    private ArrayList<Integer> connectionIds;

    @XmlTransient
    private DatabaseConnection connection;

    public Database(){
        connection = new DatabaseConnection("localhost", "3306", "test", "root", "");
        this.storageId = "";
        revisions = new ArrayList<>();
        traceChanges = false;
        appliedRevisionIds = new ArrayList<>();
        connectionIds = new ArrayList<>();
    }

    public Database(String name, String port, String userName, String password, String svnUrl, String svnUserName, String svnPassword, boolean traceChanges){
        this.name = name;
        this.port = port;
        this.svnUrl = svnUrl;
        this.traceChanges = traceChanges;
        this.userName = userName;
        this.svnUserName = svnUserName;
        setSVNPassword(svnPassword);
        setPassword(password);
        this.storageId = "";
        revisions = new ArrayList<>();
        connectionIds = new ArrayList<>();
        connection = new DatabaseConnection("localhost", port, name, userName, password);
        appliedRevisionIds = new ArrayList<>();
    }

    public Database (Database db){
        this.name = db.getName();
        this.port = db.getPort();
        this.svnUrl = db.getSvnUrl();
        this.userName = db.getUserName();
        this.svnUserName = db.getSvnUserName();
        this.setPassword(db.getPassword());
        this.setSVNPassword(db.getSVNPassword());
        this.storageId = db.getStorageId();
        this.traceChanges = db.isTraced();
        this.revisions = new ArrayList<>();
        this.connection = new DatabaseConnection("localhost", db.getPort(), db.getName(), db.getUserName(), db.getPassword());
        this.appliedRevisionIds = new ArrayList<>();
        this.appliedRevisionIds.addAll(db.getAppliedRevisionIds());
        this.connectionIds = new ArrayList<>();
        this.connectionIds.addAll(db.getConnectionIds());

        for (Revision rev : db.getRevisions())
            this.revisions.add(new Revision(rev));
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
        this.connection.setPort(port);
    }

    public String getSvnUrl() {
        return svnUrl;
    }

    public void setSvnUrl(String svnUrl) {
        this.svnUrl = svnUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.connection.setDatabaseName(name);
    }

    public String getStorageId() {
        return storageId;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    @Override
    public String toString(){
        return getName();
    }

    public boolean isConnected() throws SQLException {
        return connection.isConnected();
    }

    public void connect(boolean createInitialRevisionIfNeed) throws Exception {
        connection.connect();
        initFolder(false);

        if (traceChanges)
            enableTracing();

        boolean createInitialRevision = false;

        if (!hasRevisions())
            createInitialRevision = createInitialRevisionIfNeed;

        fillRevisions(createInitialRevision, false);
    }

    public void initFolder(boolean update) throws Exception {
        if (!DatabaseFolder.isInitialized(this)) {
            DatabaseFolder.initFolder(this);
        } else {
            if (update)
                DatabaseFolder.updateFolder(this);
        }
    }

    private void fillRevisions(boolean createInitialRevision, boolean withUpdate) throws Exception {
        if (withUpdate)
            DatabaseFolder.updateFolder(this);
        RevisionManager manager = new RevisionManager(this);
        revisions = manager.getRevisions(createInitialRevision, traceChanges);
        revisions.sort(Comparator.comparing(Revision::getTimestamp));
    }

    public void disconnect() throws SQLException {
        connection.disconnect();
    }

    public void save() throws Exception {
        Config cfg = Config.getInstance();
        cfg.saveDatabase(this);
    }

    public ArrayList<Revision> getRevisionsWithRefresh() throws Exception {
        fillRevisions(false, true);
        return getRevisions();
    }

    public ArrayList<Revision> getRevisions() {
        return revisions;
    }

    public boolean isTraced() {
        return traceChanges;
    }

    public void setTraceChanges(boolean traceChanges) {
        this.traceChanges = traceChanges;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
        this.connection.setUserName(userName);
    }

    public String getPassword() {
        if (encryptedPassword.equals(""))
            return "";

        String decryptedPassword = "";
        try {
            decryptedPassword = Encryptor.decrypt(encryptedPassword);
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error restoring password", "Cannot decrypt password. " + e.getMessage());
        }

        return decryptedPassword;
    }

    public void setPassword(String password) {
        try {
            encryptedPassword = Encryptor.encrypt(password);
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error setting password", "Cannot encrypt password. " + e.getMessage());
            return;
        }

        if (connection != null)
            connection.setPassword(password);
    }

    public String getSVNPassword() {
        if (encryptedSVNPassword.equals(""))
            return "";

        String decryptedPassword = "";
        try {
            decryptedPassword = Encryptor.decrypt(encryptedSVNPassword);
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error restoring password", "Cannot decrypt password. " + e.getMessage());
        }

        return decryptedPassword;
    }

    public void setSVNPassword(String password) {
        try {
            encryptedSVNPassword = Encryptor.encrypt(password);
        } catch (Exception e) {
            Dialogs.ErrorMessage("Error setting password", "Cannot encrypt password. " + e.getMessage());
            return;
        }
    }

    public boolean isInitialized(){
        return DatabaseFolder.isInitialized(this);
    }

    public String getPath(){
        return DatabaseFolder.getDatabseFolder(this);
    }

    public String getLogFilePath() {
        return LogFileManager.getFilePath(name, port);
    }

    public ArrayList<String> getAppliedRevisionIds() {
        return appliedRevisionIds;
    }

    public void setAppliedRevisionIds(ArrayList<String> appliedRevisionIds) {
        this.appliedRevisionIds = appliedRevisionIds;
    }

    public void markRevisionAsApplied(Revision revision) throws Exception {
        appliedRevisionIds.add(revision.getId());
        save();
    }

    public void applyRevision(Revision revision) throws Exception {
        connection.execSQL(revision.getSQL());
        markRevisionAsApplied(revision);
    }

    public boolean isApplied(Revision revision) {
        return appliedRevisionIds.contains(revision.getId());
    }

    public boolean hasPreviousUnappliedRevisions(Revision currentRevision) {
        for (Revision revision : revisions) {
            if (revision == currentRevision)
                break;

            if (revision.canBeApplied() && revision.getTimestamp().isBefore(currentRevision.getTimestamp()))
                return true;
        }
        return false;
    }

    public String getSvnUserName() {
        return svnUserName;
    }

    public void setSvnUserName(String svnUserName) {
        this.svnUserName = svnUserName;
    }

    public boolean hasRevisions() {
        return DatabaseFolder.hasRevisions(this);
    }

    public void enableTracing() throws Exception {
        String logPath = getLogPath();
        if (connection.isLogEnabled()) {
            if (!logPath.equals(connection.getLogPath()))
                throw new Exception("MySQL general log is already enabled for this server.");

        } else {
            connection.setLogPath(logPath);
            connection.enableLog();
        }
    }

    public void disableTracing() throws Exception {
        connection.disableLog();
    }

    public String getLogPath() {
        return LogFileManager.getFilePath(name, port);
    }

    public ArrayList<LogRecord> getLogRecords() throws Exception {
        LogFileManager logManager = new LogFileManager(name, port);
        logManager.setConnectionIds(connectionIds);
        ArrayList<LogRecord> records = logManager.getRecords();
        connectionIds = logManager.getConnectionIds();
        save();
        return records;
    }

    public ArrayList<Integer> getConnectionIds() {
        return connectionIds;
    }

    public void setConnectionIds(ArrayList<Integer> connectionIds) throws Exception {
        this.connectionIds = connectionIds;
        save();
    }

    public void rotateLog(boolean clearSavedConnections) throws Exception {
        connection.disableLog();

        if (clearSavedConnections) {
            connectionIds.clear();
            save();
        }

        LogFileManager.rotate(name, port);
        connection.enableLog();
    }

    public void deleteLog(boolean clearSavedConnections) throws Exception {
        if (clearSavedConnections) {
            connectionIds.clear();
            save();
        }

        LogFileManager.deleteLog(name, port);
    }

    public void deleteFolder() throws Exception {
        DatabaseFolder.deleteFolder(this);
    }
}
