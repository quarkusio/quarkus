package io.quarkus.it.infinispan.embedded;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.infinispan.commons.util.Util;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class InfinispanEmbeddedTestResource implements QuarkusTestResourceLifecycleManager {
    @Override
    public Map<String, String> start() {
        // Since 10.0.xml is not in the classpath for native and not in the image - we have to point to the src one
        String xmlLocation = Paths.get(System.getProperty("basedir"), "src", "main", "resources", "10.0.xml").toString();
        return Collections.singletonMap("quarkus.infinispan-embedded.xml-config", xmlLocation);
    }

    @Override
    public void stop() {
        // Need to clean up persistent file - so tests dont' leak between each other
        String tmpDir = System.getProperty("java.io.tmpdir");
        try {
            Files.walk(Paths.get(tmpDir), 1)
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("quarkus-"))
                    .map(Path::toFile)
                    .forEach(Util::recursiveFileRemove);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
