package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ApplicationNameAndVersionTestUtil {

    private ApplicationNameAndVersionTestUtil() {
    }

    // we don't use REST Assured because its bundled Groovy version clashes with Maven Invoker's (which is also used in this module)
    static void assertApplicationPropertiesSetCorrectly() {
        try {
            URL url = new URL("http://localhost:8080/app/hello/nameAndVersion");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.2
            connection.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                failApplicationPropertiesSetCorrectly();
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String output = br.readLine();
                assertThat(output).isEqualTo("acme/1.0-SNAPSHOT");
            }
        } catch (IOException e) {
            failApplicationPropertiesSetCorrectly();
        }
    }

    private static void failApplicationPropertiesSetCorrectly() {
        fail("Failed to assert that the application name and version were properly set");
    }
}
