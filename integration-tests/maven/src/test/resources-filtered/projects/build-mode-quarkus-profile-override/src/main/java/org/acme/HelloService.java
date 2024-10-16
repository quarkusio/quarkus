package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@IfBuildProfile("foo")
@ApplicationScoped
public class HelloService {
    public String name() {
        return "from foo";
    }
}
