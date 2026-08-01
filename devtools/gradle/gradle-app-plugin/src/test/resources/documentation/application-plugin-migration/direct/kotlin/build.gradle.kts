// Source: {{source}}
// tag::direct-legacy-build[]
plugins {
    java
    id("io.quarkus")
}

group = "org.acme"
version = "1.0.0"

val quarkusPlatformVersion = providers.gradleProperty("quarkusPlatformVersion").get()
val quarkusPlatformGroupId = providers.gradleProperty("quarkusPlatformGroupId")
    .orElse("io.quarkus.platform").get()

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:quarkus-bom:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-rest")
    quarkusDev("io.quarkus:quarkus-jdbc-h2")

    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.rest-assured:rest-assured")
}

quarkus {
    finalName = "acme-service"
    quarkusBuildProperties.put("quarkus.package.jar.type", "fast-jar")
    quarkusBuildProperties.put("quarkus.http.test-port", "0")
    cachingRelevantProperties.add("deploymentTarget")
    codeGenerationProviders = listOf("openapi")
    codeGenerationInputs = listOf("openapi")
    manifest {
        attributes(mapOf("Implementation-Title" to "Acme service"))
        attributes(mapOf("Specification-Version" to "1.0"), "Specification")
    }
    buildForkOptions {
        maxHeapSize = "512m"
        systemProperty("user.language", "en")
    }
    codeGenForkOptions {
        maxHeapSize = "256m"
    }
}

tasks.quarkusBuild {
    nativeArgs {
        "container-build" to true
    }
}
// end::direct-legacy-build[]
