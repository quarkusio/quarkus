{#include build-layout}
{#plugins}
plugins {
    kotlin("jvm") version "{kotlin.version}"
    kotlin("plugin.allopen") version "{kotlin.version}"
    id("{quarkus.gradle-plugin.id}")
}
{/plugins}
{/include}

allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_{java.version}.toString()
    kotlinOptions.javaParameters = true
}
