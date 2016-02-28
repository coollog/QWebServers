import java.io.*;
import java.net.*;
import java.util.*;

public class HTTPResponse {
  public HTTPResponse(int statusCode,
                      String statusMessage,
                      String serverName,
                      Date lastModified,
                      byte[] content) {
    if (content == null) content = new byte[0];

    appendLine("HTTP/1.0 " + statusCode + " " + statusMessage);
    appendLine("Date: " + Utilities.getHTTPDate());
    appendLine("Server: " + serverName);
    if (statusCode == 200) {
      appendLine("Content-Type: text/html");
      appendLine("Content-Length: " + content.length);
      if (lastModified != null) {
        appendLine("Last-Modified: " + Utilities.getHTTPDate(lastModified));
      }
    }
    appendLine();

    this.content = content;
  }

  public String getHeader() {
    return responseHeader.toString();
  }
  public byte[] getContent() {
    return content;
  }

  private void appendLine() {
    responseHeader.append("\r\n");
  }
  private void appendLine(String line) {
    responseHeader.append(line).append("\r\n");
  }

  private byte[] content;
  private StringBuffer responseHeader = new StringBuffer(0x1000);
}