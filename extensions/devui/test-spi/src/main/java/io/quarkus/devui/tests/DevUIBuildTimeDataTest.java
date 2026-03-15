package io.quarkus.devui.tests;

import static io.quarkus.devui.tests.DevUITestUtils.DOT;
import static io.quarkus.devui.tests.DevUITestUtils.LOCAL_BASE_URI;
import static io.quarkus.devui.tests.DevUITestUtils.managementRootPath;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.value.registry.ValueRegistry;
import io.smallrye.config.Config;

public abstract class DevUIBuildTimeDataTest {
    private static final Logger log = Logger.getLogger(DevUIBuildTimeDataTest.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory factory = mapper.getFactory();
    private final String namespace;

    private URI uri;

    public DevUIBuildTimeDataTest(String namespace) {
        // The namespace changed to be compatible with MCP. We add some code here to be backward compatible
        this.namespace = namespace.contains(DOT) ? namespace.substring(namespace.lastIndexOf(DOT) + 1) : namespace;
    }

    @SuppressWarnings("JUnitMalformedDeclaration")
    @BeforeEach
    public void beforeEach(ValueRegistry valueRegistry) {
        if (valueRegistry.containsKey(LOCAL_BASE_URI)) {
            URI localBaseUri = valueRegistry.get(LOCAL_BASE_URI);
            this.uri = URI
                    .create(localBaseUri.toString() + managementRootPath(Config.get(), "dev-ui/" + namespace + "-data.js"));
        }
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

    private String readDataFromUrl() throws IOException {
        if (uri == null) {
            throw new IllegalStateException("No URI available. Did Quarkus start with HTTP support?");
        }

        try (Scanner scanner = new Scanner(uri.toURL().openStream(), StandardCharsets.UTF_8)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : null;
        }
    }

    private static final String CONST = "export const ";
    private static final String SPACE = " ";
    private static final String EQUALS = "=";
}
