rootProject.name = "relocation"

include("common", "app")

// RelocatedBuildCacheTest builds two copies of this project, at different locations, against the same cache
buildCache {
    local {
        directory = file(providers.gradleProperty("sharedBuildCache").get())
    }
}
