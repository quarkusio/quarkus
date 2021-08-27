package io.quarkus.arc.test.context;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.inject.Inject;
import javax.inject.Scope;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ContextCreator;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.CustomScopeBuildItem;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class CustomPseudoScopeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyPseudoScope.class, SimpleBean.class, MyPseudoContext.class))
            .addBuildChainCustomizer(b -> {
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        ContextRegistrationPhaseBuildItem contextRegistrationPhase = context
                                .consume(ContextRegistrationPhaseBuildItem.class);
                        context.produce(new ContextConfiguratorBuildItem(contextRegistrationPhase.getContext()
                                .configure(MyPseudoScope.class).param("message", "PONG").creator(MyPseudoContextCreator.class)
                                .normal(false)));
                    }
                }).produces(ContextConfiguratorBuildItem.class).consumes(ContextRegistrationPhaseBuildItem.class).build();
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new CustomScopeBuildItem(MyPseudoScope.class));
                    }
                }).produces(CustomScopeBuildItem.class).build();
            });

    @Inject
    SimpleBean bean;

    @Test
    public void testBean() {
        assertEquals("PONG", bean.ping());
    }

    @MyPseudoScope
    static class SimpleBean {

        private final String message;

        // this constructor is needed because otherwise SimpleBean(String) is considered an @Inject constructor
        public SimpleBean() {
            this(null);
        }

        public SimpleBean(String message) {
            this.message = message;
        }

        public String ping() {
            return message;
        }

        @Override
        public String toString() {
            return "SimpleBean [message=" + message + "]";
        }

    }

    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @Documented
    @Scope
    @Inherited
    public @interface MyPseudoScope {

    }

    public static class MyPseudoContextCreator implements ContextCreator {

        @Override
        public InjectableContext create(Map<String, Object> params) {
            return new MyPseudoContext(params.get("message"));
        }

    }

    public static class MyPseudoContext implements InjectableContext {

        private final String message;

        public MyPseudoContext(Object message) {
            this.message = message.toString();
        }

        @Override
        public void destroy(Contextual<?> contextual) {
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return MyPseudoScope.class;
        }

        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            return get(contextual);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(Contextual<T> contextual) {
            return (T) new SimpleBean(message);
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void destroy() {
        }

        @Override
        public ContextState getState() {
            throw new UnsupportedOperationException();
        }

    }

}
