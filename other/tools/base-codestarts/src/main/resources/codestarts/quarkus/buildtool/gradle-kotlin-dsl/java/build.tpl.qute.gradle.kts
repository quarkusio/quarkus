{#include build-layout}
{#plugins}
plugins {
    java
    id("{quarkus.gradle-plugin.id}")
}
{/plugins}
{/include}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
