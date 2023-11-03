package io.quarkus.redis.datasource.search;

public enum FieldType {

    /**
     * TEXT allows full-text search queries against the value in this attribute.
     */
    TEXT,
    /**
     * TAG allows exact-match queries, such as categories or primary keys, against the value in this attribute.
     */
    TAG,
    /**
     * GEO allows geographic range queries against the value in this attribute. The value of the attribute must be a
     * string containing a longitude (first) and latitude separated by a comma.
     */
    GEO,
    /**
     * VECTOR allows vector similarity queries against the value in this attribute.
     */
    VECTOR,
    /**
     * NUMERIC allows numeric range queries against the value in this attribute.
     */
    NUMERIC
}
