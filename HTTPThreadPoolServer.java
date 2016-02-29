// HTTPThreadPoolServer.java
// usage: java HTTPThreadPoolServer -config <config_file_name>

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class HTTPThreadPoolServer extends HTTPServer {
  public static void main(String[] args) throws Exception {
    if (!init(args)) return;

    // Open listen socket.
    ServerSocket server = new ServerSocket(config.getPort());

    // Run threads.
    runThreads(server);
  }
}