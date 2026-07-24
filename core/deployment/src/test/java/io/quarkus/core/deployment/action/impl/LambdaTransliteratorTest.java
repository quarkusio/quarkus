package io.quarkus.core.deployment.action.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.core.StartContext;
import io.quarkus.core.deployment.action.Action1;
import io.quarkus.core.deployment.action.Action2;
import io.quarkus.core.deployment.action.impl.LambdaTransliterator.TransliterationException;
import io.quarkus.core.impl.ServiceGraph;
import io.quarkus.core.impl.ServiceNode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.StartupContext;

/**
 * Tests for {@link LambdaTransliterator}.
 * <p>
 * Each test extracts a lambda via {@link LambdaTransliterator#extract} and then
 * generates a consolidated class via {@link LambdaTransliterator#generateConsolidatedClass}.
 * The consolidated class is loaded and deployed via its static {@code deploy$0} method.
 */
class LambdaTransliteratorTest {

    private static final String CONSOLIDATED_CLASS_NAME = "io/quarkus/generated/service/TestConsolidated";

    @Test
    void simpleNoArgLambdaTransliterates() throws Exception {
        Action1<String, StartContext> action = (StartContext ctx) -> "hello";

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        assertThat(extracted.serviceKey()).isEqualTo("java.lang.String:");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo("hello");

    }

    @Test
    void namedServiceStoresUnderCorrectKey() throws Exception {
        Action1<Integer, StartContext> action = (StartContext ctx) -> 42;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Integer.class, List.of("answer"), List.of(), List.of(), List.of(), false, false, "test");

