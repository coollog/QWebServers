import java.io.*;
import java.net.*;
import java.util.*;

public class HTTPRequest {
  public HTTPRequest(BufferedReader request) throws IOException {
    setup(request);
  }

  public HTTPRequest(String request) throws IOException {
    this.request = request;
    setup(new BufferedReader(new StringReader(request)));
  }

  public void setup(BufferedReader request) throws IOException {
    if (Config.VERBOSE) System.out.println("Request header:");
    while (request.ready()) {
      parseHeader(request.readLine());
    }

    checkHeaders();

    parseBody(request);
  }

  public String getMethod() { return method; }
  public String getUrlPath() { return urlPath; }
  public String getUrlQuery() { return urlQuery; }
  public String getHost() { return host; }
  public Date getIfModifiedSince() { return ifModifiedSince; }
  public String getUserAgent() { return userAgent; }
  public boolean isLoad() { return isLoad; }

  private boolean parseHeader(String line) throws IOException {
    if (Config.VERBOSE) System.out.println(line);
    if (line == null) return false;

    if (line.startsWith("GET") || line.startsWith("POST")) {
      String[] tokens = line.split(" ");

      method = tokens[0];

      if (tokens[1].equals("/load")) {
        isLoad = true;
        return true;
      }

      int questionIndex = tokens[1].indexOf("?");
      if (questionIndex >= 0) {
        urlPath = tokens[1].substring(0, questionIndex);
        if (line.startsWith("GET")) {
          urlQuery = tokens[1].substring(questionIndex + 1, tokens[1].length());
        }
      } else {
        urlPath = tokens[1];
      }
    } else if (line.startsWith("Host: ")) {
      host = getHeaderLineValue(line);
    } else if (line.startsWith("If-Modified-Since: ")) {
      try {
        ifModifiedSince = Utilities.parseHTTPDate(getHeaderLineValue(line));
      } catch (Exception e) {}
    } else if (line.startsWith("User-Agent: ")) {
      userAgent = getHeaderLineValue(line);
    } else if (line.length() == 0) {
      lastLine = true;
    } else {
      if (Config.VERBOSE)
        System.out.println("Unrecognized header line: " + line);
    }

    return true;
  }

  private void parseBody(BufferedReader request) throws IOException {
    if (method != "POST") return;

    StringBuilder body = new StringBuilder(0x1000);

    String line;
    while ((line = request.readLine()) != null) {
      body.append(line).append("\r\n");
    }

    urlQuery = body.toString();
  }

  private String getHeaderLineValue(String line) {
    return line.substring(line.indexOf(":") + 2);
  }

  private void checkHeaders() throws IOException {
    if (method == null || urlPath == null || host == null || !lastLine) {
      throw new IOException("Invalid message received.");
    }
  }

  // Headers.
  private String method;
  private String urlPath;
  private String urlQuery = "";
  private String host;
  private Date ifModifiedSince;
  private String userAgent;
  private boolean isLoad;
  private boolean lastLine = false;

  private String request;
}