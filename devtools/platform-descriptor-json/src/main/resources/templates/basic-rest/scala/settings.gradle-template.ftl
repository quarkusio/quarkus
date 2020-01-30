pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
      id 'io.quarkus' version "${quarkusPluginVersion}"
    }
}
rootProject.name='${project_artifactId}'
