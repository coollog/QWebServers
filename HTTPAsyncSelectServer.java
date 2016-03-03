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
      while (readBytes != 0) {
        readBytes = client.read(inBuffer);
        if (readBytes == -1) throw new IOException("Connection closed?");
      }

      getResponse(getRequestString());
    }

    // Returns whether response is finished sending or not.
    public boolean respond() throws IOException {
      client.write(outBuffer);
      if (outBuffer.hasRemaining()) return false;

      return true;
    }

    private void getResponse(String requestString) throws IOException {
      HTTPRequest request = new HTTPRequest(requestString);

      HTTPResponse response = handler.getResponse(request);

      String responseString = response.getHeader() + response.getContent();
      outBuffer = ByteBuffer.wrap(responseString.getBytes("ASCII"));
    }

    private String getRequestString() {
      StringBuilder request = new StringBuilder(0x1000);

      inBuffer.flip();
      while (inBuffer.hasRemaining()) {
        request.append((char)inBuffer.get());
      }

      return request.toString();
    }

    private HTTPServerRequestHandler handler;
    private SocketChannel client;
    private ByteBuffer inBuffer = ByteBuffer.allocate(0x1000);
    private ByteBuffer outBuffer = ByteBuffer.allocate(0x1000);
  }

  public static void main(String[] args) throws Exception {
    if (!init(args)) return;

    DEBUG("Listening for connections on port " + config.getPort());

    HTTPAsyncSelectServer server = new HTTPAsyncSelectServer();
    server.run();
  }

  private void run() {
    // server socket channel and selector initialization
    try {
      // create selector
      selector = Selector.open();

      // open server socket for accept
      ServerSocketChannel serverChannel = openServerSocketChannel();

      // register the server channel to selector
      serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    } // end of catch

    // event loop
    while (true) {

      DEBUG("Enter selection");
      try {
        // check to see if any events
        selector.select();
      } catch (IOException ex) {
        ex.printStackTrace();
        break;
      } // end of catch

      // readKeys is a set of ready events
      Set<SelectionKey> readyKeys = selector.selectedKeys();

      // create an iterator for the set
      Iterator<SelectionKey> iterator = readyKeys.iterator();

      // iterate over all events
      while (iterator.hasNext()) {

        SelectionKey key = (SelectionKey) iterator.next();
        iterator.remove();

        try {
          if (key.isAcceptable()) {
            // a new connection is ready to be accepted
            handleAccept(key);
          } // end of isAcceptable

          if (key.isReadable()) {
            handleRead(key);
          } // end of isReadable

          if (key.isWritable()) {
            handleWrite(key);
          } // end of if isWritable
        } catch (IOException ex) {
          key.cancel();
          try {
            key.channel().close();
          } catch (IOException cex) {
          }
        }
      }
    }
  }

  private ServerSocketChannel openServerSocketChannel() {
    ServerSocketChannel serverChannel = null;

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
    } // end of catch

    return serverChannel;

  } // end of openServerSocketChannel

  private void handleAccept(SelectionKey key) throws IOException {
    ServerSocketChannel server = (ServerSocketChannel) key.channel();

    // extract the ready connection
    synchronized (server) {
      SocketChannel client = server.accept();
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
  } // end of handleAccept

  private void handleRead(SelectionKey key) throws IOException {
    // a connection is ready to be read
    DEBUG("-->handleRead");
    SocketChannel client = (SocketChannel)key.channel();
    ClientAttachment clientAttachment = (ClientAttachment)key.attachment();

    try {
      clientAttachment.read();
    } catch (Exception e) {
      e.printStackTrace();
      finish(client, key);
      return;
    }

    // Get key and interest set.
    SelectionKey sk = key.channel().keyFor(selector);
    int nextState = sk.interestOps();

    nextState = nextState & ~SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    DEBUG("   State change: request done");

    sk.interestOps(nextState);

    DEBUG("\tRead data from " + client);
    // DEBUG("   Read data from connection " + client + ": read " + readBytes
    //     + " byte(s); buffer becomes " + output);
    DEBUG("handleRead-->");

  } // end of handleRead

  private void handleWrite(SelectionKey key) throws IOException {
    DEBUG("-->handleWrite");
    SocketChannel client = (SocketChannel)key.channel();
    ClientAttachment clientAttachment = (ClientAttachment)key.attachment();

    boolean finished = clientAttachment.respond();

    if (finished) {
      finish(client, key);
    }

    DEBUG("\tWrote data to " + client);
    // DEBUG("   Write data to connection " + client + ": write " + writeBytes
    //     + " byte(s); buffer becomes " + output);
    DEBUG("handleWrite-->");
  } // end of handleWrite

  private void finish(SocketChannel client, SelectionKey key) {
    try {
      client.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    key.cancel();
  }

  private static void DEBUG(String s) {
    if (Config.VERBOSE) System.out.println(s);
  }

  private Selector selector;
} // end of class