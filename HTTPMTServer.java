// HTTPMTServer.java
// usage: java HTTPMTServer -config <config_file_name>

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class HTTPMTServer extends HTTPServer {
  public static void main(String[] args) throws Exception {
    if (!init(args)) return;

    ServerSocket server = new ServerSocket(config.getPort());

    while (true) {
      Socket conn = server.accept();

      HTTPServerThread serverThread = new HTTPServerThread(config, conn, cache);
      serverThread.start();
    }
  }
}