// HTTPSequentialServer.java
// usage: java HTTPSequentialServer -config <config_file_name>

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class HTTPSequentialServer extends HTTPServer {
  public static void main(String[] args) throws Exception {
    if (!init(args)) return;

    ServerSocket server = new ServerSocket(config.getPort());

    HTTPServerThread serverThread = new HTTPServerThread(config, server, cache);
    serverThread.run();
  }
}