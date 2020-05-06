package io.quarkus.qoder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.dependencies.Extension;

public class MavenProjectExtensionsManager implements ProjectExtensionsManager {
    private static final String OPEN_TAG_REGEX_FORMAT = ".*<%s[^>]*>.*";
    private static final String CLOSE_TAG_REGEX_FORMAT = ".*</%s[^>]*>.*";
    private static final String VALUE_TAG_REGEX_FORMAT = ".*<%s[^>]*>%s</%s[^>]*>.*";

    private final Path pomPath;
    private List<String> lines = null;

    public MavenProjectExtensionsManager(Path pomPath) {
        this.pomPath = pomPath;
    }

    @Override
    public void addExtension(Extension extension) {
        // We could just append the extension at the top of the dependencies
        throw new IllegalAccessError("Unimplemented");
    }

    @Override
    public boolean hasExtension(Extension extension) {
        // We could optimize this for multiple calls (we don't need the range)
        return detectDependencyRange(extension.getArtifactId()).isPresent();
    }

    @Override
    public void removeExtension(Extension extension) {
        final Optional<Range> interval = detectDependencyRange(extension.getArtifactId());
        if(!interval.isPresent()) {
            return;
        }
        lines.subList(interval.get().start, interval.get().end).clear();
        saveLines();
    }

    Optional<Range> detectDependencyRange(final String artifactId) {
        final List<String> lines = getLines();
        final Optional<Range> dependenciesInterval = detectDependenciesRange();
        if (!dependenciesInterval.isPresent()) {
            return Optional.empty();
        }
        int startIndex = -1;
        int endIndex = -1;
        for (int i = dependenciesInterval.get().start + 1; i < dependenciesInterval.get().end ; i++) {
            final String line = lines.get(i);
            // We don't look for version/groupId (we don't need it) and assume that artifactId is unique
            if (line.matches(getValueTagRegex("artifactId", artifactId))) {
                // Look for the dependency open tag
                for (int j = i -1; j > dependenciesInterval.get().start + 1; j--) {
                    if(lines.get(j).matches(getOpenTagRegex("dependency"))) {
                        startIndex = j;
                    }
                }
                // Look for the dependency close tag
                for (int j = i + 1; j < dependenciesInterval.get().end; j++) {
                    if(lines.get(j).matches(getOpenTagRegex("dependency"))) {
                        endIndex = j;
                    }
                }
            }
        }
        if(startIndex > 0 && endIndex > startIndex) {
            return Optional.of(new Range(startIndex, endIndex));
        }
        return Optional.empty();
    }



    Optional<Range> detectDependenciesRange() {
        boolean ignore = false;
        int startIndex = -1;
        int endIndex = -1;
        final List<String> lines = getLines();
        for (int i = 0; i < lines.size() ; i++) {
            final String line = lines.get(i);
            // We want to ignore dependencies in dependencyManagement
            if(line.matches(getOpenTagRegex("dependencyManagement"))) {
                ignore = true;
            }
            if(line.matches(getCloseTagRegex("dependencyManagement"))) {
                ignore = false;
            }
            if(!ignore && line.matches(getOpenTagRegex("dependencies"))) {
                startIndex = i;
            }
            if(!ignore && line.matches(getCloseTagRegex( "dependencies"))) {
                endIndex = i;
            }
        }
        if(startIndex > 0 && endIndex > startIndex) {
            return Optional.of(new Range(startIndex, endIndex));
        }
        return Optional.empty();
    }

    private String getOpenTagRegex(String tagName) {
        return String.format(OPEN_TAG_REGEX_FORMAT, tagName);
    }

    private String getValueTagRegex(String tagName, String value) {
        return String.format(VALUE_TAG_REGEX_FORMAT, tagName, value, tagName);
    }

    private String getCloseTagRegex(String tagName) {
        return String.format(CLOSE_TAG_REGEX_FORMAT, tagName);
    }

    private List<String> getLines()  {
        if(lines == null) {
            try {
                lines = Files.readAllLines(pomPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return lines;
    }

    private void saveLines()  {
        if(lines == null) {
            return;
        }
        try {
            Files.write(pomPath, getLines());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static class Range {
        public final int start;
        public final int end;

        Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Range range = (Range) o;
            return start == range.start &&
                end == range.end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }

}
