import java.io.*;
import java.nio.*;
import java.util.*;

public class Config {
  public static boolean VERBOSE = false;

  public static String
  getConfigFileNameFromArgs(String[] args) {
    // Extract the name of the program.
    String programName =
      Thread.currentThread().getStackTrace()[2].getClassName();

    // Check args are correct.
    if (args.length != 2 || !args[0].equals("-config")) {
      System.out.println(
        "usage: java " + programName + " -config <config_file_name>");
      return null;
    }

    return args[1];
  }

  public Config(String fileName) throws Exception {
    // Open the config file.
    BufferedReader reader =
      new BufferedReader(new FileReader(new File(fileName)));

    // Process each line.
    while (parseLine(reader.readLine()));

    reader.close();
  }

  public int getPort() { return listenPort; }
  public int getThreads() { return numThreads; }
  public int getCacheSize() { return cacheSize; }
  public String getDocumentRoot(String serverName) {
    return virtualHosts.get(serverName);
  }

  private boolean parseLine(String line) throws Exception {
    if (line == null) return false;

    // Process out tokens from line.
    String[] tokens = Utilities.tokenize(line);

    if (inVirtualHost) {
      // Process <VirtualHost> blocks.
      switch (tokens[0]) {
      case "DocumentRoot":
        curDocumentRoot = tokens[1];
      case "ServerName":
        curServerName = tokens[1];
        break;
      case "<VirtualHost>":
        if (curDocumentRoot == null || curServerName == null) {
          throw new Exception("<VirtualHost> config invalid.");
        }
        virtualHosts.put(curServerName, curDocumentRoot);
        curDocumentRoot = null;
        curServerName = null;
        inVirtualHost = false;
        break;
      default:
        throw new Exception("Invalid <VirtualHost> config line: " + line);
      }
    } else {
      // Process line by line.
      switch (tokens[0]) {
      case "Listen":
        listenPort = Integer.parseInt(tokens[1]);
        break;
      case "<VirtualHost":
        inVirtualHost = true;
        break;
      case "ThreadPoolSize":
        numThreads = Integer.parseInt(tokens[1]);
        break;
      case "CacheSize":
        cacheSize = Integer.parseInt(tokens[1]);
        break;
      case "VERBOSE":
        VERBOSE = true;
        break;
      default:
        throw new Exception("Invalid config line: " + line);
      }
    }

    return true;
  }

  // Contains the actual parsed configuration.
  private int listenPort;
  private HashMap<String, String> virtualHosts = new HashMap<String, String>();
  private int numThreads;
  private int cacheSize; // in KB

  // Variables to aid in processing VirtualHosts.
  private boolean inVirtualHost = false;
  private String curDocumentRoot;
  private String curServerName;
}