package io.quarkus.qute;

import io.quarkus.qute.IfSectionHelper.Operator;
import io.quarkus.qute.SectionHelperFactory.SectionInitContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Basic {@code when/switch} statement.
 */
public class WhenSectionHelper implements SectionHelper {

    private static final String VALUE = "value";
    private static final String WHEN = "when";
    private static final String SWITCH = "switch";
    private static final String IS = "is";
    private static final String CASE = "case";
    private static final String ELSE = "else";

    private final Expression value;
    private final List<CaseBlock> caseBlocks;

    WhenSectionHelper(SectionInitContext context) {
        this.value = context.getExpression(VALUE);
        ImmutableList.Builder<CaseBlock> builder = ImmutableList.builder();
        for (SectionBlock block : context.getBlocks()) {
            if (!SectionHelperFactory.MAIN_BLOCK_NAME.equals(block.label)) {
                switch (block.label) {
                    case IS:
                    case CASE:
                    case ELSE:
                        builder.add(new CaseBlock(block, context));
                        break;
                    case SectionHelperFactory.MAIN_BLOCK_NAME:
                        break;
                    default:
                        // This should never happen
                        throw new TemplateException("Invalid block: " + block);
                }

            }
        }
        this.caseBlocks = builder.build();
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        return context.resolutionContext().evaluate(value)
                .thenCompose(value -> {
                    if (value != null && value.getClass().isEnum()) {
                        return resolveEnumCaseBlocks(context, value, caseBlocks);
                    }
                    return resolveCaseBlocks(context, value, caseBlocks.iterator());
                });
    }

    CompletionStage<ResultNode> resolveCaseBlocks(SectionResolutionContext context, Object value,
            Iterator<CaseBlock> caseBlocks) {
        CaseBlock caseBlock = caseBlocks.next();
        return caseBlock.resolve(context, value).thenCompose(r -> {
            if (r) {
                return context.execute(caseBlock.block, context.resolutionContext());
            } else {
                if (caseBlocks.hasNext()) {
                    return resolveCaseBlocks(context, value, caseBlocks);
                } else {
                    return ResultNode.NOOP;
                }
            }
        });
    }

    CompletionStage<ResultNode> resolveEnumCaseBlocks(SectionResolutionContext context, Object value,
            List<CaseBlock> caseBlocks) {
        for (CaseBlock caseBlock : caseBlocks) {
            if (caseBlock.resolveEnum(context, value)) {
                return context.execute(caseBlock.block, context.resolutionContext());
            }
        }
        return ResultNode.NOOP;
    }

    public static class Factory implements SectionHelperFactory<WhenSectionHelper> {

        public static final String HINT_PREFIX = "<when#";
        private static final String VALUE_EXPR_ID = "<<value-expr-id>>";

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(WHEN, SWITCH);
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder().addParameter(VALUE).build();
        }

        @Override
        public WhenSectionHelper initialize(SectionInitContext context) {
            return new WhenSectionHelper(context);
        }

        @Override
        public List<String> getBlockLabels() {
            return ImmutableList.of(IS, CASE, ELSE);
        }

