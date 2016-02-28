// SHTTPTestClient.java
// usage: java SHTTPTestClient -server <server>
//                             -servname <server name>
//                             -port <server port>
//                             -parallel <# of threads>
//                             -files <file name>
//                             -T <time of test in seconds>

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.*;
import java.util.*;

public class SHTTPTestClient {
  public static String argServer;
  public static String argServerName;
  public static int argPort;
  public static int argParallel;
  public static String argFiles;
  public static int argT;

  public static List<String> filesList;
  public static InetAddress serverIPAddress;

  public static void main(String[] args) throws Exception {
    // Process arguments.
    if (!processArgs(args)) return;

    // Record threads in a pool.
    Thread[] threads = new Thread[argParallel];

    // Run the threads.
    for (int i = 0; i < argParallel; i ++) {
      SHTTPTestClientThread newThread = new SHTTPTestClientThread(i);
      threads[i] = new Thread(newThread);
      threads[i].start();
    }

    // Wait for the threads to finish.
    for (int i = 0; i < argParallel; i ++) {
      threads[i].join(argT * 1000);
    }

    // Process statistics from threads.
    System.out.format("Total transaction throughput: %s files/s\n" +
                      "Data rate throughput: %s B/s\n" +
                      "Average wait time: %s ms\n",
                      SHTTPTestClientThread.getFilesProcessedPerSecond(),
                      SHTTPTestClientThread.getBytesReceivedPerSecond(),
                      SHTTPTestClientThread.getAverageWaitTime());
  }

  public static boolean processArgs(String[] args) throws Exception {
    // Check arguments.
    if (args.length != 12 ||
        !args[0].equals("-server") ||
        !args[2].equals("-servname") ||
        !args[4].equals("-port") ||
        !args[6].equals("-parallel") ||
        !args[8].equals("-files") ||
        !args[10].equals("-T")) {
      System.out.println(
        "usage: java SHTTPTestClient -server <server> -servname <server name>" +
        "-port <server port> -parallel <# of threads> -files <file name> " +
        "-T <time of test in seconds>");
      return false;
    }

    // Store arguments.
    argServer = args[1];
    argServerName = args[3];
    argPort = Integer.parseInt(args[5]);
    argParallel = Integer.parseInt(args[7]);
    argFiles = args[9];
    argT = Integer.parseInt(args[11]);

    // Read in argFiles.
    try {
      filesList = Files.readAllLines(
        Paths.get(argFiles), Charset.defaultCharset());
    } catch (Exception e) {
      System.out.format("Cannot read file \"%s\"\n", argFiles);
      return false;
    }

    // Convert argServer into IP address.
    serverIPAddress = InetAddress.getByName(argServer);

    // Setup the thread class.
    SHTTPTestClientThread.setup(serverIPAddress, argPort, filesList, argT);

    return true;
  }
}