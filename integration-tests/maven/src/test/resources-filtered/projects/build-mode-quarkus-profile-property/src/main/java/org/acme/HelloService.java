package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@IfBuildProfile("foo")
@ApplicationScoped
public class HelloService {

    @ConfigProperty(name = "name")
    String name;

    public String name() {
        return name;
    }
}
