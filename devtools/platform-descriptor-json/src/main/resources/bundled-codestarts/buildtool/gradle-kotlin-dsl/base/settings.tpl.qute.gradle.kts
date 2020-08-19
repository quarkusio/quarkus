pluginManagement {
    val quarkusPluginVersion: String by settings
    val quarkusPluginId: String by settings
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
{#if gradle.plugin-repositories}
{#for rep in gradle.plugin-repositories}
        maven { url = uri("{rep.url}") }
{/for}
{/if}
    }
    plugins {
        id(quarkusPluginId) version quarkusPluginVersion
    }
}
rootProject.name="{project.artifact-id}"
