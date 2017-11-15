package migrator;

import migrator.helpers.Subversion;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by Walter Ego on 30.04.2017.
 */
public class RevisionManager {
    private Database database;

    public RevisionManager(Database db){
        database = db;
    }

    public ArrayList<Revision> getRevisions(boolean createInitialRevision, boolean addCurrentRevision) throws Exception {
        ArrayList<Revision> revisions = new ArrayList<>();

        if (createInitialRevision)
            createInitialRevision();

        revisions.addAll(loadRevisions());

        if (addCurrentRevision)
            revisions.add(createCurrentRevision());

        return revisions;
    }

    private Revision createInitialRevision() throws Exception {
        Revision revision = new Revision(database);
        revision.setComment("Full database metadata dump");
        String dumpFilePath = Paths.get(revision.getPath(), Revision.scriptFileName).toString();
        saveRevisionFolder(revision, false);
        dumpDatabase(database, dumpFilePath);
        commitRevisionFolder(revision);
        database.markRevisionAsApplied(revision);

        return revision;
    }

    private ArrayList<Revision> loadRevisions() throws JAXBException {
        ArrayList<Revision> revisions = new ArrayList<>();
        String databasePath = database.getPath();
        File dbFolder = new File(databasePath);
        String [] folderList = dbFolder.list();

        for (String folder : folderList) {
            if (folder.endsWith(".svn"))
                continue;

            String revisionPath = Paths.get(databasePath, folder).toString();
            File revFolder = new File(revisionPath);
            revisions.add(loadRevision(revFolder.toString()));
        }

        return revisions;
    }

    private Revision createCurrentRevision(){
        Revision revision = new Revision(database);
        revision.setCurrent(true);
        return revision;
    }

    private void dumpDatabase(Database database, String dumpFileName) throws Exception {
        String dumpUtil = Config.getInstance().getPreferences().getMySQLDumpPath();
        String command = "\"" + dumpUtil + "\" --no-data";
        command += " --port=" + database.getPort();
        command += " --user=" + database.getUserName();
        command += " --password=" + database.getPassword();
        command += " --result-file=\"" + dumpFileName + "\"";
        command += " " + database.getName();
        Runtime.getRuntime().exec(command);
    }

    private void saveRevision(Revision revision) throws Exception {
        String dataFilePath = Paths.get(revision.getPath(), Revision.revisionDataFileName).toString();
        JAXBContext jaxbContext = JAXBContext.newInstance(Revision.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(revision, new File(dataFilePath));
    }

    private Revision loadRevision(String revisionPath) throws JAXBException {
        String dataFilePath = Paths.get(revisionPath, Revision.revisionDataFileName).toString();
        JAXBContext jaxbContext = JAXBContext.newInstance(ConfigStorageContainer.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        Revision revision = (Revision) jaxbUnmarshaller.unmarshal(new File(dataFilePath));
        revision.setDatabase(database);
        return revision;
    }

    public void commitRevision(Revision revision) throws Exception {
        if (!revision.getCurrent())
            throw new Exception("Cannot commit previously saved revision.");

        saveRevisionFolder(revision, true);
        database.rotateLog(true);
        commitRevisionFolder(revision);
        revision.setCurrent(false);
    }

    private void saveRevisionFolder(Revision revision, boolean saveSQL) throws Exception {
        String sql = revision.getSQL().trim();
        if (sql.equals("")) {
            throw new Exception("Revision is empty.");
        }

        new File(revision.getPath()).mkdir();

        if (saveSQL) {
            Path scriptFile = Paths.get(revision.getPath().toString(), Revision.scriptFileName);
            Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(scriptFile.toString()), "UTF-8"));
            try {
                out.write(revision.getSQL());
            } finally {
                out.close();
            }
        }

        saveRevision(revision);
    }

    private void commitRevisionFolder(Revision revision) throws Exception {
        Subversion svn = new Subversion(Config.getInstance().getPreferences().getSvnExecutablePath(), database.getSvnUserName(), database.getSVNPassword());
        svn.add(revision.getPath());
        svn.commit(revision.getPath(), revision.getComment());
    }
}
