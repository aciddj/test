package migrator;

import migrator.helpers.Subversion;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by Walter Ego on 21.04.2017.
 */
public class MigratorModel {
    private ArrayList<Database> databases = new ArrayList<>();
    private boolean databasesLoaded = false;

    private static final MigratorModel instance = new MigratorModel();
    private MigratorModel() {};

    public static MigratorModel getInstance() {
        return instance;
    }

    public ArrayList<Database> getDatabases() throws Exception {
        if (!databasesLoaded) {
            databases = Config.getInstance().getDatabases();
            databasesLoaded = true;
        }
        return databases;
    }

    public void addDatabase(String port, String databaseName, String userName, String password, String svnURL, String svnUserName, String svnPassword, boolean traceChanges) throws Exception {
        checkConnection(port, databaseName, userName, password);
        checkSVNData(svnURL, svnUserName, svnPassword);

        Database db = new Database(databaseName, port, userName, password, svnURL, svnUserName, svnPassword, traceChanges);
        try {
            db.save();
        } catch (Exception e) {
            throw new Exception("Error saving new database.");
        }

        databases.add(db);
    }

    public void updateDatabase(Database db, String port, String databaseName, String userName, String password, String svnURL, String svnUserName, String svnPassword, boolean traceChanges) throws Exception {
        checkConnection(port, databaseName, userName, password);
        checkSVNData(svnURL, svnUserName, svnPassword);

        boolean wasTraced = db.isTraced();

        db.setPort(port);
        db.setName(databaseName);
        db.setUserName(userName);
        db.setPassword(password);
        db.setSvnUrl(svnURL);
        db.setSvnUserName(svnUserName);
        db.setSVNPassword(svnPassword);
        db.setTraceChanges(traceChanges);

        if (db.isConnected()) {
            if (wasTraced && !traceChanges) {
                db.disableTracing();
                db.deleteLog(true);
            }
            db.disconnect();
            db.connect(false);
        }

        try {
            db.save();
        } catch (Exception e) {
            throw new Exception("Error updating database.");
        }
    }

    public void removeDatabase(Database db) throws Exception {
        db.disableTracing();
        db.deleteLog(true);
        db.disconnect();
        databases.remove(db);
        Config.getInstance().removeDatabase(db);
        db.deleteFolder();
    }

    private void checkConnection(String port, String databaseName, String userName, String password) throws Exception {
        try {
            DatabaseConnection.checkConnectionParams("localhost", port, databaseName, userName, password);
        } catch (Exception e) {
            throw new Exception("MySQL connection error. " + e.getMessage());
        }
    }

    private void checkSVNData(String svnURL, String svnUserName, String svnPassword) throws Exception {
        String svnPath = Config.getInstance().getPreferences().getSvnExecutablePath();
        Subversion svn = new Subversion(svnPath, svnUserName, svnPassword);
        try {
            svn.testConnect(svnURL);
        } catch (Exception e) {
            throw new Exception("SVN connection error. " + e.getMessage());
        }
    }

    public boolean hasPreviousUnappliedRevisions(Revision revision) {
        return revision.getDatabase().hasPreviousUnappliedRevisions(revision);
    }

    public boolean isFirstLaunch() throws Exception {
        return Config.getInstance().getPreferences().isFirstLaunch();
    }

    public void resetFirstLaunch() throws Exception {
        Preferences prefs = Config.getInstance().getPreferences();
        prefs.setFirstLaunch(false);
        Config.getInstance().savePreferences(prefs);
    }

    public void checkPreferences() throws Exception {
        checkSVN();
        checkMySQLDump();
    }

    public void checkSVN() throws Exception {
        Path svnPath = Paths.get(Config.getInstance().getPreferences().getSvnExecutablePath());
        if (svnPath.toFile() == null || (!svnPath.toFile().exists()))
            throw new Exception("Path to SVN executable is not set or does not exist");
    }

    public void checkMySQLDump() throws Exception {
        Path svnPath = Paths.get(Config.getInstance().getPreferences().getMySQLDumpPath());
        if (svnPath.toFile() == null || (!svnPath.toFile().exists()))
            throw new Exception("Path to MySQLDump executable is not set or does not exist");
    }
}
