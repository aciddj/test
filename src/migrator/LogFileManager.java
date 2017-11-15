package migrator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by Walter Ego on 15.05.2017.
 */
public class LogFileManager {
    public final static String logFileNamePrefix = "mysql_";
    private String databaseName;
    private String port;
    private ArrayList<Integer> connectionIds;
    private LogFile file;

    public LogFileManager (String databaseName, String port) throws Exception {
        this.databaseName = databaseName;
        this.port = port;
        connectionIds = new ArrayList<>();
        file = new LogFile(getFilePath(databaseName, port), connectionIds, databaseName);
    }

    public static String getFilePath(String databaseName, String port) {
        return Paths.get(AppFolders.getAppFolder(true), logFileNamePrefix + port + ".log").toString();
    }

    public ArrayList<Integer> getConnectionIds() {
        return connectionIds;
    }

    public void setConnectionIds(ArrayList<Integer> connectionIds) {
        this.connectionIds = connectionIds;
    }

    public static void rotate(String databaseName, String port) throws Exception {
        deleteLog(databaseName, port);
    }

    public static void deleteLog(String databaseName, String port) throws Exception {
        Files.delete(Paths.get(getFilePath(databaseName, port)));
    }

    public ArrayList<LogRecord> getRecords() throws Exception {
        file.setConnectionIds(connectionIds);
        ArrayList<LogRecord> records = file.getRecords();
        connectionIds = file.getConnectionIds();
        return records;
    }
}
