package org.jboss.resteasy.reactive.server.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ExceptionMapping {

    final Map<String, ResourceExceptionMapper<? extends Throwable>> mappers = new HashMap<>();

    /**
     * Exceptions that indicate an blocking operation was performed on an IO thread.
     * <p>
     * We have a special log message for this.
     */
    final List<Predicate<Throwable>> blockingProblemPredicates = new ArrayList<>();
    final List<Predicate<Throwable>> nonBlockingProblemPredicate = new ArrayList<>();
    final Set<String> unwrappedExceptions = new HashSet<>();

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

    public void addUnwrappedException(String className) {
        unwrappedExceptions.add(className);
    }

    public Set<String> getUnwrappedExceptions() {
        return unwrappedExceptions;
    }

    public List<Predicate<Throwable>> getBlockingProblemPredicates() {
        return blockingProblemPredicates;
    }

    public List<Predicate<Throwable>> getNonBlockingProblemPredicate() {
        return nonBlockingProblemPredicate;
    }

    public <T extends Throwable> void addExceptionMapper(String exceptionClass, ResourceExceptionMapper<T> mapper) {
        ResourceExceptionMapper<? extends Throwable> existing = mappers.get(exceptionClass);
        if (existing != null) {
            if (existing.getPriority() < mapper.getPriority()) {
                //already a higher priority mapper
                return;
            }
        }
        mappers.put(exceptionClass, mapper);
    }

    public void initializeDefaultFactories(Function<String, BeanFactory<?>> factoryCreator) {
        for (Map.Entry<String, ResourceExceptionMapper<? extends Throwable>> entry : mappers.entrySet()) {
            if (entry.getValue().getFactory() == null) {
                entry.getValue().setFactory((BeanFactory) factoryCreator.apply(entry.getValue().getClassName()));
            }
        }
    }

    public Map<String, ResourceExceptionMapper<? extends Throwable>> getMappers() {
        return mappers;
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
