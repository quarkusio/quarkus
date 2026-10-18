package io.quarkus.cli.plugin;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public interface Catalog<T extends Catalog<T>> {

    Optional<Path> getCatalogLocation();

    default T withCatalogLocation(File catalogLocation) {
        return withCatalogLocation(catalogLocation.toPath());
    }

    default T withCatalogLocation(Path catalogLocation) {
        return withCatalogLocation(Optional.of(catalogLocation));
    }

    T withCatalogLocation(Optional<Path> catalogLocation);

    T refreshLastUpdate();
}
