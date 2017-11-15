package migrator;

import migrator.helpers.PathUtils;
import migrator.helpers.Subversion;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Walter Ego on 29.04.2017.
 */
public class DatabaseFolder {
    private static String appPath = AppFolders.getDatabasesFolder(true);

    public static boolean isInitialized(Database db){
        Path path = getDatabasePath(db);
        if (!Files.exists(path))
            return false;

        return hasWorkingCopy(path);
    }

    public static String getDatabseFolder(Database db){
        return getDatabasePath(db).toString();
    }

    private static Path getDatabasePath(Database db){
        return Paths.get(appPath,  db.getPort() + "_" + db.getName());
    }

    public static String initFolder(Database db) throws Exception {
        Path path = getDatabasePath(db);
        if (!Files.exists(path)){
            File folder = new File(path.toString());
            if(!folder.mkdirs()){
                throw new Exception("Unable to create database local path.");
            }
        }

        if (!hasWorkingCopy(path)){
            File dbFolder = new File(path.toString());
            if ((dbFolder.list() != null) && (dbFolder.list().length > 0))
                throw new Exception("Folder is not empty.");

            Subversion svn = new Subversion(Config.getInstance().getPreferences().getSvnExecutablePath(), db.getSvnUserName(), db.getSVNPassword());
            svn.checkout(db.getSvnUrl(), path.toString());
        }

        return path.toString();
    }

    private static boolean hasWorkingCopy(Path path){
        Path workingFolderPath = Paths.get(path.toString(), ".svn");
        File svnHiddenFolder = new File(workingFolderPath.toString());
        return svnHiddenFolder.exists();
    }

    public static boolean hasRevisions(Database db){
        if(!isInitialized(db))
            return false;

        Path path = getDatabasePath(db);
        File dbFolder = new File(path.toString());
        String [] folderList = dbFolder.list();
        if (folderList == null)
            return false;

        boolean revisionFound = false;
        for (String folder : folderList){
            if (folder.endsWith(".svn"))
                continue;

            String revisionPath = Paths.get(path.toString(), folder).toString();
            File revFolder = new File(revisionPath);
            if (revFolder.isDirectory()) {
                revisionFound = true;
                break;
            }
        }

        return revisionFound;
    }

    public static void updateFolder(Database db) throws Exception {
        Path path = getDatabasePath(db);
        if(!hasWorkingCopy(path))
            throw new Exception("Cannot update folder which does not contain working copy.");

        Subversion svn = new Subversion(Config.getInstance().getPreferences().getSvnExecutablePath(), db.getSvnUserName(), db.getSVNPassword());
        svn.update(path.toString());
    }

    public static void deleteFolder(Database db) throws Exception {
        Path path = getDatabasePath(db);
        PathUtils.deleteFolderWithContent(path);
    }
}
