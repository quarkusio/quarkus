package io.quarkus.amazon.lambda.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.amazon.lambda.deployment.RequestHandlerJandexUtil.RequestHandlerJandexDefinition;
import io.quarkus.deployment.index.IndexWrapper;
import io.quarkus.deployment.index.PersistentClassIndex;

public class RequestHandlerJandexUtilTest {

    private static IndexView index;

    @BeforeAll
    public static void setup() throws IOException {
        Indexer indexer = new Indexer();

        // Mimick what would happen in a regular Quarkus app:
        // forcefully index all test classes, but let the computing index index JDK classes
        indexer.indexClass(SimpleStringHandler.class);
        indexer.indexClass(SimpleIntegerHandler.class);
        indexer.indexClass(BaseHandler.class);
        indexer.indexClass(MiddleHandler.class);
        indexer.indexClass(ConcreteHandler.class);
        indexer.indexClass(CustomInterface.class);
        indexer.indexClass(InterfaceBasedHandler.class);
        indexer.indexClass(MultiLevelBase.class);
        indexer.indexClass(MultiLevelMiddle.class);
        indexer.indexClass(MultiLevelConcrete.class);
        indexer.indexClass(InvertedBase.class);
        indexer.indexClass(InvertedMiddle.class);
        indexer.indexClass(InvertedConcrete.class);
        indexer.indexClass(ComplexInvertedBase.class);
        indexer.indexClass(ComplexInvertedMiddle.class);
        indexer.indexClass(ComplexInvertedConcrete.class);
        indexer.indexClass(ResolvedObjectHandler.class);
        indexer.indexClass(AbstractParentWithAbstract.class);
        indexer.indexClass(ConcreteOverridesAbstract.class);
        indexer.indexClass(DefaultMethodInterface.class);
        indexer.indexClass(DefaultMethodHandler.class);
        indexer.indexClass(ConcreteOverridesDefault.class);
        indexer.indexClass(ConcreteParent.class);
        indexer.indexClass(ChildInheritsFromConcrete.class);
        indexer.indexClass(AbstractWithConcrete.class);
        indexer.indexClass(InheritsFromAbstractWithConcrete.class);
        indexer.indexClass(PurelyAbstractHandler.class);
        indexer.indexClass(BaseInterfaceNoDefault.class);
        indexer.indexClass(NestedDefaultInterface.class);
        indexer.indexClass(NestedInterfaceHandler.class);

        // Collection test classes
        indexer.indexClass(ListStringHandler.class);
        indexer.indexClass(ListIntegerHandler.class);
        indexer.indexClass(SetStringHandler.class);
        indexer.indexClass(CollectionDoubleHandler.class);
        indexer.indexClass(GenericListBase.class);
        indexer.indexClass(GenericListConcrete.class);

        index = new IndexWrapper(indexer.complete(), Thread.currentThread().getContextClassLoader(),
                new PersistentClassIndex());
    }

    private static void indexClass(Indexer indexer, Class<?> clazz) throws IOException {
        indexer.indexClass(clazz);
    }

