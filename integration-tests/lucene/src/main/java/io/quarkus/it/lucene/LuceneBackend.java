package io.quarkus.it.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;

/**
 * Helper class to exercise Lucene API
 */
public class LuceneBackend {

    Directory createDirectory(Class<?> directory, String folder) throws IOException {
        if (directory == RAMDirectory.class) {
            return new RAMDirectory();
        }
        if (directory == ByteBuffersDirectory.class) {
            return new ByteBuffersDirectory();
        }
        if (FSDirectory.class.isAssignableFrom(directory)) {
            Path location = Files.createTempDirectory(Paths.get("target"), folder);
            if (directory == SimpleFSDirectory.class) {
                return new SimpleFSDirectory(location.resolve("simple"));
            }
            if (directory == NIOFSDirectory.class) {
                return new NIOFSDirectory(location.resolve("nio"));
            }
            if (directory == MMapDirectory.class) {
                return new MMapDirectory(location.resolve("mmap"));
            }
        }
        throw new IllegalArgumentException("Director class " + directory + " not found");
    }

    public void indexPerson(Person person, Directory directory) throws IOException {
        Analyzer standardAnalyzer = Person.getAnalyzerPerField();
        IndexWriterConfig config = new IndexWriterConfig(standardAnalyzer);
        IndexWriter writer = new IndexWriter(directory, config);
        writer.addDocument(person.toDocument());
        writer.close();
    }

    public void forceMerge(Directory directory) throws IOException {
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig());
        writer.forceMerge(1);
    }

    public void destroyDirectory(Directory directory) throws IOException {
        if (directory instanceof FSDirectory) {
            FSDirectory fsDirectory = (FSDirectory) directory;
            Path path = fsDirectory.getDirectory();
            fsDirectory.close();
            FileUtils.deleteDirectory(path.toFile());
        } else {
            directory.close();
        }
    }

    public List<String> search(Directory directory, String queryString) throws IOException, ParseException {
        // Open index reader
        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        // Create the query
        QueryParser queryParser = new EnhancedQueryParser("name", new StandardAnalyzer());
        Query query = queryParser.parse(queryString);

        // Collect results
        TopScoreDocCollector collector = TopScoreDocCollector.create(10, 10);
        searcher.search(query, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        List<String> results = new ArrayList<>();
        for (ScoreDoc hit : hits) {
            Document result = reader.document(hit.doc);
            results.add(result.get("name"));
        }
        return results;
    }
}