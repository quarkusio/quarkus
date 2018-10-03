package org.jboss.shamrock.arc.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.InstanceHandle;
import org.jboss.protean.arc.ManagedContext;
import org.jboss.shamrock.runtime.BeanContainer;
import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.RuntimeInjector;
import org.jboss.shamrock.runtime.StartupContext;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ThreadSetupHandler;

/**
 * @author Martin Kouba
 */
public class ArcDeploymentTemplate {

    @ContextObject("arc.container")
    public ArcContainer getContainer(StartupContext startupContext) throws Exception {
        ArcContainer container = Arc.initialize();
        startupContext.addCloseable(new Closeable() {
            @Override
            public void close() throws IOException {
                Arc.shutdown();
            }
        });
        return container;
    }

    @ContextObject("bean.container")
    public BeanContainer initBeanContainer(ArcContainer container) throws Exception {
        return new BeanContainer() {

            @Override
            public <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers) {
                InstanceHandle<T> handle = container.instance(type, qualifiers);
                if (!handle.isAvailable()) {
                    return null;
                }
                return new Factory<T>() {
                    @Override
                    public T get() {
                        return handle.get();
                    }
                };
            }
        };
    }

    public void setupRequestScope(@ContextObject("deploymentInfo") DeploymentInfo deploymentInfo, @ContextObject("arc.container") ArcContainer arcContainer) {
        if(deploymentInfo == null) {
            return;
        }
        deploymentInfo.addThreadSetupAction(new ThreadSetupHandler() {
            @Override
            public <T, C> Action<T, C> create(Action<T, C> action) {
                return new Action<T, C>() {
                    @Override
                    public T call(HttpServerExchange exchange, C context) throws Exception {
                        ManagedContext requestContext = arcContainer.requestContext();
                        requestContext.activate();
                        try {
                            return action.call(exchange, context);
                        } finally {
                            requestContext.terminate();
                        }
                    }
                };
            }
        });
    }

    public void setupInjection(StartupContext startupContext, ArcContainer container) {
        InjectionFactory old = RuntimeInjector.setFactory(new InjectionFactory() {
            @Override
            public <T> InjectionInstance<T> create(Class<T> type) {
                InstanceHandle<T> instance = container.instance(type);
                if (instance.isAvailable()) {
                    return new InjectionInstance<T>() {
                        @Override
                        public T newInstance() {
                            return instance.get();
                        }
                    };
                } else {
                    return new InjectionInstance<T>() {
                        @Override
                        public T newInstance() {
                            try {
                                return type.newInstance();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                }
            }
        });
        startupContext.addCloseable(new Closeable() {
            @Override
            public void close() throws IOException {
                RuntimeInjector.setFactory(old);
            }
        });

    }

}
