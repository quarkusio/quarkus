package io.quarkus.qute;

import static io.quarkus.qute.Futures.evaluateParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Basic {@code set} statement.
 */
public class SetSectionHelper implements SectionHelper {

    private static final String SET = "set";
    private static final String LET = "let";

    // Contains all arguments, incl. the default values
    private final Map<String, Expression> parameters;
    // Contains all default keys, i.e. foo?
    private final Map<String, Expression> defaultKeys;
    // Contains all arguments which are not default keys
    private final Map<String, Expression> overridingKeys;

    SetSectionHelper(Map<String, Expression> parameters, Map<String, Expression> keys) {
        this.parameters = parameters;
        if (keys != null) {
            this.defaultKeys = keys;
            this.overridingKeys = new HashMap<>(parameters);
            overridingKeys.keySet().removeAll(defaultKeys.keySet());
        } else {
            this.defaultKeys = Collections.emptyMap();
            this.overridingKeys = null;
        }
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        CompletableFuture<ResultNode> result = new CompletableFuture<>();
        if (defaultKeys.isEmpty()) {
            evaluateParams(parameters, context.resolutionContext()).whenComplete((r, t) -> {
                if (t != null) {
                    result.completeExceptionally(t);
                } else {
                    // Execute the main block with the params as the current context object
                    context.execute(context.resolutionContext().createChild(Mapper.wrap(r), null)).whenComplete((r2, t2) -> {
                        if (t2 != null) {
                            result.completeExceptionally(t2);
                        } else {
                            result.complete(r2);
                        }
                    });
                }
            });
        } else {
            // First evaluate the keys
            evaluateParams(defaultKeys, context.resolutionContext()).whenComplete((r, t) -> {
                if (t != null) {
                    result.completeExceptionally(t);
                } else {
                    Map<String, Expression> toEval = new HashMap<>();
                    for (Entry<String, Object> e : r.entrySet()) {
                        // Identify the keys for which a value is not set (null or NotFound)
                        if (e.getValue() == null || Results.isNotFound(e.getValue())) {
                            toEval.put(e.getKey(), parameters.get(e.getKey()));
                        }
                    }
                    toEval.putAll(overridingKeys);
                    if (toEval.isEmpty()) {
                        // There is no need to evaluate the default values
                        context.execute(context.resolutionContext()).whenComplete((r2, t2) -> {
                            if (t2 != null) {
                                result.completeExceptionally(t2);
                            } else {
                                result.complete(r2);
                            }
                        });
                    } else {
                        // Evaluate the default values
                        evaluateParams(toEval, context.resolutionContext()).whenComplete((r2, t2) -> {
                            if (t2 != null) {
                                result.completeExceptionally(t2);
                            } else {
                                context.execute(context.resolutionContext().createChild(Mapper.wrap(r2), null))
                                        .whenComplete((r3, t3) -> {
                                            if (t3 != null) {
                                                result.completeExceptionally(t3);
                                            } else {
                                                result.complete(r3);
                                            }
                                        });
                            }
                        });
                    }
                }
            });
        }
        return result;
    }

    public static class Factory implements SectionHelperFactory<SetSectionHelper> {

        public static final String HINT_PREFIX = "<set#";

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(SET, LET);
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.EMPTY;
        }

        @Override
        public SetSectionHelper initialize(SectionInitContext context) {
            Map<String, Expression> params = new HashMap<>();
            Map<String, Expression> keys = null;
            for (Entry<String, String> e : context.getParameters().entrySet()) {
                String key = e.getKey();
                if (key.endsWith("?")) {
                    // foo? -> foo
                    key = key.substring(0, key.length() - 1);
                    if (keys == null) {
                        keys = new HashMap<>();
                    }
                    keys.put(key, context.parseValue(key + "??"));
                }
                params.put(key, context.getExpression(key));
            }
            return new SetSectionHelper(params, keys);
        }

        @Override
        public Scope initializeBlock(Scope previousScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                Scope newScope = new Scope(previousScope);
                for (Entry<String, String> e : block.getParameters().entrySet()) {
                    String key = e.getKey();
                    boolean isDefaultValue = key.endsWith("?");
                    if (isDefaultValue) {
                        // foo? -> foo
                        key = key.substring(0, key.length() - 1);
                    }
                    Expression expr = block.addExpression(key, e.getValue());
                    if (expr.hasTypeInfo()) {
                        // Do not override the binding for a default value
                        boolean add = !isDefaultValue || previousScope.getBinding(key) == null;
                        if (add) {
                            // item.name becomes item<set#1>.name
                            newScope.putBinding(key, key + HINT_PREFIX + expr.getGeneratedId() + ">");
                        }
                    } else {
                        newScope.putBinding(key, null);
                    }
                }
                return newScope;
            } else {
                return previousScope;
            }
        }

    }
}
