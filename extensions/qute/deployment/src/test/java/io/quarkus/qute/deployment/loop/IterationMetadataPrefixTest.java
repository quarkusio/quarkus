package io.quarkus.qute.deployment.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.Template;

public abstract class IterationMetadataPrefixTest {

    @Inject
    Template loop;

    @Test
    public void testPrefix() {
        assertEquals("1:1::2:2::3:3", loop.data("total", 3).render());
    }

}
