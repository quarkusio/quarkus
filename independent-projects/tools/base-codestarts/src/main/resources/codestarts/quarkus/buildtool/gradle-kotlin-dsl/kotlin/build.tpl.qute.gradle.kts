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
    {#if quarkus.platform.version.startsWith("2.") or quarkus.platform.version.startsWith("1.")}
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    {#else}
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    {/if}
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_{java.version}.toString()
    kotlinOptions.javaParameters = true
}
