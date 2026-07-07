package io.quarkus.gradle.application.internal.plugin;

import java.io.IOException;

import io.quarkus.gradle.testing.BaseGradleTest;

abstract class QuarkusApplicationPluginFunctionalTestSupport extends BaseGradleTest {

    final void writeMultiProjectApplication(boolean projectDependency) throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'multi-project-quarkus-app'
                include 'app', 'lib'
                """);
        writeFile(testProjectDir.resolve("lib/build.gradle"), """
                plugins {
                    id 'java-library'
                }
                """);
        writeFile(testProjectDir.resolve("lib/src/main/java/org/acme/lib/GreetingLibrary.java"), """
                package org.acme.lib;

                public class GreetingLibrary {
                    public String message() {
                        return "hello";
                    }
                }
                """);
        writeFile(testProjectDir.resolve("app/build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                version = '999-SNAPSHOT'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    %s
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:${project.version}")
                    implementation "io.quarkus:quarkus-arc"
                }

                quarkusApplication {
                    builds {
                        fastJar('app')
                    }
                }
                """.formatted(projectDependency ? "implementation project(':lib')" : ""));
        writeFile(testProjectDir.resolve("app/src/main/java/org/acme/GreetingService.java"), """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;
                %s

                @ApplicationScoped
                public class GreetingService {
                    public String hello() {
                        return %s;
                    }
                }
                """.formatted(
                projectDependency ? "import org.acme.lib.GreetingLibrary;" : "",
                projectDependency ? "new GreetingLibrary().message()" : "\"hello\""));
    }

}
