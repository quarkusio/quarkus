package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class FunqyKnativeEventsCodestartTest {
    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder().codestarts("funqy-knative-events")
            .languages(JAVA).build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.funqy.cloudevent.CloudEventGreeting");
        codestartTest.checkGeneratedSource("org.acme.funqy.cloudevent.Person");

        codestartTest.checkGeneratedTestSource("org.acme.funqy.cloudevent.FunqyTest");
        codestartTest.checkGeneratedTestSource("org.acme.funqy.cloudevent.FunqyIT");
    }

    @Test
    @EnabledIfSystemProperty(named = "build-projects", matches = "true")
    void buildAllProjectsForLocalUse() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
