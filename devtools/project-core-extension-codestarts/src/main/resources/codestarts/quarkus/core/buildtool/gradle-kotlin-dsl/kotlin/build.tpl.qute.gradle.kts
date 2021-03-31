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
    {#if java.version == "11"}
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    {#else}
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    {/if}
    kotlinOptions.javaParameters = true
}
