package migrator.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by Walter Ego on 29.04.2017.
 */
public class Subversion {
    private String svnPath;
    private String userName;
    private String password;

    public Subversion (String svnPath, String userName, String password) throws Exception {
        setSvnPath(svnPath);
        this.userName = userName;
        this.password = password;
    }

    public void testConnect(String url) throws Exception {
        if (svnPath.equals(""))
            throw new Exception("Subversion path is not set.");

        String command = getCommandStart("ls") + url;
        execCommand(command);
    }

    private String getCommandStart(String operation){
        String command = "\"" + svnPath + "\"";
        if (!userName.equals(""))
            command += " --username " + userName;
        if (!password.equals(""))
            command += " --password " + password;
        command += " " + operation + " ";
        return command;
    }

    private void execCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);

        // getting output from stderr
        StringBuilder textBuilder = new StringBuilder();
        String line;
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName(StandardCharsets.UTF_8.name())));
        while ((line = reader.readLine()) != null)
            textBuilder.append(line);

        if (process.waitFor() != 0) // svn command failed
            throw new Exception(textBuilder.toString());
    }

    public void setSvnPath (String svnPath) throws Exception {
        File svn = new File(svnPath);
        if ((!svn.exists()) || svn.isDirectory())
            throw new Exception("Subversion path is incorrect.");

        this.svnPath = svnPath;
    }

    public void checkout(String url, String localPath) throws Exception {
        if (svnPath.equals(""))
            throw new Exception("Subversion path is not set.");

        String command = getCommandStart("co") + url + " \"" + localPath + "\"";
        execCommand(command);
    }

    public void update(String localPath) throws Exception {
        if (svnPath.equals(""))
            throw new Exception("Subversion path is not set.");

        String command = getCommandStart("up") +  "\"" + localPath + "\"";
        execCommand(command);
    }

    public void add(String localPath) throws Exception {
        if (svnPath.equals(""))
            throw new Exception("Subversion path is not set.");

        String command = getCommandStart("add") + "\"" + localPath + "\"";
        execCommand(command);
    }

    public void commit(String localPath, String commitMessage) throws Exception {
        if (svnPath.equals(""))
            throw new Exception("Subversion path is not set.");

        String command = getCommandStart("commit") + "\"" + localPath + "\" -m \"" + commitMessage + "\"";
        execCommand(command);
    }
}
