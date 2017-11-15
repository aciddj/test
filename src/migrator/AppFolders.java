package migrator;

import migrator.helpers.OS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Walter Ego on 11.05.2017.
 */
public class AppFolders {
    private final static String appFolderName = "MySQLMigrator";
    private final static String databasesSubFolderName = "db";

    public static String getAppFolder(boolean createIfNotExists)  {
        Path appFolder = Paths.get(getUserHomeFolder(), "." + appFolderName.toLowerCase());
        if (OS.isWindows()) {
            if (OS.getVersionFirstDigit() > 5) // Vista and higher
                appFolder = Paths.get(getUserHomeFolder(), "AppData", "Local", appFolderName);
            else // XP
                appFolder = Paths.get(getUserHomeFolder(), "Local Settings", "Application Data", appFolderName);

        }
        if (OS.isMac()) {
            appFolder = Paths.get(getUserHomeFolder(), "Library", "Application Support", appFolderName);
        }

        if (createIfNotExists) {
            if (!appFolder.toFile().exists()) {
                appFolder.toFile().mkdir();

                try {
                    if (OS.isWindows())
                        Runtime.getRuntime().exec("icacls \"" + appFolder.toString() + "\" /grant \"*S-1-1-0:(OI)(CI)F\"").waitFor();
                    else
                        Runtime.getRuntime().exec("chmod 777 \"" + appFolder.toString()).waitFor();
                } catch (Exception e) {}
            }
        }

        return appFolder.toString();
    }

    public static String getUserHomeFolder() {
        return System.getProperty("user.home");
    }

    public static String getDatabasesFolder(boolean createIfNotExists) {
        Path dbFolder = Paths.get(getAppFolder(createIfNotExists), databasesSubFolderName);

        if (createIfNotExists) {
            if (!dbFolder.toFile().exists()) {
                dbFolder.toFile().mkdir();
            }
        }

        return dbFolder.toString();
    }
}
