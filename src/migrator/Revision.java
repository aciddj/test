package migrator;

import migrator.helpers.LocalDateTimeAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Walter Ego on 22.04.2017.
 */
@XmlRootElement(name = "revision")
@XmlAccessorType(XmlAccessType.FIELD)
public class Revision {
    @XmlTransient
    public static final String revisionDataFileName = "data.xml";
    @XmlTransient
    public static final String scriptFileName = "script.sql";

    private String id;
    private String comment;

    @XmlJavaTypeAdapter(value = LocalDateTimeAdapter.class)
    private LocalDateTime timestamp;

    @XmlTransient
    private Database database;

    @XmlTransient
    private boolean current = false;

    public Revision(){
        id = UUID.randomUUID().toString();
        timestamp = LocalDateTime.now();
        comment = "";
    }

    public Revision(Database db){
        id = UUID.randomUUID().toString();
        timestamp = LocalDateTime.now();
        database = db;
        comment = "";
    }

    public Revision(Revision rev){
        id = rev.getId();
        timestamp = rev.getTimestamp();
        comment = rev.getComment();
    }

    @Override
    public String toString(){
        if (current)
            return "Current changes";

        return timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() throws Exception {
        if (database == null)
            throw new Exception("Database is not initialized.");

        return Paths.get(database.getPath(), getId()).toString();
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public boolean getCurrent() {
        return current;
    }

    public void commit(String comment) throws Exception {
        if (!current) {
            throw new Exception("Only current changes can be committed.");
        }

        setComment(comment);
        RevisionManager revManager = new RevisionManager(database);
        revManager.commitRevision(this);
        markAsApplied();
    }

    public boolean canBeCommited() {
        return current;
    }

    public String getSQL() throws Exception {
        String sql = "";
        if (!current) {
            Path scriptFile = Paths.get(getPath().toString(), scriptFileName);
            byte[] encoded = Files.readAllBytes(scriptFile); // TODO : load file partially as a preview ?? optionally ??
            return new String(encoded, StandardCharsets.UTF_8);
        }

        ArrayList<LogRecord> records = database.getLogRecords();
        for (LogRecord record : records) {
            sql += record.getSQL() + "\n\n";
        }

        return sql;
    }

    public void markAsApplied() throws Exception {
        database.markRevisionAsApplied(this);
    }

    public void apply() throws Exception {
        database.applyRevision(this);
    }

    public boolean canBeApplied() {
        return !(isCurrent() || database.isApplied(this));
    }
}
