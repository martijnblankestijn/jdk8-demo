import nl.ordina.java8.control.http.HttpUtil;
import nl.ordina.java8.control.SearchProviderService;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Properties;

/***/
public class BingRetrieval {
    @Test
    public void test() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = SearchProviderService.class.getResourceAsStream("/etc/configuration.properties")) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        URLConnection urlConnection = new URL(properties.getProperty("bing_url") + "'Computers'").openConnection();
            String key = properties.getProperty("bing_key");
        String encode = Base64.getEncoder().encodeToString((key + ":" + key).getBytes());
        urlConnection.setRequestProperty("Authorization", "Basic " + encode);
        System.out.println(HttpUtil.readCompleteResponse(urlConnection));

    }
}
