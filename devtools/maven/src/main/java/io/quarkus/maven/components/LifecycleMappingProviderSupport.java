package io.quarkus.maven.components;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;

import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;

/**
 * See <a href="https://maveniverse.eu/blog/2024/09/07/how-to-add-new-maven-lifecycle-mapping/">this article</a>
 * for more details about this approach, as kindly explained by Tamas Cservenak.
 */
public abstract class LifecycleMappingProviderSupport implements Provider<LifecycleMapping> {

    private static final String DEFAULT_LIFECYCLE_KEY = "default";

    private final Lifecycle defaultLifecycle;
    private final LifecycleMapping lifecycleMapping;

    public LifecycleMappingProviderSupport() {
        this.defaultLifecycle = new Lifecycle();
        this.defaultLifecycle.setId(DEFAULT_LIFECYCLE_KEY);
        this.defaultLifecycle.setLifecyclePhases(loadMapping());

        this.lifecycleMapping = new LifecycleMapping() {
            @Override
            public Map<String, Lifecycle> getLifecycles() {
                return Collections.singletonMap(DEFAULT_LIFECYCLE_KEY, defaultLifecycle);
            }

            @Override
            public List<String> getOptionalMojos(String lifecycle) {
                return null;
            }

            @Override
            public Map<String, String> getPhases(String lifecycle) {
                if (DEFAULT_LIFECYCLE_KEY.equals(lifecycle)) {
                    return defaultLifecycle.getPhases();
                } else {
                    return null;
                }
            }
        };
    }

    private Map<String, LifecyclePhase> loadMapping() {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream(getClass().getSimpleName() + ".properties")) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        HashMap<String, LifecyclePhase> result = new HashMap<>();
        for (String phase : properties.stringPropertyNames()) {
            result.put(phase, new LifecyclePhase(properties.getProperty(phase)));
        }
        return result;
    }

    @Override
    public LifecycleMapping get() {
        return lifecycleMapping;
    }
}
