package io.quarkus.qute;

import static io.quarkus.qute.Parameter.EMPTY;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Basic sequential {@code loop} statement.
 */
public class LoopSectionHelper implements SectionHelper {

    private static final String DEFAULT_ALIAS = "it";

    private final String alias;
    private final Expression iterable;

    LoopSectionHelper(String alias, Expression iterable) {
        this.alias = Parameter.EMPTY.equals(alias) ? DEFAULT_ALIAS : alias;
        this.iterable = Objects.requireNonNull(iterable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        return context.resolutionContext().evaluate(iterable).thenCompose(it -> {
            if (it == null) {
                throw new TemplateException(String.format(
                        "Iteration error in template [%s] on line %s: {%s} resolved to null, use {%<s.orEmpty} to ignore this error",
                        iterable.getOrigin().getTemplateId(), iterable.getOrigin().getLine(), iterable.toOriginalString()));
            }
            // Try to extract the capacity for collections, maps and arrays to avoid resize
            List<CompletionStage<ResultNode>> results = new ArrayList<>(extractSize(it));
            Iterator<?> iterator = extractIterator(it);
            int idx = 0;
            // Ideally, we should not block here but we still need to retain the order of results
            while (iterator.hasNext()) {
                results.add(nextElement(iterator.next(), idx++, iterator.hasNext(), context));
            }
            if (results.isEmpty()) {
                return ResultNode.NOOP;
            }
            if (results.size() == 1) {
                return results.get(0);
            }
            CompletableFuture<ResultNode> result = new CompletableFuture<>();
            CompletableFuture<ResultNode>[] allResults = new CompletableFuture[results.size()];
            idx = 0;
            for (CompletionStage<ResultNode> r : results) {
                allResults[idx++] = r.toCompletableFuture();
            }
            CompletableFuture
                    .allOf(allResults)
                    .whenComplete((v, t) -> {
                        if (t != null) {
                            result.completeExceptionally(t);
                        } else {
                            result.complete(new MultiResultNode(allResults));
                        }
                    });
            return result;
        });
    }

    private static int extractSize(Object it) {
        if (it instanceof Collection) {
            return ((Collection<?>) it).size();
        } else if (it instanceof Map) {
            return ((Map<?, ?>) it).size();
        } else if (it.getClass().isArray()) {
            return Array.getLength(it);
        } else if (it instanceof Integer) {
            return ((Integer) it);
        }
        return 10;
    }

    private Iterator<?> extractIterator(Object it) {
        if (it instanceof Iterable) {
            return ((Iterable<?>) it).iterator();
        } else if (it instanceof Iterator) {
            return (Iterator<?>) it;
        } else if (it instanceof Map) {
            return ((Map<?, ?>) it).entrySet().iterator();
        } else if (it instanceof Stream) {
            return ((Stream<?>) it).sequential().iterator();
        } else if (it instanceof Integer) {
            return IntStream.rangeClosed(1, (Integer) it).iterator();
        } else if (it.getClass().isArray()) {
            int length = Array.getLength(it);
            List<Object> elements = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                // The val is automatically wrapped for primitive types
                elements.add(Array.get(it, i));
            }
            return elements.iterator();
        } else {
            String msg;
            if (Results.isNotFound(it)) {
                msg = String.format(
                        "Iteration error in template [%s] on line %s: {%s} not found, use {%<s.orEmpty} to ignore this error",
                        iterable.getOrigin().getTemplateId(), iterable.getOrigin().getLine(), iterable.toOriginalString());
            } else {
                msg = String.format(
                        "Iteration error in template [%s] on line %s: {%s} resolved to [%s] which is not iterable",
                        iterable.getOrigin().getTemplateId(), iterable.getOrigin().getLine(), iterable.toOriginalString(),
                        it.getClass().getName());
            }
            throw new TemplateException(msg);
        }
    }

    CompletionStage<ResultNode> nextElement(Object element, int index, boolean hasNext, SectionResolutionContext context) {
        ResolutionContext child = context.resolutionContext().createChild(new IterationElement(alias, element, index, hasNext),
                null);
        return context.execute(child);
    }

