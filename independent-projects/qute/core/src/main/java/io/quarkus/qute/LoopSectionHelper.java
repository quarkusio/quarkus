package io.quarkus.qute;

import static io.quarkus.qute.Parameter.EMPTY;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import io.quarkus.qute.SectionHelperFactory.SectionInitContext;

/**
 * Basic sequential {@code loop} statement.
 */
public class LoopSectionHelper implements SectionHelper {

    private static final String DEFAULT_ALIAS = "it";
    private static final String ELSE = "else";
    private static final String ALIAS = "alias";
    private static final String ITERABLE = "iterable";

    private final String alias;
    private final Expression iterable;
    private final SectionBlock elseBlock;
    private final Engine engine;

    private final String metadataPrefix;

    LoopSectionHelper(SectionInitContext context, String metadataPrefix) {
        this.alias = context.getParameterOrDefault(ALIAS, DEFAULT_ALIAS);
        this.metadataPrefix = LoopSectionHelper.Factory.prefixValue(alias, metadataPrefix);
        this.iterable = Objects.requireNonNull(context.getExpression(ITERABLE));
        this.elseBlock = context.getBlock(ELSE);
        this.engine = context.getEngine();
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        return context.resolutionContext().evaluate(iterable).thenCompose(it -> {
            if (it == null) {
                // Treat null as no-op, as it is handled by SingleResultNode
                return ResultNode.NOOP;
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
                // Execute the {#else} block if present
                if (elseBlock != null) {
                    return context.execute(elseBlock, context.resolutionContext());
                } else {
                    return ResultNode.NOOP;
                }
            }
            if (results.size() == 1) {
                return results.get(0);
            }
            return Results.process(results);
        });
    }

    private static int extractSize(Object it) {
        // Note that we intentionally use "instanceof" to test interfaces as the last resort in order to mitigate the "type pollution"
        // See https://github.com/RedHatPerf/type-pollution-agent for more information
        if (it instanceof AbstractCollection<?> collection) {
            return collection.size();
        } else if (it instanceof AbstractMap<?, ?> map) {
            return map.size();
        } else if (it.getClass().isArray()) {
            return Array.getLength(it);
        } else if (it instanceof Integer integer) {
            return integer;
        } else if (it instanceof Long longValue) {
            return longValue.intValue();
        } else if (it instanceof Collection<?> collection) {
            return collection.size();
        } else if (it instanceof Map<?, ?> map) {
            return map.size();
        }
        return 10;
    }

