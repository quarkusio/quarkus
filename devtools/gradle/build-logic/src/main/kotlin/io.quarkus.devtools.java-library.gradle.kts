plugins {
    id("java-library")
    id("java-test-fixtures")
    id("maven-publish")
}

dependencies {
    api(gradleApi())

    val libs = project.the<VersionCatalogsExtension>().named("libs")
    implementation(platform("io.quarkus:quarkus-bom:$version"))

    implementation(libs.getLibrary("quarkus-bootstrap-core"))
    implementation(libs.getLibrary("quarkus-bootstrap-gradle-resolver"))
    implementation(libs.getLibrary("quarkus-core-deployment"))

    testFixturesApi(gradleTestKit())

    testFixturesApi(platform(libs.getLibrary("junit-bom")))
    testFixturesApi(libs.getLibrary("junit-api"))
    testFixturesApi(libs.getLibrary("assertj"))

    testFixturesApi(platform("io.quarkus:quarkus-bom:$version"))
    testFixturesApi(libs.getLibrary("quarkus-devtools-testing"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

tasks.named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
    options.encoding = "UTF-8"
    (options as? CoreJavadocOptions)?.addStringOption("Xdoclint:-reference", "-quiet")
}

tasks.named<Test>(JavaPlugin.TEST_TASK_NAME) {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }

    // propagate the custom local maven repo, in case it's configured
	val mavenRepoLocal = providers.systemProperty("maven.repo.local")
	if (mavenRepoLocal.isPresent) {
		systemProperty("maven.repo.local", mavenRepoLocal.get())
	}
}
