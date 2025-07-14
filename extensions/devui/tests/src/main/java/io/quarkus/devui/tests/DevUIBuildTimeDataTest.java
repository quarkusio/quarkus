package io.quarkus.devui.tests;

import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.config.TestConfigProviderResolver;

public abstract class DevUIBuildTimeDataTest {

    protected static final Logger log = Logger.getLogger(DevUIBuildTimeDataTest.class);

    protected URI uri;

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory factory = mapper.getFactory();

    public DevUIBuildTimeDataTest(String namespace) {
        Config config = ((TestConfigProviderResolver) ConfigProviderResolver.instance()).getConfig(DEVELOPMENT);
        String testUrl = config.getValue("test.url", String.class);
        String nonApplicationRoot = config.getOptionalValue("quarkus.http.non-application-root-path", String.class).orElse("q");
        if (!nonApplicationRoot.startsWith("/")) {
            nonApplicationRoot = "/" + nonApplicationRoot;
        }
        this.uri = URI.create(testUrl + nonApplicationRoot + "/dev-ui/" + namespace + "-data.js");
    }

    public List<String> getAllKeys() throws IOException {
        String data = readDataFromUrl();
        String[] kvs = data.split(CONST);
        List<String> l = new ArrayList<>();
        for (String kv : kvs) {
            String k = kv.split(EQUALS)[0];
            if (!k.startsWith("// Generated") && !k.isBlank()) {
                l.add(k.trim());
            }
        }
        return l;
    }

    public JsonNode getBuildTimeData(String key) throws Exception {

        String data = readDataFromUrl();
        String[] kvs = data.split(CONST);

        for (String kv : kvs) {
            if (kv.startsWith(key + SPACE + EQUALS + SPACE)) {
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
}
