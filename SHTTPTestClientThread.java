import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class SHTTPTestClientThread implements Runnable {
  private static InetAddress SERVER_ADDRESS;
  private static int SERVER_PORT;
  private static List<String> FILES;
  private static int TIME_TO_RUN;

  private static int totalFilesProcessed = 0;
  private static int totalBytesReceived = 0;
  private static int totalWaitTime = 0;

  public static void setup(InetAddress serverIPAddress,
                           int argPort,
                           List<String> filesList,
                           int argT) {
    SERVER_ADDRESS = serverIPAddress;
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

  public SHTTPTestClientThread() {}

  public void run() {
    // Time the operation.
    long startTime = System.currentTimeMillis();

    while (System.currentTimeMillis() - startTime < TIME_TO_RUN) {
      String fileName = FILES.get(filesProcessed % FILES.size());

      try {
        // Connect to the server.
        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

        // Send request.
        String request = "GET " + fileName + " HTTP/1.0\r\n" +
                         "Host: " + SERVER_ADDRESS + "\r\n\r\n";
        long sendTime = System.currentTimeMillis();
        send(socket, request);
        bytesReceived += receive(socket, sendTime);

        // Close the connection.
        socket.close();
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Increment the file counter.
      filesProcessed ++;
    }

    finish();
  }

  private synchronized void finish() {
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
    while ((numBytes = inStream.read(buffer)) != -1) {
      if (!received) {
        waitTime += System.currentTimeMillis() - sendTime;
      }
      totalBytes += numBytes;
    }

    return totalBytes;
  }

  // Counter for which file to request (also equals total # files received).
  private int filesProcessed = 0;
  // Counter for number of bytes received.
  private int bytesReceived = 0;
  // How long we had to wait in total (average = divide by filesProcessed).
  private int waitTime = 0;
}