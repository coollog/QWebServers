public class LoadMonitor implements ILoadMonitor {
  public synchronized void add() {
    openConnections ++;
  }

  public synchronized void remove() {
    openConnections --;
  }

  public boolean isOverloaded() {
    return openConnections > MAX_CONNECTIONS;
  }

  private int openConnections = 0;
}