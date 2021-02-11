pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()${maven_plugin_repositories}
    }
    plugins {
      id 'io.quarkus' version "${quarkusPluginVersion}"
    }
}
rootProject.name='${project_artifactId}'
