import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Executors;

public class HTTPAsyncFutureServer extends HTTPServer {
  public static void main(String[] args) throws Exception {
    if (!init(args)) return;

    DEBUG("Listening for connections on port " + config.getPort());

    AsynchronousChannelGroup group = AsynchronousChannelGroup
      .withThreadPool(Executors.newFixedThreadPool(config.getThreads()));

    HTTPAsyncFutureServer server = new HTTPAsyncFutureServer(group);

    Thread.currentThread().join();
  }

  private static void finish(AsynchronousSocketChannel client) {
    try {
      client.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    config.loadMonitorRemove();
  }

  public HTTPAsyncFutureServer(AsynchronousChannelGroup group)
  throws IOException {
    open(group);
    accept();
  }

  private void open(AsynchronousChannelGroup group) throws IOException {
    // Create server channel.
    InetSocketAddress hostAddress = new InetSocketAddress(config.getPort());
    serverChannel =
      AsynchronousServerSocketChannel.open(group).bind(hostAddress, 200);
    System.out.println("Server running...");
  }

  private void accept() {
    serverChannel.accept(null, new AcceptHandler());
  }

  private class AcceptHandler
    implements CompletionHandler<AsynchronousSocketChannel, Object> {

    public void completed(AsynchronousSocketChannel client, Object attachment) {
      // Set nagle?

      try {
        ByteBuffer buffer = ByteBuffer.allocate(0x1000);
        client.read(buffer, buffer, new ReadHandler(client));
      } catch (Exception e) {
        e.printStackTrace();
      }

      serverChannel.accept(null, this);

      config.loadMonitorAdd();
    }

    public void failed(Throwable e, Object attachment) { e.printStackTrace(); }
  }

  private class ReadHandler
    implements CompletionHandler<Integer, ByteBuffer> {

    public ReadHandler(AsynchronousSocketChannel client) throws Exception {
      this.client = client;
      this.handler = handler =
        new HTTPServerRequestHandler(HTTPAsyncFutureServer.config,
                                     client,
                                     HTTPAsyncFutureServer.cache);
    }

    public void completed(Integer result, ByteBuffer attachment) {
      // Add buffer to string builder.
      try {
        attachment.flip();
        byte[] buffer = new byte[attachment.limit()];
        attachment.get(buffer).clear();
        String str = new String(buffer, "ASCII");
        stringBuilder.append(str);
      } catch (UnsupportedEncodingException e) {}

      String requestString = stringBuilder.toString();

      HTTPRequest request;
      try {
        request = new HTTPRequest(requestString);
      } catch (IOException e) {
        client.read(attachment, attachment, this);
        return;
      }

      try {
        HTTPResponse response = handler.getResponse(request);

        String responseString =
          response.getHeader() + new String(response.getContent(), "ASCII");
        ByteBuffer outBuffer = ByteBuffer.wrap(responseString.getBytes("ASCII"));

        client.write(outBuffer, outBuffer, new WriteHandler(client));
      } catch (Exception e) {
        e.printStackTrace();
        HTTPAsyncFutureServer.finish(client);
      }
    }

    public void failed(Throwable e, ByteBuffer attachment) {
      HTTPAsyncFutureServer.finish(client);
    }

    private AsynchronousSocketChannel client;
    private HTTPServerRequestHandler handler;
    private StringBuilder stringBuilder = new StringBuilder();
  }

  private class WriteHandler
    implements CompletionHandler<Integer, ByteBuffer> {

    public WriteHandler(AsynchronousSocketChannel client) {
      this.client = client;
    }

    public void completed(Integer result, ByteBuffer attachment) {
      HTTPAsyncFutureServer.finish(client);
    }

    public void failed(Throwable e, ByteBuffer attachment) {
      e.printStackTrace();
      HTTPAsyncFutureServer.finish(client);
    }

    private AsynchronousSocketChannel client;
  }

  private static AsynchronousServerSocketChannel serverChannel;
}