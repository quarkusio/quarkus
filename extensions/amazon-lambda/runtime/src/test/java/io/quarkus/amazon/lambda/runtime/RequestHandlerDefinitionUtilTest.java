package io.quarkus.amazon.lambda.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class RequestHandlerDefinitionUtilTest {

    @Test
    public void testSimpleStringHandler() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(SimpleStringHandler.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(String.class, definition.inputOutputTypes().inputType());
        assertEquals(Integer.class, definition.inputOutputTypes().outputType());
    }

    @Test
    public void testSimpleIntegerHandler() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(SimpleIntegerHandler.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(Integer.class, definition.inputOutputTypes().inputType());
        assertEquals(Boolean.class, definition.inputOutputTypes().outputType());
    }

    @Test
    public void testConcreteHandler() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(ConcreteHandler.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(Integer.class, definition.inputOutputTypes().inputType());
        assertEquals(String.class, definition.inputOutputTypes().outputType());
    }

    @Test
    public void testInterfaceBasedHandler() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(InterfaceBasedHandler.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(String.class, definition.inputOutputTypes().inputType());
        assertEquals(Boolean.class, definition.inputOutputTypes().outputType());
    }

    @Test
    public void testMultiLevelConcrete() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(MultiLevelConcrete.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(Double.class, definition.inputOutputTypes().inputType());
        assertEquals(Float.class, definition.inputOutputTypes().outputType());
    }

    @Test
    public void testInvertedConcrete() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(InvertedConcrete.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(Integer.class, definition.inputOutputTypes().inputType());
        assertEquals(String.class, definition.inputOutputTypes().outputType());
    }

    @Test
    public void testComplexInvertedConcrete() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(ComplexInvertedConcrete.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(Long.class, definition.inputOutputTypes().inputType());
        assertEquals(Boolean.class, definition.inputOutputTypes().outputType());
    }

    @Test
    public void testObjectHandlerUsesObjectTypes() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(ResolvedObjectHandler.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(Object.class, definition.inputOutputTypes().inputType());
        assertEquals(Object.class, definition.inputOutputTypes().outputType());
    }

    @Test
    public void testConcreteMethodPrefersOverAbstractParent() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(ConcreteOverridesAbstract.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(String.class, definition.inputOutputTypes().inputType());
        assertEquals(Boolean.class, definition.inputOutputTypes().outputType());
        // Should find the concrete method in the child class, not the abstract one in parent
        assertEquals(ConcreteOverridesAbstract.class, definition.method().getDeclaringClass());
    }

    @Test
    public void testDefaultInterfaceMethod() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(DefaultMethodHandler.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(Double.class, definition.inputOutputTypes().inputType());
        assertEquals(Long.class, definition.inputOutputTypes().outputType());
        // Should find the default method in the interface
        assertEquals(DefaultMethodInterface.class, definition.method().getDeclaringClass());
    }

    @Test
    public void testConcreteMethodPrefersOverDefaultMethod() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(ConcreteOverridesDefault.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(Double.class, definition.inputOutputTypes().inputType());
        assertEquals(Long.class, definition.inputOutputTypes().outputType());
        // Should prefer the concrete implementation over the default method
        assertEquals(ConcreteOverridesDefault.class, definition.method().getDeclaringClass());
    }

    @Test
    public void testInheritsConcreteFromParent() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(ChildInheritsFromConcrete.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(Integer.class, definition.inputOutputTypes().inputType());
        assertEquals(String.class, definition.inputOutputTypes().outputType());
        // Should find the concrete method in the parent class
        assertEquals(ConcreteParent.class, definition.method().getDeclaringClass());
    }

    @Test
    public void testAbstractClassWithConcreteMethod() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(InheritsFromAbstractWithConcrete.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(String.class, definition.inputOutputTypes().inputType());
        assertEquals(String.class, definition.inputOutputTypes().outputType());
        // Should find the concrete method in the abstract parent
        assertEquals(AbstractWithConcrete.class, definition.method().getDeclaringClass());
    }

    @Test
    public void testPurelyAbstractShouldFail() {
        assertThrows(IllegalStateException.class, () -> {
            RequestHandlerDefinitionUtil.discoverHandlerMethod(PurelyAbstractHandler.class);
        });
    }

    @Test
    public void testNestedInterfaceDefault() {
        RequestHandlerDefinitionUtil.RequestHandlerDefinition definition = RequestHandlerDefinitionUtil
                .discoverHandlerMethod(NestedInterfaceHandler.class);

        assertNotNull(definition);
        assertEquals("handleRequest", definition.method().getName());
        assertEquals(Float.class, definition.inputOutputTypes().inputType());
        assertEquals(Byte.class, definition.inputOutputTypes().outputType());
        // Should find the default method in the nested interface
        assertEquals(NestedDefaultInterface.class, definition.method().getDeclaringClass());
    }

    // Simple hierarchy test cases
    static class SimpleStringHandler implements RequestHandler<String, Integer> {
        @Override
        public Integer handleRequest(String input, Context context) {
            return input.length();
        }
    }

    static class SimpleIntegerHandler implements RequestHandler<Integer, Boolean> {
        @Override
        public Boolean handleRequest(Integer input, Context context) {
            return input > 0;
        }
    }

    // Complex hierarchy test cases
    static abstract class BaseHandler<I, O> implements RequestHandler<I, O> {
    }

    static abstract class MiddleHandler<T> extends BaseHandler<T, String> {
    }

    static class ConcreteHandler extends MiddleHandler<Integer> {
        @Override
        public String handleRequest(Integer input, Context context) {
            return String.valueOf(input);
        }
    }

    static interface CustomInterface<A, B> extends RequestHandler<A, B> {
    }

    static class InterfaceBasedHandler implements CustomInterface<String, Boolean> {
        @Override
        public Boolean handleRequest(String input, Context context) {
            return Boolean.valueOf(input);
        }
    }

    static abstract class MultiLevelBase<X, Y, Z> implements RequestHandler<X, Y> {
    }

    static abstract class MultiLevelMiddle<P, Q> extends MultiLevelBase<P, Q, String> {
    }

    static class MultiLevelConcrete extends MultiLevelMiddle<Double, Float> {
        @Override
        public Float handleRequest(Double input, Context context) {
            return input.floatValue();
        }
    }

    // Inverted type parameters test cases
    static abstract class InvertedBase<A, B> implements RequestHandler<A, B> {
    }

    static abstract class InvertedMiddle<T, S> extends InvertedBase<S, T> { // Note: inverted order
    }

    static class InvertedConcrete extends InvertedMiddle<String, Integer> {
        @Override
        public String handleRequest(Integer input, Context context) {
            return String.valueOf(input);
        }
    }

    static abstract class ComplexInvertedBase<X, Y, Z> implements RequestHandler<Y, X> { // Y, X instead of X, Y
    }

    static abstract class ComplexInvertedMiddle<A, B> extends ComplexInvertedBase<B, A, String> { // B, A instead of A, B
    }

    static class ComplexInvertedConcrete extends ComplexInvertedMiddle<Long, Boolean> {
        @Override
        public Boolean handleRequest(Long input, Context context) {
            return input > 0;
        }
    }

    static class ResolvedObjectHandler implements RequestHandler<Object, Object> {
        @Override
        public Object handleRequest(Object input, Context context) {
            return input;
        }
    }

    // Abstract parent with concrete child override
    static abstract class AbstractParentWithAbstract implements RequestHandler<String, Boolean> {
        // Abstract method - should be ignored
        public abstract Boolean handleRequest(String input, Context context);
    }

    static class ConcreteOverridesAbstract extends AbstractParentWithAbstract {
        @Override
        public Boolean handleRequest(String input, Context context) {
            return Boolean.valueOf(input);
        }
    }

    // Default interface method test
    interface DefaultMethodInterface extends RequestHandler<Double, Long> {
        @Override
        default Long handleRequest(Double input, Context context) {
            return input.longValue();
        }
    }

    static class DefaultMethodHandler implements DefaultMethodInterface {
        // Uses the default implementation from interface
    }

    // Concrete method overrides default method
    static class ConcreteOverridesDefault implements DefaultMethodInterface {
        @Override
        public Long handleRequest(Double input, Context context) {
            return input.longValue() * 2; // Different implementation
        }
    }

    // Concrete parent with inheriting child
    static class ConcreteParent implements RequestHandler<Integer, String> {
        @Override
        public String handleRequest(Integer input, Context context) {
            return String.valueOf(input);
        }
    }

    static class ChildInheritsFromConcrete extends ConcreteParent {
        // Inherits the concrete handleRequest method from parent
    }

    // Abstract class with concrete method
    static abstract class AbstractWithConcrete implements RequestHandler<String, String> {
        @Override
        public String handleRequest(String input, Context context) {
            return input.toUpperCase();
        }
    }

    static class InheritsFromAbstractWithConcrete extends AbstractWithConcrete {
        // Inherits concrete method from abstract parent
    }

    // Purely abstract - should fail
    static abstract class PurelyAbstractHandler implements RequestHandler<Object, Object> {
        // Only has abstract methods - should fail
    }

    // Nested interface default method
    interface BaseInterfaceNoDefault extends RequestHandler<Float, Byte> {
        // No default method here
    }

    interface NestedDefaultInterface extends BaseInterfaceNoDefault {
        @Override
        default Byte handleRequest(Float input, Context context) {
            return input.byteValue();
        }
    }

    static class NestedInterfaceHandler implements NestedDefaultInterface {
        // Uses the nested default method
    }
}
