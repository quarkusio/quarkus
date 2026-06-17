package io.quarkus.signals.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TopologicalSortTest {

    private static final String TYPE = "TestComponent";

    @Test
    public void testNoEdges() {
        List<String> result = TopologicalSort.sort(
                Set.of("a", "b", "c"),
                Map.of(),
                Map.of(),
                TYPE);
        assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    public void testSingleBeforeEdge() {
        List<String> result = TopologicalSort.sort(
                Set.of("a", "b"),
                Map.of("b", List.of("a")),
                Map.of(),
                TYPE);
        assertEquals(List.of("b", "a"), result);
    }

    @Test
    public void testSingleAfterEdge() {
        List<String> result = TopologicalSort.sort(
                Set.of("a", "b"),
                Map.of(),
                Map.of("a", List.of("b")),
                TYPE);
        assertEquals(List.of("b", "a"), result);
    }

    @Test
    public void testChain() {
        List<String> result = TopologicalSort.sort(
                Set.of("a", "b", "c"),
                Map.of("a", List.of("b"), "b", List.of("c")),
                Map.of(),
                TYPE);
        assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    public void testMixedBeforeAndAfter() {
        List<String> result = TopologicalSort.sort(
                Set.of("first", "middle", "last"),
                Map.of("first", List.of("last"), "middle", List.of("last")),
                Map.of("middle", List.of("first")),
                TYPE);
        assertEquals(List.of("first", "middle", "last"), result);
    }

    @Test
    public void testCycleDetected() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            TopologicalSort.sort(
                    Set.of("a", "b"),
                    Map.of("a", List.of("b"), "b", List.of("a")),
                    Map.of(),
                    TYPE);
        });
        assertTrue(e.getMessage().contains("Cycle detected"));
    }

    @Test
    public void testCycleInThreeNodes() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            TopologicalSort.sort(
                    Set.of("a", "b", "c"),
                    Map.of("a", List.of("b"), "b", List.of("c"), "c", List.of("a")),
                    Map.of(),
                    TYPE);
        });
        assertTrue(e.getMessage().contains("Cycle detected"));
    }

    @Test
    public void testBeforeNonExistentId() {
        List<String> result = TopologicalSort.sort(
                Set.of("a"),
                Map.of("a", List.of("nonexistent")),
                Map.of(),
                TYPE);
        assertEquals(List.of("a"), result);
    }

    @Test
    public void testAfterNonExistentId() {
        List<String> result = TopologicalSort.sort(
                Set.of("a"),
                Map.of(),
                Map.of("a", List.of("nonexistent")),
                TYPE);
        assertEquals(List.of("a"), result);
    }

    @Test
    public void testSingleNode() {
        List<String> result = TopologicalSort.sort(
                Set.of("a"),
                Map.of(),
                Map.of(),
                TYPE);
        assertEquals(List.of("a"), result);
    }

    @Test
    public void testEmptySet() {
        List<String> result = TopologicalSort.sort(
                Set.of(),
                Map.of(),
                Map.of(),
                TYPE);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDiamondShape() {
        List<String> result = TopologicalSort.sort(
                Set.of("b", "a", "c", "d"),
                Map.of("a", List.of("b", "c"), "b", List.of("d"), "c", List.of("d")),
                Map.of(),
                TYPE);
        assertEquals(List.of("a", "b", "c", "d"), result);
    }
}
