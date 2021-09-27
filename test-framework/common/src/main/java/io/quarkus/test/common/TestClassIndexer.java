package io.quarkus.test.common;

import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.UnsupportedVersion;

public final class TestClassIndexer {

    private TestClassIndexer() {
    }

    public static Index indexTestClasses(Class<?> testClass) {
        final Indexer indexer = new Indexer();
        final Path testClassesLocation = getTestClassesLocation(testClass);
        try {
            if (Files.isDirectory(testClassesLocation)) {
                indexTestClassesDir(indexer, testClassesLocation);
            } else {
                try (FileSystem jarFs = FileSystems.newFileSystem(testClassesLocation, null)) {
                    for (Path p : jarFs.getRootDirectories()) {
                        indexTestClassesDir(indexer, p);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to index the test-classes/ directory.", e);
        }
        return indexer.complete();
    }

    public static void writeIndex(Index index, Class<?> testClass) {
        try (FileOutputStream fos = new FileOutputStream(indexPath(testClass).toFile(), false)) {
            IndexWriter indexWriter = new IndexWriter(fos);
            indexWriter.write(index);
        } catch (IOException ignored) {
            // don't fail to write the index because this error is recoverable at the read site (by just recreating the index)
            // this is necessary for tests that are not part of the application itself, but instead reside in a jar (like the Quarkus Platform tests)
        }
    }

    public static Index readIndex(Class<?> testClass) {
        Path path = indexPath(testClass);
        if (path.toFile().exists()) {
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                return new IndexReader(fis).read();
            } catch (UnsupportedVersion e) {
                throw new UnsupportedVersion("Can't read Jandex index from " + path + ": " + e.getMessage());
            } catch (IOException e) {
                // be lenient since the error is recoverable
                return indexTestClasses(testClass);
            }
        } else {
            return indexTestClasses(testClass);
        }

    }

    private static Path indexPath(Class<?> testClass) {
        return PathTestHelper.getTestClassesLocation(testClass).resolve(testClass.getSimpleName() + ".idx");
    }

    private static void indexTestClassesDir(Indexer indexer, final Path testClassesLocation) throws IOException {
        Files.walkFileTree(testClassesLocation, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.toString().endsWith(".class")) {
                    return FileVisitResult.CONTINUE;
                }
                try (InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {
                    indexer.index(inputStream);
                } catch (Exception e) {
                    // ignore
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void removeIndex(Class<?> requiredTestClass) {
        Path indexPath = indexPath(requiredTestClass);
        if (Files.exists(indexPath)) {
            try {
                Files.delete(indexPath);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to delete file index", e);
            }
        }
    }
}
