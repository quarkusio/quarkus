package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.resteasy.util.MediaTypeHelper;

import io.quarkus.hibernate.validator.runtime.interceptor.AbstractMethodValidationInterceptor;

@JaxrsEndPointValidated
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER + 800)
public class JaxrsEndPointValidationInterceptor extends AbstractMethodValidationInterceptor {

    private static final Logger LOGGER = Logger.getLogger(JaxrsEndPointValidationInterceptor.class);

    @AroundInvoke
    @Override
    public Object validateMethodInvocation(InvocationContext ctx) throws Exception {
        try {
            return super.validateMethodInvocation(ctx);
        } catch (ConstraintViolationException e) {
            throw new ResteasyViolationExceptionImpl(e.getConstraintViolations(), getAccept(ctx.getMethod()));
        }
    }

    @AroundConstruct
    @Override
    public void validateConstructorInvocation(InvocationContext ctx) throws Exception {
        super.validateConstructorInvocation(ctx);
    }

    private List<MediaType> getAccept(Method method) {
        MediaType[] producedMediaTypes = MediaTypeHelper.getProduces(method.getDeclaringClass(), method);

        if (producedMediaTypes == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(producedMediaTypes);
    }
}
