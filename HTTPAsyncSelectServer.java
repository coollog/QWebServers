import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.io.IOException;

public class HTTPAsyncSelectServer extends HTTPServer implements Runnable {
  public static int DEFAULT_PORT = 6789;

  private class ClientAttachment {
    public ClientAttachment(SocketChannel client) throws IOException {
      this.client = client;
      handler = new HTTPServerRequestHandler(config, client.socket(), cache);
    }

    // Returns whether finished reading or not.
    public void read() throws IOException {
      int readBytes = -1;
      readBytes = client.read(inBuffer);
      if (readBytes == -1) throw new IOException("Connection closed?");

      requestString += getRequestString();
    }

    // Returns whether response is finished sending or not, or -1 if request
    // is not done receiving.
    public int respond() throws IOException {
      try {
        getResponse(requestString);
      } catch (Exception e) {
        return -1;
      }
      if (Config.VERBOSE) System.out.println("Request:\n" + requestString);

      client.write(outBuffer);
      if (outBuffer.hasRemaining()) return 0;

      return 1;
    }

    private void getResponse(String requestString) throws IOException {
      if (responseReady) return;

      HTTPRequest request = new HTTPRequest(requestString);

      HTTPResponse response = handler.getResponse(request);

      String responseString =
        response.getHeader() + new String(response.getContent(), "ASCII");
      outBuffer = ByteBuffer.wrap(responseString.getBytes("ASCII"));

      responseReady = true;
    }

    private String getRequestString() {
      StringBuilder request = new StringBuilder(0x1000);

      inBuffer.flip();
      while (inBuffer.hasRemaining()) {
        request.append((char)inBuffer.get());
      }
      inBuffer.flip();

      return request.toString();
    }

    private boolean responseReady = false;
    private String requestString = "";
    private HTTPServerRequestHandler handler;
    private SocketChannel client;
    private ByteBuffer inBuffer = ByteBuffer.allocate(0x1000);
    private ByteBuffer outBuffer = ByteBuffer.allocate(0x1000);
  }

  public static void main(String[] args) throws Exception {
    if (!init(args)) return;

    DEBUG("Listening for connections on port " + config.getPort());

    openServerSocketChannel();

    // Setup connection timeout.
    HTTPAsyncSelectServer.connectionTimer =
      new HTTPAsyncConnectionTimer(config);

    for (int i = 0; i < config.getThreads(); i ++) {
      Thread newThread = new Thread(new HTTPAsyncSelectServer());
      newThread.setPriority(Thread.MAX_PRIORITY);
      newThread.start();
    }
  }

  public static void openServerSocketChannel() {
    try {
      // create server channel
      serverChannel = ServerSocketChannel.open();

      // extract server socket of the server channel and bind the port
      ServerSocket ss = serverChannel.socket();
      ss.bind(new InetSocketAddress(config.getPort()), 200);

      // configure it to be non blocking
      serverChannel.configureBlocking(false);

      System.out.println("Server running...");
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  public void run() {
    // Server socket channel and selector initialization.
    try {
      // Create selector.
      selector = Selector.open();

      // Register the server channel to selector.
      serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    while (true) {
      DEBUG("Enter selection");
      try {
        selector.select();
      } catch (IOException ex) {
        ex.printStackTrace();
        break;
      }

      Set<SelectionKey> readyKeys = selector.selectedKeys();
      Iterator<SelectionKey> iterator = readyKeys.iterator();

      while (iterator.hasNext()) {
        SelectionKey key = (SelectionKey) iterator.next();
        iterator.remove();

        try {
          if (key.isAcceptable()) handleAccept(key);
          if (key.isReadable()) handleRead(key);
          if (key.isWritable()) handleWrite(key);
        } catch (IOException ex) {
          ex.printStackTrace();
          finish(key);
        } catch (CancelledKeyException e) {
          break;
        }
      }
    }
  }

  private void handleAccept(SelectionKey key) throws IOException {
    // Extract the ready connection.
    SocketChannel client;
    synchronized (serverChannel) {
      client = serverChannel.accept();
      if (client == null) return;
    }
    DEBUG("handleAccept: Accepted connection from " + client);

    // Configure the connection to be non-blocking.
    client.configureBlocking(false);

    // Disable nagle.
    // client.setOption(StandardSocketOptions.TCP_NODELAY, true);

    // Register the new connection with interests.
    SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
    clientKey.attach(new ClientAttachment(client));

    // Register key with timeout thread.
    connectionTimer.timeKey(clientKey);

    config.loadMonitorAdd();
  }

  private void handleRead(SelectionKey key) throws IOException {
    ClientAttachment clientAttachment = (ClientAttachment)key.attachment();

    // Read.
    try {
      clientAttachment.read();
    } catch (Exception e) {
      e.printStackTrace();
      finish(key);
      return;
    }

    // Respond.
    handleWrite(key);

    int nextState = key.interestOps();
    key.interestOps(nextState & ~SelectionKey.OP_WRITE);

    DEBUG("\tRead data from " + key.channel());
  }

  private void handleWrite(SelectionKey key) throws IOException {
    ClientAttachment clientAttachment = (ClientAttachment)key.attachment();

    switch (clientAttachment.respond()) {
    case -1: break; // Request not finished receiving.
    case 0: // Stop reading, into write-only mode.
      int nextState = key.interestOps();
      key.interestOps(nextState & ~SelectionKey.OP_READ);
      break;
    case 1: finish(key); break; // All done writing.
    }

    DEBUG("\tWrote data to " + key.channel());
  }

  private static void finish(SelectionKey key) {
    try {
      key.channel().close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    key.cancel();

    config.loadMonitorRemove();

    Thread.yield();
  }

  private static HTTPAsyncConnectionTimer connectionTimer;
  private static ServerSocketChannel serverChannel;
  private Selector selector;
}