package base;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MainBaseClass {
	Instant startTime = null;

	public MainBaseClass() {
		startTime = Instant.now();
		timeNow(ZonedDateTime.now(), "Start Time: ");
	}

	public void timeNow(ZonedDateTime zonedDateTime, String msg) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		String formattedTime = formatter.format(zonedDateTime);
		System.out.println(msg + formattedTime);
	}

	public void closingTime() {
	      Instant endTime = Instant.now();
	      Duration duration = Duration.between(startTime, endTime);
	      long seconds = duration.getSeconds();
	      String durStr = duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();
	      System.out.println("[DBG] Duration: " + seconds + ", " + durStr);
	      timeNow(ZonedDateTime.now(), "End Time: ");
	}
}
