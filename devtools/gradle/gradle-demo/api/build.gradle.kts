plugins {
    `java-library`
}

group = "org.acme.gradledemo"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