        @Override
        public Scope initializeBlock(Scope previousScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                String value = block.getParameters().get(VALUE);
                if (value == null) {
                    throw new IllegalStateException("Value param not present");
                }
                Expression valueExpr = block.addExpression(VALUE, value);
                if (valueExpr.hasTypeInfo()) {
                    // If type info is available we do add the expression id 
                    previousScope.putAttribute(VALUE_EXPR_ID, valueExpr.getGeneratedId());
                }
            } else if (ELSE.equals(block.getLabel())) {
                // No special handling required for "else"
            } else if (IS.equals(block.getLabel()) || CASE.equals(block.getLabel())) {
                Object valueExprId = previousScope.getAttribute(VALUE_EXPR_ID);
                int added = 0;
                Iterator<String> it = block.getParameters().values().iterator();
                while (it.hasNext()) {
                    String param = it.next();
                    if (added == 0 && it.hasNext()) {
                        // Skip the operator param
                        continue;
                    }
                    added++;
                    if (valueExprId != null) {
                        // This could be an enum switch - we need to add a hint in order to validate the enum constants properly 
                        String previousBinding = previousScope.getBinding(param);
                        String newBinding = previousBinding;
                        if (newBinding == null) {
                            newBinding = param;
                        }
                        // Append hint to the existing binding if needed
                        // E.g. ON -> ON<when:12345>
                        newBinding += HINT_PREFIX + valueExprId + ">";
                        previousScope.putBinding(param, newBinding);
                        block.addExpression(param, param);
                        previousScope.putBinding(param, previousBinding);
                    } else {
                        block.addExpression(param, param);
                    }
                }
            } else {
                throw block.createParserError("Invalid case block used in a {#when} section: " + block.getLabel());
            }
            return previousScope;
        }

    }

    enum CaseOperator {

        EQ(new Predicate<Integer>() {
            @Override
            public boolean test(Integer params) {
                return params == 1;
            }
        }),
        NE("not", "ne", "!="),
        GT("gt", ">"),
        GE("ge", ">="),
        LE("le", "<="),
        LT("lt", "<"),
        IN(new Predicate<Integer>() {
            @Override
            public boolean test(Integer params) {
                return params >= 1;
            }
        }, "in"),
        NOT_IN(new Predicate<Integer>() {
            @Override
            public boolean test(Integer params) {
                return params >= 1;
            }
        }, "!in", "ni");

        final Predicate<Integer> params;
        final List<String> aliases;

        CaseOperator(String... aliases) {
            this(new Predicate<Integer>() {
                @Override
                public boolean test(Integer params) {
                    return params == 1;
                }
            }, aliases);
        }

        CaseOperator(Predicate<Integer> paramsTest, String... aliases) {
            this.params = paramsTest;
            this.aliases = Arrays.asList(aliases);
        }

        static CaseOperator from(Collection<String> blockParams) {
            if (blockParams.size() == 1) {
                return EQ;
            } else if (blockParams.size() > 1) {
                // The first param is the operator name
                Iterator<String> iterator = blockParams.iterator();
                String name = iterator.next();
                for (CaseOperator op : values()) {
                    if (!op.params.test(blockParams.size() - 1)) {
                        continue;
                    }
                    for (String alias : op.aliases) {
                        if (alias.equals(name)) {
                            return op;
                        }
                    }

                }
            }
            return null;
        }

        boolean evaluate(Object value, List<?> params) {
            switch (this) {
                case EQ:
                    return Objects.equals(value, params.get(0));
                case NE:
                    return !Objects.equals(value, params.get(0));
                case GE:
                case GT:
                case LE:
                case LT:
                    return compare(value, params.get(0));
                case IN:
                    return params.contains(value);
                case NOT_IN:
                    return !params.contains(value);
                default:
                    throw new TemplateException("Not a legal operator: " + this);
            }
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        boolean compare(Object op1, Object op2) {
            if (op1 == null || op2 == null) {
                throw new TemplateException("Unable to compare null operands [op1=" + op1 + ", op2=" + op2 + "]");
            }
            Comparable c1;
            Comparable c2;
            if (op1 instanceof Comparable && op1.getClass().equals(op2.getClass())) {
                c1 = (Comparable) op1;
                c2 = (Comparable) op2;
            } else {
                c1 = Operator.getDecimal(op1);
                c2 = Operator.getDecimal(op2);
            }
            int result = c1.compareTo(c2);
            switch (this) {
                case GE:
                    return result >= 0;
                case GT:
                    return result > 0;
                case LE:
                    return result <= 0;
                case LT:
                    return result < 0;
                default:
                    return false;
            }
        }

    }

    static class CaseBlock {

        private final SectionBlock block;
        private final CaseOperator caseOperator;
        private final List<Expression> params;

        public CaseBlock(SectionBlock block, SectionInitContext context) {
            this.block = block;
            this.caseOperator = CaseOperator.from(block.parameters.values());
            ImmutableList.Builder<Expression> builder = ImmutableList.builder();
            Iterator<String> iterator = block.parameters.values().iterator();
            if (block.parameters.size() > 1) {
                // Skip the first param -> operator
                iterator.next();
            }
            while (iterator.hasNext()) {
                builder.add(context.parseValue(iterator.next()));
            }
            this.params = builder.build();
        }

        CompletionStage<Boolean> resolve(SectionResolutionContext context, Object value) {
            if (params.isEmpty()) {
                return CompletedStage.of(true);
            } else if (params.size() == 1) {
                Expression paramExpr = params.get(0);
                if (paramExpr.isLiteral()) {
                    // A param is very often a literal, there's no need for async constructs
                    return CompletedStage.of(
                            caseOperator.evaluate(value, Collections.singletonList(paramExpr.getLiteral())));
                }
                return context.resolutionContext().evaluate(paramExpr)
                        .thenApply(p -> caseOperator.evaluate(value, Collections.singletonList(p)));
            } else {
                // in, not in
                CompletableFuture<?>[] allResults = new CompletableFuture<?>[params.size()];
                List<CompletableFuture<?>> results = new LinkedList<>();
                int i = 0;
                Iterator<Expression> it = params.iterator();
                while (it.hasNext()) {
                    Expression expression = it.next();
                    CompletableFuture<Object> result = context.resolutionContext().evaluate(expression).toCompletableFuture();
                    allResults[i++] = result;
                    if (!expression.isLiteral()) {
                        results.add(result);
                    }
                }
                if (results.isEmpty()) {
                    // Parameters are literals only
                    return CompletedStage.of(caseOperator.evaluate(value,
                            Arrays.stream(allResults).map(t1 -> {
                                try {
                                    return t1.get();
                                } catch (InterruptedException | ExecutionException e) {
                                    throw new IllegalStateException(e);
                                }
                            }).collect(Collectors.toList())));
                }
                return CompletableFuture.allOf(results.toArray(new CompletableFuture[0]))
                        .thenApply(new Function<Void, Boolean>() {
                            @Override
                            public Boolean apply(Void t) {
                                return caseOperator.evaluate(value,
                                        Arrays.stream(allResults).map(t1 -> {
                                            try {
                                                return t1.get();
                                            } catch (InterruptedException | ExecutionException e) {
                                                throw new IllegalStateException(e);
                                            }
                                        }).collect(Collectors.toList()));
                            }
                        });
            }
        }

        boolean resolveEnum(SectionResolutionContext context, Object value) {
            if (params.isEmpty()) {
                return true;
            }
            String enumValue = value.toString();
            if (params.size() == 1) {
                // case enum value with the current value
                return caseOperator.evaluate(enumValue, Collections.singletonList(params.get(0).toOriginalString()));
            } else {
                List<String> paramValues = new ArrayList<>(params.size());
                for (Expression param : params) {
                    paramValues.add(param.toOriginalString());
                }
                return caseOperator.evaluate(enumValue, paramValues);
            }
        }

    }
}
