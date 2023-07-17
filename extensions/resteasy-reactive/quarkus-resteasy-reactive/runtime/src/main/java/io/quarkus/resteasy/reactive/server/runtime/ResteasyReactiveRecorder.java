package io.quarkus.resteasy.reactive.server.runtime;

import static io.quarkus.resteasy.reactive.server.runtime.NotFoundExceptionMapper.classMappers;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.DefaultAuthFailureHandler.extractRootCause;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.ws.rs.core.Application;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.core.SingletonBeanFactory;
import org.jboss.resteasy.reactive.common.model.ResourceContextResolver;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.DeploymentInfo;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.core.startup.RuntimeDeploymentManager;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;
import org.jboss.resteasy.reactive.server.model.ContextResolvers;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;
import org.jboss.resteasy.reactive.server.spi.EndpointInvokerFactory;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.server.util.RuntimeResourceVisitor;
import org.jboss.resteasy.reactive.server.util.ScoreSystem;
import org.jboss.resteasy.reactive.server.vertx.ResteasyReactiveVertxHandler;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.resteasy.reactive.common.runtime.ArcBeanFactory;
import io.quarkus.resteasy.reactive.common.runtime.ArcThreadSetupAction;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveCommonRecorder;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.rest.DisabledRestEndpoints;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.DefaultAuthFailureHandler;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ResteasyReactiveRecorder extends ResteasyReactiveCommonRecorder implements EndpointInvokerFactory {

    static final Logger logger = Logger.getLogger("io.quarkus");

    public static final Supplier<Executor> EXECUTOR_SUPPLIER = new Supplier<Executor>() {
        @Override
        public Executor get() {
            return ExecutorRecorder.getCurrent();
        }
    };
    public static final Supplier<Executor> VIRTUAL_EXECUTOR_SUPPLIER = new Supplier<Executor>() {
        Executor current = null;

        /**
         * This method is used to specify a custom executor to dispatch virtual threads on carrier threads
         * We need reflection for both ease of use (see {@link #get() Get} method) but also because we call methods
         * of private classes from the java.lang package.
         *
         * It is used for testing purposes only for now
         */
        private Executor setVirtualThreadCustomScheduler(Executor executor) throws ClassNotFoundException,
                InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
            var vtf = Class.forName("java.lang.ThreadBuilders").getDeclaredClasses()[0];
            Constructor constructor = vtf.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            ThreadFactory tf = (ThreadFactory) constructor.newInstance(
                    new Object[] { executor, "quarkus-virtual-factory-", 0, 0,
                            null });

            return (Executor) Executors.class.getMethod("newThreadPerTaskExecutor", ThreadFactory.class)
                    .invoke(this, tf);
        }

        /**
         * This method uses reflection in order to allow developers to quickly test quarkus-loom without needing to
         * change --release, --source, --target flags and to enable previews.
         * Since we try to load the "Loom-preview" classes/methods at runtime, the application can even be compiled
         * using java 11 and executed with a loom-compliant JDK.
         * <p>
         * IMPORTANT: we still need to use a duplicated context to have all the propagation working.
         * Thus, the context is captured and applied/terminated in the virtual thread.
         */
        @Override
        public Executor get() {
            if (current == null) {
                try {
                    var virtual = (Executor) Executors.class.getMethod("newVirtualThreadPerTaskExecutor")
                            .invoke(this);
                    current = new Executor() {
                        @Override
                        public void execute(Runnable command) {
                            var context = Vertx.currentContext();
                            if (!(context instanceof ContextInternal)) {
                                virtual.execute(command);
                            } else {
                                virtual.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        final var previousContext = ((ContextInternal) context).beginDispatch();
                                        try {
                                            command.run();
                                        } finally {
                                            ((ContextInternal) context).endDispatch(previousContext);
                                        }
                                    }
                                });
                            }
                        }
                    };
                } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                    logger.debug("Unable to invoke java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor", e);
                    //quite ugly but works
                    logger.warnf("You weren't able to create an executor that spawns virtual threads, the default" +
                            " blocking executor will be used, please check that your JDK is compatible with " +
                            "virtual threads");
                    //if for some reason a class/method can't be loaded or invoked we return the traditional EXECUTOR
                    current = EXECUTOR_SUPPLIER.get();
                }
            }
            return current;
        }
    };

    static volatile Deployment currentDeployment;

    public static Deployment getCurrentDeployment() {
        return currentDeployment;
    }

    public RuntimeValue<Deployment> createDeployment(DeploymentInfo info,
            BeanContainer beanContainer,
            ShutdownContext shutdownContext, HttpBuildTimeConfig vertxConfig,
            RequestContextFactory contextFactory,
            BeanFactory<ResteasyReactiveInitialiser> initClassFactory,
            LaunchMode launchMode, boolean resumeOn404) {

        if (resumeOn404) {
            info.setResumeOn404(true);
        }

        CurrentRequestManager
                .setCurrentRequestInstance(new QuarkusCurrentRequest(beanContainer.beanInstance(CurrentVertxRequest.class)));

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
        CurrentIdentityAssociation currentIdentityAssociation = Arc.container().select(CurrentIdentityAssociation.class)
                .orNull();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (contextFactory == null) {
            contextFactory = new RequestContextFactory() {
                @Override
                public ResteasyReactiveRequestContext createContext(Deployment deployment,
                        Object context, ThreadSetupAction requestContext,
                        ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain) {
                    return new QuarkusResteasyReactiveRequestContext(deployment, (RoutingContext) context,
                            requestContext,
                            handlerChain,
                            abortHandlerChain, launchMode == LaunchMode.DEVELOPMENT ? tccl : null, currentIdentityAssociation);
                }

            };
        }

        RuntimeDeploymentManager runtimeDeploymentManager = new RuntimeDeploymentManager(info, EXECUTOR_SUPPLIER,
                VIRTUAL_EXECUTOR_SUPPLIER,
                closeTaskHandler, contextFactory, new ArcThreadSetupAction(beanContainer.requestContext()),
                vertxConfig.rootPath);
        Deployment deployment = runtimeDeploymentManager.deploy();
        DisabledRestEndpoints.set(deployment.getDisabledEndpoints());
        initClassFactory.createInstance().getInstance().init(deployment);
        currentDeployment = deployment;

        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            classMappers = deployment.getClassMappers();
            RuntimeResourceVisitor.visitRuntimeResources(classMappers, ScoreSystem.ScoreVisitor);
        }
        return new RuntimeValue<>(deployment);
    }

    public RuntimeValue<RestInitialHandler> restInitialHandler(RuntimeValue<Deployment> deploymentRuntimeValue) {
        Deployment deployment = deploymentRuntimeValue.getValue();
        return new RuntimeValue<>(new RestInitialHandler(deployment));
    }

    public Handler<RoutingContext> handler(RuntimeValue<RestInitialHandler> restInitialHandlerRuntimeValue) {
        RestInitialHandler initialHandler = restInitialHandlerRuntimeValue.getValue();

        // ensure our ex. mappers are called when security exceptions are thrown and proactive auth is disabled
        final Consumer<RoutingContext> eventCustomizer = new Consumer<>() {

            @Override
            public void accept(RoutingContext routingContext) {
                // remove default auth failure handler so that exception is not handled by both failure and abort handlers
                if (routingContext.get(QuarkusHttpUser.AUTH_FAILURE_HANDLER) instanceof FailingDefaultAuthFailureHandler) {
                    routingContext.remove(QuarkusHttpUser.AUTH_FAILURE_HANDLER);
                }
            }
        };

        return new ResteasyReactiveVertxHandler(eventCustomizer, initialHandler);
    }

    public Handler<RoutingContext> failureHandler(RuntimeValue<RestInitialHandler> restInitialHandlerRuntimeValue,
            boolean noCustomAuthCompletionExMapper, boolean noCustomAuthFailureExMapper, boolean noCustomAuthRedirectExMapper,
            boolean proactive) {
        final RestInitialHandler restInitialHandler = restInitialHandlerRuntimeValue.getValue();
        // process auth failures with abort handlers
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {

                // special handling when proactive auth is enabled as then we know default auth failure handler already run
                if (proactive && event.get(QuarkusHttpUser.AUTH_FAILURE_HANDLER) instanceof DefaultAuthFailureHandler) {
                    // we want to prevent repeated handling of exceptions if user don't want to handle exception himself
                    // we do not pass exception to abort handlers if proactive auth is enabled and user did not
                    // provide custom ex. mapper; we replace default auth failure handler as soon as we can, so that
                    // we can handle Quarkus Security Exceptions ourselves
                    if (event.failure() instanceof AuthenticationFailedException) {
                        if (noCustomAuthFailureExMapper) {
                            event.next();
                        } else {
                            // allow response customization
                            restInitialHandler.beginProcessing(event, event.failure());
                        }
                        return;
                    } else if (event.failure() instanceof AuthenticationCompletionException) {
                        if (noCustomAuthCompletionExMapper) {
                            event.next();
                        } else {
                            // allow response customization
                            restInitialHandler.beginProcessing(event, event.failure());
                        }
                        return;
                    } else if (event.failure() instanceof AuthenticationRedirectException) {
                        if (noCustomAuthRedirectExMapper) {
                            event.next();
                        } else {
                            // allow response customization
                            restInitialHandler.beginProcessing(event, event.failure());
                        }
                        return;
                    }
                }

                if (event.failure() instanceof AuthenticationException
                        || event.failure() instanceof ForbiddenException) {
                    restInitialHandler.beginProcessing(event, event.failure());
                } else {
                    event.next();
                }
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
        exceptionMapping.addExceptionMapper(string, mapper);
    }

    public void registerContextResolver(ContextResolvers contextResolvers, String string,
            ResourceContextResolver resolver) {
        contextResolvers.addContextResolver(loadClass(string), resolver);
    }

    @Override
    public Supplier<EndpointInvoker> invoker(String baseName) {
        return new Supplier<EndpointInvoker>() {
            @Override
            public EndpointInvoker get() {
                try {
                    return (EndpointInvoker) loadClass(baseName).getDeclaredConstructor().newInstance();
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException
                        | InvocationTargetException e) {
                    throw new RuntimeException("Unable to generate endpoint invoker", e);
                }

            }
        };
    }

    public Function<Class<?>, BeanFactory<?>> factoryCreator(BeanContainer container) {
        return new Function<Class<?>, BeanFactory<?>>() {
            @Override
            public BeanFactory<?> apply(Class<?> aClass) {
                return new ArcBeanFactory<>(aClass, container);
            }
        };
    }

    public Function<Object, Object> clientProxyUnwrapper() {
        return ClientProxy::unwrap;
    }

    public Supplier<Boolean> disableIfPropertyMatches(String propertyName, String propertyValue, boolean disableIfMissing) {
        return new Supplier<>() {
            @Override
            public Boolean get() {
                String value = ConfigProvider.getConfig().getConfigValue(propertyName).getValue();
                if (value == null) {
                    return disableIfMissing;
                } else {
                    return value.equals(propertyValue);
                }
            }
        };
    }

    public ServerSerialisers createServerSerialisers() {
        return new ServerSerialisers();
    }

    public Handler<RoutingContext> defaultAuthFailureHandler() {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (event.get(QuarkusHttpUser.AUTH_FAILURE_HANDLER) instanceof DefaultAuthFailureHandler) {
                    // fail event rather than end it, so it's handled by abort handlers (see #addFailureHandler method)
                    event.put(QuarkusHttpUser.AUTH_FAILURE_HANDLER, new FailingDefaultAuthFailureHandler());
                }
                event.next();
            }
        };
    }

    public Supplier<Boolean> beanUnavailable(String className) {
        return new Supplier<>() {
            @Override
            public Boolean get() {
                try {
                    return !Arc.container().select(Class.forName(className, false, Thread.currentThread()
                            .getContextClassLoader())).isResolvable();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Unable to determine if bean '" + className + "' is available", e);
                }
            }
        };
    }

    private static final class FailingDefaultAuthFailureHandler implements BiConsumer<RoutingContext, Throwable> {

        @Override
        public void accept(RoutingContext event, Throwable throwable) {
            if (!event.failed()) {
                event.fail(extractRootCause(throwable));
            }
        }
    }

}
