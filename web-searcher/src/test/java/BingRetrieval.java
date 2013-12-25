import nl.ordina.java8.composable.http.HttpUtil;
import nl.ordina.java8.composable.parsers.SearchProviderService;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Properties;

/**
 * Created by martijn on 12/25/13.
 */
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
        String encode = Base64.getEncoder().encodeToString(new String(key + ":" + key).getBytes());
        urlConnection.setRequestProperty("Authorization", "Basic " + encode);
        System.out.println(HttpUtil.readCompleteResponse(urlConnection));

    }
}
