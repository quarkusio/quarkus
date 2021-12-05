package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.test.QuarkusUnitTest;

public class CollectionTemplateExtensionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Inject
    Engine engine;

    @Test
    public void testListGetByIndex() {
        assertEquals("true=true=NOT_FOUND",
                engine.parse("{@java.util.List<Boolean> list}{list.0.booleanValue}={list[0]}={list[100] ?: 'NOT_FOUND'}")
                        .data("list", Collections.singletonList(true)).render());
    }

    @Test
    public void testListReversed() {
        List<String> names = new ArrayList<>();
        names.add("alpha");
        names.add("bravo");
        names.add("charlie");
        assertEquals("CHARLIE::BRAVO::ALPHA::",
                engine.parse("{@java.util.List<String> list}{#each list.reversed}{it.toUpperCase}::{/each}").data("list", names)
                        .render());
    }

    @Test
    public void testTake() {
        List<String> names = new ArrayList<>();
        names.add("alpha");
        names.add("bravo");
        names.add("charlie");
        assertEquals(
                "alpha::bravo::",
                engine.parse("{@java.util.List<String> list}{#each list.take(2)}{it}::{/each}").data("list", names)
                        .render());
    }

    @Test
    public void testTakeLast() {
        List<String> names = new ArrayList<>();
        names.add("alpha");
        names.add("bravo");
        names.add("charlie");
        assertEquals(
                "bravo::charlie::",
                engine.parse("{@java.util.List<String> list}{#each list.takeLast(2)}{it}::{/each}").data("list", names)
                        .render());
    }

}
