package io.quarkus.arc.test.interceptor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.jandex.DotName;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class InterceptorBindingRegistrarTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(NotAnInterceptorBinding.class, SimpleBean.class, SimpleInterceptor.class))
            .addBuildChainCustomizer(b -> {
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new InterceptorBindingRegistrarBuildItem(new InterceptorBindingRegistrar() {
                            @Override
                            public Map<DotName, Set<String>> registerAdditionalBindings() {
                                return Collections.singletonMap(DotName.createSimple(NotAnInterceptorBinding.class.getName()),
                                        Collections.emptySet());
                            }

                        }));
                    }
                }).produces(InterceptorBindingRegistrarBuildItem.class).build();
            });

    @Inject
    SimpleBean bean;

    @Test
    public void testInterceptor() {
        assertEquals("OK:PONG", bean.ping());
    }

    @Singleton
    static class SimpleBean {

        @NotAnInterceptorBinding
        public String ping() {
            return "PONG";
        }

    }

    @Priority(1)
    @Interceptor
    @NotAnInterceptorBinding
    static class SimpleInterceptor {

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            return "OK:" + ctx.proceed();
        }

    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @interface NotAnInterceptorBinding {

    }

}
