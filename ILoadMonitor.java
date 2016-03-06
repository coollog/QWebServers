public interface ILoadMonitor {
  static final int MAX_CONNECTIONS = 50;

  public void add();
  public void remove();
  public boolean isOverloaded();
}