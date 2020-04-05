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
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write test classes index", e);
        }
    }

    public static Index readIndex(Class<?> testClass) {
        try (FileInputStream fis = new FileInputStream(indexPath(testClass).toFile())) {
            return new IndexReader(fis).read();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read test classes index", e);
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
}
