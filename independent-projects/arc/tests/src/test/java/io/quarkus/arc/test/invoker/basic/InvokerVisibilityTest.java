package io.quarkus.arc.test.invoker.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.InvokerInfo;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class InvokerVisibilityTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyPublicService.class, MyProtectedService.class, MyPackagePrivateService.class)
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    Map<String, InvokerInfo> invokers = new LinkedHashMap<>();
                    for (Class<?> clazz : List.of(MyPublicService.class, MyProtectedService.class,
                            MyPackagePrivateService.class)) {
                        BeanInfo bean = context.beans().withBeanClass(clazz).firstResult().orElseThrow();
                        for (MethodInfo method : bean.getImplClazz().methods()) {
                            if (method.isConstructor()) {
                                continue;
                            }
                            invokers.put(clazz.getSimpleName() + "_" + method.name(),
                                    context.getInvokerFactory().createInvoker(bean, method).build());
                        }
                    }
                    InvokerHelperRegistrar.synthesizeInvokerHelper(context, invokers);
                }
            })
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        for (Class<?> clazz : List.of(MyPublicService.class, MyProtectedService.class, MyPackagePrivateService.class)) {
            InstanceHandle<?> service = Arc.container().instance(clazz);

            for (String method : List.of("hello", "helloProtected", "helloPackagePrivate",
                    "helloStatic", "helloProtectedStatic", "helloPackagePrivateStatic")) {
                String id = clazz.getSimpleName() + "_" + method;
                assertEquals(id, helper.getInvoker(id).invoke(service.get(), null));
            }
        }
    }

    @Singleton
    public static class MyPublicService {
        public String hello() {
            return "MyPublicService_hello";
        }

        protected String helloProtected() {
            return "MyPublicService_helloProtected";
        }

        String helloPackagePrivate() {
            return "MyPublicService_helloPackagePrivate";
        }

        public static String helloStatic() {
            return "MyPublicService_helloStatic";
        }

        protected static String helloProtectedStatic() {
            return "MyPublicService_helloProtectedStatic";
        }

        static String helloPackagePrivateStatic() {
            return "MyPublicService_helloPackagePrivateStatic";
        }
    }

    @Singleton
    protected static class MyProtectedService {
        public String hello() {
            return "MyProtectedService_hello";
        }

        protected String helloProtected() {
            return "MyProtectedService_helloProtected";
        }

        String helloPackagePrivate() {
            return "MyProtectedService_helloPackagePrivate";
        }

        public static String helloStatic() {
            return "MyProtectedService_helloStatic";
        }

        protected static String helloProtectedStatic() {
            return "MyProtectedService_helloProtectedStatic";
        }

        static String helloPackagePrivateStatic() {
            return "MyProtectedService_helloPackagePrivateStatic";
        }
    }

    @Singleton
    static class MyPackagePrivateService {
        public String hello() {
            return "MyPackagePrivateService_hello";
        }

        protected String helloProtected() {
            return "MyPackagePrivateService_helloProtected";
        }

        String helloPackagePrivate() {
            return "MyPackagePrivateService_helloPackagePrivate";
        }

        public static String helloStatic() {
            return "MyPackagePrivateService_helloStatic";
        }

        protected static String helloProtectedStatic() {
            return "MyPackagePrivateService_helloProtectedStatic";
        }

        static String helloPackagePrivateStatic() {
            return "MyPackagePrivateService_helloPackagePrivateStatic";
        }
    }
}
