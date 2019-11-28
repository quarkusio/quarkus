package io.quarkus.arquillian;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.test.impl.client.protocol.local.LocalDeploymentPackager;
import org.jboss.arquillian.container.test.impl.execution.event.LocalExecutionEvent;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.container.test.spi.client.protocol.ProtocolConfiguration;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;

import io.quarkus.arquillian.QuarkusProtocol.QuarkusProtocolConfiguration;

class QuarkusProtocol implements Protocol<QuarkusProtocolConfiguration> {

    @Inject
    Instance<Injector> injector;

    @Override
    public Class<QuarkusProtocolConfiguration> getProtocolConfigurationClass() {
        return QuarkusProtocolConfiguration.class;
    }

    @Override
    public ProtocolDescription getDescription() {
        return new ProtocolDescription("Quarkus");
    }

    @Override
    public DeploymentPackager getPackager() {
        return new LocalDeploymentPackager();
    }

    @Override
    public ContainerMethodExecutor getExecutor(QuarkusProtocolConfiguration protocolConfiguration, ProtocolMetaData metaData,
            CommandCallback callback) {
        return injector.get().inject(new QuarkusMethodExecutor());
    }

    public static class QuarkusProtocolConfiguration implements ProtocolConfiguration {
    }

    static class QuarkusMethodExecutor implements ContainerMethodExecutor {

        @Inject
        Event<LocalExecutionEvent> event;

        @Inject
        Instance<TestResult> testResult;

        @Inject
        Instance<ClassLoader> classLoaderInstance;

        @Override
        public TestResult invoke(TestMethodExecutor testMethodExecutor) {

            event.fire(new LocalExecutionEvent(new TestMethodExecutor() {

                @Override
                public void invoke(Object... parameters) throws Throwable {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(classLoaderInstance.get());

                        Object actualTestInstance = QuarkusDeployableContainer.testInstance;
                        Method actualMethod = null;
                        try {
                            actualMethod = actualTestInstance.getClass().getMethod(getMethod().getName(),
                                    convertToTCCL(getMethod().getParameterTypes()));
                        } catch (NoSuchMethodException e) {
                            // the method should still be present, just not public, let's try declared methods
                            actualMethod = actualTestInstance.getClass().getDeclaredMethod(getMethod().getName(),
                                    convertToTCCL(getMethod().getParameterTypes()));
                            actualMethod.setAccessible(true);
                        }
                        try {
                            actualMethod.invoke(actualTestInstance, parameters);
                        } catch (InvocationTargetException e) {
                            Throwable cause = e.getCause();
                            if (cause != null) {
                                throw cause;
                            } else {
                                throw e;
                            }
                        }
                    } finally {
                        Thread.currentThread().setContextClassLoader(loader);
                    }
                }

                @Override
                public Method getMethod() {
                    return testMethodExecutor.getMethod();
                }

                @Override
                public Object getInstance() {
                    return QuarkusDeployableContainer.testInstance;
                }

                @Override
                public String getMethodName() {
                    return testMethodExecutor.getMethod().getName();
                }
            }));
            return testResult.get();
        }

    }

    /**
     * getMethod() returns a method found using the system class loader, but the actual parameters are loaded by
     * TCCL
     * so to be able to invoke the method we find the same method using TCCL
     */
    static Class<?>[] convertToTCCL(Class<?>[] classes) throws ClassNotFoundException {
        return convertToCL(classes, Thread.currentThread().getContextClassLoader());
    }

    static Class<?>[] convertToCL(Class<?>[] classes, ClassLoader classLoader) throws ClassNotFoundException {
        Class<?>[] result = new Class<?>[classes.length];
        for (int i = 0; i < classes.length; i++) {
            if (classes[i].getClassLoader() != classLoader) {
                result[i] = classLoader.loadClass(classes[i].getName());
            } else {
                result[i] = classes[i];
            }
        }
        return result;
    }

}
