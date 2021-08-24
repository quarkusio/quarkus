package org.jboss.resteasy.reactive.server.core;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResteasyReactiveClientProblem;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveAsyncExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveExceptionMapper;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ExceptionMapping {

    private static final Logger log = Logger.getLogger(ExceptionMapping.class);

    private final Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers = new HashMap<>();

    /**
     * Exceptions that indicate an blocking operation was performed on an IO thread.
     * <p>
     * We have a special log message for this.
     */
    private final List<Predicate<Throwable>> blockingProblemPredicates = new ArrayList<>();
    private final List<Predicate<Throwable>> nonBlockingProblemPredicate = new ArrayList<>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void mapException(Throwable throwable, ResteasyReactiveRequestContext context) {
        Class<?> klass = throwable.getClass();
        //we don't thread WebApplicationException's thrown from the client as true 'WebApplicationException'
        //we consider it a security risk to transparently pass on the result to the calling server
        boolean isWebApplicationException = throwable instanceof WebApplicationException
                && !(throwable instanceof ResteasyReactiveClientProblem);
        Response response = null;
        if (isWebApplicationException) {
            response = ((WebApplicationException) throwable).getResponse();
        }
        if (response != null && response.hasEntity()) {
            context.setResult(response);
            return;
        }

        // we match superclasses only if not a WebApplicationException according to spec 3.3.4 Exceptions
        ExceptionMapper exceptionMapper = getExceptionMapper((Class<Throwable>) klass, context);
        if (exceptionMapper != null) {
            context.requireCDIRequestScope();
            if (exceptionMapper instanceof ResteasyReactiveAsyncExceptionMapper) {
                ((ResteasyReactiveAsyncExceptionMapper) exceptionMapper).asyncResponse(throwable,
                        new AsyncExceptionMapperContextImpl(context));
                logBlockingErrorIfRequired(throwable, context);
                logNonBlockingErrorIfRequired(throwable, context);
                return;
            } else if (exceptionMapper instanceof ResteasyReactiveExceptionMapper) {
                response = ((ResteasyReactiveExceptionMapper) exceptionMapper).toResponse(throwable, context);
            } else {
                response = exceptionMapper.toResponse(throwable);
            }
            context.setResult(response);
            logBlockingErrorIfRequired(throwable, context);
            logNonBlockingErrorIfRequired(throwable, context);
            return;
        }
        if (isWebApplicationException) {
            context.setResult(response);
            logBlockingErrorIfRequired(throwable, context);
            return;
        }
        if (throwable instanceof IOException) {
            log.debugf(throwable,
                    "IOError processing HTTP request to %s failed, the client likely terminated the connection.",
                    context.serverRequest().getRequestAbsoluteUri());
        } else if (context.handlesUnmappedException()) {
            log.error("Request failed ", throwable);
        }
        logBlockingErrorIfRequired(throwable, context);
        logNonBlockingErrorIfRequired(throwable, context);
        context.handleUnmappedException(throwable);
    }

    private void logBlockingErrorIfRequired(Throwable throwable, ResteasyReactiveRequestContext context) {
        if (isBlockingProblem(throwable)) {
            log.error("A blocking operation occurred on the IO thread. This likely means you need to annotate "
                    + context.getTarget().getResourceClass().getName() + "#" + context.getTarget().getJavaMethodName() + "("
                    + Arrays.stream(context.getTarget().getParameterTypes()).map(Objects::toString)
                            .collect(Collectors.joining(", "))
                    + ") with @"
                    + Blocking.class.getName()
                    + ". Alternatively you can annotate the class " + context.getTarget().getResourceClass().getName()
                    + " to make every method on the class blocking, or annotate your sub class of the "
                    + Application.class.getName() + " class to make the whole application blocking");
        }
    }

    private void logNonBlockingErrorIfRequired(Throwable throwable, ResteasyReactiveRequestContext context) {
        if (isNonBlockingProblem(throwable)) {
            log.error(
                    "An operation that needed be run on a Vert.x EventLoop thread was run on a worker pool thread. This likely means you need to annotate "
                            + context.getTarget().getResourceClass().getName() + "#" + context.getTarget().getJavaMethodName()
                            + "("
                            + Arrays.stream(context.getTarget().getParameterTypes()).map(Objects::toString)
                                    .collect(Collectors.joining(", "))
                            + ") with @"
                            + NonBlocking.class.getName()
                            + ". Alternatively you can annotate the class " + context.getTarget().getResourceClass().getName()
                            + " to make every method on the class run on a Vert.x EventLoop thread, or annotate your sub class of the "
                            + Application.class.getName() + " class to affect the entire application");
        }
    }

    private boolean isBlockingProblem(Throwable throwable) {
        return isKnownProblem(throwable, blockingProblemPredicates);
    }

    private boolean isNonBlockingProblem(Throwable throwable) {
        return isKnownProblem(throwable, nonBlockingProblemPredicate);
    }

    private boolean isKnownProblem(Throwable throwable, List<Predicate<Throwable>> predicates) {
        Throwable e = throwable;
        while (e != null) {
            for (Predicate<Throwable> predicate : predicates) {
                if (predicate.test(e)) {
                    return true;
                }
            }
            e = e.getCause();
        }
        return false;
    }

    /**
     * Return the proper Exception that handles {@param throwable} or {@code null}
     * if none is found.
     * First checks if the Resource class that contained the Resource method contained class-level exception mappers
     */
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> clazz, ResteasyReactiveRequestContext context) {
        Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> classExceptionMappers = getClassExceptionMappers(
                context);
        if ((classExceptionMappers != null) && !classExceptionMappers.isEmpty()) {
            ExceptionMapper<T> result = doGetExceptionMapper(clazz, classExceptionMappers);
            if (result != null) {
                return result;
            }
        }
        return doGetExceptionMapper(clazz, mappers);
    }

    private Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> getClassExceptionMappers(
            ResteasyReactiveRequestContext context) {
        if (context == null) {
            return null;
        }
        return context.getTarget() != null ? context.getTarget().getClassExceptionMappers() : null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> ExceptionMapper<T> doGetExceptionMapper(Class<T> clazz,
            Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers) {
        Class<?> klass = clazz;
        do {
            ResourceExceptionMapper<? extends Throwable> mapper = mappers.get(klass);
            if (mapper != null) {
                return (ExceptionMapper<T>) mapper.getFactory()
                        .createInstance().getInstance();
            }
            klass = klass.getSuperclass();
        } while (klass != null);

        return null;
    }

    public void addBlockingProblem(Class<? extends Throwable> throwable) {
        blockingProblemPredicates.add(new ExceptionTypePredicate(throwable));
    }

    public void addBlockingProblem(Predicate<Throwable> predicate) {
        blockingProblemPredicates.add(predicate);
    }

    public void addNonBlockingProblem(Class<? extends Throwable> throwable) {
        nonBlockingProblemPredicate.add(new ExceptionTypePredicate(throwable));
    }

    public void addNonBlockingProblem(Predicate<Throwable> predicate) {
        nonBlockingProblemPredicate.add(predicate);
    }

    public <T extends Throwable> void addExceptionMapper(Class<T> exceptionClass, ResourceExceptionMapper<T> mapper) {
        ResourceExceptionMapper<? extends Throwable> existing = mappers.get(exceptionClass);
        if (existing != null) {
            if (existing.getPriority() < mapper.getPriority()) {
                //already a higher priority mapper
                return;
            }
        }
        mappers.put(exceptionClass, mapper);
    }

    public Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> getMappers() {
        return mappers;
    }

    public List<Predicate<Throwable>> getBlockingProblemPredicates() {
        return blockingProblemPredicates;
    }

    public List<Predicate<Throwable>> getNonBlockingProblemPredicate() {
        return nonBlockingProblemPredicate;
    }

    public void initializeDefaultFactories(Function<String, BeanFactory<?>> factoryCreator) {
        for (Map.Entry<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> entry : mappers.entrySet()) {
            if (entry.getValue().getFactory() == null) {
                entry.getValue().setFactory((BeanFactory) factoryCreator.apply(entry.getValue().getClassName()));
            }
        }
    }

    public static class ExceptionTypePredicate implements Predicate<Throwable> {

        private Class<? extends Throwable> throwable;

        // needed for bytecode recording
        public ExceptionTypePredicate() {
        }

        public ExceptionTypePredicate(Class<? extends Throwable> throwable) {
            this.throwable = throwable;
        }

        public Class<? extends Throwable> getThrowable() {
            return throwable;
        }

        public void setThrowable(Class<? extends Throwable> throwable) {
            this.throwable = throwable;
        }

        @Override
        public boolean test(Throwable t) {
            return t.getClass().equals(throwable);
        }
    }

    public static class ExceptionTypeAndMessageContainsPredicate implements Predicate<Throwable> {

        private Class<? extends Throwable> throwable;
        private String messagePart;

        // needed for bytecode recording
        public ExceptionTypeAndMessageContainsPredicate() {
        }

        public ExceptionTypeAndMessageContainsPredicate(Class<? extends Throwable> throwable, String messagePart) {
            this.throwable = throwable;
            this.messagePart = messagePart;
        }

        public Class<? extends Throwable> getThrowable() {
            return throwable;
        }

        public void setThrowable(Class<? extends Throwable> throwable) {
            this.throwable = throwable;
        }

        public String getMessagePart() {
            return messagePart;
        }

        public void setMessagePart(String messagePart) {
            this.messagePart = messagePart;
        }

        @Override
        public boolean test(Throwable t) {
            return t.getClass().equals(throwable) && t.getMessage().contains(messagePart);
        }
    }
}
