import java.io.*;
import java.net.*;
import java.util.*;

public class HTTPResponse {
  public HTTPResponse(int statusCode,
                      String statusMessage,
                      String serverName,
                      Date lastModified,
                      byte[] content) {
    appendLine("HTTP/1.0 " + statusCode + " " + statusMessage);
    appendLine("Date: " + Utilities.getHTTPDate());
    appendLine("Server: " + serverName);
    appendLine("Content-Type: text/html");
    appendLine("Content-Length: " + content.length);

    this.content = content;
  }

  public byte[] getHeaderBytes() {
    return responseHeader.toString().getBytes();
  }
  public byte[] getContent() {
    return content;
  }

  private void appendLine(String line) {
    responseHeader.append(line).append("\r\n");
  }

  private byte[] content;
  private StringBuffer responseHeader;
}