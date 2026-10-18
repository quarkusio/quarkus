package io.quarkus.bootstrap.resolver.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;

public class OrderedDependencyVisitorTest {

    private static final String ORG_ACME = "org.acme";
    private static final String JAR = "jar";
    private static final String VERSION = "1.0";

    @Test
    public void main() {

        var root = newNode("root");

        // direct dependencies
        var colors = newNode("colors");
        var pets = newNode("pets");
        var trees = newNode("trees");
        root.setChildren(List.of(colors, pets, trees));

        // colors
        var red = newNode("red");
        var green = newNode("green");
        var blue = newNode("blue");
        colors.setChildren(List.of(red, green, blue));

        // pets
        var dog = newNode("dog");
        var cat = newNode("cat");
        pets.setChildren(List.of(dog, cat));
        // pets, puppy
        var puppy = newNode("puppy");
        dog.setChildren(List.of(puppy));

        // trees
        var pine = newNode("pine");
        trees.setChildren(Arrays.asList(pine)); // List.of() can't be used for replace
        var oak = newNode("oak");
        // oak, acorn
        var acorn = newNode("acorn");
        oak.setChildren(List.of(acorn));

        // create a visitor
        var visitor = new OrderedDependencyVisitor(root);

        // assertions
        assertThat(visitor.hasNext()).isTrue();

        // distance 0
        assertThat(visitor.next()).isSameAs(root);
        assertThat(visitor.getCurrent()).isSameAs(root);
        assertThat(visitor.getCurrentDistance()).isEqualTo(0);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(0);
        assertThat(visitor.hasNext()).isTrue();

        // distance 1, colors
        assertThat(visitor.next()).isSameAs(colors);
        assertThat(visitor.getCurrent()).isSameAs(colors);
        assertThat(visitor.getCurrentDistance()).isEqualTo(1);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(1);
        assertThat(visitor.hasNext()).isTrue();

        // distance 1, pets
        assertThat(visitor.next()).isSameAs(pets);
        assertThat(visitor.getCurrent()).isSameAs(pets);
        assertThat(visitor.getCurrentDistance()).isEqualTo(1);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(2);
        assertThat(visitor.hasNext()).isTrue();

        // distance 1, trees
        assertThat(visitor.next()).isSameAs(trees);
        assertThat(visitor.getCurrent()).isSameAs(trees);
        assertThat(visitor.getCurrentDistance()).isEqualTo(1);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(3);
        assertThat(visitor.hasNext()).isTrue();

        // distance 2, colors, red
        assertThat(visitor.next()).isSameAs(red);
        assertThat(visitor.getCurrent()).isSameAs(red);
        assertThat(visitor.getCurrentDistance()).isEqualTo(2);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(1);
        assertThat(visitor.hasNext()).isTrue();

        // distance 2, colors, green
        assertThat(visitor.next()).isSameAs(green);
        assertThat(visitor.getCurrent()).isSameAs(green);
        assertThat(visitor.getCurrentDistance()).isEqualTo(2);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(1);
        assertThat(visitor.hasNext()).isTrue();

        // distance 2, colors, blue
        assertThat(visitor.next()).isSameAs(blue);
        assertThat(visitor.getCurrent()).isSameAs(blue);
        assertThat(visitor.getCurrentDistance()).isEqualTo(2);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(1);
        assertThat(visitor.hasNext()).isTrue();

        // distance 2, pets, dog
        assertThat(visitor.next()).isSameAs(dog);
        assertThat(visitor.getCurrent()).isSameAs(dog);
        assertThat(visitor.getCurrentDistance()).isEqualTo(2);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(2);
        assertThat(visitor.hasNext()).isTrue();

        // distance 2, pets, cat
        assertThat(visitor.next()).isSameAs(cat);
        assertThat(visitor.getCurrent()).isSameAs(cat);
        assertThat(visitor.getCurrentDistance()).isEqualTo(2);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(2);
        assertThat(visitor.hasNext()).isTrue();

        // distance 2, trees, pine
        assertThat(visitor.next()).isSameAs(pine);
        assertThat(visitor.getCurrent()).isSameAs(pine);
        assertThat(visitor.getCurrentDistance()).isEqualTo(2);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(3);
        assertThat(visitor.hasNext()).isTrue();
        // replace the current node
        visitor.replaceCurrent(oak);
        assertThat(visitor.getCurrent()).isSameAs(oak);
        assertThat(visitor.getCurrentDistance()).isEqualTo(2);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(3);
        assertThat(visitor.hasNext()).isTrue();

        // distance 3, pets, dog, puppy
        assertThat(visitor.next()).isSameAs(puppy);
        assertThat(visitor.getCurrent()).isSameAs(puppy);
        assertThat(visitor.getCurrentDistance()).isEqualTo(3);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(2);
        assertThat(visitor.hasNext()).isTrue();

        // distance 3, trees, oak, acorn
        assertThat(visitor.next()).isSameAs(acorn);
        assertThat(visitor.getCurrent()).isSameAs(acorn);
        assertThat(visitor.getCurrentDistance()).isEqualTo(3);
        assertThat(visitor.getSubtreeIndex()).isEqualTo(3);
        assertThat(visitor.hasNext()).isFalse();
    }

    private static DependencyNode newNode(String artifactId) {
        return new DefaultDependencyNode(new DefaultArtifact(ORG_ACME, artifactId, JAR, VERSION));
    }
}
