import java.util.*;
import java.util.concurrent.*;

public class Cache {
  public static class DateContentPair {
    public final Date lastRetrievedDate;
    public final byte[] content;

    public DateContentPair(byte[] content) {
      lastRetrievedDate = Utilities.now();
      this.content = content;
    }

    public String toString() {
      return "<" + lastRetrievedDate + ", " + content.length + ">";
    }
  }

  public Cache(int cacheSize) {
    cacheSize *= 0x400; // B to KB
    map = new ConcurrentHashMap<String, DateContentPair>(cacheSize);
    size = cacheSize;
  }

  public boolean hasKey(String path) {
    return map.containsKey(path);
  }
  public DateContentPair get(String path) {
    return map.get(path);
  }
  public synchronized void put(String path, byte[] content) {
    if (used + content.length > size) return;

    DateContentPair inserted = new DateContentPair(content);
    DateContentPair removed = map.put(path, inserted);

    if (removed != null) used -= removed.content.length;
    used += content.length;
  }

  private ConcurrentHashMap<String, DateContentPair> map;
  private int used = 0;
  private final int size;
}