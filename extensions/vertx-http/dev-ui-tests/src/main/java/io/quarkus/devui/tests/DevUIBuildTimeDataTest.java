package io.quarkus.devui.tests;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class DevUIBuildTimeDataTest {

    protected static final Logger log = Logger.getLogger(DevUIBuildTimeDataTest.class);

    protected URI uri;

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory factory = mapper.getFactory();

    public DevUIBuildTimeDataTest(String namespace) {
        String testUrl = ConfigProvider.getConfig().getValue("test.url", String.class);
        String nonApplicationRoot = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.http.non-application-root-path", String.class).orElse("q");
        if (!nonApplicationRoot.startsWith("/")) {
            nonApplicationRoot = "/" + nonApplicationRoot;
        }
        this.uri = URI.create(testUrl + nonApplicationRoot + "/dev-ui/" + namespace + "-data.js");
    }

    public JsonNode getBuildTimeData(String key) throws Exception {

        String data = readDataFromUrl();
        String[] kvs = data.split(CONST);

        for (String kv : kvs) {
            if (kv.startsWith(key + SPACE + EQUALS + SPACE + OPEN_CURLY_BRACKET)) {
                String json = kv.substring(kv.indexOf(EQUALS) + 1).trim();
                log.debug("json = " + json);
                return toJsonNode(json);
            }
        }

        return null;
    }

    protected JsonNode toJsonNode(String json) {
        try {
            JsonParser parser = factory.createParser(json);
            return mapper.readTree(parser);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String readDataFromUrl() throws MalformedURLException, IOException {
        try (Scanner scanner = new Scanner(uri.toURL().openStream(),
                StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : null;
        }
    }

    private static final String CONST = "export const ";
    private static final String SPACE = " ";
    private static final String EQUALS = "=";
    private static final String OPEN_CURLY_BRACKET = "{";
}
