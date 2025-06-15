package org.jboss.resteasy.reactive.server.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.spi.BeanFactory;

@SuppressWarnings({ "unchecked", "unused" })
public class ExceptionMapping {

    /**
     * The idea behind having two different maps is the following: Under normal circumstances, mappers are added to the
     * first map, and we don't need to track multiple mappings because the priority is enough to distinguish. However,
     * in the case where ExceptionMapper classes may not be active at runtime (due to the presence of conditional bean
     * annotations), then we need to track all the possible mappings and at runtime determine the one with the lowest
     * priority value that is active.
     */
    final Map<String, ResourceExceptionMapper<? extends Throwable>> mappers = new HashMap<>();
    // this is going to be used when there are mappers that are removable at runtime
    final Map<String, List<ResourceExceptionMapper<? extends Throwable>>> runtimeCheckMappers = new HashMap<>();

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
                // we already have a lower priority mapper registered
                return;
            } else {
                mappers.remove(exceptionClass);
                List<ResourceExceptionMapper<?>> list = new ArrayList<>(2);
                list.add(mapper);
                list.add(existing);
                runtimeCheckMappers.put(exceptionClass, list);
            }
        } else {
            var list = runtimeCheckMappers.get(exceptionClass);
            if (list == null) {
                if (mapper.getDiscardAtRuntime() == null) {
                    mappers.put(exceptionClass, mapper);
                } else {
                    list = new ArrayList<>(1);
                    list.add(mapper);
                    runtimeCheckMappers.put(exceptionClass, list);
                }
            } else {
                list.add(mapper);
                Collections.sort(list);
            }
        }
    }

    public void initializeDefaultFactories(Function<String, BeanFactory<?>> factoryCreator) {
        for (var resourceExceptionMapper : mappers.values()) {
            if (resourceExceptionMapper.getFactory() == null) {
                resourceExceptionMapper
                        .setFactory((BeanFactory) factoryCreator.apply(resourceExceptionMapper.getClassName()));
            }
        }
        for (var list : runtimeCheckMappers.values()) {
            for (var resourceExceptionMapper : list) {
                if (resourceExceptionMapper.getFactory() == null) {
                    resourceExceptionMapper
                            .setFactory((BeanFactory) factoryCreator.apply(resourceExceptionMapper.getClassName()));
                }
            }
        }
    }

    public Map<String, ResourceExceptionMapper<? extends Throwable>> getMappers() {
        return mappers;
    }

    public Map<String, List<ResourceExceptionMapper<? extends Throwable>>> getRuntimeCheckMappers() {
        return runtimeCheckMappers;
    }

    public Map<String, ResourceExceptionMapper<? extends Throwable>> effectiveMappers() {
        if (runtimeCheckMappers.isEmpty()) {
            return mappers;
        }
        Map<String, ResourceExceptionMapper<? extends Throwable>> result = new HashMap<>();
        for (var entry : runtimeCheckMappers.entrySet()) {
            String exceptionClass = entry.getKey();
            var list = entry.getValue();
            for (var resourceExceptionMapper : list) {
                if (resourceExceptionMapper.getDiscardAtRuntime() == null) {
                    result.put(exceptionClass, resourceExceptionMapper);
                    break;
                } else {
                    if (!resourceExceptionMapper.getDiscardAtRuntime().get()) {
                        result.put(exceptionClass, resourceExceptionMapper);
                        break;
                    }
                }
            }
        }
        result.putAll(mappers);
        return result;
    }

    public void replaceDiscardAtRuntimeIfBeanIsUnavailable(Function<String, Supplier<Boolean>> function) {
        if (runtimeCheckMappers.isEmpty()) {
            return;
        }
        for (var list : runtimeCheckMappers.values()) {
            for (var resourceExceptionMapper : list) {
                if (resourceExceptionMapper
                        .getDiscardAtRuntime() instanceof ResourceExceptionMapper.DiscardAtRuntimeIfBeanIsUnavailable) {
                    var discardAtRuntimeIfBeanIsUnavailable = (ResourceExceptionMapper.DiscardAtRuntimeIfBeanIsUnavailable) resourceExceptionMapper
                            .getDiscardAtRuntime();
                    resourceExceptionMapper
                            .setDiscardAtRuntime(function.apply(discardAtRuntimeIfBeanIsUnavailable.getBeanClass()));
                }
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
