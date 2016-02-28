// HTTPSequentialServer.java
// usage: java HTTPSequentialServer -config <config_file_name>

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class HTTPSequentialServer extends HTTPServer {
  public static void main(String[] args) throws Exception {
    init(args);

    ServerSocket server = new ServerSocket(config.getPort());

    while (true) {
      Socket conn = server.accept();
      System.out.println("receive request from " + conn);

      HTTPServerRequestHandler handler =
        new HTTPServerRequestHandler(config, conn, cache);
      handler.handle();
    }
  }
}