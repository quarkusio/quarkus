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

package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;

/**
 * @author Martin Kouba
 */
@Template
public class ArcDeploymentTemplate {

    private static final Logger LOGGER = Logger.getLogger(ArcDeploymentTemplate.class.getName());

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

    public BeanContainer initBeanContainer(ArcContainer container, List<BeanContainerListener> listeners,
            Collection<String> removedBeanTypes)
            throws Exception {
        BeanContainer beanContainer = new BeanContainer() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers) {
                Supplier<InstanceHandle<T>> handleSupplier = container.instanceSupplier(type, qualifiers);
                if (handleSupplier == null) {
                    if (removedBeanTypes.contains(type.getName())) {
                        // Note that this only catches the simplest use cases
                        LOGGER.warnf(
                                "Bean matching %s was marked as unused and removed during build.\nExtensions can eliminate false positives using:\n\t- a custom UnremovableBeanBuildItem\n\t- AdditionalBeanBuildItem(false, beanClazz)",
                                type);
                    } else {
                        LOGGER.debugf(
                                "No matching bean found for type %s and qualifiers %s. The bean might have been marked as unused and removed during build.",
                                type, Arrays.toString(qualifiers));
                    }
                    return new DefaultInstanceFactory<>(type);
                }
                return new Factory<T>() {
                    @Override
                    public Instance<T> create() {
                        InstanceHandle<T> handle = handleSupplier.get();
                        return new Instance<T>() {
                            @Override
                            public T get() {
                                return handle.get();
                            }

                            @Override
                            public void close() {
                                handle.close();
                            }
                        };
                    }
                };
            }

            @Override
            public ManagedContext requestContext() {
                return container.requestContext();
            }
        };
        for (BeanContainerListener listener : listeners) {
            listener.created(beanContainer);
        }
        return beanContainer;
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

    private static final class DefaultInstanceFactory<T> implements BeanContainer.Factory<T> {

        final Class<T> type;

        private DefaultInstanceFactory(Class<T> type) {
            this.type = type;
        }

        @Override
        public BeanContainer.Instance<T> create() {
            try {
                T instance = type.newInstance();
                return new BeanContainer.Instance<T>() {
                    @Override
                    public T get() {
                        return instance;
                    }
                };
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
