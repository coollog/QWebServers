import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class HTTPServer {
  protected static boolean init(String[] args) throws Exception {
    if (!initConfig(args)) return false;
    initCache();
    return true;
  }

  protected static void runThreads(ServerSocket server) {
    runThreadsCommon(server, null, false);
  }
  protected static void runThreads(LinkedBlockingQueue<Socket> queue,
                                   boolean sleep) {
    runThreadsCommon(null, queue, sleep);
  }

  protected static void DEBUG(String s) {
    if (Config.VERBOSE) System.out.println(s);
  }

  private static void runThreadsCommon(ServerSocket server,
                                       LinkedBlockingQueue<Socket> queue,
                                       boolean sleep) {
    int threadCount = config.getThreads();
    HTTPServerThread[] threads = new HTTPServerThread[threadCount];

    for (int i = 0; i < threadCount; i ++) {
      if (server == null) {
        threads[i] = new HTTPServerThread(config, queue, cache, sleep);
      } else {
        threads[i] = new HTTPServerThread(config, server, cache);
      }
      threads[i].start();
    }
  }

  private static boolean initConfig(String[] args) throws Exception {
    String configFileName = Config.getConfigFileNameFromArgs(args);
    if (configFileName == null) return false;

    config = new Config(configFileName);
    return true;
  }
  private static void initCache() {
    cache = new Cache(config.getCacheSize());
  }

  protected static Config config;
  protected static Cache cache;
}