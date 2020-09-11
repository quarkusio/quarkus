package io.quarkus.rest.runtime.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.NameBinding;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.ExceptionMapper;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.rest.runtime.core.ArcBeanFactory;
import io.quarkus.rest.runtime.core.ExceptionMapping;
import io.quarkus.rest.runtime.core.UnmanagedBeanFactory;
import io.quarkus.rest.runtime.model.ResourceExceptionMapper;
import io.quarkus.rest.runtime.model.ResourceInterceptors;
import io.quarkus.rest.runtime.model.ResourceRequestInterceptor;
import io.quarkus.rest.runtime.model.ResourceResponseInterceptor;
import io.quarkus.rest.runtime.model.SettableResourceInterceptor;
import io.quarkus.rest.runtime.spi.BeanFactory;

public class QuarkusRestFeatureContext implements FeatureContext {

    protected final ResourceInterceptors interceptors;
    private final ExceptionMapping exceptionMapping;
    private final BeanContainer beanContainer;
    private final QuarkusRestConfiguration configuration;

    private boolean filtersNeedSorting = false;

    public QuarkusRestFeatureContext(ResourceInterceptors interceptors, ExceptionMapping exceptionMapping,
            QuarkusRestConfiguration configuration, BeanContainer beanContainer) {
        this.interceptors = interceptors;
        this.exceptionMapping = exceptionMapping;
        this.configuration = configuration;
        this.beanContainer = beanContainer;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public FeatureContext property(String name, Object value) {
        configuration.property(name, value);
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass) {
        doRegister(componentClass, null, null);
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass, int priority) {
        doRegister(componentClass, null, priority);
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Class<?>... contracts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureContext register(Object component) {
        doRegister(component.getClass(), new UnmanagedBeanFactory<>(component), null);
        return this;
    }

    @Override
    public FeatureContext register(Object component, int priority) {
        doRegister(component.getClass(), new UnmanagedBeanFactory<>(component), priority);
        return this;
    }

    @Override
    public FeatureContext register(Object component, Class<?>... contracts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureContext register(Object component, Map<Class<?>, Integer> contracts) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doRegister(Class<?> componentClass, BeanFactory<?> beanFactory, Integer priority) {
        if (!isAllowed(componentClass)) {
            //TODO: log a warning
            return;
        }
        if (ExceptionMapper.class.isAssignableFrom(componentClass)) {
            Type[] genericInterfaces = componentClass.getGenericInterfaces();
            for (Type type : genericInterfaces) {
                // TODO: use proper generic handling
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    if (parameterizedType.getRawType().equals(ExceptionMapper.class)) {
                        Type exceptionType = parameterizedType.getActualTypeArguments()[0];
                        if (exceptionType instanceof Class) {
                            Class exceptionClass = (Class) exceptionType;
                            ResourceExceptionMapper resourceExceptionMapper = new ResourceExceptionMapper();
                            resourceExceptionMapper.setFactory(getFactory(componentClass, beanFactory));
                            exceptionMapping.addExceptionMapper(exceptionClass, resourceExceptionMapper);
                            break;
                        }
                    }
                }
            }
        }
        if (isFilter(componentClass)) {
            registerFilters(componentClass, beanFactory, priority);
            filtersNeedSorting = true;
        }

        //TODO: log warning if nothing was done
    }

    protected boolean isFilter(Class<?> componentClass) {
        return ContainerRequestFilter.class.isAssignableFrom(componentClass)
                || ContainerResponseFilter.class.isAssignableFrom(componentClass);
    }

    protected void registerFilters(Class<?> componentClass, BeanFactory<?> beanFactory, Integer priority) {
        boolean isRequest = ContainerRequestFilter.class.isAssignableFrom(componentClass);
        boolean isResponse = ContainerResponseFilter.class.isAssignableFrom(componentClass);
        if (isRequest) {
            ResourceRequestInterceptor requestInterceptor = new ResourceRequestInterceptor();
            Set<String> nameBindings = setCommonFilterProperties(componentClass, beanFactory, priority, requestInterceptor);
            if (componentClass.isAnnotationPresent(PreMatching.class)) {
                requestInterceptor.setPreMatching(true);
                interceptors.addGlobalRequestInterceptor(requestInterceptor);
            } else {
                if (nameBindings.isEmpty()) {
                    interceptors.addGlobalRequestInterceptor(requestInterceptor);
                } else {
                    interceptors.addNameRequestInterceptor(requestInterceptor);
                }
            }
        }
        if (isResponse) {
            ResourceResponseInterceptor responseInterceptor = new ResourceResponseInterceptor();
            Set<String> nameBindings = setCommonFilterProperties(componentClass, beanFactory, priority,
                    responseInterceptor);
            if (nameBindings.isEmpty()) {
                interceptors.addGlobalResponseInterceptor(responseInterceptor);
            } else {
                interceptors.addNameResponseInterceptor(responseInterceptor);
            }
        }
    }

    protected boolean isAllowed(Class<?> componentClass) {
        return true;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Set<String> setCommonFilterProperties(Class<?> componentClass, BeanFactory<?> beanFactory, Integer priority,
            SettableResourceInterceptor interceptor) {
        interceptor.setFactory(getFactory(componentClass, beanFactory));
        setFilterPriority(componentClass, priority, interceptor);
        Set<String> nameBindings = new HashSet<>();
        Annotation[] annotations = componentClass.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotation()) {
                if (annotation.annotationType().isAnnotationPresent(NameBinding.class)) {
                    nameBindings.add(annotation.annotationType().getName());
                }
            }
        }
        if (!nameBindings.isEmpty()) {
            interceptor.setNameBindingNames(nameBindings);
        }
        return nameBindings;
    }

    protected void setFilterPriority(Class<?> componentClass, Integer priority, SettableResourceInterceptor interceptor) {
        if (priority == null) {
            if (componentClass.isAnnotationPresent(Priority.class)) {
                interceptor.setPriority(componentClass.getDeclaredAnnotation(Priority.class).value());
            }
        } else {
            interceptor.setPriority(priority);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected BeanFactory getFactory(Class<?> componentClass, BeanFactory explicitValue) {
        if (explicitValue != null) {
            return explicitValue;
        }
        return new ArcBeanFactory(componentClass, beanContainer);
    }

    public boolean isFiltersNeedSorting() {
        return filtersNeedSorting;
    }
}
