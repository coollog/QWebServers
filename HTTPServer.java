import java.io.*;
import java.util.*;

public class HTTPServer {
  protected static void init(String[] args) throws Exception {
    initConfig(args);
    initCache();
  }

  private static void initConfig(String[] args) throws Exception {
    String configFileName = Config.getConfigFileNameFromArgs(args);
    if (configFileName == null) return;

    config = new Config(configFileName);
  }
  private static void initCache() {
    cache = new Cache(config.getCacheSize());
  }

  protected static Config config;
  protected static Cache cache;
}