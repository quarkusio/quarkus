package io.quarkus.arc.processor;

import java.io.IOException;
import java.io.InputStream;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

public final class Basics {

    public static DotName name(Class<?> clazz) {
        return DotName.createSimple(clazz.getName());
    }

    public static Index index(Class<?>... classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (InputStream stream = Basics.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }

}
