package io.quarkus.arc.processor;

import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BeanArchivesTest {

    @Test
    public void testIndex() {
        final Indexer indexer = new Indexer();
        Assertions.assertFalse(BeanArchives.index(indexer, "String"));
        Assertions.assertTrue(BeanArchives.index(indexer, String.class.getName()));
    }

}
