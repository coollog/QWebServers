import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.*;
import java.util.*;

public class SHTTPTestClient {
  public static String argServer;
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
      SHTTPTestClientThread newThread = new SHTTPTestClientThread();
      threads[i] = new Thread(newThread);
      threads[i].start();
    }

    // Wait for the threads to finish.
    for (int i = 0; i < argParallel; i ++) {
      threads[i].join();
    }

    // Process statistics from threads.
    System.out.format("Total transaction throughput: %s\n" +
                      "Data rate throughput: %s\n" +
                      "Average wait time: %s\n",
                      SHTTPTestClientThread.getFilesProcessedPerSecond(),
                      SHTTPTestClientThread.getBytesReceivedPerSecond(),
                      SHTTPTestClientThread.getAverageWaitTime());
  }

  public static boolean processArgs(String[] args) throws Exception {
    // Check arguments.
    if (args.length != 10 ||
        !args[0].equals("-server") ||
        !args[2].equals("-port") ||
        !args[4].equals("-parallel") ||
        !args[6].equals("-files") ||
        !args[8].equals("-T")) {
      System.out.println(
        "usage: java SHTTPTestClient -server <server> -port <server port> " +
        "-parallel <# of threads> -files <file name> " +
        "-T <time of test in seconds>");
      return false;
    }

    // Store arguments.
    argServer = args[1];
    argPort = Integer.parseInt(args[3]);
    argParallel = Integer.parseInt(args[5]);
    argFiles = args[7];
    argT = Integer.parseInt(args[9]);

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