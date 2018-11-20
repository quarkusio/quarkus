package org.jboss.shamrock.arc.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Supplier;

import javax.servlet.ServletContext;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.InstanceHandle;
import org.jboss.protean.arc.ManagedContext;
import org.jboss.shamrock.runtime.ShutdownContext;
import org.jboss.shamrock.runtime.Template;
import org.jboss.shamrock.runtime.cdi.BeanContainer;
import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.StartupContext;
import org.jboss.shamrock.runtime.cdi.BeanContainerListener;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ThreadSetupHandler;

/**
 * @author Martin Kouba
 */
@Template
public class ArcDeploymentTemplate {

    public ArcContainer getContainer(ShutdownContext shutdown) throws Exception {
        ArcContainer container = Arc.initialize();
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                Arc.shutdown();
            }
        });
        return container;
    }

    public BeanContainer initBeanContainer(ArcContainer container, List<BeanContainerListener> beanConfigurators) throws Exception {
        BeanContainer beanContainer = new BeanContainer() {

            @Override
            public <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers) {
                Supplier<InstanceHandle<T>> handle = container.instanceSupplier(type, qualifiers);
                if (handle == null) {
                    return null;
                }
                return new Factory<T>() {
                    @Override
                    public T get() {
                        return handle.get().get();
                    }
                };
            }
        };
        for(BeanContainerListener i : beanConfigurators) {
            i.created(beanContainer);
        }
        return beanContainer;
    }

    public ServletExtension setupRequestScope(ArcContainer arcContainer) {
        return new ServletExtension() {
            @Override
            public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
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
        };
    }

    public InjectionFactory setupInjection(ArcContainer container) {
        return new InjectionFactory() {
            @Override
            public <T> InjectionInstance<T> create(Class<T> type) {
                Supplier<InstanceHandle<T>> instance = container.instanceSupplier(type);
                if (instance != null) {
                    return new InjectionInstance<T>() {
                        @Override
                        public T newInstance() {
                            return instance.get().get();
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
        };
    }

    public void fireStartupEvent(BeanContainer beanContainer) {
        beanContainer.instance(StartupEventRunner.class).fireEvent();
    }

}
