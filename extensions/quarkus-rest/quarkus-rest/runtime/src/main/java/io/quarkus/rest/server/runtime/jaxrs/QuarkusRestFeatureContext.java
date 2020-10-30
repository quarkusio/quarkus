package io.quarkus.rest.server.runtime.jaxrs;

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
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.rest.common.runtime.core.ArcBeanFactory;
import io.quarkus.rest.common.runtime.core.UnmanagedBeanFactory;
import io.quarkus.rest.common.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.common.runtime.model.InterceptorContainer;
import io.quarkus.rest.common.runtime.model.PreMatchInterceptorContainer;
import io.quarkus.rest.common.runtime.model.ResourceExceptionMapper;
import io.quarkus.rest.common.runtime.model.ResourceInterceptor;
import io.quarkus.rest.common.runtime.model.ResourceInterceptors;
import io.quarkus.rest.common.runtime.model.SettableResourceInterceptor;
import io.quarkus.rest.server.runtime.core.ExceptionMapping;
import io.quarkus.rest.spi.BeanFactory;

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
        if (isInterceptor(componentClass)) {
            registerInterceptors(componentClass, beanFactory, priority);
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
            register(componentClass, beanFactory, priority, interceptors.getContainerRequestFilters());
        }
        if (isResponse) {
            register(componentClass, beanFactory, priority, interceptors.getContainerResponseFilters());
        }
    }

    protected boolean isInterceptor(Class<?> componentClass) {
        return ReaderInterceptor.class.isAssignableFrom(componentClass)
                || WriterInterceptor.class.isAssignableFrom(componentClass);
    }

    protected void registerInterceptors(Class<?> componentClass, BeanFactory<?> beanFactory, Integer priority) {
        boolean isReader = ReaderInterceptor.class.isAssignableFrom(componentClass);
        boolean isWriter = WriterInterceptor.class.isAssignableFrom(componentClass);
        if (isReader) {
            register(componentClass, beanFactory, priority, interceptors.getReaderInterceptors());
        }
        if (isWriter) {
            register(componentClass, beanFactory, priority, interceptors.getWriterInterceptors());
        }
    }

    private <T> void register(Class<?> componentClass, BeanFactory<?> beanFactory, Integer priority,
            InterceptorContainer<T> interceptorContainer) {
        ResourceInterceptor<T> interceptor = interceptorContainer.create();
        Set<String> nameBindings = setCommonFilterProperties(componentClass, beanFactory, priority,
                interceptor);
        if (interceptorContainer instanceof PreMatchInterceptorContainer
                && componentClass.isAnnotationPresent(PreMatching.class)) {
            ((PreMatchInterceptorContainer<T>) interceptorContainer).addPreMatchInterceptor(interceptor);
        } else if (nameBindings.isEmpty()) {
            interceptorContainer.addGlobalRequestInterceptor(interceptor);
        } else {
            interceptorContainer.addNameRequestInterceptor(interceptor);
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
