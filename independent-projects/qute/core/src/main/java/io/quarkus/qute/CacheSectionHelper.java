package io.quarkus.qute;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This section can be used to cache parts of the template.
 */
public class CacheSectionHelper implements SectionHelper {

    private final String keyPrefix;
    private final Expression key;
    private final Cache cache;

    CacheSectionHelper(String keyPrefix, Expression key, Cache cache) {
        this.keyPrefix = keyPrefix;
        this.key = key;
        this.cache = cache;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        if (key == null) {
            return resolveInternal(keyPrefix, context);
        }
        return context.resolutionContext().evaluate(key)
                .thenCompose(key -> {
                    return resolveInternal(keyPrefix + key.toString(), context);
                });
    }

    private CompletionStage<ResultNode> resolveInternal(String key, SectionResolutionContext context) {
        return cache.getValue(key, new Function<String, CompletionStage<ResultNode>>() {
            @Override
            public CompletionStage<ResultNode> apply(String key) {
                return context.execute().thenCompose(rn -> {
                    // Note that we cannot cache the ResultNode but the string representation instead
                    StringBuilder sb = new StringBuilder();
                    rn.process(sb::append);
                    String result = sb.toString();
                    return CompletedStage.of(new ResultNode() {

                        @Override
                        public void process(Consumer<String> resultConsumer) {
                            resultConsumer.accept(result);
                        }
                    });
                });
            }
        });
    }

    public static class Factory implements SectionHelperFactory<CacheSectionHelper> {

        static final String KEY = "key";
        static final String DEFAULT_KEY = "$default$";

        private final Cache cache;

        public Factory(Cache cache) {
            this.cache = cache;
        }

        @Override
        public List<String> getDefaultAliases() {
            return List.of("cached");
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder().addParameter(KEY, DEFAULT_KEY).build();
        }

        @Override
        public CacheSectionHelper initialize(SectionInitContext context) {
            StringBuilder keyPrefix = new StringBuilder().append(context.getOrigin().getTemplateId())
                    .append(":")
                    .append(context.getOrigin().getLine())
                    .append(":")
                    .append(context.getOrigin().getLineCharacterStart())
                    .append("_");
            Expression key = null;
            if (!context.getParameter(KEY).equals(DEFAULT_KEY)) {
                key = context.getExpression(KEY);
            }
            return new CacheSectionHelper(keyPrefix.toString(), key, cache);
        }

        @Override
        public Scope initializeBlock(Scope previousScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                String key = block.getParameters().get(KEY);
                if (!key.equals(DEFAULT_KEY)) {
                    block.addExpression(KEY, key);
                }
                return previousScope;
            } else {
                return previousScope;
            }
        }

    }

    /**
     * A cache abstraction. An implementation must be thread-safe.
     */
    public interface Cache {

        /**
         *
         * @param key The key for the cached part of the template
         * @param loader The loader that can be used to load the cached value
         * @return the cached result
         */
        CompletionStage<ResultNode> getValue(String key, Function<String, CompletionStage<ResultNode>> loader);

    }

}
