{#include build-layout}
{#plugins}
plugins {
    scala
    id("{quarkus.gradle-plugin.id}")
}
{/plugins}
{/include}

tasks.withType<ScalaCompile> {
    scalaCompileOptions.encoding = "UTF-8"
    {#if java.version == "11"}
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
    {#else}
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
    {/if}
}
