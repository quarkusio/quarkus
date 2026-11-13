rootProject.name = "configuration-cache-multi-module"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

include(":app")
include(":lib")
