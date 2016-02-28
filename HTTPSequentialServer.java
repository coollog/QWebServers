

public class HTTPSequentialServer {
  public static void main(String[] args) throws Exception {
    String configFileName = Config.getConfigFileNameFromArgs(args);
    if (configFileName == null) return;

    Config config = new Config(configFileName);
  }
}