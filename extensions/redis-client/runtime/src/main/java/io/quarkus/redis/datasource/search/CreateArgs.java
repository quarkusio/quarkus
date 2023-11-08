package io.quarkus.redis.datasource.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.quarkus.redis.runtime.datasource.Validation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represents the argument of the {@code ft.create} command.
 */
public class CreateArgs implements RedisCommandExtraArguments {
    private boolean onJson;
    private boolean onHash;
    private String[] prefixes;
    private String filter;
    private String language;
    private String languageField;
    private double score = -1.0;
    private String scoreField;
    private String payloadField;
    private boolean maxTextFields;
    private boolean noOffsets;
    private Duration temporary;
    private boolean noHl;
    private boolean noFields;
    private boolean noFreqs;
    private String[] stopWords;
    private boolean skipInitialScan;

    private final List<IndexedField> schema = new ArrayList<>();

    /**
     * Enables indexing hash objects.
     *
     * @return the current {@code CreateArgs}
     */
    public CreateArgs onHash() {
        this.onHash = true;
        return this;
    }

    /**
     * Enable indexing JSON documents.
     * To index JSON, you must have the RedisJSON module installed.
     *
     * @return the current {@code CreateArgs}
     */
    public CreateArgs onJson() {
        this.onJson = true;
        return this;
    }

    /**
     * Tells the index which keys it should index. You can add several prefixes to index.
     * Because the argument is optional, the default is * (all keys).
     *
     * @param prefixes the prefix of the keys
     * @return the current {@code CreateArgs}
     */
    public CreateArgs prefixes(String... prefixes) {
        this.prefixes = doesNotContainNull(notNullOrEmpty(prefixes, "prefixes"), "prefixes");
        return this;
    }

    /**
     * Sets a filter expression with the full RediSearch aggregation expression language.
     * It is possible to use @__key to access the key that was just added/changed.
     * A field can be used to set field name by passing 'FILTER @indexName=="myindexname"'.
     *
     * @param filter the filter
     * @return the current {@code CreateArgs}
     */
    public CreateArgs filter(String filter) {
        this.filter = notNullOrBlank(filter, "filter");
        return this;
    }

    /**
     * If set, indicates the default language for documents in the index. Default to English.
     *
     * @param language the language
     * @return the current {@code CreateArgs}
     */
    public CreateArgs language(String language) {
        this.language = notNullOrBlank(language, "language");
        return this;
    }

    /**
     * Set a document attribute used as the document language.
     * A stemmer is used for the supplied language during indexing. If an unsupported language is sent, the command
     * returns an error.
     *
     * @param languageField the language field
     * @return the current {@code CreateArgs}
     */
    public CreateArgs languageField(String languageField) {
        this.languageField = notNullOrBlank(languageField, "languageField");
        return this;
    }

    /**
     * Set the default score for documents in the index. Default score is 1.0.
     *
     * @param score the score
     * @return the current {@code CreateArgs}
     */
    public CreateArgs score(double score) {
        positive(score, "score");
        this.score = score;
        return this;
    }

    /**
     * Sets the document attribute that you use as the document rank based on the user ranking.
     * Ranking must be between 0.0 and 1.0. If not set, the default score is 1.
     *
     * @param field the field
     * @return the current {@code CreateArgs}
     */
    public CreateArgs scoreField(String field) {
        this.scoreField = notNullOrBlank(field, "field");
        return this;
    }

    /**
     * Sets the document attribute that you use as a binary safe payload string to the document that can be evaluated
     * at query time by a custom scoring function or retrieved to the client.
     *
     * @param field the field
     * @return the current {@code CreateArgs}
     */
    public CreateArgs payloadField(String field) {
        this.payloadField = notNullOrBlank(field, "field");
        return this;
    }

    /**
     * Forces RediSearch to encode indexes as if there were more than 32 text attributes, which allows you to add
     * additional attributes (beyond 32) using {@code FT.ALTER}. For efficiency, RediSearch encodes indexes differently
     * if they are created with less than 32 text attributes.
     *
     * @return the current {@code CreateArgs}
     */
    public CreateArgs maxTextFields() {
        this.maxTextFields = true;
        return this;
    }

    /**
     * Sets to not store term offsets for documents.
     * It saves memory, but does not allow exact searches or highlighting. It implies {@code NOHL}.
     *
     * @return the current {@code CreateArgs}
     */
    public CreateArgs noOffsets() {
        this.noOffsets = true;
        return this;
    }

    /**
     * Creates a lightweight temporary index that expires after a specified period of inactivity.
     * The internal idle timer is reset whenever the index is searched or added to. Because such indexes are lightweight,
     * you can create thousands of such indexes without negative performance implications and, therefore, you should
     * consider using {@code SKIPINITIALSCAN} to avoid costly scanning.
     * <p>
     * When dropped, a temporary index does not delete the hashes as they may have been indexed in several indexes.
     * Adding the {@code DD} flag deletes the hashes as well.
     *
     * @param duration the duration
     * @return the current {@code CreateArgs}
     */
    public CreateArgs temporary(Duration duration) {
        this.temporary = validate(duration, "duration");
        return this;
    }

