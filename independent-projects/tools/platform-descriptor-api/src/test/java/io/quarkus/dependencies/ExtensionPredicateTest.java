package io.quarkus.dependencies;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExtensionPredicateTest {

    @Test
    void rejectUnlisted() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        Extension extension = new Extension("g", "a", "v");
        extension.setUnlisted(true);
        assertThat(predicate).rejects(extension);
    }

    @Test
    void acceptKeywordInArtifactId() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        Extension extension = new Extension("g", "foo-bar", "1.0");
        assertThat(predicate).accepts(extension);
    }

    @Test
    void acceptKeywordInLabel() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        Extension extension = new Extension("g", "a", "1.0");
        extension.setKeywords(new String[] { "foo", "bar" });
        assertThat(predicate).accepts(extension);
    }

}
