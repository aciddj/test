package migrator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Walter Ego on 23.04.2017.
 */
public class Config {
    private static final String configFileName = "config.xml";

    private static Config instance;
    private String file;
    private ConfigStorageContainer container;

    public static Config getInstance() throws Exception{
        if (instance == null)
              instance = new Config();
        return instance;
    }

    private Config() throws Exception {
        container = new ConfigStorageContainer();
        file = Paths.get(AppFolders.getAppFolder(true), configFileName).toString();
        read();
    }

    public Preferences getPreferences(){
        return container.getPreferences();
    }

    public void savePreferences(Preferences prefs) throws JAXBException{
        container.setPreferences(prefs);
        write();
    }

    public ArrayList<Database> getDatabases(){
        return container.getDatabases();
    }

    public void saveDatabase(Database db) throws JAXBException {
        if (db.getStorageId().equals(""))
            addDatabase(db);
        else
            updateDatabase(db);
    }

    private void addDatabase(Database db) throws JAXBException {
        db.setStorageId(UUID.randomUUID().toString());
        container.addDatabase(db);
        write();
    }

    private void updateDatabase(Database db) throws JAXBException {
        container.updateDatabase(db);
        write();
    }

    public void removeDatabase(Database db) throws JAXBException {
        container.removeDatabase(db);
        write();
    }

    private void write() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ConfigStorageContainer.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(container, new File(file));
    }

    private void read() throws  JAXBException{
        File configFile = new File(file);
        if(!configFile.exists()) {
            write(); // writes current/default state
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(ConfigStorageContainer.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        container = (ConfigStorageContainer) jaxbUnmarshaller.unmarshal(configFile);
    }
}

@XmlRootElement(name = "mysql_migrator_config")
@XmlAccessorType(XmlAccessType.FIELD)
class ConfigStorageContainer{

    @XmlElement(name = "preferences")
    private Preferences prefs;

    @XmlElement(name = "database")
    private ArrayList<Database> databases;

    ConfigStorageContainer(){
        prefs = new Preferences();
        databases = new ArrayList<>();
    }

    public Preferences getPreferences() {
        return new Preferences(prefs);
    }

    public void setPreferences(Preferences prefs) {
        this.prefs = new Preferences(prefs);
    }

    public ArrayList<Database> getDatabases() {
        ArrayList<Database> dbs = new ArrayList<>();
        for (Database db : databases)
            dbs.add(new Database(db));
        return dbs;
    }

    public void setDatabases(ArrayList<Database> databases) {
        for (Database db : databases){
            this.databases.add(new Database(db));
        }
    }

    public void addDatabase(Database db){
        databases.add(new Database(db));
    }

    public void removeDatabase(Database db) {
        for (Database lodalDb : databases){
            if (lodalDb.getStorageId().equals(db.getStorageId())) {
                databases.remove(lodalDb);
                break;
            }
        }
    }

    public void updateDatabase(Database updatedDb){
        for (int i = 0; i < databases.size(); i++)
            if ((!updatedDb.getStorageId().equals("")) && updatedDb.getStorageId().equals(databases.get(i).getStorageId()))
                databases.set(i, new Database(updatedDb));
    }
}