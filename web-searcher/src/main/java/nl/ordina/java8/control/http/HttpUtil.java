package nl.ordina.java8.control.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import static java.util.logging.Level.FINEST;

public class HttpUtil {
    private static final Logger LOG = Logger.getLogger(HttpUtil.class.getName());
    public static String getPage(final URL site) {

        LOG.log(FINEST, "Getting site {0}", site);

        URLConnection yc;
        try {
            yc = site.openConnection();
            return readCompleteResponse(yc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readCompleteResponse(URLConnection connection) throws IOException {
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

        StringBuilder builder = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) builder.append(inputLine);
        }
        return builder.toString();
    }

  public static URL createUrl(String url) {
    final URL site;
    try {
      site = new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    return site;
  }
}
