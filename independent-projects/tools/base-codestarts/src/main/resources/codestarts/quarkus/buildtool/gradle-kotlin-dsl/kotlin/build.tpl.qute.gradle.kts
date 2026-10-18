{#let kotlinSerialization = input.selected-extensions-ga.contains('io.quarkus:quarkus-rest-kotlin-serialization') or input.selected-extensions-ga.contains('io.quarkus:quarkus-rest-client-kotlin-serialization')}
{#include build-layout}
{#plugins}
plugins {
    kotlin("jvm") version "{kotlin.version}"
    kotlin("plugin.allopen") version "{kotlin.version}"
    {#if kotlinSerialization}
    kotlin("plugin.serialization") version "{kotlin.version}"
    {/if}
    id("{quarkus.gradle-plugin.id}")
}
{/plugins}
{/include}

allOpen {
    {#if quarkus.platform.version.startsWith("2.") or quarkus.platform.version.startsWith("1.")}
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("javax.persistence.Entity")
    {#else}
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    {/if}
    annotation("io.quarkus.test.junit.QuarkusTest")
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_{java.version}
        javaParameters = true
    }
}
