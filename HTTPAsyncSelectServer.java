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

    // Returns whether response is finished sending or not.
    public boolean respond() throws IOException {
      try {
        getResponse(requestString);
      } catch (Exception e) {
        return false;
      }
      if (Config.VERBOSE) System.out.println("Request:\n" + requestString);

      client.write(outBuffer);
      if (outBuffer.hasRemaining()) return false;

      return true;
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

    for (int i = 0; i < config.getThreads(); i ++) {
      (new Thread(new HTTPAsyncSelectServer())).start();
    }
  }

  public static void openServerSocketChannel() {
    try {
      // create server channel
      serverChannel = ServerSocketChannel.open();

      // extract server socket of the server channel and bind the port
      ServerSocket ss = serverChannel.socket();
      ss.bind(new InetSocketAddress(config.getPort()));

      // configure it to be non blocking
      serverChannel.configureBlocking(false);
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

    // configure the connection to be non-blocking
    client.configureBlocking(false);

    // register the new connection with interests
    SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);

    // attach a buffer to the new connection
    // you may want to read up on ByteBuffer.allocateDirect on performance
    clientKey.attach(new ClientAttachment(client));
  }

  private void handleRead(SelectionKey key) throws IOException {
    // a connection is ready to be read
    DEBUG("-->handleRead");
    ClientAttachment clientAttachment = (ClientAttachment)key.attachment();

    try {
      clientAttachment.read();
    } catch (Exception e) {
      e.printStackTrace();
      finish(key);
      return;
    }

    // Get key and interest set.
    SelectionKey sk = key.channel().keyFor(selector);
    int nextState = sk.interestOps();

    nextState = nextState | SelectionKey.OP_WRITE;
    DEBUG("   State change: request pending");

    sk.interestOps(nextState);

    DEBUG("\tRead data from " + key.channel());
    // DEBUG("   Read data from connection " + client + ": read " + readBytes
    //     + " byte(s); buffer becomes " + output);
    DEBUG("handleRead-->");

  }

  private void handleWrite(SelectionKey key) throws IOException {
    DEBUG("-->handleWrite");
    ClientAttachment clientAttachment = (ClientAttachment)key.attachment();

    if (clientAttachment.respond()) {
      finish(key);
    }

    DEBUG("\tWrote data to " + key.channel());
    // DEBUG("   Write data to connection " + client + ": write " + writeBytes
    //     + " byte(s); buffer becomes " + output);
    DEBUG("handleWrite-->");
  }

  private void finish(SelectionKey key) {
    try {
      key.channel().close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    key.cancel();

    Thread.yield();
  }

  private static void DEBUG(String s) {
    if (Config.VERBOSE) System.out.println(s);
  }

  private static ServerSocketChannel serverChannel;
  private Selector selector;
}