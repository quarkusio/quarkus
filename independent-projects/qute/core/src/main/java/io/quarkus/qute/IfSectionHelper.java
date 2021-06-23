package io.quarkus.qute;

import static io.quarkus.qute.Booleans.isFalsy;

import io.quarkus.qute.SectionHelperFactory.ParserDelegate;
import io.quarkus.qute.SectionHelperFactory.SectionInitContext;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Basic {@code if} statement.
 */
public class IfSectionHelper implements SectionHelper {

    private static final String ELSE = "else";
    private static final String IF = "if";
    private static final String LOGICAL_COMPLEMENT = "!";

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
        if (blocks.size() == 1) {
            IfBlock block = blocks.get(0);
            return block.condition.evaluate(context).thenCompose(r -> {
                if (isFalsy(r)) {
                    return ResultNode.NOOP;
                } else {
                    return context.execute(block.block, context.resolutionContext());
                }
            });
        }
        return resolveBlocks(context, blocks.iterator());
    }

    private CompletionStage<ResultNode> resolveBlocks(SectionResolutionContext context,
            Iterator<IfBlock> blocks) {
        IfBlock block = blocks.next();
        if (block.condition.isEmpty()) {
            // else without operands
            return context.execute(block.block, context.resolutionContext());
        }
        return block.condition.evaluate(context).thenCompose(r -> {
            if (isFalsy(r)) {
                if (blocks.hasNext()) {
                    return resolveBlocks(context, blocks);
                }
                return ResultNode.NOOP;
            } else {
                return context.execute(block.block, context.resolutionContext());
            }
        });
    }

    public static class Factory implements SectionHelperFactory<IfSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(IF);
        }

        @Override
        public ParametersInfo getParameters() {
            ParametersInfo.Builder builder = ParametersInfo.builder();
            // {#if} must declare at least one condition param
            builder.addParameter("condition");
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
        public Scope initializeBlock(Scope previousScope, BlockInfo block) {
            List<Object> params = null;
            if (MAIN_BLOCK_NAME.equals(block.getLabel())) {
                params = parseParams(new ArrayList<>(block.getParameters().values()), block);
            } else if (ELSE.equals(block.getLabel())) {
                params = parseParams(new ArrayList<>(block.getParameters().values()), block);
                if (!params.isEmpty()) {
                    // else if <-- remove "if"
                    params.remove(0);
                }
            }
            addExpressions(params, block);
            // {#if} never changes the scope
            return previousScope;
        }

        @SuppressWarnings("unchecked")
        private void addExpressions(List<Object> params, BlockInfo block) {
            if (params != null && !params.isEmpty()) {
                for (Object param : params) {
                    if (param instanceof String) {
                        block.addExpression(param.toString(), param.toString());
                    } else if (param instanceof List) {
                        addExpressions((List<Object>) param, block);
                    }
                }
            }
        }

    }

    static class IfBlock {

        final SectionBlock block;
        final Condition condition;

        public IfBlock(SectionBlock block, SectionInitContext context) {
            this.block = block;
            List<Object> params = parseParams(new ArrayList<>(block.parameters.values()), context);
            if (!params.isEmpty() && !SectionHelperFactory.MAIN_BLOCK_NAME.equals(block.label)) {
                params = params.subList(1, params.size());
            }
            this.condition = createCondition(params, block, null, context);
        }

    }

    interface Condition {

        CompletionStage<Object> evaluate(SectionResolutionContext context);

        Operator getOperator();

        default boolean isLogicalComplement() {
            return Operator.NOT.equals(getOperator());
        }

        default boolean isEmpty() {
            return false;
        }

        default Object getLiteralValue() {
            return null;
        }

    }

    static class OperandCondition implements Condition {

        final Operator operator;
        final Expression expression;
        final Object literalValue;

        OperandCondition(Operator operator, Expression expression) {
            this.operator = operator;
            this.expression = expression;
            CompletableFuture<Object> literalVal = expression.getLiteralValue();
            if (literalVal != null) {
                try {
                    this.literalValue = literalVal.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                this.literalValue = null;
            }

        }

        @Override
        public CompletionStage<Object> evaluate(SectionResolutionContext context) {
            return context.resolutionContext().evaluate(expression);
        }

        @Override
        public Operator getOperator() {
            return operator;
        }

        @Override
        public Object getLiteralValue() {
            return literalValue;
        }

        @Override
        public String toString() {
            return "OperandCondition [operator=" + operator + ", expression=" + expression.toOriginalString() + "]";
        }

    }

    static class CompositeCondition implements Condition {

        final List<Condition> conditions;
        final Operator operator;

        public CompositeCondition(Operator operator, List<Condition> conditions) {
            this.operator = operator;
            this.conditions = conditions;
        }

        @Override
        public CompletionStage<Object> evaluate(SectionResolutionContext context) {
            return evaluateNext(context, null, conditions.iterator());
        }

        static CompletionStage<Object> evaluateNext(SectionResolutionContext context, Object value, Iterator<Condition> iter) {
            CompletableFuture<Object> result = new CompletableFuture<>();
            Condition next = iter.next();
            Boolean shortResult = null;
            Operator operator = next.getOperator();
            if (operator != null && operator.isShortCircuiting()) {
                shortResult = operator.evaluate(value);
            }
            if (shortResult != null) {
                // There is no need to continue with the next operand
                result.complete(shortResult);
            } else {
                Object literalVal = next.getLiteralValue();
                if (literalVal != null) {
                    processConditionValue(context, next, operator, value, literalVal, result, iter);
                } else {
                    next.evaluate(context).whenComplete((r, t) -> {
                        if (t != null) {
                            result.completeExceptionally(t);
                        } else {
                            processConditionValue(context, next, operator, value, r, result, iter);
                        }
                    });
                }
            }
            return result;
        }

        @Override
        public Operator getOperator() {
            return operator;
        }

        @Override
        public boolean isEmpty() {
            return conditions.isEmpty();
        }

        @Override
        public String toString() {
            return "CompositeCondition [conditions=" + conditions.size() + ", operator=" + operator + "]";
        }

        static void processConditionValue(SectionResolutionContext context, Condition condition, Operator operator,
                Object previousValue, Object conditionValue, CompletableFuture<Object> result, Iterator<Condition> iter) {
            Object val;
            if (condition.isLogicalComplement()) {
                conditionValue = Booleans.isFalsy(conditionValue) ? Boolean.TRUE : Boolean.FALSE;
            }
            if (operator == null || !operator.isBinary()) {
                val = conditionValue;
            } else {
                // Binary operator
                try {
                    if (Results.isNotFound(conditionValue)) {
                        conditionValue = null;
                    }
                    Object localValue = previousValue;
                    if (Results.isNotFound(localValue)) {
                        localValue = null;
                    }
                    val = operator.evaluate(localValue, conditionValue);
                } catch (Throwable e) {
                    result.completeExceptionally(e);
                    throw e;
                }
            }
            if (!iter.hasNext()) {
                result.complete(val);
            } else {
                evaluateNext(context, val, iter).whenComplete((r2, t2) -> {
                    if (t2 != null) {
                        result.completeExceptionally(t2);
                    } else {
                        result.complete(r2);
                    }
                });
            }
        }

    }

    enum Operator {

        EQ(2, "eq", "==", "is"),
        NE(2, "ne", "!="),
        GT(3, "gt", ">"),
        GE(3, "ge", ">="),
        LE(3, "le", "<="),
        LT(3, "lt", "<"),
        AND(1, "and", "&&"),
        OR(1, "or", "||"),
        NOT(4, IfSectionHelper.LOGICAL_COMPLEMENT);

        private final List<String> aliases;
        private final int precedence;

        Operator(int precedence, String... aliases) {
            this.aliases = Arrays.asList(aliases);
            this.precedence = precedence;
        }

        int getPrecedence() {
            return precedence;
        }

        boolean evaluate(Object op1, Object op2) {
            switch (this) {
                case EQ:
                    return Objects.equals(op1, op2);
                case NE:
                    return !Objects.equals(op1, op2);
                case GE:
                case GT:
                case LE:
                case LT:
                    return compare(op1, op2);
                case AND:
                case OR:
                    return !isFalsy(op2);
                default:
                    throw new TemplateException("Not a binary operator: " + this);
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
                c1 = getDecimal(op1);
                c2 = getDecimal(op2);
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

        Boolean evaluate(Object op1) {
            switch (this) {
                case AND:
                    return isFalsy(op1) ? Boolean.FALSE : null;
                case OR:
                    return isFalsy(op1) ? null : Boolean.TRUE;
                default:
                    throw new TemplateException("Not a short-circuiting operator: " + this);
            }
        }

        boolean isShortCircuiting() {
            return AND.equals(this) || OR.equals(this);
        }

        boolean isBinary() {
            return !NOT.equals(this);
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
                throw new TemplateException("Not a valid number: " + value);
            }
            return decimal;
        }

    }

    static List<Object> parseParams(List<Object> params, ParserDelegate parserDelegate) {

        int highestPrecedence = 0;
        // Replace operators and composite params if needed
        for (ListIterator<Object> iterator = params.listIterator(); iterator.hasNext();) {
            Object param = iterator.next();
            if (param instanceof String) {
                String stringParam = param.toString();
                Operator operator = Operator.from(stringParam);
                if (operator != null) {
                    // Binary operator
                    if (operator.getPrecedence() > highestPrecedence) {
                        highestPrecedence = operator.getPrecedence();
                    }
                    if (operator.isBinary() && !iterator.hasNext()) {
                        throw parserDelegate.createParserError(
                                "binary operator [" + operator + "] set but the second operand not present for {#if} section");
                    }
                    iterator.set(operator);
                } else {
                    if (stringParam.length() > 1 && stringParam.startsWith(LOGICAL_COMPLEMENT)) {
                        // !item.active
                        iterator.set(Operator.NOT);
                        stringParam = stringParam.substring(1);
                        if (stringParam.charAt(0) == Parser.START_COMPOSITE_PARAM) {
                            iterator.add(processCompositeParam(stringParam, parserDelegate));
                        } else {
                            iterator.add(stringParam);
                        }
                    } else {
                        if (stringParam.charAt(0) == Parser.START_COMPOSITE_PARAM) {
                            iterator.set(processCompositeParam(stringParam, parserDelegate));
                        }
                    }
                }
            }
        }

        if (params.stream().filter(p -> p instanceof Operator).map(p -> ((Operator) p).getPrecedence())
                .collect(Collectors.toSet()).size() <= 1) {
            // No binary operators or all of the same precedence
            return params;
        }

        // Take the operators with highest precedence and form groups
        List<Object> highestGroup = null;
        List<Object> ret = new ArrayList<>();
        int lastGroupdIdx = 0;

        for (ListIterator<Object> iterator = params.listIterator(); iterator.hasNext();) {
            int prevIdx = iterator.previousIndex();
            Object param = iterator.next();
            if (isBinaryOperatorEq(param, highestPrecedence)) {
                if (highestGroup == null) {
                    highestGroup = new ArrayList<>();
                    highestGroup.add(params.get(prevIdx));
                }
                highestGroup.add(param);
                // Add non-grouped elements 
                if (prevIdx > lastGroupdIdx) {
                    params.subList(lastGroupdIdx > 0 ? lastGroupdIdx + 1 : 0, prevIdx).forEach(ret::add);
                }
            } else if (isBinaryOperatorLt(param, highestPrecedence)) {
                if (highestGroup != null) {
                    ret.add(highestGroup);
                    lastGroupdIdx = prevIdx;
                    highestGroup = null;
                }
            } else if (highestGroup != null) {
                highestGroup.add(param);
            }
        }
        if (highestGroup != null) {
            ret.add(highestGroup);
        } else {
            // Add all remaining non-grouped elements
            if (lastGroupdIdx + 1 != params.size()) {
                params.subList(lastGroupdIdx + 1, params.size()).forEach(ret::add);
            }
        }
        return parseParams(ret, parserDelegate);
    }

    static List<Object> processCompositeParam(String stringParam, ParserDelegate parserDelegate) {
        // Composite params
        if (!stringParam.endsWith("" + Parser.END_COMPOSITE_PARAM)) {
            throw new TemplateException("Invalid composite parameter found: " + stringParam);
        }
        List<Object> split = new ArrayList<>();
        Parser.splitSectionParams(stringParam.substring(1, stringParam.length() - 1), TemplateException::new)
                .forEachRemaining(split::add);
        return parseParams(split, parserDelegate);
    }

    private static boolean isBinaryOperatorEq(Object val, int precedence) {
        return val instanceof Operator && ((Operator) val).getPrecedence() == precedence;
    }

    private static boolean isBinaryOperatorLt(Object val, int precedence) {
        return val instanceof Operator && ((Operator) val).getPrecedence() < precedence;
    }

    @SuppressWarnings("unchecked")
    static Condition createCondition(Object param, SectionBlock block, Operator operator, SectionInitContext context) {

        Condition condition;

        if (param instanceof String) {
            String stringParam = param.toString();
            boolean logicalComplement = stringParam.startsWith(LOGICAL_COMPLEMENT);
            if (logicalComplement) {
                stringParam = stringParam.substring(1);
            }
            Expression expr = block.expressions.get(stringParam);
            if (expr == null) {
                throw new TemplateException("Expression not found for param [" + stringParam + "]: " + block);
            }
            condition = new OperandCondition(operator, expr);
        } else if (param instanceof List) {
            List<Object> params = (List<Object>) param;
            if (params.size() == 1) {
                return createCondition(params.get(0), block, operator, context);
            }

            List<Condition> conditions = new ArrayList<>();
            Operator nextOperator = null;

            for (Object p : params) {
                if (p instanceof Operator) {
                    nextOperator = (Operator) p;
                } else {
                    conditions.add(createCondition(p, block, nextOperator, context));
                    nextOperator = null;
                }
            }
            condition = new CompositeCondition(operator, ImmutableList.copyOf(conditions));
        } else {
            throw new TemplateException("Unsupported param type: " + param);
        }
        return condition;
    }

}
