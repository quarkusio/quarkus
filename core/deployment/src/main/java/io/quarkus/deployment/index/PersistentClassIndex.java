package io.quarkus.deployment.index;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

public class PersistentClassIndex {

    final Map<DotName, Optional<ClassInfo>> additionalClasses = new ConcurrentHashMap<>();

    public Map<DotName, Optional<ClassInfo>> getAdditionalClasses() {
        return additionalClasses;
    }
}
