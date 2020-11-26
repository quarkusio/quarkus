package io.quarkus.rest.server.runtime;

import static io.quarkus.rest.server.runtime.NotFoundExceptionMapper.classMappers;

import java.io.Closeable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.core.Application;

import org.jboss.resteasy.reactive.common.core.SingletonBeanFactory;
import org.jboss.resteasy.reactive.common.core.ThreadSetupAction;
import org.jboss.resteasy.reactive.common.model.ResourceContextResolver;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.core.ContextResolvers;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.core.QuarkusRestDeployment;
import org.jboss.resteasy.reactive.server.core.QuarkusRestDeploymentInfo;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.startup.RuntimeDeploymentManager;
import org.jboss.resteasy.reactive.server.handlers.QuarkusRestInitialHandler;
import org.jboss.resteasy.reactive.server.handlers.ServerRestHandler;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestProviders;
import org.jboss.resteasy.reactive.server.util.RuntimeResourceVisitor;
import org.jboss.resteasy.reactive.server.util.ScoreSystem;
import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.resteasy.reactive.common.runtime.ArcBeanFactory;
import io.quarkus.resteasy.reactive.common.runtime.ArcThreadSetupAction;
import io.quarkus.resteasy.reactive.common.runtime.QuarkusRestCommonRecorder;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ResteasyReactiveRecorder extends QuarkusRestCommonRecorder {

    public static final Supplier<Executor> EXECUTOR_SUPPLIER = new Supplier<Executor>() {
        @Override
        public Executor get() {
            return ExecutorRecorder.getCurrent();
        }
    };

    private static volatile QuarkusRestDeployment currentDeployment;

    public static QuarkusRestDeployment getCurrentDeployment() {
        return currentDeployment;
    }

    public RuntimeValue<QuarkusRestDeployment> createDeployment(QuarkusRestDeploymentInfo info,
            BeanContainer beanContainer,
            ShutdownContext shutdownContext, HttpBuildTimeConfig vertxConfig,
            RequestContextFactory contextFactory,
            BeanFactory<ResteasyReactiveInitialiser> initClassFactory) {

        CurrentRequestManager
                .setCurrentRequestInstance(new QuarkusCurrentRequest(beanContainer.instance(CurrentVertxRequest.class)));

        BlockingOperationSupport.setIoThreadDetector(new BlockingOperationSupport.IOThreadDetector() {
            @Override
            public boolean isBlockingAllowed() {
                return BlockingOperationControl.isBlockingAllowed();
            }
        });

        Consumer<Closeable> closeTaskHandler = new Consumer<Closeable>() {
            @Override
            public void accept(Closeable closeable) {
                shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(closeable));
            }
        };
        if (contextFactory == null) {
            contextFactory = new RequestContextFactory() {
                @Override
                public ResteasyReactiveRequestContext createContext(QuarkusRestDeployment deployment,
                        QuarkusRestProviders providers, Object context, ThreadSetupAction requestContext,
                        ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain) {
                    return new QuarkusResteasyReactiveRequestContext(deployment, providers, (RoutingContext) context,
                            requestContext,
                            handlerChain,
                            abortHandlerChain);
                }
            };
        }

        RuntimeDeploymentManager runtimeDeploymentManager = new RuntimeDeploymentManager(info, EXECUTOR_SUPPLIER,
                closeTaskHandler, contextFactory, new ArcThreadSetupAction(beanContainer.requestContext()),
                vertxConfig.rootPath);
        QuarkusRestDeployment deployment = runtimeDeploymentManager.deploy();
        initClassFactory.createInstance().getInstance().init(deployment);
        currentDeployment = deployment;

        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            classMappers = deployment.getClassMappers();
            RuntimeResourceVisitor.visitRuntimeResources(classMappers, ScoreSystem.ScoreVisitor);
        }
        return new RuntimeValue<>(deployment);
    }

    public Handler<RoutingContext> handler(RuntimeValue<QuarkusRestDeployment> deploymentRuntimeValue) {
        QuarkusRestDeployment deployment = deploymentRuntimeValue.getValue();
        QuarkusRestInitialHandler initialHandler = new QuarkusRestInitialHandler(deployment);
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                initialHandler.beginProcessing(event);
            }
        };
    }

    /**
     * This is Quarkus specific.
     * <p>
     * We have a strategy around handling the Application class and build time init, that allows for the singletons
     * to still work.
     *
     * @param applicationClass
     * @param singletonClassesEmpty
     * @return
     */
    public Supplier<Application> handleApplication(final Class<? extends Application> applicationClass,
            final boolean singletonClassesEmpty) {
        Supplier<Application> applicationSupplier;
        if (singletonClassesEmpty) {
            applicationSupplier = new Supplier<Application>() {
                @Override
                public Application get() {
                    try {
                        return applicationClass.getConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } else {
            try {
                final Application application = applicationClass.getConstructor().newInstance();
                for (Object i : application.getSingletons()) {
                    SingletonBeanFactory.setInstance(i.getClass().getName(), i);
                }
                applicationSupplier = new Supplier<Application>() {
                    @Override
                    public Application get() {
                        return application;
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return applicationSupplier;
    }

    @SuppressWarnings("unchecked")
    public void registerExceptionMapper(ExceptionMapping exceptionMapping, String string,
            ResourceExceptionMapper<Throwable> mapper) {
        exceptionMapping.addExceptionMapper(loadClass(string), mapper);
    }

    public void registerContextResolver(ContextResolvers contextResolvers, String string,
            ResourceContextResolver resolver) {
        contextResolvers.addContextResolver(loadClass(string), resolver);
    }

    public Function<Class<?>, BeanFactory<?>> factoryCreator(BeanContainer container) {
        return new Function<Class<?>, BeanFactory<?>>() {
            @Override
            public BeanFactory<?> apply(Class<?> aClass) {
                return new ArcBeanFactory<>(aClass, container);
            }
        };
    }
}