    public static class Factory implements SectionHelperFactory<LoopSectionHelper> {

        public static final String HINT_ELEMENT = "<loop-element>";
        public static final String HINT_PREFIX = "<loop#";
        private static final String ALIAS = "alias";
        private static final String IN = "in";
        private static final String ITERABLE = "iterable";

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of("for", "each");
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder()
                    .addParameter(ALIAS, EMPTY)
                    .addParameter(IN, EMPTY)
                    .addParameter(new Parameter(ITERABLE, null, true))
                    .build();
        }

        @Override
        public LoopSectionHelper initialize(SectionInitContext context) {
            return new LoopSectionHelper(context.getParameter(ALIAS), context.getExpression(ITERABLE));
        }

        @Override
        public Scope initializeBlock(Scope previousScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                String iterable = block.getParameters().get(ITERABLE);
                if (iterable == null) {
                    iterable = ValueResolvers.THIS;
                }
                // foo.items becomes |org.acme.Foo|.items<loop-element>
                previousScope.setLastPartHint(HINT_ELEMENT);
                Expression iterableExpr = block.addExpression(ITERABLE, iterable);
                previousScope.setLastPartHint(null);

                String alias = block.getParameters().get(ALIAS);

                if (iterableExpr.hasTypeInfo()) {
                    // it.name becomes it<loop#123>.name
                    alias = alias.equals(Parameter.EMPTY) ? DEFAULT_ALIAS : alias;
                    Scope newScope = new Scope(previousScope);
                    newScope.putBinding(alias, alias + HINT_PREFIX + iterableExpr.getGeneratedId() + ">");
                    // Put bindings for iteration metadata
                    newScope.putBinding("count", Expressions.typeInfoFrom(Integer.class.getName()));
                    newScope.putBinding("index", Expressions.typeInfoFrom(Integer.class.getName()));
                    newScope.putBinding("indexParity", Expressions.typeInfoFrom(String.class.getName()));
                    newScope.putBinding("hasNext", Expressions.typeInfoFrom(Boolean.class.getName()));
                    newScope.putBinding("odd", Expressions.typeInfoFrom(Boolean.class.getName()));
                    newScope.putBinding("isOdd", Expressions.typeInfoFrom(Boolean.class.getName()));
                    newScope.putBinding("even", Expressions.typeInfoFrom(Boolean.class.getName()));
                    newScope.putBinding("isEven", Expressions.typeInfoFrom(Boolean.class.getName()));
                    return newScope;
                } else {
                    // Make sure we do not try to validate against the parent context
                    Scope newScope = new Scope(previousScope);
                    newScope.putBinding(alias, null);
                    return newScope;
                }
            } else {
                return previousScope;
            }
        }
    }

    static class IterationElement implements Mapper {

        static final CompletableFuture<Object> EVEN = CompletableFuture.completedFuture("even");
        static final CompletableFuture<Object> ODD = CompletableFuture.completedFuture("odd");

        final String alias;
        final CompletableFuture<Object> element;
        final int index;
        final boolean hasNext;

        public IterationElement(String alias, Object element, int index, boolean hasNext) {
            this.alias = alias;
            this.element = CompletableFuture.completedFuture(element);
            this.index = index;
            this.hasNext = hasNext;
        }

        @Override
        public CompletionStage<Object> getAsync(String key) {
            if (alias.equals(key)) {
                return element;
            }
            // Iteration metadata
            switch (key) {
                case "count":
                    return CompletableFuture.completedFuture(index + 1);
                case "index":
                    return CompletableFuture.completedFuture(index);
                case "indexParity":
                    return index % 2 != 0 ? EVEN : ODD;
                case "hasNext":
                    return hasNext ? Results.TRUE : Results.FALSE;
                case "isOdd":
                case "odd":
                    return (index % 2 == 0) ? Results.TRUE : Results.FALSE;
                case "isEven":
                case "even":
                    return (index % 2 != 0) ? Results.TRUE : Results.FALSE;
                default:
                    return Results.notFound(key);
            }
        }

    }

}
