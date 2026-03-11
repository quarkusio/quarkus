package io.quarkus.devtools.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

/**
 * Provides Quarkus documentation search using BGE embeddings + pgvector.
 * Connects to a pgvector Docker container with pre-indexed Quarkus docs
 * (produced by chappie-docling-rag).
 */
public class DocSearchService {

    private static final String DEFAULT_IMAGE = "ghcr.io/quarkusio/chappie-ingestion-quarkus:3.31.4";
    private static final String CONTAINER_NAME = "quarkus-docs-pgvector";
    private static final int PG_PORT = 5433; // use non-default to avoid conflicts
    private static final String PG_USER = "quarkus";
    private static final String PG_PASSWORD = "quarkus";
    private static final String PG_DB = "quarkus";
    private static final String TABLE_NAME = "rag_documents";
    private static final int DIMENSION = 384;
    private static final double MIN_SCORE = 0.82;
    private static final int SEARCH_CANDIDATES = 50; // fetch more for reranking
    private static final int DEFAULT_MAX_RESULTS = 4;

    private volatile EmbeddingModel embeddingModel;
    private volatile PgVectorEmbeddingStore embeddingStore;

    /**
     * Search Quarkus documentation for the given query.
     * Starts the pgvector Docker container if not already running.
     */
    public List<Map<String, Object>> search(String query, int maxResults) {
        ensureInitialized();

        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(SEARCH_CANDIDATES)
                .minScore(MIN_SCORE)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();

        // Apply metadata boosting
        List<ScoredMatch> boosted = applyMetadataBoost(matches, query);

        // Take top N
        int limit = maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS;
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, boosted.size()); i++) {
            ScoredMatch sm = boosted.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("score", Math.round(sm.score * 1000.0) / 1000.0);
            entry.put("text", sm.match.embedded().text());

            // Add metadata
            Map<String, String> metadata = new LinkedHashMap<>();
            sm.match.embedded().metadata().toMap().forEach((k, v) -> metadata.put(k, String.valueOf(v)));
            if (!metadata.isEmpty()) {
                entry.put("metadata", metadata);
            }
            results.add(entry);
        }
        return results;
    }

    private synchronized void ensureInitialized() {
        if (embeddingModel == null) {
            System.err.println("[mcp] Loading BGE Small EN v1.5 embedding model...");
            embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
            System.err.println("[mcp] Embedding model loaded.");
        }
        if (embeddingStore == null) {
            ensureContainerRunning();
            // Retry connection a few times — pgvector extension may need a moment
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    System.err.println("[mcp] Connecting to pgvector at localhost:" + PG_PORT
                            + " (attempt " + attempt + ")...");
                    embeddingStore = PgVectorEmbeddingStore.builder()
                            .host("localhost")
                            .port(PG_PORT)
                            .database(PG_DB)
                            .user(PG_USER)
                            .password(PG_PASSWORD)
                            .table(TABLE_NAME)
                            .dimension(DIMENSION)
                            .createTable(false) // table already exists in the Docker image
                            .useIndex(false) // index already exists in the Docker image
                            .build();
                    System.err.println("[mcp] Connected to pgvector.");
                    break;
                } catch (Exception e) {
                    if (attempt == 3) {
                        throw new RuntimeException("Failed to connect to pgvector after 3 attempts: " + e.getMessage(),
                                e);
                    }
                    System.err.println("[mcp] Connection attempt " + attempt + " failed: " + e.getMessage()
                            + ". Retrying in 3s...");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while connecting to pgvector", ie);
                    }
                }
            }
        }
    }

    private void ensureContainerRunning() {
        if (isContainerRunning()) {
            System.err.println("[mcp] pgvector container '" + CONTAINER_NAME + "' is already running.");
            return;
        }

        // Check if container exists but is stopped
        if (containerExists()) {
            System.err.println("[mcp] Starting existing pgvector container...");
            exec("docker", "start", CONTAINER_NAME);
        } else {
            System.err.println("[mcp] Pulling and starting pgvector container with Quarkus docs...");
            exec("docker", "run", "-d",
                    "--name", CONTAINER_NAME,
                    "-p", PG_PORT + ":5432",
                    "-e", "POSTGRES_USER=" + PG_USER,
                    "-e", "POSTGRES_PASSWORD=" + PG_PASSWORD,
                    "-e", "POSTGRES_DB=" + PG_DB,
                    DEFAULT_IMAGE);
        }

        // Wait for PostgreSQL to be ready
        System.err.println("[mcp] Waiting for PostgreSQL to be ready...");
        for (int i = 0; i < 30; i++) {
            try {
                Thread.sleep(1000);
                String result = execOutput("docker", "exec", CONTAINER_NAME,
                        "pg_isready", "-U", PG_USER);
                if (result.contains("accepting connections")) {
                    // Verify the database and pgvector extension are ready
                    try {
                        String extCheck = execOutput("docker", "exec", CONTAINER_NAME,
                                "psql", "-U", PG_USER, "-d", PG_DB, "-c",
                                "SELECT count(*) FROM " + TABLE_NAME + " LIMIT 1");
                        if (extCheck.contains("count")) {
                            System.err.println("[mcp] PostgreSQL and pgvector are ready.");
                            return;
                        }
                    } catch (Exception e) {
                        // table not ready yet, keep waiting
                    }
                }
            } catch (Exception e) {
                // not ready yet
            }
        }
        throw new RuntimeException("PostgreSQL did not become ready in 30 seconds");
    }

    private boolean isContainerRunning() {
        try {
            String output = execOutput("docker", "inspect", "-f", "{{.State.Running}}", CONTAINER_NAME);
            return "true".equals(output.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean containerExists() {
        try {
            execOutput("docker", "inspect", CONTAINER_NAME);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void exec(String... command) {
        try {
            Process p = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            p.getInputStream().transferTo(System.err);
            int exit = p.waitFor();
            if (exit != 0) {
                throw new RuntimeException("Command failed (exit " + exit + "): " + String.join(" ", command));
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute: " + String.join(" ", command) + ": " + e.getMessage(), e);
        }
    }

    private String execOutput(String... command) {
        try {
            Process p = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().reduce("", (a, b) -> a + b + "\n").trim();
            }
            int exit = p.waitFor();
            if (exit != 0) {
                throw new RuntimeException("Command failed (exit " + exit + "): " + output);
            }
            return output;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute: " + String.join(" ", command) + ": " + e.getMessage(), e);
        }
    }

    // --- Metadata boosting ---

    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("startup", "lifecycle"),
            Map.entry("injection", "cdi"),
            Map.entry("di", "cdi"),
            Map.entry("dependency injection", "cdi"),
            Map.entry("rest", "resteasy"),
            Map.entry("api", "rest"),
            Map.entry("database", "datasource"),
            Map.entry("db", "datasource"),
            Map.entry("orm", "hibernate"),
            Map.entry("jpa", "hibernate"),
            Map.entry("security", "authentication"),
            Map.entry("auth", "authentication"),
            Map.entry("test", "testing"),
            Map.entry("container", "docker"),
            Map.entry("reactive", "mutiny"),
            Map.entry("config", "configuration"),
            Map.entry("deploy", "deployment"),
            Map.entry("native", "native-image"),
            Map.entry("grpc", "grpc"),
            Map.entry("graphql", "graphql"),
            Map.entry("websocket", "websockets"),
            Map.entry("kafka", "messaging"),
            Map.entry("amqp", "messaging"));

    private List<ScoredMatch> applyMetadataBoost(List<EmbeddingMatch<TextSegment>> matches, String query) {
        String queryLower = query.toLowerCase();
        String[] queryTerms = queryLower.split("\\s+");

        List<ScoredMatch> scored = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            double score = match.score();
            TextSegment segment = match.embedded();
            if (segment == null) {
                continue;
            }

            String title = segment.metadata().getString("title");
            String repoPath = segment.metadata().getString("repo_path");
            if (title == null) {
                title = "";
            }
            if (repoPath == null) {
                repoPath = "";
            }
            String titleLower = title.toLowerCase();
            String pathLower = repoPath.toLowerCase();

            for (String term : queryTerms) {
                // Direct matches
                if (titleLower.contains(term)) {
                    score += 0.15;
                }
                if (pathLower.contains(term)) {
                    score += 0.10;
                }

                // Synonym matches
                String synonym = SYNONYMS.get(term);
                if (synonym != null) {
                    if (titleLower.contains(synonym)) {
                        score += 0.12;
                    }
                    if (pathLower.contains(synonym)) {
                        score += 0.08;
                    }
                }
            }

            scored.add(new ScoredMatch(match, score));
        }

        // Sort by boosted score descending
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored;
    }

    private record ScoredMatch(EmbeddingMatch<TextSegment> match, double score) {
    }
}
