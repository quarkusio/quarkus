package io.quarkus.qute;

import io.quarkus.qute.SectionHelperFactory.SectionInitContext;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * Basic {@code if} statement.
 */
public class IfSectionHelper implements SectionHelper {

    static final String CONDITION = "condition";
    private static final String OPERATOR = "operator";
    private static final String OPERAND = "operand";
    private static final String ELSE = "else";
    private static final String IF = "if";
    private static final String NEGATE = "!";

    private final List<IfBlock> blocks;

    IfSectionHelper(SectionInitContext context) {
        ImmutableList.Builder<IfBlock> builder = ImmutableList.builder();
        for (SectionBlock part : context.getBlocks()) {
            if (SectionHelperFactory.MAIN_BLOCK_NAME.equals(part.label) || ELSE.equals(part.label)) {
                builder.add(new IfBlock(part, context));
            }
        }
        this.blocks = builder.build();
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        return resolveCondition(context, blocks.iterator());
    }

    private CompletionStage<ResultNode> resolveCondition(SectionResolutionContext context,
            Iterator<IfBlock> blocks) {
        IfBlock block = blocks.next();
        if (block.condition == null) {
            // else without condition
            return context.execute(block.block, context.resolutionContext());
        }
        if (block.operator != null) {
            // If operator is used we need to compare the results of condition and operand
            CompletableFuture<ResultNode> result = new CompletableFuture<ResultNode>();
            CompletableFuture<?> cf1 = context.resolutionContext().evaluate(block.condition).toCompletableFuture();
            CompletableFuture<?> cf2 = context.resolutionContext().evaluate(block.operand).toCompletableFuture();
            CompletableFuture.allOf(cf1, cf2).whenComplete((v, t1) -> {
                if (t1 != null) {
                    result.completeExceptionally(t1);
                } else {
                    Object op1;
                    Object op2;
                    try {
                        op1 = cf1.get();
                        op2 = cf2.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new IllegalStateException(e);
                    }
                    try {
                        if (block.operator.evaluate(op1, op2)) {
                            context.execute(block.block, context.resolutionContext()).whenComplete((r, t2) -> {
                                if (t2 != null) {
                                    result.completeExceptionally(t2);
                                } else {
                                    result.complete(r);
                                }
                            });
                        } else {
                            if (blocks.hasNext()) {
                                resolveCondition(context, blocks).whenComplete((r, t2) -> {
                                    if (t2 != null) {
                                        result.completeExceptionally(t2);
                                    } else {
                                        result.complete(r);
                                    }
                                });
                            } else {
                                result.complete(ResultNode.NOOP);
                            }
                        }
                    } catch (Exception e) {
                        result.completeExceptionally(e);
                    }
                }
            });
            return result;
        } else {
            return context.resolutionContext().evaluate(block.condition).thenCompose(r -> {
                if (Boolean.TRUE.equals(r)) {
                    return context.execute(block.block, context.resolutionContext());
                } else {
                    if (blocks.hasNext()) {
                        return resolveCondition(context, blocks);
                    }
                    return CompletableFuture.completedFuture(ResultNode.NOOP);
                }
            });
        }
    }

