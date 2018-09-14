package org.jboss.shamrock.deployment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ArchiveContextBuilder {

    private List<Path> additionalApplicationArchives = new ArrayList<>();

    public ArchiveContextBuilder addAdditionalApplicationArchive(Path path) {
        additionalApplicationArchives.add(path);
        return this;
    }

    List<Path> getAdditionalApplicationArchives() {
        return additionalApplicationArchives;
    }
}
