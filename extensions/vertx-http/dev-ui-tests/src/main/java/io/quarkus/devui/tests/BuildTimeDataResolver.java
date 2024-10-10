package io.quarkus.devui.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads the build time data from a given namespace.
 */
public class BuildTimeDataResolver {

    protected static final Logger log = Logger.getLogger(BuildTimeDataResolver.class);

    private static final String CONST = "export const ";
    private static final String EQUALS = "=";
    private static final String COMMENT = "// ";

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory factory = mapper.getFactory();

    private final URI uri;

    public BuildTimeDataResolver(final String namespace, DevUiResourceResolver resourceResolver) {
        this.uri = resourceResolver.resolve(namespace + "-data.js");
    }

    private JsonNode toJsonNode(String json) {
        try {
            JsonParser parser = factory.createParser(json);
            return mapper.readTree(parser);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * The object to send a request.
     */
    @FunctionalInterface
    public interface Request {
        /**
         * Sends the request.
         *
         * @return the map of build time properties
         */
        Map<String, JsonNode> send() throws IOException;
    }

    public Request request() throws IOException {
        return () -> {
            final var result = new HashMap<String, JsonNode>();
            try (InputStream in = uri.toURL().openStream();
                    Scanner scanner = new Scanner(in, StandardCharsets.UTF_8)) {

                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    final String js = scanner.next();
                    for (String assignment : js.split(CONST)) {
                        if (!assignment.trim().startsWith(COMMENT)) {
                            final var indexOfEquals = assignment.indexOf(EQUALS);
                            if (indexOfEquals > 0) {
                                result.put(
                                        assignment.substring(0, indexOfEquals).trim(),
                                        toJsonNode(
                                                assignment.substring(indexOfEquals + EQUALS.length()).trim()));
                            }
                        }
                    }
                }

            }
            return result;
        };
    }

}
