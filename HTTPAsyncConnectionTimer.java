import java.io.IOException;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class HTTPAsyncConnectionTimer {
  public HTTPAsyncConnectionTimer(int timeout) {
    this.timeout = timeout;
  }

  public void timeKey(SelectionKey key) {
    KeyCloser keyCloser = new KeyCloser(key);
    timer.schedule(keyCloser, timeout * 1000);
  }

  private class KeyCloser extends TimerTask {
    public KeyCloser(SelectionKey key) {
      this.key = key;
    }

    public void run() {
      if (!key.isValid()) return;

      try {
        key.channel().close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      key.cancel();
    }

    private SelectionKey key;
  }

  private final int timeout;
  private Timer timer = new Timer();
}