package io.quarkus.kotlin.deployment;

import java.util.Arrays;
import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "allopen")
public class AllOpenConfig {

    public final List<DotName> defaultAnnotations = Arrays.asList(
            DotName.createSimple("javax.ws.rs.Path"),
            DotName.createSimple("javax.enterprise.context.ApplicationScoped"),
            DotName.createSimple("io.quarkus.test.junit.QuarkusTest"));
}