    @Test
    public void testSimpleStringHandler() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(SimpleStringHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals(SimpleStringHandler.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertEquals(String.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(Integer.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testSimpleIntegerHandler() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(SimpleIntegerHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals(SimpleIntegerHandler.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertEquals(Integer.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(Boolean.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testConcreteHandler() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(ConcreteHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals(ConcreteHandler.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertEquals(Integer.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(String.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testInterfaceBasedHandler() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(InterfaceBasedHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals(InterfaceBasedHandler.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertEquals(String.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(Boolean.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testMultiLevelConcrete() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(MultiLevelConcrete.class.getName(), index);

        assertNotNull(definition);
        assertEquals(MultiLevelConcrete.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertEquals(Double.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(Float.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testInvertedConcrete() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(InvertedConcrete.class.getName(), index);

        assertNotNull(definition);
        assertEquals(InvertedConcrete.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertEquals(Integer.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(String.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testComplexInvertedConcrete() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(ComplexInvertedConcrete.class.getName(), index);

        assertNotNull(definition);
        assertEquals(ComplexInvertedConcrete.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertEquals(Long.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(Boolean.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testObjectHandlerUsesObjectTypes() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(ResolvedObjectHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals(ResolvedObjectHandler.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertEquals(Object.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(Object.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testConcreteMethodPrefersOverAbstractParent() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(ConcreteOverridesAbstract.class.getName(), index);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().name());
        assertEquals(String.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(Boolean.class.getName(), definition.inputOutputTypes().outputType().name().toString());
        // Should find the concrete method in the child class, not the abstract one in parent
        assertEquals(ConcreteOverridesAbstract.class.getName(), definition.method().declaringClass().name().toString());
    }

    @Test
    public void testDefaultInterfaceMethod() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(DefaultMethodHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().name());
        assertEquals(Double.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(Long.class.getName(), definition.inputOutputTypes().outputType().name().toString());
        // Should find the default method in the interface
        assertEquals(DefaultMethodInterface.class.getName(), definition.method().declaringClass().name().toString());
    }

    @Test
    public void testConcreteMethodPrefersOverDefaultMethod() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(ConcreteOverridesDefault.class.getName(), index);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().name());
        assertEquals(Double.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(Long.class.getName(), definition.inputOutputTypes().outputType().name().toString());
        // Should prefer the concrete implementation over the default method
        assertEquals(ConcreteOverridesDefault.class.getName(), definition.method().declaringClass().name().toString());
    }

    @Test
    public void testInheritsConcreteFromParent() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(ChildInheritsFromConcrete.class.getName(), index);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().name());
        assertEquals(Integer.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(String.class.getName(), definition.inputOutputTypes().outputType().name().toString());
        // Should find the concrete method in the parent class
        assertEquals(ConcreteParent.class.getName(), definition.method().declaringClass().name().toString());
    }

    @Test
    public void testAbstractClassWithConcreteMethod() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(InheritsFromAbstractWithConcrete.class.getName(), index);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().name());
        assertEquals(String.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(String.class.getName(), definition.inputOutputTypes().outputType().name().toString());
        // Should find the concrete method in the abstract parent
        assertEquals(AbstractWithConcrete.class.getName(), definition.method().declaringClass().name().toString());
    }

    @Test
    public void testPurelyAbstractShouldFail() {
        assertThrows(IllegalStateException.class, () -> {
            RequestHandlerJandexUtil.discoverHandlerMethod(PurelyAbstractHandler.class.getName(), index);
        });
    }

    @Test
    public void testNestedInterfaceDefault() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(NestedInterfaceHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().name());
        assertEquals(Float.class.getName(), definition.inputOutputTypes().inputType().name().toString());
        assertEquals(Byte.class.getName(), definition.inputOutputTypes().outputType().name().toString());
        // Should find the default method in the nested interface
        assertEquals(NestedDefaultInterface.class.getName(), definition.method().declaringClass().name().toString());
    }

    // Collection handler tests
    @Test
    public void testListStringHandler() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(ListStringHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals(ListStringHandler.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertTrue(definition.inputOutputTypes().isCollection(), "Input type should be detected as collection");
        assertEquals(String.class.getName(), definition.inputOutputTypes().elementType().name().toString());
        assertEquals(Integer.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testListIntegerHandler() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(ListIntegerHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals(ListIntegerHandler.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertTrue(definition.inputOutputTypes().isCollection(), "Input type should be detected as collection");
        assertEquals(Integer.class.getName(), definition.inputOutputTypes().elementType().name().toString());
        assertEquals(String.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testSetStringHandler() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(SetStringHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals(SetStringHandler.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertTrue(definition.inputOutputTypes().isCollection(), "Input type should be detected as collection");
        assertEquals(String.class.getName(), definition.inputOutputTypes().elementType().name().toString());
        assertEquals(Boolean.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testCollectionDoubleHandler() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(CollectionDoubleHandler.class.getName(), index);

        assertNotNull(definition);
        assertEquals(CollectionDoubleHandler.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertTrue(definition.inputOutputTypes().isCollection(), "Input type should be detected as collection");
        assertEquals(Double.class.getName(), definition.inputOutputTypes().elementType().name().toString());
        assertEquals(Long.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testGenericListConcrete() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(GenericListConcrete.class.getName(), index);

        assertNotNull(definition);
        assertEquals(GenericListConcrete.class.getName(), definition.method().declaringClass().name().toString());
        assertEquals("handleRequest", definition.method().name());
        assertTrue(definition.inputOutputTypes().isCollection(), "Input type should be detected as collection");
        assertEquals(String.class.getName(), definition.inputOutputTypes().elementType().name().toString());
        assertEquals(Float.class.getName(), definition.inputOutputTypes().outputType().name().toString());
    }

    @Test
    public void testNonCollectionHandlerNotDetectedAsCollection() {
        RequestHandlerJandexDefinition definition = RequestHandlerJandexUtil
                .discoverHandlerMethod(SimpleStringHandler.class.getName(), index);

        assertNotNull(definition);
        assertFalse(definition.inputOutputTypes().isCollection(), "Non-collection input should not be detected as collection");
    }

    // Simple hierarchy test cases
    public static class SimpleStringHandler implements RequestHandler<String, Integer> {
        @Override
        public Integer handleRequest(String input, Context context) {
            return input.length();
        }
    }

    public static class SimpleIntegerHandler implements RequestHandler<Integer, Boolean> {
        @Override
        public Boolean handleRequest(Integer input, Context context) {
            return input > 0;
        }
    }

    // Complex hierarchy test cases
    public static abstract class BaseHandler<I, O> implements RequestHandler<I, O> {
    }

    public static abstract class MiddleHandler<T> extends BaseHandler<T, String> {
    }

    public static class ConcreteHandler extends MiddleHandler<Integer> {
        @Override
        public String handleRequest(Integer input, Context context) {
            return String.valueOf(input);
        }
    }

    public static interface CustomInterface<A, B> extends RequestHandler<A, B> {
    }

    public static class InterfaceBasedHandler implements CustomInterface<String, Boolean> {
        @Override
        public Boolean handleRequest(String input, Context context) {
            return Boolean.valueOf(input);
        }
    }

    public static abstract class MultiLevelBase<X, Y, Z> implements RequestHandler<X, Y> {
    }

    public static abstract class MultiLevelMiddle<P, Q> extends MultiLevelBase<P, Q, String> {
    }

    public static class MultiLevelConcrete extends MultiLevelMiddle<Double, Float> {
        @Override
        public Float handleRequest(Double input, Context context) {
            return input.floatValue();
        }
    }

    // Inverted type parameters test cases
    public static abstract class InvertedBase<A, B> implements RequestHandler<A, B> {
    }

    public static abstract class InvertedMiddle<T, S> extends InvertedBase<S, T> { // Note: inverted order
    }

    public static class InvertedConcrete extends InvertedMiddle<String, Integer> {
        @Override
        public String handleRequest(Integer input, Context context) {
            return String.valueOf(input);
        }
    }

    public static abstract class ComplexInvertedBase<X, Y, Z> implements RequestHandler<Y, X> { // Y, X instead of X, Y
    }

    public static abstract class ComplexInvertedMiddle<A, B> extends ComplexInvertedBase<B, A, String> { // B, A instead of A, B
    }

    public static class ComplexInvertedConcrete extends ComplexInvertedMiddle<Long, Boolean> {
        @Override
        public Boolean handleRequest(Long input, Context context) {
            return input > 0;
        }
    }

    public static class ResolvedObjectHandler implements RequestHandler<Object, Object> {
        @Override
        public Object handleRequest(Object input, Context context) {
            return input;
        }
    }

    // Abstract parent with concrete child override
    public static abstract class AbstractParentWithAbstract implements RequestHandler<String, Boolean> {
        // Abstract method - should be ignored
        public abstract Boolean handleRequest(String input, Context context);
    }

    public static class ConcreteOverridesAbstract extends AbstractParentWithAbstract {
        @Override
        public Boolean handleRequest(String input, Context context) {
            return Boolean.valueOf(input);
        }
    }

    // Default interface method test
    public interface DefaultMethodInterface extends RequestHandler<Double, Long> {
        @Override
        default Long handleRequest(Double input, Context context) {
            return input.longValue();
        }
    }

    public static class DefaultMethodHandler implements DefaultMethodInterface {
        // Uses the default implementation from interface
    }

    // Concrete method overrides default method
    public static class ConcreteOverridesDefault implements DefaultMethodInterface {
        @Override
        public Long handleRequest(Double input, Context context) {
            return input.longValue() * 2; // Different implementation
        }
    }

    // Concrete parent with inheriting child
    public static class ConcreteParent implements RequestHandler<Integer, String> {
        @Override
        public String handleRequest(Integer input, Context context) {
            return String.valueOf(input);
        }
    }

    public static class ChildInheritsFromConcrete extends ConcreteParent {
        // Inherits the concrete handleRequest method from parent
    }

    // Abstract class with concrete method
    public static abstract class AbstractWithConcrete implements RequestHandler<String, String> {
        @Override
        public String handleRequest(String input, Context context) {
            return input.toUpperCase();
        }
    }

    public static class InheritsFromAbstractWithConcrete extends AbstractWithConcrete {
        // Inherits concrete method from abstract parent
    }

    // Purely abstract - should fail
    public static abstract class PurelyAbstractHandler implements RequestHandler<Object, Object> {
        // Only has abstract methods - should fail
    }

    // Nested interface default method
    public interface BaseInterfaceNoDefault extends RequestHandler<Float, Byte> {
        // No default method here
    }

    public interface NestedDefaultInterface extends BaseInterfaceNoDefault {
        @Override
        default Byte handleRequest(Float input, Context context) {
            return input.byteValue();
        }
    }

    public static class NestedInterfaceHandler implements NestedDefaultInterface {
        // Uses the nested default method
    }

    // Collection test handler classes
    public static class ListStringHandler implements RequestHandler<List<String>, Integer> {
        @Override
        public Integer handleRequest(List<String> input, Context context) {
            return input.size();
        }
    }

    public static class ListIntegerHandler implements RequestHandler<List<Integer>, String> {
        @Override
        public String handleRequest(List<Integer> input, Context context) {
            return input.toString();
        }
    }

    public static class SetStringHandler implements RequestHandler<Set<String>, Boolean> {
        @Override
        public Boolean handleRequest(Set<String> input, Context context) {
            return !input.isEmpty();
        }
    }

    public static class CollectionDoubleHandler implements RequestHandler<Collection<Double>, Long> {
        @Override
        public Long handleRequest(Collection<Double> input, Context context) {
            return (long) input.size();
        }
    }

    // Generic list with type resolution
    public static abstract class GenericListBase<T> implements RequestHandler<List<T>, Float> {
    }

    public static class GenericListConcrete extends GenericListBase<String> {
        @Override
        public Float handleRequest(List<String> input, Context context) {
            return (float) input.size();
        }
    }
}
