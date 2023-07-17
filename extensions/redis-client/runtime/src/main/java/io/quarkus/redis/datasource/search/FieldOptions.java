package io.quarkus.redis.datasource.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows customizing the indexed field.
 */
public class FieldOptions {

    private boolean sortable;
    private boolean unf;
    private boolean noStem;
    private boolean noIndex;
    private String phonetic;
    private double weight = -1;
    private char separator;
    private boolean caseSensitive;
    private boolean withSuffixTrie;

    /**
     * Numeric, tag (not supported with JSON) or text attributes can have the optional SORTABLE argument.
     * As the user sorts the results by the value of this attribute, the results will be available with very low
     * latency. (this adds memory overhead so consider not to declare it on large text attributes).
     *
     * @return the current {@code FieldOptions}
     */
    public FieldOptions sortable() {
        this.sortable = true;
        return this;
    }

    /**
     * By default, {@code SORTABLE} applies a normalization to the indexed value (characters set to lowercase, removal
     * of diacritics). When using un-normalized form (UNF), you can disable the normalization and keep the original
     * form of the value.
     *
     * @return the current {@code FieldOptions}
     */
    public FieldOptions unf() {
        this.unf = true;
        return this;
    }

    /**
     * Text attributes can have the {@code NOSTEM} argument which will disable stemming when indexing its values.
     * This may be ideal for things like proper names.
     *
     * @return the current {@code FieldOptions}
     */
    public FieldOptions noStem() {
        this.noStem = true;
        return this;
    }

    /**
     * Attributes can have the {@code NOINDEX} option, which means they will not be indexed. This is useful in
     * conjunction with {@code SORTABLE}, to create attributes whose update using {@code PARTIAL} will not cause full
     * reindexing of the document. If an attribute has {@code NOINDEX} and doesn't have {@code SORTABLE}, it will just
     * be ignored by the index.
     *
     * @return the current {@code FieldOptions}
     */
    public FieldOptions noIndex() {
        this.noIndex = true;
        return this;
    }

    /**
     * Declaring a text attribute as {@code PHONETIC} will perform phonetic matching on it in searches by default.
     * The obligatory argument specifies the phonetic algorithm and language used.
     *
     * @param phonetic the phonetic algorithm
     * @return the current {@code FieldOptions}
     */
    public FieldOptions phonetic(String phonetic) {
        this.phonetic = phonetic;
        return this;
    }

    /**
     * For TEXT attributes, declares the importance of this attribute when calculating result accuracy.
     * This is a multiplication factor, and defaults to 1 if not specified.
     *
     * @param weight the weight
     * @return the current {@code FieldOptions}
     */
    public FieldOptions weight(double weight) {
        this.weight = weight;
        return this;
    }

    /**
     * For TAG attributes, indicates how the text contained in the attribute is to be split into individual tags.
     * The default is {@code ,}. The value must be a single character.
     *
     * @param separator the separator
     * @return the current {@code FieldOptions}
     */
    public FieldOptions separator(char separator) {
        this.separator = separator;
        return this;
    }

    /**
     * For TAG attributes, keeps the original letter cases of the tags. If not specified, the characters are converted
     * to lowercase.
     *
     * @return the current {@code FieldOptions}
     */
    public FieldOptions caseSensitive() {
        this.caseSensitive = true;
        return this;
    }

    /**
     * For TEXT and TAG attributes, keeps a suffix trie with all terms which match the suffix. It is used to optimize
     * contains (foo) and suffix (*foo) queries. Otherwise, a brute-force search on the trie is performed.
     * If suffix trie exists for some fields, these queries will be disabled for other fields.
     *
     * @return the current {@code FieldOptions}
     */
    public FieldOptions withSuffixTrie() {
        this.withSuffixTrie = true;
        return this;
    }

    public List<String> toArgs() {
        List<String> list = new ArrayList<>();
        if (sortable) {
            list.add("SORTABLE");
        }
        if (unf) {
            if (!sortable) {
                throw new IllegalArgumentException("Using `UNF` requires `SORTABLE`");
            }
            list.add("UNF");
        }
        if (noStem) {
            list.add("NOSTEM");
        }
        if (noIndex) {
            list.add("NOINDEX");
        }
        if (phonetic != null) {
            list.add("PHONETIC");
            list.add(phonetic);
        }
        if (weight != -1) {
            list.add("WEIGHT");
            list.add(Double.toString(weight));
        }
        if (separator != 0) {
            list.add("SEPARATOR");
            list.add(Character.toString(separator));
        }
        if (caseSensitive) {
            list.add("CASESENSITIVE");
        }
        if (withSuffixTrie) {
            list.add("WITHSUFFIXTRIE");
        }
        return list;
    }
}
