import java.io.*;
import java.net.*;
import java.util.*;

public class HTTPServerRequestHandler implements Runnable {
  public HTTPServerRequestHandler(Config config, Socket conn, Cache cache) {
    this.config = config;
    this.conn = conn;
    this.cache = cache;
  }
  public void run() {
    try {
      handle();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public void handle() throws Exception {
    // Define send/receive buffers.
    BufferedReader receiveBuffer =
      new BufferedReader(new InputStreamReader(conn.getInputStream()));
    DataOutputStream sendBuffer =
      new DataOutputStream(conn.getOutputStream());

    // Process the request.
    HTTPRequest request = new HTTPRequest(receiveBuffer);

    statusCode = 200;
    statusMessage = "OK";

    // Handle special /load request.
    if (request.isLoad()) {
      statusCode = 503;
      statusMessage = "Overload";
    } else {
      // Do the request.
      getContent(request);
    }

    // Construct the response.
    HTTPResponse response = new HTTPResponse(
      statusCode, statusMessage, request.getHost(), lastModified, content);

    // Send the response.
    sendBuffer.writeBytes(response.getHeader());
    sendBuffer.write(response.getContent(), 0, response.getContent().length);

    // Close the connection.
    conn.close();
  }

  private void getContent(HTTPRequest request) throws Exception {
    String urlPath = request.getUrlPath();
    HashMap<String, String> urlQuery = request.getUrlQuery();
    String serverName = request.getHost();
    String userAgent = request.getUserAgent();
    Date ifModifiedSince = request.getIfModifiedSince();
    String root = config.getDocumentRoot(serverName);

    File file = getFile(root, urlPath, userAgent);

    if (file == null) {
      statusCode = 404;
      statusMessage = "Not found";
      return;
    }

    if (file.canExecute()) {
      executeFile(file, root, urlPath, urlQuery);
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
      if (userAgent.contains("iPhone")) {
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
                           String root,
                           String urlPath,
                           HashMap<String, String> urlQuery) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(urlPath);

    // Set up process.
    Map<String, String> env = pb.environment();
    env.putAll(urlQuery);
    pb.directory(new File(root + "/" + file.getParent()));

    // Run process.
    Process process = pb.start();
    process.waitFor();

    InputStream stdout = process.getInputStream();
    int contentLength = stdout.available();
    content = new byte[contentLength];
    stdout.read(content);

    lastModified = Utilities.now();
  }

  // Helper method for getContent().
  private void readFile(File file, Date ifModifiedSince) throws Exception {
    lastModified = new Date(file.lastModified());

    // Check if modified since ifModifiedSince.
    if (ifModifiedSince != null && ifModifiedSince.after(lastModified)) {
      statusCode = 304;
      statusMessage = "Not Modified";
      return;
    }

    // Check cache.
    String path = file.getCanonicalPath();
    if (cache.hasKey(path)) {
      Cache.DateContentPair entry = cache.get(path);
      if (lastModified.before(entry.modifiedDate)) {
        content = entry.content;
        return;
      }
    }

    // Read in file.
    int contentLength = (int)file.length();
    FileInputStream fileStream  = new FileInputStream(file);

    content = new byte[contentLength];
    fileStream.read(content);

    cache.put(path, lastModified, content);
  }

  private Config config;
  private Socket conn;
  private Cache cache;

  private byte[] content;
  private int statusCode;
  private String statusMessage;
  private Date lastModified;
}