    /**
     * Conserves storage space and memory by disabling highlighting support. If set, the corresponding byte offsets
     * for term positions are not stored. {@code NOHL} is also implied by {@code NOOFFSETS}.
     *
     * @return the current {@code CreateArgs}
     */
    public CreateArgs noHl() {
        this.noHl = true;
        return this;
    }

    /**
     * Does not store attribute bits for each term. It saves memory, but it does not allow filtering by specific
     * attributes.
     *
     * @return the current {@code CreateArgs}
     */
    public CreateArgs noFields() {
        this.noFields = true;
        return this;
    }

    /**
     * Avoids saving the term frequencies in the index. It saves memory, but does not allow sorting based on the
     * frequencies of a given term within the document.
     *
     * @return the current {@code CreateArgs}
     */
    public CreateArgs noFreqs() {
        this.noFreqs = true;
        return this;
    }

    /**
     * Sets the index with a custom stop word list, to be ignored during indexing and search time.
     *
     * @param words the stop word list
     * @return the current {@code CreateArgs}
     */
    public CreateArgs stopWords(String... words) {
        this.stopWords = doesNotContainNull(notNullOrEmpty(words, "words"), "words");
        return this;
    }

    /**
     * If set, does not scan and index.
     *
     * @return the current {@code CreateArgs}
     */
    public CreateArgs skipInitialScan() {
        this.skipInitialScan = true;
        return this;
    }

    /**
     * Adds a field to the schema.
     *
     * @param field the field
     * @param alias the alias, can be {@code null}
     * @param type the field type
     * @param options the additional options
     * @return the current {@code CreateArgs}
     */
    public CreateArgs indexedField(String field, String alias, FieldType type, FieldOptions options) {
        schema.add(new IndexedField(field, alias, type, options));
        return this;
    }

    /**
     * Adds a field to the schema.
     *
     * @param field the field
     * @param type the field type
     * @param options the additional options
     * @return the current {@code CreateArgs}
     */
    public CreateArgs indexedField(String field, FieldType type, FieldOptions options) {
        return indexedField(field, null, type, options);
    }

    /**
     * Adds a field to the schema.
     *
     * @param field the field
     * @param type the field type
     * @return the current {@code CreateArgs}
     */
    public CreateArgs indexedField(String field, FieldType type) {
        return indexedField(field, null, type, null);
    }

    /**
     * Adds a field to the schema.
     *
     * @param field the field
     * @param alias the alias, can be {@code null}
     * @param type the field type
     * @return the current {@code CreateArgs}
     */
    public CreateArgs indexedField(String field, String alias, FieldType type) {
        return indexedField(field, alias, type, null);
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();

        if (onHash) {
            if (onJson) {
                throw new IllegalArgumentException("Cannot use `ON HASH` and `ON JSON` at the same time");
            }
            list.add("ON");
            list.add("HASH");
        }

        if (onJson) {
            list.add("ON");
            list.add("JSON");
        }

        if (prefixes != null && prefixes.length > 0) {
            list.add("PREFIX");
            list.add(Integer.toString(prefixes.length));
            list.addAll(Arrays.asList(prefixes));
        }

        if (filter != null) {
            list.add("FILTER");
            list.add(filter);
        }

        if (language != null) {
            list.add("LANGUAGE");
            list.add(language);
        }

        if (languageField != null) {
            list.add("LANGUAGE_FIELD");
            list.add(languageField);
        }

        if (score > 0) {
            list.add("SCORE");
            list.add(Double.toString(score));
        }

        if (scoreField != null) {
            list.add("SCORE_FIELD");
            list.add(scoreField);
        }

        if (payloadField != null) {
            list.add("PAYLOAD_FIELD");
            list.add(payloadField);
        }

        if (maxTextFields) {
            list.add("MAXTEXTFIELDS");
        }

        if (temporary != null) {
            list.add("TEMPORARY");
            list.add(Long.toString(temporary.toSeconds()));
        }

        if (noOffsets) {
            list.add("NOOFFSETS");
        }

        if (noHl) {
            list.add("NOHL");
        }

        if (noFields) {
            list.add("NOFIELDS");
        }

        if (noFreqs) {
            list.add("NOFREQS");
        }

        if (stopWords != null && stopWords.length > 0) {
            list.add("STOPWORDS");
            list.add(Integer.toString(stopWords.length));
            Collections.addAll(list, stopWords);
        }

        if (skipInitialScan) {
            list.add("SKIPINITIALSCAN");
        }

        if (schema.isEmpty()) {
            throw new IllegalArgumentException("The schema cannot be empty, you configure the indexed fields");
        }
        list.add("SCHEMA");
        for (IndexedField field : schema) {
            list.addAll(field.toArgs());
        }

        return list;
    }

}
