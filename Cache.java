import java.util.*;
import java.util.concurrent.*;

public class Cache {
  public static class DateContentPair {
    public final Date modifiedDate;
    public final byte[] content;
    public DateContentPair(Date modifiedDate, byte[] content) {
      this.modifiedDate = modifiedDate;
      this.content = content;
    }
  }

  public Cache(int cacheSize) {
    map = new ConcurrentHashMap<String, DateContentPair>(cacheSize);
    size = cacheSize;
  }

  public boolean hasKey(String path) {
    return map.containsKey(path);
  }
  public DateContentPair get(String path) {
    return map.get(path);
  }
  public synchronized void put(String path, Date modifiedDate, byte[] content) {
    if (used + content.length > size) return;

    DateContentPair inserted = new DateContentPair(modifiedDate, content);
    DateContentPair removed = map.put(path, inserted);

    if (removed != null) used -= removed.content.length;
    used += content.length;
  }

  private ConcurrentHashMap<String, DateContentPair> map;
  private int used = 0;
  private final int size;
}