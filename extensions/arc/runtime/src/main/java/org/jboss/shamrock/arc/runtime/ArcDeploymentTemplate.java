/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Supplier;

import javax.servlet.ServletContext;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.InstanceHandle;
import org.jboss.protean.arc.ManagedContext;
import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.ShutdownContext;
import org.jboss.shamrock.runtime.Template;

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

    public void handleLifecycleEvents(ShutdownContext context, BeanContainer beanContainer) {
        LifecycleEventRunner instance = beanContainer.instance(LifecycleEventRunner.class);
        instance.fireStartupEvent();
        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                instance.fireShutdownEvent();
            }
        });
    }

}
