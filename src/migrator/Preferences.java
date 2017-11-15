package migrator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Walter Ego on 23.04.2017.
 */
@XmlRootElement(name = "preferences")
@XmlAccessorType (XmlAccessType.FIELD)
public class Preferences {

    @XmlElement(name = "svn_executable_path")
    private String svnExecutablePath;

    @XmlElement(name = "mysqldump_path")
    private String mySQLDumpPath;

    @XmlElement(name = "first_launch")
    private boolean firstLaunch = true;

    public Preferences(String svnExecutablePath, String mySQLDumpPath){
        this.svnExecutablePath = svnExecutablePath;
        this.mySQLDumpPath = mySQLDumpPath;
        this.firstLaunch = true;
    }

    public Preferences(){
        this.svnExecutablePath = "svn.exe";
        this.mySQLDumpPath = "mysqldump.exe";
        this.firstLaunch = true;
    }

    public Preferences(Preferences prefs){
        this.svnExecutablePath = prefs.getSvnExecutablePath();
        this.mySQLDumpPath = prefs.getMySQLDumpPath();
        this.firstLaunch = prefs.firstLaunch;
    }

    public String getSvnExecutablePath() {
        return svnExecutablePath;
    }

    public void setSvnExecutablePath(String svnExecutablePath) {
        this.svnExecutablePath = svnExecutablePath;
    }

    public String getMySQLDumpPath() {
        return mySQLDumpPath;
    }

    public void setMySQLDumpPath(String mySQLDumpPath) {
        this.mySQLDumpPath = mySQLDumpPath;
    }

    public boolean isFirstLaunch() {
        return firstLaunch;
    }

    public void setFirstLaunch(boolean firstLaunch) {
        this.firstLaunch = firstLaunch;
    }
}
