import java.text.*;
import java.util.*;
import java.util.regex.*;

public class Utilities {
  public static String[] tokenize(String line) {
    // Code from StackOverflow.
    String regex = "\"(\\\"|[^\"])*?\"|[^ ]+";
    Matcher matcher = Pattern.compile(regex).matcher(line);
    List<String> matches = new ArrayList<String>();
    while (matcher.find()) matches.add(matcher.group());
    return matches.toArray(new String[] {});
  }

  public static Date parseHTTPDate(String dateString) throws Exception {
    // Code from StackOverflow.
    SimpleDateFormat dateFormat =
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    return dateFormat.parse(dateString);
  }

  public static String getHTTPDate() {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat dateFormat =
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("EST"));
    return dateFormat.format(calendar.getTime());
  }
}