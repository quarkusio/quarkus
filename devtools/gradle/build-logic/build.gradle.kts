plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(plugin("com.gradle.plugin-publish", "1.2.0"))
}

fun DependencyHandler.plugin(id: String, version: String) =
    create("$id:$id.gradle.plugin:$version")
