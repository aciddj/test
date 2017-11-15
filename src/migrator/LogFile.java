package migrator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum OperationType {CONNECT, QUERY, DISCONNECT, UNKNOWN};

class RecordInfo {
	public OperationType operation;
	public int connectionId;
	public String database;
}

public class LogFile{
	private String fileName;
	private ArrayList<Integer> connectionIds;
	private String databaseName;

	LogFile(String fileName, ArrayList<Integer> connectionIds, String databaseName){
		this.fileName = fileName;
		this.connectionIds = connectionIds;
		this.databaseName = databaseName;
	}
	
	public void setFileName(String fileName){
		this.fileName = fileName;
	}

    public ArrayList<LogRecord> getRecords() throws IOException {
	    return getRecords(LocalDateTime.MIN, LocalDateTime.MAX);
    }

	public ArrayList<LogRecord> getRecords(LocalDateTime startTime, LocalDateTime endTime) throws IOException {
		boolean inMainSection = false;
		String currentSQL = "";
		ArrayList<LogRecord> records = new ArrayList<>();
   		String line;
		LocalDateTime dt = LocalDateTime.now();
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
    		while ((line = br.readLine()) != null) {
				if(!inMainSection){
					if(isTableHeader(line))
						inMainSection = true;
				} else {
					RecordInfo info;
					if ((info = parseRecordHeader(line)) != null) {
						if ((!currentSQL.equals("")) &&
								(dt.isAfter(startTime) || dt.isBefore(endTime) || dt.equals(startTime) || dt.equals(endTime))) {
                            records.add(new LogRecord(dt, wrapStatement(currentSQL, dt)));
							currentSQL = "";
						}
						switch (info.operation) {
							case QUERY:
								if (connectionIds.contains(info.connectionId)) {
									dt = getDateTime(line);
									currentSQL = getFirstQueryLine(line, true);
								}
								break;
							case CONNECT:
								if (info.database.equals(databaseName))
									if (!connectionIds.contains(info.connectionId))
									    connectionIds.add(info.connectionId);
								break;
							case DISCONNECT:
								int idx = 0;
								for (Integer connId : connectionIds) {
									if (connId == info.connectionId) {
										connectionIds.remove(idx);
										break;
									}
									idx++;
								}
								break;
						}
					} else {
						if (!currentSQL.equals(""))
							currentSQL += "\n" + line;
					}
				}
    		}
			if((!currentSQL.equals("")) && 
			(dt.isAfter(startTime) || dt.isBefore(endTime) || dt.equals(startTime) || dt.equals(endTime))){
                records.add(new LogRecord(dt, wrapStatement(currentSQL, dt)));
				currentSQL = "";
			}
		}

		return records;
	}
	
	private LocalDateTime getDateTime(String str){
		String year = str.substring(0, 4);
 		String month = str.substring(5, 7);
		String day = str.substring(8, 10);
		String hour = str.substring(11, 13);
		String minute = str.substring(14, 16);
		String second = str.substring(17, 19);
		String nanosecond = str.substring(20, 26) + "000"; // There should be 9 digits for nanoseconds

        LocalDateTime utcTimestamp = LocalDateTime.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day), Integer.parseInt(hour),
                Integer.parseInt(minute), Integer.parseInt(second), Integer.parseInt(nanosecond));

        Calendar calendar = new GregorianCalendar();
        TimeZone timeZone = calendar.getTimeZone();

		return utcTimestamp.atOffset(ZoneOffset.UTC).atZoneSameInstant(timeZone.toZoneId()).toLocalDateTime(); // converting from UTC to locale timezone
	}
	
	private RecordInfo parseRecordHeader(String str){
		Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z\\t +(\\d+) ((Query)|(Quit)|(Connect))(\\t(.*))*");
		Matcher matcher = pattern.matcher(str);
		RecordInfo info = new RecordInfo();
		if (matcher.find()) {
			info.connectionId = Integer.parseInt(matcher.group(1));
			switch (matcher.group(2)) {
				case "Connect" :
					info.operation = OperationType.CONNECT;
					break;
				case "Query" :
					info.operation = OperationType.QUERY;
					break;
				case "Quit" :
					info.operation = OperationType.DISCONNECT;
					break;
				default:
					info.operation = OperationType.UNKNOWN;
			}
			if (info.operation == OperationType.CONNECT) {
				String connString = matcher.group(7);
				Pattern pattern2 = Pattern.compile(".+ on (.+) using.+");
				Matcher matcher2 = pattern2.matcher(connString);
				if (matcher2.find())
					info.database = matcher2.group(1);
			}
		} else
			return null;

		return info;
	}

	private boolean isTableHeader(String str){
		return str.startsWith("Time                 Id Command    Argument");
	}
	
	private String getFirstQueryLine(String str, boolean filterDDL){
		String statement = "";
		Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z\\t *\\d+ ((Query)|(Quit)|(Connect))\\t(.*)");
		Matcher matcher = pattern.matcher(str);
		if (matcher.find())
    		statement = matcher.group(5);
		
		if (filterDDL){
			if ((!statement.startsWith("CREATE")) && (!statement.startsWith("ALTER")) && (!statement.startsWith("DROP")))
				return "";
		}

		return statement;
	}

	private String wrapStatement(String statement, LocalDateTime dt){
	    return  "-- " + dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)) + "\n" + statement + ";";
    }

	public ArrayList<Integer> getConnectionIds() {
		return connectionIds;
	}

	public void setConnectionIds(ArrayList<Integer> connectionIds) {
		this.connectionIds = connectionIds;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
}