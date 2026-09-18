package io.quarkus.flyway.mongodb.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the connection-string splicing helpers in
 * {@link FlywayMongodbContainerProducer}. These helpers do not require a
 * running MongoDB or Quarkus container.
 */
class FlywayMongodbConnectionStringHelpersTest {

    // --- appendDatabase ---

    @Test
    void appendDatabaseToHostOnly() {
        assertThat(FlywayMongodbContainerProducer.appendDatabase("mongodb://host:27017", "appdb"))
                .isEqualTo("mongodb://host:27017/appdb");
    }

    @Test
    void appendDatabaseToHostWithTrailingSlash() {
        assertThat(FlywayMongodbContainerProducer.appendDatabase("mongodb://host:27017/", "appdb"))
                .isEqualTo("mongodb://host:27017/appdb");
    }

    @Test
    void appendDatabaseWithExistingQueryStringAndNoPath() {
        assertThat(FlywayMongodbContainerProducer.appendDatabase("mongodb://host:27017?retryWrites=true", "appdb"))
                .isEqualTo("mongodb://host:27017/appdb?retryWrites=true");
    }

    @Test
    void appendDatabaseWithTrailingSlashAndQueryString() {
        assertThat(FlywayMongodbContainerProducer.appendDatabase("mongodb://host:27017/?retryWrites=true", "appdb"))
                .isEqualTo("mongodb://host:27017/appdb?retryWrites=true");
    }

    @Test
    void appendDatabaseToSrvScheme() {
        assertThat(FlywayMongodbContainerProducer.appendDatabase("mongodb+srv://cluster.example.net", "appdb"))
                .isEqualTo("mongodb+srv://cluster.example.net/appdb");
    }

    @Test
    void appendDatabaseEncodesSpecialCharacters() {
        // database names with reserved URI characters get percent-encoded
        assertThat(FlywayMongodbContainerProducer.appendDatabase("mongodb://host:27017", "with space"))
                .isEqualTo("mongodb://host:27017/with%20space");
    }

    // --- hasPathSegment ---

    @Test
    void hasPathSegmentRecognisesTrailingSlashOnly() {
        assertThat(FlywayMongodbContainerProducer.hasPathSegment("mongodb://host:27017/")).isTrue();
    }

    @Test
    void hasPathSegmentRecognisesDatabasePath() {
        assertThat(FlywayMongodbContainerProducer.hasPathSegment("mongodb://host:27017/dbname")).isTrue();
    }

    @Test
    void hasPathSegmentReturnsFalseForHostOnly() {
        assertThat(FlywayMongodbContainerProducer.hasPathSegment("mongodb://host:27017")).isFalse();
    }

    @Test
    void hasPathSegmentReturnsFalseForSrvHostOnly() {
        assertThat(FlywayMongodbContainerProducer.hasPathSegment("mongodb+srv://cluster.example.net")).isFalse();
    }

    // --- appendQueryParam ---

    @Test
    void appendQueryParamToHostOnly() {
        assertThat(FlywayMongodbContainerProducer.appendQueryParam("mongodb://host:27017", "authSource", "admin"))
                .isEqualTo("mongodb://host:27017/?authSource=admin");
    }

    @Test
    void appendQueryParamToHostWithDatabase() {
        assertThat(FlywayMongodbContainerProducer.appendQueryParam("mongodb://host:27017/appdb", "authSource", "admin"))
                .isEqualTo("mongodb://host:27017/appdb?authSource=admin");
    }

    @Test
    void appendQueryParamToUrlWithExistingQueryString() {
        assertThat(FlywayMongodbContainerProducer.appendQueryParam(
                "mongodb://host:27017/appdb?retryWrites=true", "authSource", "admin"))
                .isEqualTo("mongodb://host:27017/appdb?retryWrites=true&authSource=admin");
    }

    @Test
    void appendQueryParamPercentEncodesSpaces() {
        // MongoDB connection strings follow RFC 3986: space -> %20, not the form-encoded '+'.
        assertThat(FlywayMongodbContainerProducer.appendQueryParam(
                "mongodb://host:27017/appdb", "authSource", "auth db"))
                .isEqualTo("mongodb://host:27017/appdb?authSource=auth%20db");
    }

    @Test
    void appendQueryParamPercentEncodesReservedCharacters() {
        // Reserved sub-delimiters and gen-delimiters still use percent-encoding.
        assertThat(FlywayMongodbContainerProducer.appendQueryParam(
                "mongodb://host:27017/appdb", "authSource", "a&b=c"))
                .isEqualTo("mongodb://host:27017/appdb?authSource=a%26b%3Dc");
    }

    // --- hasQueryParam ---

    @Test
    void hasQueryParamFindsExistingAuthSource() {
        assertThat(FlywayMongodbContainerProducer.hasQueryParam(
                "mongodb://host/db?authSource=admin", "authSource"))
                .isTrue();
    }

    @Test
    void hasQueryParamFindsExistingAuthSourceAmongOtherOptions() {
        assertThat(FlywayMongodbContainerProducer.hasQueryParam(
                "mongodb://host/db?retryWrites=true&authSource=admin&w=majority", "authSource"))
                .isTrue();
    }

    @Test
    void hasQueryParamIsCaseInsensitive() {
        assertThat(FlywayMongodbContainerProducer.hasQueryParam(
                "mongodb://host/db?AUTHSOURCE=admin", "authSource"))
                .isTrue();
    }

    @Test
    void hasQueryParamReturnsFalseWhenAbsent() {
        assertThat(FlywayMongodbContainerProducer.hasQueryParam(
                "mongodb://host/db?retryWrites=true", "authSource"))
                .isFalse();
    }

    @Test
    void hasQueryParamReturnsFalseWhenNoQueryString() {
        assertThat(FlywayMongodbContainerProducer.hasQueryParam(
                "mongodb://host/db", "authSource"))
                .isFalse();
    }

    @Test
    void hasQueryParamDoesNotMatchPrefixOnly() {
        // "authSourceAlt" must not be misread as "authSource"
        assertThat(FlywayMongodbContainerProducer.hasQueryParam(
                "mongodb://host/db?authSourceAlt=foo", "authSource"))
                .isFalse();
    }
}
