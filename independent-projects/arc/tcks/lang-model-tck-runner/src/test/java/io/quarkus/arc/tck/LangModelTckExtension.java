package io.quarkus.arc.tck;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.lang.model.declarations.ClassInfo;

import org.jboss.cdi.lang.model.tck.LangModelVerifier;

public class LangModelTckExtension implements BuildCompatibleExtension {
    @Discovery
    public void addClass(ScannedClasses scan) {
        // `LangModelVerifier` has no bean defining annotation
        // and isn't discovered in annotated discovery
        scan.add(LangModelVerifier.class.getName());
    }

    @Enhancement(types = LangModelVerifier.class)
    public void run(ClassInfo clazz) {
        LangModelVerifier.verify(clazz);
    }
}
