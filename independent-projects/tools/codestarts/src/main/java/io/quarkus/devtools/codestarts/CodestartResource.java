package io.quarkus.devtools.codestarts;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;

public interface CodestartResource {

    String pathName();

    String read(String codestartRelativePath);

    Path copyTo(String codestartRelativePath, Path target, CopyOption... copyOptions);

    List<Source> listSources(String languageDir);

    boolean dirExists(String languageDir);

    List<String> getLanguageDirs();

    Optional<Source> getSource(String languageDir, String name);

    class Source {

        private final CodestartResource resource;
        private final String languageDir;
        private final String path;

        public Source(CodestartResource resource, String languageDir, String path) {
            this.resource = resource;
            this.languageDir = languageDir;
            this.path = path;
        }

        public CodestartResource getCodestartResource() {
            return resource;
        }

        public String getLanguageDir() {
            return languageDir;
        }

        public String getFileName() {
            return FilenameUtils.getName(path);
        }

        public String getFileDir() {
            return FilenameUtils.getPath(this.path);
        }

        public String path() {
            return path;
        }

        public String read() {
            return resource.read(pathInCodestart());
        }

        public Path copyTo(Path target, CopyOption... copyOptions) {
            return resource.copyTo(pathInCodestart(), target, copyOptions);
        }

        public String pathInCodestart() {
            return languageDir + "/" + path;
        }

        public String absolutePath() {
            return resource.pathName() + "/" + pathInCodestart();
        }
    }

    class PathCodestartResource implements CodestartResource {

        Path codestartDir;

        public PathCodestartResource(Path codestartDir) {
            this.codestartDir = codestartDir;
        }

        @Override
        public String pathName() {
            return codestartDir.toString();
        }

        @Override
        public String read(String codestartRelativePath) {
            try {
                return new String(Files.readAllBytes(codestartDir.resolve(codestartRelativePath)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public List<Source> listSources(String languageDir) {
            final Path dir = codestartDir.resolve(languageDir);
            try (final Stream<Path> pathStream = Files.walk(dir)) {
                return pathStream
                        .filter(Files::isRegularFile)
                        .map(path -> dir.relativize(path).toString())
                        .map(n -> new Source(this, languageDir, n))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public boolean dirExists(String languageDir) {
            return Files.isDirectory(codestartDir.resolve(languageDir));
        }

        @Override
        public List<String> getLanguageDirs() {
            try (final Stream<Path> files = Files.list(codestartDir)) {
                return files
                        .filter(Files::isDirectory)
                        .map(CodestartCatalogLoader::getDirName)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public Optional<Source> getSource(String languageDir, String name) {
            final Path path = codestartDir.resolve(languageDir).resolve(name);
            if (!Files.isRegularFile(path)) {
                return Optional.empty();
            }
            return Optional.of(new Source(this, languageDir, name));
        }

        @Override
        public Path copyTo(String codestartRelativePath, Path target, CopyOption... copyOptions) {
            try {
                return Files.copy(codestartDir.resolve(codestartRelativePath), target, copyOptions);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

}
