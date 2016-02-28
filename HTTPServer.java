import java.io.*;
import java.util.*;

public class HTTPServer {
  protected static boolean init(String[] args) throws Exception {
    if (!initConfig(args)) return false;
    initCache();
    return true;
  }

  private static boolean initConfig(String[] args) throws Exception {
    String configFileName = Config.getConfigFileNameFromArgs(args);
    if (configFileName == null) return false;

    config = new Config(configFileName);
    return true;
  }
  private static void initCache() {
    cache = new Cache(config.getCacheSize());
  }

  protected static Config config;
  protected static Cache cache;
}