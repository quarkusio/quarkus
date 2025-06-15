package io.quarkus.vertx.http.deployment.webjar;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Visitor which holds all web jar resources in memory.
 */
public class InMemoryTargetVisitor implements WebJarResourcesTargetVisitor {
    private Map<String, byte[]> content = new HashMap<>();

    public Map<String, byte[]> getContent() {
        return content;
    }

    @Override
    public void visitFile(String path, InputStream stream) throws IOException {
        content.put(path, stream.readAllBytes());
    }
}
