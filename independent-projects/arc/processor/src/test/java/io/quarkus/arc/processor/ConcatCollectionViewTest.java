package io.quarkus.arc.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class ConcatCollectionViewTest {

    @Test
    public void testSizeAndContents() {
        List<String> first = List.of("a", "b");
        List<String> second = List.of("c", "d", "e");
        ConcatCollectionView<String> view = new ConcatCollectionView<>(first, second);

        assertThat(view).hasSize(5).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    public void testEmptyCollections() {
        ConcatCollectionView<String> bothEmpty = new ConcatCollectionView<>(List.of(), List.of());
        assertThat(bothEmpty).isEmpty();

        ConcatCollectionView<String> firstEmpty = new ConcatCollectionView<>(List.of(), List.of("x"));
        assertThat(firstEmpty).hasSize(1).containsExactly("x");

        ConcatCollectionView<String> secondEmpty = new ConcatCollectionView<>(List.of("x"), List.of());
        assertThat(secondEmpty).hasSize(1).containsExactly("x");
    }

    @Test
    public void testContains() {
        ConcatCollectionView<String> view = new ConcatCollectionView<>(List.of("a"), List.of("b"));

        assertThat(view.contains("a")).isTrue();
        assertThat(view.contains("b")).isTrue();
        assertThat(view.contains("c")).isFalse();
    }

    @Test
    public void testLiveView() {
        List<String> first = new ArrayList<>(List.of("a"));
        List<String> second = new ArrayList<>();
        ConcatCollectionView<String> view = new ConcatCollectionView<>(first, second);

        assertThat(view).hasSize(1).containsExactly("a");

        second.add("b");
        assertThat(view).hasSize(2).containsExactly("a", "b");

        first.add("a2");
        assertThat(view).hasSize(3).containsExactly("a", "a2", "b");

        first.clear();
        assertThat(view).hasSize(1).containsExactly("b");
    }

    @Test
    public void testIteratorExhausted() {
        ConcatCollectionView<String> view = new ConcatCollectionView<>(List.of("a"), List.of());
        Iterator<String> it = view.iterator();
        assertThat(it.next()).isEqualTo("a");
        assertThat(it.hasNext()).isFalse();
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
    }

}
