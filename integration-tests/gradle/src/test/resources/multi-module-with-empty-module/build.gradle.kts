plugins {
  id("io.quarkus").apply(false)
}


allprojects {

  group = "org.acme"
  version = "1.0.0-SNAPSHOT"

  repositories {
    if (System.getProperties().containsKey("maven.repo.local")) {
      maven(url = System.getProperties().get("maven.repo.local")!!)
    } else {
      mavenLocal()
    }
    mavenCentral()
  }

}

subprojects {

  apply(plugin = "java")
  apply(plugin = "jacoco")
  apply(plugin = "io.quarkus")

  val quarkusPlatformGroupId: String by project
  val quarkusPlatformArtifactId: String by project
  val quarkusPlatformVersion: String by project

  val javaVersion = "11"

  dependencies {
    "implementation"(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))

    "testImplementation"("io.quarkus:quarkus-junit5")
    "testImplementation"("io.rest-assured:rest-assured")
  }

  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
  }

  tasks {

    withType<JavaCompile>().configureEach {
      options.encoding = "UTF-8"
      options.compilerArgs.add("-parameters")
    }

    named<Test>("test") {
      systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    }

  }

}
