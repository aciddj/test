package migrator;

import java.time.LocalDateTime;

/**
 * Created by Walter Ego on 28.04.2017.
 */
public class LogRecord{
    private LocalDateTime time;
    private String sql;

    LogRecord(LocalDateTime time, String sql){
        this.time = time;
        this.sql = sql;
    }

    public void setTime(LocalDateTime time){
        this.time = time;
    }

    public LocalDateTime getTime(){
        return time;
    }

    public void setSQL(String sql){
        this.sql = sql;
    }

    public String getSQL(){
        return sql;
    }
}
