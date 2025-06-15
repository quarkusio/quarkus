package io.quarkus.arc.arquillian;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedCallable;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.test.impl.client.protocol.local.LocalDeploymentPackager;
import org.jboss.arquillian.container.test.impl.execution.event.LocalExecutionEvent;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.arquillian.utils.ClassLoading;

public class ArcProtocol implements Protocol<ArcProtocolConfiguration> {
    @Inject
    Instance<Injector> injector;

    @Override
    public Class<ArcProtocolConfiguration> getProtocolConfigurationClass() {
        return ArcProtocolConfiguration.class;
    }

    @Override
    public ProtocolDescription getDescription() {
        return new ProtocolDescription("ArC");
    }

    @Override
    public DeploymentPackager getPackager() {
        return new LocalDeploymentPackager();
    }

    @Override
    public ContainerMethodExecutor getExecutor(ArcProtocolConfiguration protocolConfiguration, ProtocolMetaData metaData,
            CommandCallback callback) {
        return injector.get().inject(new ArcMethodExecutor());
    }

    static class ArcMethodExecutor implements ContainerMethodExecutor {
        @Inject
        Event<LocalExecutionEvent> event;

        @Inject
        Instance<TestResult> testResult;

        @Inject
        Instance<DeploymentClassLoader> deploymentClassLoader;

        @Inject
        Instance<ArcContainer> runningArc;

        @Override
        public TestResult invoke(TestMethodExecutor testMethodExecutor) {
            event.fire(new LocalExecutionEvent(new TestMethodExecutor() {
                private final ArcContainer arc = runningArc.get();

                @Override
                public String getMethodName() {
                    return testMethodExecutor.getMethod().getName();
                }

                @Override
                public Method getMethod() {
                    return testMethodExecutor.getMethod();
                }

                @Override
                public Object getInstance() {
                    return ArcDeployableContainer.testInstance;
                }

                @Override
                public void invoke(Object... ignored) throws Throwable {
                    ClassLoader old = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(deploymentClassLoader.get());

                        arc.requestContext().activate();

                        Object actualTestInstance = ArcDeployableContainer.testInstance;

                        Method actualMethod = null;
                        try {
                            actualMethod = actualTestInstance.getClass().getMethod(getMethod().getName(),
                                    ClassLoading.convertToTCCL(getMethod().getParameterTypes()));
                        } catch (NoSuchMethodException e) {
                            actualMethod = actualTestInstance.getClass().getDeclaredMethod(getMethod().getName(),
                                    ClassLoading.convertToTCCL(getMethod().getParameterTypes()));
                            actualMethod.setAccessible(true);
                        }

                        AtomicReference<CreationalContext<?>> contextToDestroy = new AtomicReference<>();
                        Object[] parameters = lookupParameters(actualMethod, contextToDestroy);

                        try {
                            actualMethod.invoke(actualTestInstance, parameters);
                        } catch (InvocationTargetException e) {
                            Throwable cause = e.getCause();
                            throw ClassLoading.cloneExceptionIntoSystemCL(cause);
                        } finally {
                            CreationalContext<?> creationalContext = contextToDestroy.get();
                            if (creationalContext != null) {
                                creationalContext.release();
                            }
                        }
                    } finally {
                        arc.requestContext().terminate();

                        Thread.currentThread().setContextClassLoader(old);
                    }
                }

                private Object[] lookupParameters(Method method, AtomicReference<CreationalContext<?>> context) {
                    Parameter[] parameters = method.getParameters();
                    Object[] result = new Object[parameters.length];

                    boolean hasNonArquillianDataProvider = false;
                    for (Annotation annotation : method.getAnnotations()) {
                        if (annotation.annotationType().getName().equals("org.testng.annotations.Test")) {
                            try {
                                Method dataProviderMember = annotation.annotationType().getDeclaredMethod("dataProvider");
                                String value = dataProviderMember.invoke(annotation).toString();
                                hasNonArquillianDataProvider = !value.equals("") && !value.equals("ARQUILLIAN_DATA_PROVIDER");
                                break;
                            } catch (ReflectiveOperationException ignored) {
                            }
                        }
                    }
                    if (hasNonArquillianDataProvider) {
                        return result;
                    }

                    BeanManager beanManager = arc.beanManager();
                    CreationalContext<Object> creationalContext = beanManager.createCreationalContext(null);
                    context.set(creationalContext);
                    for (int i = 0; i < parameters.length; i++) {
                        result[i] = beanManager.getInjectableReference(new FakeInjectionPoint<>(parameters[i],
                                beanManager), creationalContext);
                    }
                    return result;
                }
            }));

            return testResult.get();
        }
    }

    private static class FakeInjectionPoint<T> implements InjectionPoint {
        private final Parameter parameter;
        private final BeanManager beanManager;

        FakeInjectionPoint(Parameter parameter, BeanManager beanManager) {
            this.parameter = parameter;
            this.beanManager = beanManager;
        }

        @Override
        public Type getType() {
            return parameter.getParameterizedType();
        }

        @Override
        public Set<Annotation> getQualifiers() {
            Set<Annotation> qualifiers = new HashSet<>();
            for (Annotation annotation : parameter.getAnnotations()) {
                if (beanManager.isQualifier(annotation.annotationType())) {
                    qualifiers.add(annotation);
                }
            }
            return qualifiers;
        }

        @Override
        public Bean<?> getBean() {
            Set<Bean<?>> beans = beanManager.getBeans(getType(), getQualifiers().toArray(new Annotation[0]));
            return beanManager.resolve(beans);
        }

        @Override
        public Member getMember() {
            return parameter.getDeclaringExecutable();
        }

        @Override
        public Annotated getAnnotated() {
            return new FakeAnnotatedParameter<T>();
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }

        class FakeAnnotatedParameter<T> implements AnnotatedParameter<T> {
            @Override
            public int getPosition() {
                throw new UnsupportedOperationException(); // not used anywhere
            }

            @Override
            public AnnotatedCallable<T> getDeclaringCallable() {
                throw new UnsupportedOperationException(); // not used anywhere
            }

            @Override
            public Type getBaseType() {
                return FakeInjectionPoint.this.getType();
            }

            @Override
            public Set<Type> getTypeClosure() {
                return Set.of(getBaseType(), Object.class);
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
                for (Annotation annotation : parameter.getAnnotations()) {
                    if (annotation.annotationType() == annotationType) {
                        return annotationType.cast(annotation);
                    }
                }
                return null;
            }

            @Override
            public Set<Annotation> getAnnotations() {
                return Set.of(parameter.getAnnotations());
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
                return getAnnotation(annotationType) != null;
            }
        }
    }
}
