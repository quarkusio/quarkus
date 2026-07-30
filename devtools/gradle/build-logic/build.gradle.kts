plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(plugin("com.gradle.plugin-publish", "1.2.0"))

    val libs = project.the<VersionCatalogsExtension>().named("libs")
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-api").get())
    testImplementation(libs.findLibrary("assertj").get())
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java { toolchain {
    // this is fine, even for Java 1.x
    val javaMajor = JavaVersion.current().majorVersion.toInt()
    // Need to limit the Java version for Kotlin to 25.
    // Also prefer the current version to prevent JDK downloads.
    languageVersion.set(JavaLanguageVersion.of(javaMajor.coerceAtMost(25)))
} }

fun DependencyHandler.plugin(id: String, version: String) =
    create("$id:$id.gradle.plugin:$version")

tasks.test {
    useJUnitPlatform()
}
