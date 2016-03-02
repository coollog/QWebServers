// HTTPThreadPoolQBusyServer.java
// usage: java HTTPThreadPoolQBusyServer -config <config_file_name>

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

public class HTTPThreadPoolQBusyServer extends HTTPServer {
  public static void main(String[] args) throws Exception {
    if (!init(args)) return;

    // Open listen socket.
    ServerSocket server = new ServerSocket(config.getPort());

    // Create the queue.
    ConcurrentLinkedQueue<Socket> queue = new ConcurrentLinkedQueue<Socket>();

    // Run threads.
    runThreads(queue, false);

    while (true) {
      Socket conn = server.accept();

      synchronized (queue) {
        queue.add(conn);
      }
    }
  }
}