package migrator.helpers;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;

/**
 * Created by Walter Ego on 01.05.2017.
 */
public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {
    public LocalDateTime unmarshal(String ts) throws Exception {
        return LocalDateTime.parse(ts);
    }

    public String marshal(LocalDateTime ts) throws Exception {
        return ts.toString();
    }
}
