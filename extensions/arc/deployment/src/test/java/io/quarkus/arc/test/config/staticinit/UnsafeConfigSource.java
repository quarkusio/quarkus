package io.quarkus.arc.test.config.staticinit;

import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

// Intentionally not annotated with @StaticInitSafe so that it's not considered durin the STATIC_INIT
public class UnsafeConfigSource implements ConfigSource {

    @Override
    public Set<String> getPropertyNames() {
        return Set.of("apfelstrudel");
    }

    @Override
    public String getValue(String propertyName) {
        return propertyName.equals("apfelstrudel") ? "gizmo" : null;
    }

    @Override
    public String getName() {
        return "Unsafe Test";
    }

    @Override
    public int getOrdinal() {
        return 500;
    }

}
