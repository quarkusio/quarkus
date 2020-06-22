pluginManagement {
    repositories {
        mavenLocal()${maven_plugin_repositories}
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
      id 'io.quarkus' version "${quarkusPluginVersion}"
    }
}
rootProject.name='${project_artifactId}'
