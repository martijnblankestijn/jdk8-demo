package nl.ordina.java8.composable.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public static String readCompleteResponse(URLConnection yc) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) builder.append(inputLine);
        }
        return builder.toString();
    }
}
