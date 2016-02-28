import java.io.*;
import java.net.*;
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

  public static Date now() {
    return Calendar.getInstance().getTime();
  }

  public static String getHTTPDate(Date date) {
    // Code from StackOverflow.
    SimpleDateFormat dateFormat =
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("EST"));
    return dateFormat.format(date);
  }
  public static String getHTTPDate() {
    return getHTTPDate(now());
  }

  public static HashMap<String, String>
  splitURLQuery(String query) throws UnsupportedEncodingException {
    HashMap<String, String> queryPairs = new HashMap<String, String>();
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      queryPairs.put(
        URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
        URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    return queryPairs;
  }
}