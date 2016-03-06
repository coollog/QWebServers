import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

public class HTTPServerRequestHandler {
  public HTTPServerRequestHandler(Config config, Socket conn, Cache cache) {
    this.config = config;
    this.conn = conn;
    this.cache = cache;

    hostAddress = conn.getInetAddress().getHostAddress();
    hostName = conn.getInetAddress().getHostName();
  }

  public HTTPServerRequestHandler(Config config,
                                  AsynchronousSocketChannel channel,
                                  Cache cache) throws Exception {
    this.config = config;
    this.cache = cache;

    InetSocketAddress address = (InetSocketAddress)channel.getRemoteAddress();
    hostAddress = address.getAddress().getHostAddress();
    hostName = address.getHostName();
  }

  public void handle() throws Exception {
    // Define send/receive buffers.
    BufferedReader receiveBuffer =
      new BufferedReader(new InputStreamReader(conn.getInputStream()));
    DataOutputStream sendBuffer =
      new DataOutputStream(conn.getOutputStream());

    // Process the request.
    HTTPRequest request = new HTTPRequest(receiveBuffer);

    // Construct the response.
    HTTPResponse response = getResponse(request);

    // Send the response.
    String sendContent = new String(response.getContent(), "ASCII");
    sendBuffer.writeBytes(response.getHeader());
    sendBuffer.writeBytes(sendContent);

    if (Config.VERBOSE)
      System.out.println("Response header: \n" + response.getHeader());

    // Close the connection.
    conn.close();
  }

  public HTTPResponse getResponse(HTTPRequest request) throws IOException {
    handleRequest(request);

    return new HTTPResponse(statusCode,
                            statusMessage,
                            request.getHost(),
                            lastModified,
                            contentType,
                            content);
  }

  private void handleRequest(HTTPRequest request) throws IOException {
    setStatus(200);

    // Handle special /load request.
    if (request.isLoad()) {
      if (config.loadMonitorIsOverloaded()) setStatus(503);
      else setStatus(200);
    } else {
      // Do the request.
      getContent(request);
    }
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

  private void getContent(HTTPRequest request) throws IOException {
    String urlPath = request.getUrlPath();
    String urlQuery = request.getUrlQuery();
    String serverName = request.getHost();
    String method = request.getMethod();
    String userAgent = request.getUserAgent();
    Date ifModifiedSince = request.getIfModifiedSince();

    String root = config.getDocumentRoot(serverName);
    if (root == null) { setStatus(404); return; }

    Path path = getPath(root, urlPath, userAgent);
    if (Config.VERBOSE)
      System.out.println("READING FILE " + root + "," + urlPath + "," + userAgent);

    if (path == null) { setStatus(404); return; }

    if (Files.isExecutable(path)) {
      executeFile(path, urlQuery, serverName, method);
    } else {
      readFile(path, ifModifiedSince);
    }
  }

  // Helper method for getContent().
  private Path getPath(String root, String url, String userAgent) {
    String rootUrl = root + "/" + url;

    // If just normal file.
    Path path = Paths.get(root, url);
    if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
      return path;
    }

    if (Files.isDirectory(path)) {
      // If is directory, look for index.html/index_m.html.
      if (userAgent != null && userAgent.contains("iPhone")) {
        path = path.resolve("index_m.html");
      } else {
        path = path.resolve("index.html");
      }
      if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return path;
    }

    return null;
  }

  // Helper method for getContent().
  private void executeFile(Path path,
                           String urlQuery,
                           String serverName,
                           String method) throws IOException {
    ProcessBuilder pb = new ProcessBuilder(path.toAbsolutePath().toString());

    // Set up process.
    Map<String, String> env = pb.environment();
    env.put("QUERY_STRING", urlQuery);
    env.put("REMOTE_ADDR", hostAddress);
    env.put("REMOTE_HOST", hostName);
    env.put("REQUEST_METHOD", method);
    env.put("SERVER_NAME", serverName);
    env.put("SERVER_PORT", Integer.toString(config.getPort()));
    env.put("SERVER_PROTOCOL", "HTTP/1.0");
    env.put("SERVER_SOFTWARE", "QWebServer/0.1");
    pb.directory(path.getParent().toFile());

    // Run process.
    Process process = pb.start();
    try {
      process.waitFor();
    } catch (InterruptedException e) {
      executeFile(path, urlQuery, serverName, method);
      return;
    }

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
  }

  // Helper method for getContent().
  private void readFile(Path path, Date ifModifiedSince) throws IOException {
    lastModified = new Date(
      Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis());

    // Check if modified since ifModifiedSince.
    if (ifModifiedSince != null && ifModifiedSince.after(lastModified)) {
      setStatus(304);
      return;
    }

    String pathString = path.toAbsolutePath().toString();

    // Content type
    if (pathString.endsWith(".jpg") || pathString.endsWith(".jpeg"))
      contentType = "image/jpeg";

    // Check cache.
    if (Config.VERBOSE) System.out.println("CHECKING CACHE");
    if (cache.hasKey(pathString)) {
      Cache.DateContentPair entry = cache.get(pathString);
      if (Config.VERBOSE) System.out.println(entry.toString());
      if (lastModified.before(entry.lastRetrievedDate)) {
        if (Config.VERBOSE) System.out.println("CACHE HIT");
        content = entry.content;
        return;
      }
    }

    // Read in file.
    long contentLength = Files.size(path);
    content = Files.readAllBytes(path);

    cache.put(pathString, content);
  }

  private Config config;
  private Socket conn;
  private Cache cache;

  private byte[] content;
  private String contentType = "text/html";
  private int statusCode;
  private String statusMessage;
  private Date lastModified;

  private String hostAddress;
  private String hostName;
}