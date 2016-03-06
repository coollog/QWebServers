import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

public class HTTPServerThread extends Thread {
  public static boolean useCondVar = false;

  public HTTPServerThread(Config config, ServerSocket server, Cache cache) {
    this.server = server;
    this.config = config;
    this.cache = cache;
    type = Type.SERVER;
  }
  public HTTPServerThread(Config config, Socket conn, Cache cache) {
    this.conn = conn;
    this.config = config;
    this.cache = cache;
    type = Type.CONN;
  }
  public HTTPServerThread(Config config,
                          LinkedBlockingQueue<Socket> queue,
                          Cache cache,
                          boolean sleep) {
    this.queue = queue;
    this.config = config;
    this.cache = cache;
    if (sleep) type = Type.QUEUE_SLEEP;
    else type = Type.QUEUE_BUSY;
  }

  public void run() {
    System.out.println("Server running...");

    switch (type) {
    case SERVER:
      while (true) {
        synchronized (server) {
          try {
            conn = server.accept();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        handleConn(conn);
      }
    case CONN:
      handleConn(conn);
      break;
    case QUEUE_BUSY:
      while (true) {
        conn = null;
        while (conn == null) {
          conn = queue.poll();
        }

        handleConn(conn);
      }
    case QUEUE_SLEEP:
      while (true) {
        conn = null;

        synchronized (queue) {
          while (queue.isEmpty()) {
            try {
              queue.wait();
            } catch (InterruptedException e) {
              System.out.println("Waiting for queue interrupted.");
            }
          }
          conn = queue.poll();
        }

        handleConn(conn);
      }
    }
  }

  private void handleConn(Socket conn) {
    try {
      if (Config.VERBOSE) {
        System.out.println("receive request from " + conn);
      }

      // Disable nagle.
      // conn.setTcpNoDelay(true);

      HTTPServerRequestHandler handler =
        new HTTPServerRequestHandler(config, conn, cache);

      handler.handle();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private enum Type {
    SERVER, CONN, QUEUE_BUSY, QUEUE_SLEEP
  }

  private Type type;

  private ServerSocket server;
  private Socket conn;
  private LinkedBlockingQueue<Socket> queue;

  private Config config;
  private Cache cache;
}