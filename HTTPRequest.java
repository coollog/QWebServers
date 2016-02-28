import java.io.*;
import java.net.*;
import java.util.*;

public class HTTPRequest {
  public HTTPRequest(BufferedReader request) throws Exception {
    while (parseHeader(request.readLine()));

    checkHeaders();

    parseBody(request);
  }

  public String getMethod() { return method; }
  public String getUrl() { return url; }
  public String getHost() { return host; }
  public Date getIfModifiedSince() { return ifModifiedSince; }
  public String getUserAgent() { return userAgent; }
  public String getBody() { return body.toString(); }

  private boolean parseHeader(String line) throws Exception {
    if (line == null || line.length() == 0) return false;

    if (line.startsWith("GET") || line.startsWith("POST")) {
      String[] tokens = line.split(" ");
      method = tokens[0];
      url = tokens[1];
    } else if (line.startsWith("Host: ")) {
      host = getHeaderLineValue(line);
    } else if (line.startsWith("If-Modified-Since: ")) {
      ifModifiedSince = Utilities.parseHTTPDate(getHeaderLineValue(line));
    } else if (line.startsWith("User-Agent: ")) {
      userAgent = getHeaderLineValue(line);
    }

    return true;
  }

  private void parseBody(BufferedReader request) throws Exception {
    if (method != "POST") return;

    String line;
    while ((line = request.readLine()) != null) {
      body.append(line).append("\r\n");
    }
  }

  private String getHeaderLineValue(String line) {
    return line.substring(line.indexOf(":") + 2);
  }

  private void checkHeaders() throws Exception {
    if (method == null || url == null || host == null) {
      throw new Exception("Invalid message received.");
    }
  }

  // Headers.
  private String method;
  private String url;
  private String host;
  private Date ifModifiedSince;
  private String userAgent;

  private StringBuffer body;
}