        assertThat(extracted.serviceKey()).isEqualTo("java.lang.Integer:answer");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo(42);

    }

    @Test
    void lambdaWithDependencyReadsDependencyFromContext() throws Exception {
        Action2<String, StartContext, Integer> action = (StartContext ctx,
                Integer dep) -> "value-" + dep;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(),
                List.of(new Dependency(Integer.class, List.of(), Dependency.FL_INJECTED)),
                List.of(), List.of(), false, false, "test");

        ServiceGraph graph = new ServiceGraph();
        ServiceNode depNode = completedNode(graph, 99);
        ServiceNode node = deployAndReturn(graph, extracted, depNode);
        assertThat(node.value()).isEqualTo("value-99");

    }

    @Test
    void lambdaCapturingConstantValueTransliterates() throws Exception {
        String captured = "world";
        Action1<String, StartContext> action = (StartContext ctx) -> "hello " + captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo("hello world");

    }

    @Test
    void instanceCapturingLambdaRejected() {
        final LambdaTransliteratorTest self = this;
        Action1<String, StartContext> action = (StartContext ctx) -> self.toString();

        assertThatThrownBy(() -> LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test"))
                .isInstanceOf(TransliterationException.class);
    }

    @Test
    void extractIsDeterministic() {
        Action1<String, StartContext> action = (StartContext ctx) -> "a";

        TransliteratedAction.ActionService r1 = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");
        TransliteratedAction.ActionService r2 = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        assertThat(r1.serviceKey()).isEqualTo(r2.serviceKey());
    }

    @Test
    void nestedLambdaTransliterates() throws Exception {
        Action1<String, StartContext> action = (StartContext ctx) -> {
            List<String> items = List.of("a", "b", "c");
            return items.stream().map(String::toUpperCase).collect(Collectors.joining(","));
        };

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo("A,B,C");

    }

    @Test
    void nestedLambdaCapturingOuterLocal() throws Exception {
        Action1<String, StartContext> action = (StartContext ctx) -> {
            String prefix = "X-";
            List<String> items = List.of("a", "b");
            return items.stream().map(s -> prefix + s).collect(Collectors.joining(","));
        };

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo("X-a,X-b");

    }

    @Test
    void nestedLambdaCapturingOuterCaptureAndLocal() throws Exception {
        // reproduces the pattern from CertificatesProcessor: outer lambda captures a Set,
        // creates a local, and a nested lambda captures both the Set and the local
        Set<String> captured = Set.of("x", "y");
        Action1<String, StartContext> action = (StartContext ctx) -> {
            StringBuilder sb = new StringBuilder();
            captured.forEach(sb::append);
            return sb.toString();
        };

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        String result = (String) node.value();
        assertThat(result).contains("x").contains("y");

    }

    @Test
    void deeplyNestedLambda() throws Exception {
        Action1<String, StartContext> action = (StartContext ctx) -> {
            List<List<String>> nested = List.of(List.of("a", "b"), List.of("c"));
            return nested.stream()
                    .map(list -> list.stream().map(String::toUpperCase).collect(Collectors.joining()))
                    .collect(Collectors.joining("-"));
        };

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo("AB-C");

    }

    @Test
    void methodReferenceToRuntimeClassPassesThrough() throws Exception {
        Action1<String, StartContext> action = (StartContext ctx) -> {
            List<String> items = List.of("a", "b", "c");
            return items.stream().map(String::toUpperCase).collect(Collectors.joining(","));
        };

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo("A,B,C");

    }

    @Test
    void multipleNestedLambdasInSameBody() throws Exception {
        Action1<String, StartContext> action = (StartContext ctx) -> {
            List<String> items = List.of("a", "b", "c");
            String upper = items.stream().map(String::toUpperCase).collect(Collectors.joining(","));
            String lengths = items.stream().map(s -> String.valueOf(s.length())).collect(Collectors.joining(","));
            return upper + "|" + lengths;
        };

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo("A,B,C|1,1,1");

    }

    // ── Static-init tests ──

    @Test
    void staticInitSimpleTransliterates() throws Exception {
        Action1<String, StartContext> action = (StartContext ctx) -> "static-hello";

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, true, "test");

        assertThat(extracted.staticInit()).isTrue();

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo("static-hello");

    }

    @Test
    void staticInitWithDependencyTransliterates() throws Exception {
        Action2<String, StartContext, Integer> action = (StartContext ctx,
                Integer dep) -> "static-" + dep;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(),
                List.of(new Dependency(Integer.class, List.of(), Dependency.FL_INJECTED)),
                List.of(), List.of(), false, true, "test");

        ServiceGraph graph = new ServiceGraph();
        ServiceNode depNode = completedNode(graph, 77);
        ServiceNode node = deployAndReturn(graph, extracted, depNode);
        assertThat(node.value()).isEqualTo("static-77");

    }

    @Test
    void staticInitRejectsAsync() {
        Action1<String, StartContext> action = (StartContext ctx) -> "nope";

        assertThatThrownBy(() -> LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), true, true, "test"))
                .isInstanceOf(TransliterationException.class)
                .hasMessageContaining("async");
    }

    @Test
    void staticInitNestedLambda() throws Exception {
        Action1<String, StartContext> action = (StartContext ctx) -> {
            List<String> items = List.of("x", "y", "z");
            return items.stream().map(String::toUpperCase).collect(Collectors.joining(","));
        };

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, true, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo("X,Y,Z");

    }

    // ── Consolidation tests ──

    @Test
    void multipleActionsConsolidateIntoSingleClass() throws Exception {
        Action1<String, StartContext> action1 = (StartContext ctx) -> "first";
        Action1<Integer, StartContext> action2 = (StartContext ctx) -> 42;

        TransliteratedAction.ActionService extracted1 = LambdaTransliterator.extract(
                action1, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");
        TransliteratedAction.ActionService extracted2 = LambdaTransliterator.extract(
                action2, Integer.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        Map<String, byte[]> classes = LambdaTransliterator.generateConsolidatedClass(
                CONSOLIDATED_CLASS_NAME, List.of(extracted1, extracted2));
        String className = CONSOLIDATED_CLASS_NAME.replace('/', '.');

        ClassLoader cl = makeClassLoader(classes);
        Class<?> clazz = cl.loadClass(className);

        // deploy$0 should produce "first"
        ServiceGraph graph1 = new ServiceGraph();
        ServiceNode node1 = runDeploy(graph1, clazz, 0);
        assertThat(node1.value()).isEqualTo("first");

        // deploy$1 should produce 42
        ServiceGraph graph2 = new ServiceGraph();
        ServiceNode node2 = runDeploy(graph2, clazz, 1);
        assertThat(node2.value()).isEqualTo(42);
    }

    @Test
    void consolidatedClassWithNestedLambdasDoNotCollide() throws Exception {
        // both actions use nested lambdas; inner method names must not collide
        Action1<String, StartContext> action1 = (StartContext ctx) -> {
            List<String> items = List.of("a", "b");
            return items.stream().map(String::toUpperCase).collect(Collectors.joining(","));
        };
        Action1<String, StartContext> action2 = (StartContext ctx) -> {
            List<String> items = List.of("x", "y");
            return items.stream().map(String::toLowerCase).collect(Collectors.joining("-"));
        };

        TransliteratedAction.ActionService extracted1 = LambdaTransliterator.extract(
                action1, String.class, List.of("svc1"), List.of(), List.of(), List.of(), false, false, "test");
        TransliteratedAction.ActionService extracted2 = LambdaTransliterator.extract(
                action2, String.class, List.of("svc2"), List.of(), List.of(), List.of(), false, false, "test");

        Map<String, byte[]> classes = LambdaTransliterator.generateConsolidatedClass(
                CONSOLIDATED_CLASS_NAME, List.of(extracted1, extracted2));
        String className = CONSOLIDATED_CLASS_NAME.replace('/', '.');

        ClassLoader cl = makeClassLoader(classes);
        Class<?> clazz = cl.loadClass(className);

        ServiceGraph graph1 = new ServiceGraph();
        ServiceNode node1 = runDeploy(graph1, clazz, 0);
        assertThat(node1.value()).isEqualTo("A,B");

        ServiceGraph graph2 = new ServiceGraph();
        ServiceNode node2 = runDeploy(graph2, clazz, 1);
        assertThat(node2.value()).isEqualTo("x-y");
    }

    @Test
    void aliasServiceConsolidates() throws Exception {
        Action1<String, StartContext> action = (StartContext ctx) -> "original";

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        // alias copies a recorder proxy value (from the values map) to a service key
        // simulate a recorder having stored a value under a proxy key
        String recorderProxyKey = "recorder-proxy-key";
        TransliteratedAction.AliasService alias = new TransliteratedAction.AliasService(
                "java.lang.String:alias", false, recorderProxyKey, "test");

        Map<String, byte[]> classes = LambdaTransliterator.generateConsolidatedClass(
                CONSOLIDATED_CLASS_NAME, List.of(extracted, alias));
        String className = CONSOLIDATED_CLASS_NAME.replace('/', '.');

        ClassLoader cl = makeClassLoader(classes);
        Class<?> clazz = cl.loadClass(className);

        // deploy$0 produces "original"
        ServiceGraph graph = new ServiceGraph();
        ServiceNode sourceNode = runDeploy(graph, clazz, 0);
        assertThat(sourceNode.value()).isEqualTo("original");

        // simulate a recorder storing the value under its proxy key
        graph.startupContext().putValue(recorderProxyKey, "original");

        // deploy$1 reads from the recorder values map and produces the alias value
        ServiceNode aliasNode = runDeploy(graph, clazz, 1);
        assertThat(aliasNode.value()).isEqualTo("original");
    }

    @Test
    void runtimeValueWrapperConsolidates() throws Exception {
        Action1<String, StartContext> action = (StartContext ctx) -> "wrapped-value";

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, String.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        // wrapper reads from the service key and stores a RuntimeValue at the rv key
        TransliteratedAction.RuntimeValueWrapper wrapper = new TransliteratedAction.RuntimeValueWrapper(
                "java.lang.String:", false, "runtimevalue:java.lang.String:", "test");

        Map<String, byte[]> classes = LambdaTransliterator.generateConsolidatedClass(
                CONSOLIDATED_CLASS_NAME, List.of(extracted, wrapper));
        String className = CONSOLIDATED_CLASS_NAME.replace('/', '.');

        ClassLoader cl = makeClassLoader(classes);
        Class<?> clazz = cl.loadClass(className);

        ServiceGraph graph = new ServiceGraph();
        // deploy$0 produces the bare value
        ServiceNode sourceNode = runDeploy(graph, clazz, 0);
        assertThat(sourceNode.value()).isEqualTo("wrapped-value");

        // deploy$1 wraps it in RuntimeValue via indexed dependency access (dep 0 = source)
        ServiceNode wrapperNode = runDeploy(graph, clazz, 1, sourceNode);
        assertThat(wrapperNode.value()).isInstanceOf(RuntimeValue.class);
        @SuppressWarnings("unchecked")
        RuntimeValue<String> rv = (RuntimeValue<String>) wrapperNode.value();
        assertThat(rv.getValue()).isEqualTo("wrapped-value");
    }

    // ── Anonymous inner class tests ──

    /**
     * Abstract generic class used to verify that anonymous inner class
     * transliteration preserves generic type information (like {@code TypeLiteral}).
     *
     * @param <T> the carried type
     */
    public static abstract class TypeCarrier<T> {
        /**
         * Retrieve the actual type argument from the generic superclass.
         *
         * @return the type argument
         */
        public Type getType() {
            return ((ParameterizedType) getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0];
        }
    }

    // Anonymous class actions must be created in static methods to avoid capturing
    // `this` — javac treats anonymous classes in instance context as inner classes
    // with an implicit enclosing instance parameter.

    private static Action1<Object, StartContext> typeCarrierStringAction() {
        return (StartContext ctx) -> new TypeCarrier<String>() {
        };
    }

    private static Action1<Object, StartContext> nestedLambdaTypeCarrierAction() {
        return (StartContext ctx) -> {
            List<String> items = List.of("a");
            return items.stream().map(s -> new TypeCarrier<Integer>() {
            }).findFirst().orElseThrow();
        };
    }

    @Test
    void anonymousInnerClassTransliterates() throws Exception {
        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                typeCarrierStringAction(), Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        Map<String, byte[]> classes = LambdaTransliterator.generateConsolidatedClass(
                CONSOLIDATED_CLASS_NAME, List.of(extracted));

        // at least the consolidated class + one anonymous class copy
        assertThat(classes.size()).isGreaterThanOrEqualTo(2);

        ClassLoader cl = makeClassLoader(classes);
        String className = CONSOLIDATED_CLASS_NAME.replace('/', '.');
        Class<?> clazz = cl.loadClass(className);

        ServiceGraph graph = new ServiceGraph();
        ServiceNode node = runDeploy(graph, clazz, 0);

        assertThat(node.value()).isInstanceOf(TypeCarrier.class);
        @SuppressWarnings("unchecked")
        TypeCarrier<String> carrier = (TypeCarrier<String>) node.value();
        assertThat(carrier.getType()).isEqualTo(String.class);
    }

    @Test
    void anonymousInnerClassInNestedLambda() throws Exception {
        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                nestedLambdaTypeCarrierAction(), Object.class, List.of(), List.of(), List.of(), List.of(), false, false,
                "test");

        Map<String, byte[]> classes = LambdaTransliterator.generateConsolidatedClass(
                CONSOLIDATED_CLASS_NAME, List.of(extracted));

        assertThat(classes.size()).isGreaterThanOrEqualTo(2);

        ClassLoader cl = makeClassLoader(classes);
        String className = CONSOLIDATED_CLASS_NAME.replace('/', '.');
        Class<?> clazz = cl.loadClass(className);

        ServiceGraph graph = new ServiceGraph();
        ServiceNode node = runDeploy(graph, clazz, 0);

        assertThat(node.value()).isInstanceOf(TypeCarrier.class);
        @SuppressWarnings("unchecked")
        TypeCarrier<Integer> carrier = (TypeCarrier<Integer>) node.value();
        assertThat(carrier.getType()).isEqualTo(Integer.class);
    }

    // ── Collection capture tests ──

    @Test
    void capturedImmutableListTransliterates() throws Exception {
        List<String> captured = List.of("a", "b", "c");
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo(List.of("a", "b", "c"));

    }

    @Test
    void capturedImmutableSetTransliterates() throws Exception {
        Set<Integer> captured = Set.of(1, 2, 3);
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        @SuppressWarnings("unchecked")
        Set<Integer> result = (Set<Integer>) node.value();
        assertThat(result).containsExactlyInAnyOrder(1, 2, 3);

    }

    @Test
    void capturedImmutableMapTransliterates() throws Exception {
        Map<String, String> captured = Map.of("k1", "v1", "k2", "v2");
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) node.value();
        assertThat(result).containsEntry("k1", "v1").containsEntry("k2", "v2").hasSize(2);

    }

    @Test
    void capturedEmptyCollectionsTransliterate() throws Exception {
        List<String> emptyList = List.of();
        Set<String> emptySet = Set.of();
        Map<String, String> emptyMap = Map.of();
        Action1<Object, StartContext> action = (StartContext ctx) -> List.of(emptyList, emptySet, emptyMap);

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) node.value();
        assertThat(result).hasSize(3);
        assertThat((List<?>) result.get(0)).isEmpty();
        assertThat((Set<?>) result.get(1)).isEmpty();
        assertThat((Map<?, ?>) result.get(2)).isEmpty();

    }

    @Test
    void capturedSingletonCollectionsTransliterate() throws Exception {
        List<String> singletonList = Collections.singletonList("x");
        Set<String> singletonSet = Collections.singleton("y");
        Map<String, String> singletonMap = Collections.singletonMap("k", "v");
        Action1<Object, StartContext> action = (StartContext ctx) -> List.of(singletonList, singletonSet, singletonMap);

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) node.value();
        assertThat(result.get(0)).isEqualTo(List.of("x"));
        assertThat(result.get(1)).isEqualTo(Set.of("y"));
        @SuppressWarnings("unchecked")
        Map<String, String> mapResult = (Map<String, String>) result.get(2);
        assertThat(mapResult).containsEntry("k", "v").hasSize(1);

    }

    @Test
    void capturedUnmodifiableListTransliterates() throws Exception {
        List<String> captured = Collections.unmodifiableList(new ArrayList<>(List.of("a", "b")));
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo(List.of("a", "b"));

    }

    @Test
    void capturedUnmodifiableSetTransliterates() throws Exception {
        Set<String> captured = Collections.unmodifiableSet(new HashSet<>(Set.of("a", "b")));
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) node.value();
        assertThat(result).containsExactlyInAnyOrder("a", "b");

    }

    @Test
    void capturedUnmodifiableMapTransliterates() throws Exception {
        Map<String, String> captured = Collections.unmodifiableMap(
                new HashMap<>(Map.of("k1", "v1", "k2", "v2")));
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) node.value();
        assertThat(result).containsEntry("k1", "v1").containsEntry("k2", "v2").hasSize(2);

    }

    @Test
    void capturedNestedCollectionTransliterates() throws Exception {
        List<List<String>> captured = List.of(List.of("a", "b"), List.of("c"));
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value()).isEqualTo(List.of(List.of("a", "b"), List.of("c")));

    }

    @Test
    void capturedMutableListRejected() {
        ArrayList<String> captured = new ArrayList<>(List.of("a"));
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        assertThatThrownBy(() -> LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test"))
                .isInstanceOf(TransliterationException.class)
                .hasMessageContaining("Unsupported collection/map type")
                .hasMessageContaining("ArrayList");
    }

    @Test
    void capturedMutableMapRejected() {
        HashMap<String, String> captured = new HashMap<>(Map.of("k", "v"));
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        assertThatThrownBy(() -> LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test"))
                .isInstanceOf(TransliterationException.class)
                .hasMessageContaining("Unsupported collection/map type")
                .hasMessageContaining("HashMap");
    }

    @Test
    void capturedLargeListTransliterates() throws Exception {
        List<String> captured = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l");
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        assertThat(node.value())
                .isEqualTo(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"));

    }

    @Test
    void capturedSharedObjectsDedup() throws Exception {
        List<String> shared = List.of("shared");
        List<Object> captured = List.of(shared, shared);
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) node.value();
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(List.of("shared"));
        assertThat(result.get(1)).isEqualTo(List.of("shared"));

    }

    // ── Record capture tests ──

    public record SimpleRecord(String name, int value) {
    }

    public record NestedRecord(String label, SimpleRecord inner) {
    }

    public record RecordWithList(String name, List<String> items) {
    }

    @Test
    void capturedRecordTransliterates() throws Exception {
        SimpleRecord captured = new SimpleRecord("hello", 42);
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        Object result = node.value();
        assertThat(result).isInstanceOf(SimpleRecord.class);
        SimpleRecord r = (SimpleRecord) result;
        assertThat(r.name()).isEqualTo("hello");
        assertThat(r.value()).isEqualTo(42);

    }

    @Test
    void capturedNestedRecordTransliterates() throws Exception {
        NestedRecord captured = new NestedRecord("outer", new SimpleRecord("inner", 7));
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        Object result = node.value();
        assertThat(result).isInstanceOf(NestedRecord.class);
        NestedRecord r = (NestedRecord) result;
        assertThat(r.label()).isEqualTo("outer");
        assertThat(r.inner().name()).isEqualTo("inner");
        assertThat(r.inner().value()).isEqualTo(7);

    }

    @Test
    void capturedRecordWithCollectionTransliterates() throws Exception {
        RecordWithList captured = new RecordWithList("items", List.of("a", "b", "c"));
        Action1<Object, StartContext> action = (StartContext ctx) -> captured;

        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, Object.class, List.of(), List.of(), List.of(), List.of(), false, false, "test");

        ServiceNode node = deployAndReturn(extracted);
        Object result = node.value();
        assertThat(result).isInstanceOf(RecordWithList.class);
        RecordWithList r = (RecordWithList) result;
        assertThat(r.name()).isEqualTo("items");
        assertThat(r.items()).containsExactly("a", "b", "c");

    }

    // ── Helpers ──

    /** Action that completes a node with a bound value. */
    static void completeWithValue(ServiceNode node, Object value) {
        node.startComplete(value);
    }

    private static final MethodHandle COMPLETE_WITH_VALUE;

    static {
        try {
            COMPLETE_WITH_VALUE = MethodHandles.lookup().findStatic(
                    LambdaTransliteratorTest.class, "completeWithValue",
                    MethodType.methodType(void.class, ServiceNode.class, Object.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Create a {@link ServiceNode} that is already completed with the given value.
     *
     * @param graph the owning graph
     * @param value the service value
     * @return a completed node whose {@link ServiceNode#value()} returns {@code value}
     */
    private static ServiceNode completedNode(ServiceGraph graph, Object value) {
        MethodHandle mh = MethodHandles.insertArguments(COMPLETE_WITH_VALUE, 1, value);
        ServiceNode node = new ServiceNode("dep", mh, graph, 0);
        node.run();
        return node;
    }

    /**
     * Generate a consolidated class with a single action, load it, invoke {@code deploy$0},
     * and return the resulting {@link StartupContext}.
     *
     * @param extracted the extracted action service
     * @return the startup context after deployment
     * @throws Exception if class loading or invocation fails
     */
    private static ServiceNode deployAndReturn(TransliteratedAction.ActionService extracted) throws Exception {
        return deployAndReturn(new ServiceGraph(), extracted);
    }

    /**
     * Generate a consolidated class with a single action, load it, invoke {@code deploy$0}
     * on a graph with the given dependency nodes, and return the completed {@link ServiceNode}.
     *
     * @param graph the service graph (provides the startup context)
     * @param extracted the extracted action service
     * @param deps dependency nodes (already completed) to wire into the deploy node
     * @return the startup context after deployment
     * @throws Exception if class loading or invocation fails
     */
    private static ServiceNode deployAndReturn(ServiceGraph graph,
            TransliteratedAction.ActionService extracted, ServiceNode... deps) throws Exception {
        Map<String, byte[]> classes = LambdaTransliterator.generateConsolidatedClass(
                CONSOLIDATED_CLASS_NAME, List.of(extracted));
        ClassLoader cl = makeClassLoader(classes);
        String className = CONSOLIDATED_CLASS_NAME.replace('/', '.');
        Class<?> clazz = cl.loadClass(className);
        return runDeploy(graph, clazz, 0, deps);
    }

    /**
     * Load deploy method {@code deploy$<index>} from the given class, wrap it in a
     * {@link ServiceNode}, run it, and return the node.
     *
     * @param graph the owning graph
     * @param clazz the consolidated class containing deploy methods
     * @param deployIndex the deploy method index (0, 1, ...)
     * @param deps dependency nodes to wire in
     * @return the completed service node
     * @throws Exception if method lookup or invocation fails
     */
    private static ServiceNode runDeploy(ServiceGraph graph, Class<?> clazz, int deployIndex,
            ServiceNode... deps) throws Exception {
        Method deploy = clazz.getMethod("deploy$" + deployIndex, ServiceNode.class);
        MethodHandle mh = MethodHandles.lookup().unreflect(deploy);
        ServiceNode node = new ServiceNode("deploy-" + deployIndex, mh, graph, 0, List.of(deps));
        node.run();
        return node;
    }

    /**
     * Create a classloader that can define one or more generated classes.
     * The input map uses internal names (with {@code /} separators); they
     * are converted to dot-separated names for class loading.
     *
     * @param classes map of internal class name → class bytes
     * @return a classloader that will define the classes on demand
     */
    private static ClassLoader makeClassLoader(Map<String, byte[]> classes) {
        Map<String, byte[]> dotNamed = new HashMap<>();
        for (var entry : classes.entrySet()) {
            dotNamed.put(entry.getKey().replace('/', '.'), entry.getValue());
        }
        return new ClassLoader(LambdaTransliteratorTest.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = dotNamed.get(name);
                if (bytes != null) {
                    return defineClass(name, bytes, 0, bytes.length);
                }
                throw new ClassNotFoundException(name);
            }
        };
    }
}
