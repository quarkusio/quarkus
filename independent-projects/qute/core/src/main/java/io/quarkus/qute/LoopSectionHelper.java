package io.quarkus.qute;

import static io.quarkus.qute.Parameter.EMPTY;

import io.quarkus.qute.Results.Result;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        this.alias = alias.equals(Parameter.EMPTY) ? DEFAULT_ALIAS : alias;
        this.iterable = Objects.requireNonNull(iterable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        return context.resolutionContext().evaluate(iterable).thenCompose(it -> {
            // Ideally, we should not block here but we still need to retain the order of results 
            List<CompletionStage<ResultNode>> results = new ArrayList<>();
            Iterator<?> iterator;
            if (it instanceof Iterable) {
                iterator = ((Iterable<?>) it).iterator();
            } else if (it instanceof Map) {
                iterator = ((Map<?, ?>) it).entrySet().iterator();
            } else if (it instanceof Stream) {
                iterator = ((Stream<?>) it).sequential().iterator();
            } else if (it instanceof Integer) {
                iterator = IntStream.rangeClosed(1, (Integer) it).iterator();
            } else {
                throw new IllegalStateException("Cannot iterate over: " + it);
            }
            int idx = 0;
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

    CompletionStage<ResultNode> nextElement(Object element, int index, boolean hasNext, SectionResolutionContext context) {
        AtomicReference<ResolutionContext> resolutionContextHolder = new AtomicReference<>();
        ResolutionContext child = context.resolutionContext().createChild(new IterationElement(alias, element, index, hasNext),
                null);
        resolutionContextHolder.set(child);
        return context.execute(child);
    }

    public static class Factory implements SectionHelperFactory<LoopSectionHelper> {

        public static final String HINT = "<for-element>";
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
        public Map<String, String> initializeBlock(Map<String, String> outerNameTypeInfos, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                String iterable = block.getParameters().get(ITERABLE);
                if (iterable == null) {
                    iterable = ValueResolvers.THIS;
                }
                Expression iterableExpr = block.addExpression(ITERABLE, iterable);
                String alias = block.getParameters().get(ALIAS);
                if (iterableExpr.typeCheckInfo != null) {
                    alias = alias.equals(Parameter.EMPTY) ? DEFAULT_ALIAS : alias;
                    Map<String, String> typeInfos = new HashMap<String, String>(outerNameTypeInfos);
                    typeInfos.put(alias, iterableExpr.typeCheckInfo + HINT);
                    return typeInfos;
                } else {
                    Map<String, String> typeInfos = new HashMap<String, String>(outerNameTypeInfos);
                    // Make sure we do not try to validate against the parent context
                    typeInfos.put(alias, null);
                    return typeInfos;
                }
            } else {
                return Collections.emptyMap();
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
