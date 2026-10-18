package io.quarkus.arc.test.buildextension.context;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ContextCreator;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.processor.ContextConfigurator;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class CustomContextTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(FieldScoped.class, MeadowScoped.class, Mina.class, InvalidNestedContext.class,
                    InvalidAbstractContext.class, InvalidConstructorContext.class, FieldContext.class, MeadowContext.class)
            .contextRegistrars(new ContextRegistrar() {
                @Override
                public void register(RegistrationContext ctx) {
                    ContextConfigurator configurator = ctx.configure(FieldScoped.class);
                    assertThrows(IllegalArgumentException.class, () -> configurator.contextClass(InvalidNestedContext.class));
                    assertThrows(IllegalArgumentException.class, () -> configurator.contextClass(InvalidAbstractContext.class));
                    assertThrows(IllegalArgumentException.class,
                            () -> configurator.contextClass(InvalidConstructorContext.class));
                    configurator.contextClass(FieldContext.class).done();
                }
            })
            .contextRegistrars(new ContextRegistrar() {
                @Override
                public void register(RegistrationContext ctx) {
                    ctx.configure(MeadowScoped.class).creator(MeadowCreator.class).done();
                }

            })
            .build();

    @Test
    public void testCustomScope() {
        ArcContainer arc = Arc.container();
        assertEquals("bac", arc.instance(Mina.class).get().bum());
    }

    @FieldScoped
    public static class Mina {

        public String bum() {
            return "bac";
        }

    }

    @MeadowScoped
    public static class Flower {

        public void bloom() {
        }

    }

    @NormalScope
    @Inherited
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    public @interface FieldScoped {
    }

    @NormalScope
    @Inherited
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    public @interface MeadowScoped {
    }

    public static class FieldContext implements InjectableContext {

        public FieldContext(CurrentContextFactory ccf) {
            assertNotNull(ccf);
        }

        @Override
        public void destroy(Contextual<?> contextual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return FieldScoped.class;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            return (T) new Mina();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(Contextual<T> contextual) {
            return (T) new Mina();
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void destroy() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ContextState getState() {
            throw new UnsupportedOperationException();
        }

    }

    public static class MeadowCreator implements ContextCreator {

        @Override
        public InjectableContext create(Map<String, Object> params) {
            assertNotNull(params.get(KEY_CURRENT_CONTEXT_FACTORY));
            return new MeadowContext();
        }

    }

    public static class MeadowContext implements InjectableContext {

        @Override
        public void destroy(Contextual<?> contextual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return MeadowScoped.class;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            return (T) new Flower();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(Contextual<T> contextual) {
            return (T) new Flower();
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void destroy() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ContextState getState() {
            throw new UnsupportedOperationException();
        }

    }

    class InvalidNestedContext implements InjectableContext {

        @Override
        public void destroy(Contextual<?> contextual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return FieldScoped.class;
        }

        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T get(Contextual<T> contextual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isActive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void destroy() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ContextState getState() {
            throw new UnsupportedOperationException();
        }

    }

    public abstract class InvalidAbstractContext implements InjectableContext {

        @Override
        public void destroy(Contextual<?> contextual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return FieldScoped.class;
        }

        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T get(Contextual<T> contextual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isActive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void destroy() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ContextState getState() {
            throw new UnsupportedOperationException();
        }

    }

    public class InvalidConstructorContext implements InjectableContext {

        public InvalidConstructorContext(Long age) {
        }

        @Override
        public void destroy(Contextual<?> contextual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return FieldScoped.class;
        }

        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T get(Contextual<T> contextual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isActive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void destroy() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ContextState getState() {
            throw new UnsupportedOperationException();
        }

    }

}
