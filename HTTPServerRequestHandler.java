import java.io.*;
import java.net.*;
import java.util.*;

public class HTTPServerRequestHandler {
  public HTTPServerRequestHandler(Config config, Socket conn, Cache cache) {
    this.config = config;
    this.conn = conn;
    this.cache = cache;
  }

  public void handle() throws Exception {
    // Define send/receive buffers.
    BufferedReader receiveBuffer =
      new BufferedReader(new InputStreamReader(conn.getInputStream()));
    DataOutputStream sendBuffer =
      new DataOutputStream(conn.getOutputStream());

    // Process the request.
    HTTPRequest request = new HTTPRequest(receiveBuffer);

    setStatus(200);

    // Handle special /load request.
    if (request.isLoad()) {
      setStatus(503);
    } else {
      // Do the request.
      getContent(request);
    }

    // Construct the response.
    HTTPResponse response = new HTTPResponse(
      statusCode, statusMessage, request.getHost(), lastModified, content);

    // Send the response.
    String sendContent = new String(response.getContent(), "ASCII");
    sendBuffer.writeBytes(response.getHeader());
    sendBuffer.writeBytes(sendContent);

    if (Config.VERBOSE)
      System.out.println("Response header: \n" + response.getHeader());

    // Close the connection.
    conn.close();
  }

  private void setStatus(int code) {
    statusCode = code;
    switch (code) {
    case 200: statusMessage = "OK"; break;
    case 304: statusMessage = "Not Modified"; break;
    case 404: statusMessage = "Not Found"; break;
    case 503: statusMessage = "Overload"; break;
    }
  }

  private void getContent(HTTPRequest request) throws Exception {
    String urlPath = request.getUrlPath();
    String urlQuery = request.getUrlQuery();
    String serverName = request.getHost();
    String method = request.getMethod();
    String userAgent = request.getUserAgent();
    Date ifModifiedSince = request.getIfModifiedSince();

    String root = config.getDocumentRoot(serverName);
    if (root == null) { setStatus(404); return; }

    File file = getFile(root, urlPath, userAgent);
    if (Config.VERBOSE)
      System.out.println("READING FILE " + root + "," + urlPath + "," + userAgent);

    if (file == null) { setStatus(404); return; }

    if (file.canExecute()) {
      executeFile(file, urlQuery, serverName, method);
    } else {
      readFile(file, ifModifiedSince);
    }
  }

  // Helper method for getContent().
  private File getFile(String root, String url, String userAgent) {
    String rootUrl = root + "/" + url;

    // If just normal file.
    File file = new File(rootUrl);
    if (file.isFile()) return file;

    // If is directory, look for index.html/index_m.html.
    if (url.endsWith("/") || file.isDirectory()) {
      if (userAgent != null && userAgent.contains("iPhone")) {
        file = new File(rootUrl + "/index_m.html");
        if (file.isFile()) return file;
      }
      file = new File(rootUrl + "/index.html");
      if (file.isFile()) return file;
    }

    return null;
  }

  // Helper method for getContent().
  private void executeFile(File file,
                           String urlQuery,
                           String serverName,
                           String method) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(file.getAbsolutePath());

    // Set up process.
    Map<String, String> env = pb.environment();
    env.put("QUERY_STRING", urlQuery);
    env.put("REMOTE_ADDR", conn.getInetAddress().getHostAddress());
    env.put("REMOTE_HOST", conn.getInetAddress().getHostName());
    env.put("REQUEST_METHOD", method);
    env.put("SERVER_NAME", serverName);
    env.put("SERVER_PORT", Integer.toString(config.getPort()));
    env.put("SERVER_PROTOCOL", "HTTP/1.0");
    env.put("SERVER_SOFTWARE", "QWebServer/0.1");
    pb.directory(new File(file.getParent()));

    // Run process.
    Process process = pb.start();
    process.waitFor();

    // Capture output.
    InputStream stdout = process.getInputStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

    StringBuilder stdoutString = new StringBuilder(0x1000);
    String line;
    while ((line = reader.readLine()) != null) {
      stdoutString.append(line).append("\n");
    }
    int contentLength = stdoutString.length();
    content = stdoutString.toString().getBytes();

    lastModified = Utilities.now();
  }

  // Helper method for getContent().
  private void readFile(File file, Date ifModifiedSince) throws Exception {
    lastModified = new Date(file.lastModified());

    // Check if modified since ifModifiedSince.
    if (ifModifiedSince != null && ifModifiedSince.after(lastModified)) {
      setStatus(304);
      return;
    }

    // Check cache.
    String path = file.getCanonicalPath();
    if (Config.VERBOSE) System.out.println("CHECKING CACHE");
    if (cache.hasKey(path)) {
      Cache.DateContentPair entry = cache.get(path);
      if (Config.VERBOSE) System.out.println(entry.toString());
      if (lastModified.before(entry.lastRetrievedDate)) {
        if (Config.VERBOSE) System.out.println("CACHE HIT");
        content = entry.content;
        return;
      }
    }

    // Read in file.
    int contentLength = (int)file.length();
    FileInputStream fileStream  = new FileInputStream(file);
    BufferedInputStream stream = new BufferedInputStream(fileStream);

    content = new byte[contentLength];
    stream.read(content);

    cache.put(path, content);
  }

  private Config config;
  private Socket conn;
  private Cache cache;

  private byte[] content;
  private int statusCode;
  private String statusMessage;
  private Date lastModified;
}