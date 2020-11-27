package io.quarkus.qute;

import static io.quarkus.qute.Parameter.EMPTY;

import io.quarkus.qute.Results.Result;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
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
                        "Loop section error in template %s on line %s: [%s] resolved to [null] which is not iterable",
                        iterable.getOrigin().getTemplateId(), iterable.getOrigin().getLine(), iterable.toOriginalString()));
            }
            List<CompletionStage<ResultNode>> results = new ArrayList<>();
            Iterator<?> iterator = extractIterator(it);
            int idx = 0;
            // Ideally, we should not block here but we still need to retain the order of results
            while (iterator.hasNext()) {
                results.add(nextElement(iterator.next(), idx++, iterator.hasNext(), context));
            }
            if (results.isEmpty()) {
                return CompletableFuture.completedFuture(ResultNode.NOOP);
            }
            CompletableFuture<ResultNode> result = new CompletableFuture<>();
            CompletableFuture<ResultNode>[] all = new CompletableFuture[results.size()];
            idx = 0;
            for (CompletionStage<ResultNode> r : results) {
                all[idx++] = r.toCompletableFuture();
            }
            CompletableFuture
                    .allOf(all)
                    .whenComplete((v, t) -> {
                        if (t != null) {
                            result.completeExceptionally(t);
                        } else {
                            result.complete(new MultiResultNode(all));
                        }
                    });
            return result;
        });
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
            return Arrays.stream((Object[]) it).iterator();
        } else {
            throw new TemplateException(String.format(
                    "Loop section error in template %s on line %s: [%s] resolved to [%s] which is not iterable",
                    iterable.getOrigin().getTemplateId(), iterable.getOrigin().getLine(), iterable.toOriginalString(),
                    it.getClass().getName()));
        }
    }

    CompletionStage<ResultNode> nextElement(Object element, int index, boolean hasNext, SectionResolutionContext context) {
        AtomicReference<ResolutionContext> resolutionContextHolder = new AtomicReference<>();
        ResolutionContext child = context.resolutionContext().createChild(new IterationElement(alias, element, index, hasNext),
                null, null);
        resolutionContextHolder.set(child);
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
                // foo.items
                // > |org.acme.Foo|.items<loop-element>
                previousScope.setLastPartHint(HINT_ELEMENT);
                Expression iterableExpr = block.addExpression(ITERABLE, iterable);
                previousScope.setLastPartHint(null);

                String alias = block.getParameters().get(ALIAS);

                if (iterableExpr.hasTypeInfo()) {
                    // it.name
                    // > it<loop#123>.name
                    alias = alias.equals(Parameter.EMPTY) ? DEFAULT_ALIAS : alias;
                    Scope newScope = new Scope(previousScope);
                    newScope.putBinding(alias, alias + HINT_PREFIX + iterableExpr.getGeneratedId() + ">");
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

        final String alias;
        final Object element;
        final int index;
        final boolean hasNext;

        public IterationElement(String alias, Object element, int index, boolean hasNext) {
            this.alias = alias;
            this.element = element;
            this.index = index;
            this.hasNext = hasNext;
        }

        @Override
        public Object get(String key) {
            if (alias.equals(key)) {
                return element;
            }
            // Iteration metadata
            switch (key) {
                case "count":
                    return index + 1;
                case "index":
                    return index;
                case "indexParity":
                    return index % 2 != 0 ? "even" : "odd";
                case "hasNext":
                    return hasNext;
                case "isOdd":
                case "odd":
                    return index % 2 == 0;
                case "isEven":
                case "even":
                    return index % 2 != 0;
                default:
                    return Result.NOT_FOUND;
            }
        }
    }

}
