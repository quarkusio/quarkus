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
    sourceCompatibility = JavaVersion.VERSION_{java.version}.toString()
    targetCompatibility = JavaVersion.VERSION_{java.version}.toString()
}