    public static class Factory implements SectionHelperFactory<IfSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(IF);
        }

        @Override
        public ParametersInfo getParameters() {
            ParametersInfo.Builder builder = ParametersInfo.builder();
            // if params
            builder.addParameter(CONDITION);
            builder.addParameter(new Parameter(OPERATOR, null, true));
            builder.addParameter(new Parameter(OPERAND, null, true));
            // else parts
            // dummy "if" param first
            builder.addParameter(ELSE, new Parameter(IF, null, true));
            builder.addParameter(ELSE, new Parameter(CONDITION, null, true));
            builder.addParameter(ELSE, new Parameter(OPERATOR, null, true));
            builder.addParameter(ELSE, new Parameter(OPERAND, null, true));
            return builder
                    .build();
        }

        public List<String> getBlockLabels() {
            return Collections.singletonList(ELSE);
        }

        @Override
        public IfSectionHelper initialize(SectionInitContext context) {
            return new IfSectionHelper(context);
        }

        @Override
        public Map<String, String> initializeBlock(Map<String, String> outerNameTypeInfos, BlockInfo block) {
            if (MAIN_BLOCK_NAME.equals(block.getLabel()) || ELSE.equals(block.getLabel())) {
                if (MAIN_BLOCK_NAME.equals(block.getLabel()) && !block.hasParameter(CONDITION)) {
                    throw new IllegalStateException("Condition param must be present");
                }
                String conditionParam = block.getParameter(CONDITION);
                if (conditionParam != null) {
                    if (conditionParam.startsWith(NEGATE)) {
                        if (block.hasParameter(OPERAND)) {
                            throw new IllegalArgumentException(
                                    "Logical complement operator may not be used for multiple operands");
                        } else {
                            conditionParam = conditionParam.substring(1, conditionParam.length());
                        }
                    } else if (block.hasParameter(OPERAND)) {
                        block.addExpression(OPERAND, block.getParameter(OPERAND));
                    }
                    block.addExpression(CONDITION, conditionParam);
                }
            }
            // If section never changes the scope
            return Collections.emptyMap();
        }

    }

    static class IfBlock {

        final SectionBlock block;
        final Expression condition;
        final Expression operand;
        final Operator operator;

        public IfBlock(SectionBlock block, SectionInitContext context) {
            this.block = block;
            Operator operator = Operator.from(block.parameters.get(OPERATOR));
            Expression operand;
            if (operator != null) {
                operand = block.expressions.get(OPERAND);
                if (operand == null) {
                    throw new IllegalArgumentException("Operator set but no operand param present");
                }
            } else {
                operand = null;
            }
            if (block.parameters.containsKey(CONDITION) && block.parameters.get(CONDITION).startsWith(NEGATE)) {
                operator = Operator.EQ;
                operand = Expression.literal("false");
            }
            this.condition = block.expressions.get(CONDITION);
            this.operand = operand;
            this.operator = operator;
        }

    }

    enum Operator {

        EQ("eq", "==", "is"),
        NE("ne", "!="),
        GT("gt", ">"),
        GE("ge", ">="),
        LE("le", "<="),
        LT("lt", "<"),
        ;

        private List<String> aliases;

        Operator(String... aliases) {
            this.aliases = Arrays.asList(aliases);
        }

        boolean evaluate(Object op1, Object op2) {
            // TODO better handling of Comparable, numbers, etc.
            switch (this) {
                case EQ:
                    return Objects.equals(op1, op2);
                case NE:
                    return !Objects.equals(op1, op2);
                case GE:
                    return getDecimal(op1).compareTo(getDecimal(op2)) >= 0;
                case GT:
                    return getDecimal(op1).compareTo(getDecimal(op2)) > 0;
                case LE:
                    return getDecimal(op1).compareTo(getDecimal(op2)) <= 0;
                case LT:
                    return getDecimal(op1).compareTo(getDecimal(op2)) < 0;
                default:
                    return false;
            }
        }

        static Operator from(String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            for (Operator operator : values()) {
                if (operator.aliases.contains(value)) {
                    return operator;
                }
            }
            return null;
        }

    }

    static BigDecimal getDecimal(Object value) {
        BigDecimal decimal;
        if (value instanceof BigDecimal) {
            decimal = (BigDecimal) value;
        } else if (value instanceof BigInteger) {
            decimal = new BigDecimal((BigInteger) value);
        } else if (value instanceof Long) {
            decimal = new BigDecimal((Long) value);
        } else if (value instanceof Integer) {
            decimal = new BigDecimal((Integer) value);
        } else if (value instanceof Double) {
            decimal = new BigDecimal((Double) value);
        } else if (value instanceof String) {
            decimal = new BigDecimal(value.toString());
        } else {
            throw new IllegalArgumentException("Not a valid number: " + value);
        }
        return decimal;
    }

}
