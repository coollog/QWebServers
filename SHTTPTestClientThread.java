import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class SHTTPTestClientThread implements Runnable {
  private static final boolean VERBOSE = true;

  private static InetAddress SERVER_ADDRESS;
  private static String SERVER_NAME;
  private static int SERVER_PORT;
  private static List<String> FILES;
  private static int TIME_TO_RUN;

  private static int totalFilesProcessed = 0;
  private static int totalBytesReceived = 0;
  private static int totalWaitTime = 0;

  public static void setup(InetAddress serverIPAddress,
                           String serverName,
                           int argPort,
                           List<String> filesList,
                           int argT) {
    SERVER_ADDRESS = serverIPAddress;
    SERVER_NAME = serverName;
    SERVER_PORT = argPort;
    FILES = filesList;
    TIME_TO_RUN = argT;
  }

  public static double getFilesProcessedPerSecond() {
    return (double)totalFilesProcessed / TIME_TO_RUN;
  }
  public static double getBytesReceivedPerSecond() {
    return (double)totalBytesReceived / TIME_TO_RUN;
  }
  public static double getAverageWaitTime() {
    return (double)totalWaitTime / totalFilesProcessed;
  }

  public SHTTPTestClientThread(int id) {
    this.id = id;
  }

  public void run() {
    // Time the operation.
    long startTime = System.currentTimeMillis();

    while (System.currentTimeMillis() - startTime < TIME_TO_RUN * 1000) {
      String fileName = FILES.get(filesProcessed % FILES.size());
      if (VERBOSE)
        System.out.println("Thread " + id + " retrieving " + fileName);

      try {
        // Connect to the server.
        Socket socket = new Socket();
        socket.connect(
          new InetSocketAddress(SERVER_ADDRESS, SERVER_PORT), 3000);

        // Send request.
        String request = "GET " + fileName + " HTTP/1.0\r\n" +
                         "Host: " + SERVER_NAME + "\r\n\r\n";
        long sendTime = System.currentTimeMillis();
        send(socket, request);
        bytesReceived += receive(socket, sendTime);

        // Close the connection.
        socket.close();

        // Increment the file counter.
        filesProcessed ++;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    finish(filesProcessed, bytesReceived, waitTime);
  }

  private synchronized static void finish(int filesProcessed,
                                          int bytesReceived,
                                          int waitTime) {
    totalFilesProcessed += filesProcessed;
    totalBytesReceived += bytesReceived;
    totalWaitTime += waitTime;
  }

  private void send(Socket socket, String request) throws Exception {
    // Write to server.
    DataOutputStream outStream
      = new DataOutputStream(socket.getOutputStream());
    outStream.writeBytes(request);
  }

  // Returns the total bytes received.
  private int receive(Socket socket, long sendTime) throws Exception {
    // create read stream and receive from server
    DataInputStream inStream = new DataInputStream(socket.getInputStream());

    boolean received = false; // Flag for if we have processed wait time.
    int totalBytes = 0; // All bytes received.
    int numBytes = 0; // Current number of bytes read.
    byte[] buffer = new byte[0x1000];
    while (true) {
      System.out.println(numBytes);
      try {
        numBytes = inStream.read(buffer);
      } catch (SocketException e) {
        e.printStackTrace();
        break;
      }
      if (numBytes == -1) break;
      if (numBytes == 0) continue;

      if (!received) {
        long curWaitTime = System.currentTimeMillis() - sendTime;
        waitTime += curWaitTime;
        if (VERBOSE) System.out.println("\tReceived in " + curWaitTime + "ms");
        received = true;
      }
      totalBytes += numBytes;
    }

    return totalBytes;
  }

  private final int id;

  // Counter for which file to request (also equals total # files received).
  private int filesProcessed = 0;
  // Counter for number of bytes received.
  private int bytesReceived = 0;
  // How long we had to wait in total (average = divide by filesProcessed).
  private int waitTime = 0;
}