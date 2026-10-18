package io.quarkus.it.testing.repro44320;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Startup;

@ApplicationScoped
@Startup
@Unremovable
public class MyService {
    public Set<String> get() {
        return Set.of("a", "b", "c");
    }
}
