package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.test.QuarkusUnitTest;

public class CollectionTemplateExtensionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset(
                            "{@java.util.List<Boolean> list}{list.0.booleanValue}={list[0]}"),
                            "templates/getByIndex.html")
                    .addAsResource(new StringAsset(
                            "{@java.util.List<String> list}{#each list.reversed}{it.toUpperCase}::{/each}"),
                            "templates/reversed.html")
                    .addAsResource(new StringAsset(
                            "{@java.util.List<String> list}{#each list.take(2)}{it}::{/each}"),
                            "templates/take.html")
                    .addAsResource(new StringAsset(
                            "{@java.util.List<String> list}{#each list.takeLast(2)}{it}::{/each}"),
                            "templates/takeLast.html")
                    .addAsResource(new StringAsset(
                            "{@java.util.List<String> list}{list.first.toUpperCase}"),
                            "templates/first.html")
                    .addAsResource(new StringAsset(
                            "{@java.util.List<String> list}{list.last.toUpperCase}"),
                            "templates/last.html")

            );

    @Inject
    Engine engine;

    @Test
    public void testListGetByIndex() {
        assertEquals("true=true",
                engine.getTemplate("getByIndex").data("list", Collections.singletonList(true)).render());
    }

    @Test
    public void testListReversed() {
        assertEquals("CHARLIE::BRAVO::ALPHA::", engine.getTemplate("reversed").data("list", listOfNames()).render());
    }

    @Test
    public void testTake() {
        assertEquals("alpha::bravo::", engine.getTemplate("take").data("list", listOfNames()).render());
    }

    @Test
    public void testTakeLast() {
        assertEquals("bravo::charlie::", engine.getTemplate("takeLast").data("list", listOfNames()).render());
    }

    @Test
    public void testFirst() {
        assertEquals("ALPHA", engine.getTemplate("first").data("list", listOfNames()).render());
    }

    @Test
    public void testLast() {
        assertEquals("CHARLIE", engine.getTemplate("last").data("list", listOfNames()).render());
    }

    private List<String> listOfNames() {
        return List.of("alpha", "bravo", "charlie");
    }

}
