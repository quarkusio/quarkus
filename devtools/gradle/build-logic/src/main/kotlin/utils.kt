import org.gradle.api.artifacts.VersionCatalog

fun VersionCatalog.getLibrary(alias: String) =
    findLibrary(alias).orElseThrow { IllegalArgumentException("library $alias not found in catalog $name") }