    private Iterator<?> extractIterator(Object it) {
        // Note that we intentionally use "instanceof" to test interfaces as the last resort in order to mitigate the "type pollution"
        // See https://github.com/RedHatPerf/type-pollution-agent for more information
        if (it instanceof AbstractCollection<?> col) {
            return col.iterator();
        } else if (it instanceof AbstractMap<?, ?> map) {
            return map.entrySet().iterator();
        } else if (it instanceof Integer integer) {
            return IntStream.rangeClosed(1, integer).iterator();
        } else if (it instanceof Long longValue) {
            return LongStream.rangeClosed(1, longValue).iterator();
        } else if (it.getClass().isArray()) {
            int length = Array.getLength(it);
            List<Object> elements = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                // The val is automatically wrapped for primitive types
                elements.add(Array.get(it, i));
            }
            return elements.iterator();
        } else if (it instanceof Iterable<?> iterable) {
            return iterable.iterator();
        } else if (it instanceof Iterator<?> iterator) {
            return iterator;
        } else if (it instanceof Map<?, ?> map) {
            return map.entrySet().iterator();
        } else if (it instanceof Stream<?> stream) {
            return stream.sequential().iterator();
        } else {
            TemplateException.Builder builder;
            if (Results.isNotFound(it)) {
                builder = engine.error("Iteration error - \\{{expr}} not found, use \\{{expr}.orEmpty} to ignore this error")
                        .code(Code.ITERABLE_NOT_FOUND)
                        .argument("expr", iterable.toOriginalString())
                        .origin(iterable.getOrigin());
            } else {
                builder = engine.error("Iteration error - \\{{expr}} resolved to [{clazz}] which is not iterable")
                        .code(Code.NOT_AN_ITERABLE)
                        .argument("expr", iterable.toOriginalString())
                        .argument("clazz", it.getClass().getName())
                        .origin(iterable.getOrigin());
            }
            throw builder.build();
        }
    }

    CompletionStage<ResultNode> nextElement(Object element, int index, boolean hasNext, SectionResolutionContext context) {
        ResolutionContext child = context.resolutionContext().createChild(
                new IterationElement(element, index, hasNext),
                null);
        return context.execute(child);
    }

    public static class Factory implements SectionHelperFactory<LoopSectionHelper> {

        /**
         * Constant value for iteration metadata prefix indicating that the alias suffixed with a question mark should be used.
         */
        public static final String ITERATION_METADATA_PREFIX_ALIAS_QM = "<alias?>";

        /**
         * Constant value for iteration metadata prefix indicating that the alias suffixed with an underscore should be used.
         */
        public static final String ITERATION_METADATA_PREFIX_ALIAS_UNDERSCORE = "<alias_>";

        /**
         * Constant value for iteration metadata prefix indicating that no prefix should be used.
         */
        public static final String ITERATION_METADATA_PREFIX_NONE = "<none>";

        public static final String HINT_ELEMENT = "<loop-element>";
        public static final String HINT_PREFIX = "<loop#";
        private static final String IN = "in";

        private final String metadataPrefix;

        public Factory() {
            this(ITERATION_METADATA_PREFIX_ALIAS_UNDERSCORE);
        }

        public Factory(String metadataPrefix) {
            Objects.requireNonNull(metadataPrefix, "Iteration metadata must not be null");
            if (metadataPrefix.isBlank() || metadataPrefix.equals(ITERATION_METADATA_PREFIX_NONE)) {
                this.metadataPrefix = null;
            } else {
                this.metadataPrefix = metadataPrefix;
            }
        }

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of("for", "each");
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder()
                    .addParameter(ALIAS, EMPTY)
                    .addParameter(IN, EMPTY)
                    .addParameter(Parameter.builder(ITERABLE).optional())
                    .build();
        }

        public List<String> getBlockLabels() {
            return Collections.singletonList(ELSE);
        }

        @Override
        public LoopSectionHelper initialize(SectionInitContext context) {
            return new LoopSectionHelper(context, metadataPrefix);
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
                    String prefix = prefixValue(alias, metadataPrefix);
                    putMetadataBinding(newScope, prefix, "count", Integer.class.getName());
                    putMetadataBinding(newScope, prefix, "index", Integer.class.getName());
                    putMetadataBinding(newScope, prefix, "indexParity", String.class.getName());
                    putMetadataBinding(newScope, prefix, "hasNext", Boolean.class.getName());
                    putMetadataBinding(newScope, prefix, "isLast", Boolean.class.getName());
                    putMetadataBinding(newScope, prefix, "isFirst", Boolean.class.getName());
                    putMetadataBinding(newScope, prefix, "odd", Boolean.class.getName());
                    putMetadataBinding(newScope, prefix, "isOdd", Boolean.class.getName());
                    putMetadataBinding(newScope, prefix, "even", Boolean.class.getName());
                    putMetadataBinding(newScope, prefix, "isEven", Boolean.class.getName());
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

        private void putMetadataBinding(Scope scope, String prefix, String name, String typeName) {
            scope.putBinding(prefix != null ? prefix + name : name, Expressions.typeInfoFrom(typeName) + HINT_METADATA);
        }

        static String prefixValue(String alias, String metadataPrefix) {
            if (metadataPrefix == null || ITERATION_METADATA_PREFIX_NONE.equals(metadataPrefix)) {
                return null;
            } else if (ITERATION_METADATA_PREFIX_ALIAS_UNDERSCORE.equals(metadataPrefix)) {
                return alias + "_";
            } else if (ITERATION_METADATA_PREFIX_ALIAS_QM.equals(metadataPrefix)) {
                return alias + "?";
            } else {
                return metadataPrefix;
            }
        }
    }

    class IterationElement implements Mapper {

        static final CompletedStage<Object> EVEN = CompletedStage.of("even");
        static final CompletedStage<Object> ODD = CompletedStage.of("odd");

        final CompletedStage<Object> element;
        final int index;
        final boolean hasNext;

        public IterationElement(Object element, int index, boolean hasNext) {
            this.element = CompletedStage.of(element);
            this.index = index;
            this.hasNext = hasNext;
        }

        @Override
        public CompletionStage<Object> getAsync(String key) {
            if (alias.equals(key)) {
                return element;
            }
            if (metadataPrefix != null && !key.startsWith(metadataPrefix)) {
                return Results.notFound(key);
            }
            int keyStartIndex = metadataPrefix == null ? 0 : metadataPrefix.length();
            // Iteration metadata
            switch (key.length() - keyStartIndex) {
                case 3:
                    if (key.indexOf("odd", keyStartIndex) == keyStartIndex) {
                        return (index + 1) % 2 != 0 ? Results.TRUE : Results.FALSE;
                    }
                    return Results.notFound(key);
                case 4:
                    if (key.indexOf("even", keyStartIndex) == keyStartIndex) {
                        return (index + 1) % 2 == 0 ? Results.TRUE : Results.FALSE;
                    }
                    return Results.notFound(key);
                case 5:
                    if (key.indexOf("count", keyStartIndex) == keyStartIndex) {
                        return CompletedStage.of(index + 1);
                    }
                    if (key.indexOf("index", keyStartIndex) == keyStartIndex) {
                        return CompletedStage.of(index);
                    }
                    if (key.indexOf("isOdd", keyStartIndex) == keyStartIndex) {
                        return (index + 1) % 2 != 0 ? Results.TRUE : Results.FALSE;
                    }
                    return Results.notFound(key);
                case 6:
                    if (key.indexOf("isEven", keyStartIndex) == keyStartIndex) {
                        return (index + 1) % 2 == 0 ? Results.TRUE : Results.FALSE;
                    }
                    if (key.indexOf("isLast", keyStartIndex) == keyStartIndex) {
                        return hasNext ? Results.FALSE : Results.TRUE;
                    }
                    return Results.notFound(key);
                case 7:
                    if (key.indexOf("hasNext", keyStartIndex) == keyStartIndex) {
                        return hasNext ? Results.TRUE : Results.FALSE;
                    }
                    if (key.indexOf("isFirst", keyStartIndex) == keyStartIndex) {
                        return index == 0 ? Results.TRUE : Results.FALSE;
                    }
                    return Results.notFound(key);
                case 11:
                    if (key.indexOf("indexParity", keyStartIndex) == keyStartIndex) {
                        return (index + 1) % 2 == 0 ? EVEN : ODD;
                    }
                    return Results.notFound(key);
                default:
                    return Results.notFound(key);
            }
        }

        @Override
        public Set<String> mappedKeys() {
            if (metadataPrefix != null) {
                return Set.of(alias, metadataPrefix + "count", metadataPrefix + "index", metadataPrefix + "indexParity",
                        metadataPrefix + "hasNext", metadataPrefix + "isLast", metadataPrefix + "isFirst",
                        metadataPrefix + "isOdd", metadataPrefix + "odd", metadataPrefix + "isEven", metadataPrefix + "even");
            } else {
                return Set.of(alias, "count", "index", "indexParity", "hasNext", "isLast", "isFirst", "isOdd",
                        "odd", "isEven", "even");
            }
        }

    }

    enum Code implements ErrorCode {

        ITERABLE_NOT_FOUND,
        NOT_AN_ITERABLE,
        ;

        @Override
        public String getName() {
            return "LOOP_" + name();
        }

    }
}
