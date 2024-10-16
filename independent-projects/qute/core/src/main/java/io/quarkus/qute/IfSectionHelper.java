package io.quarkus.qute;

import static io.quarkus.qute.Booleans.isFalsy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import io.quarkus.qute.SectionHelperFactory.SectionInitContext;

/**
 * Basic {@code if} statement.
 */
public class IfSectionHelper implements SectionHelper {

    private static final String ELSE = "else";
    private static final String IF = "if";
    private static final String LOGICAL_COMPLEMENT = "!";

    private final IfContext ifContext;

    IfSectionHelper(SectionInitContext context) {
        List<ConditionBlock> conditionBlocks = new ArrayList<>();
        for (SectionBlock block : context.getBlocks()) {
            if (SectionHelperFactory.MAIN_BLOCK_NAME.equals(block.label) || ELSE.equals(block.label)) {
                conditionBlocks.add(new ConditionBlock(block, context));
            }
        }
        if (conditionBlocks.size() == 1) {
            this.ifContext = new SingletonContext(conditionBlocks.get(0));
        } else if (conditionBlocks.size() == 2) {
            this.ifContext = new DoubletonContext(conditionBlocks.get(0), conditionBlocks.get(1));
        } else {
            this.ifContext = new ListContext(ImmutableList.copyOf(conditionBlocks));
        }
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        return ifContext.resolve(context);
    }

    interface IfContext {

        CompletionStage<ResultNode> resolve(SectionResolutionContext context);

    }

    static final class SingletonContext implements IfContext {

        private final ConditionBlock block;

        SingletonContext(ConditionBlock block) {
            this.block = block;
        }

        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            return block.condition.evaluate(context).thenCompose(r -> {
                if (isFalsy(r)) {
                    return ResultNode.NOOP;
                } else {
                    return context.execute(block.section, context.resolutionContext());
                }
            });
        }

    }

    static final class DoubletonContext implements IfContext {

        private final ConditionBlock block;
        private final ConditionBlock next;

        public DoubletonContext(ConditionBlock block, ConditionBlock next) {
            this.block = block;
            this.next = next;
        }

        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            return block.condition.evaluate(context).thenCompose(r -> {
                if (isFalsy(r)) {
                    if (next.condition.isEmpty()) {
                        // else without operands
                        return context.execute(next.section, context.resolutionContext());
                    }
                    return next.condition.evaluate(context).thenCompose(nr -> {
                        if (isFalsy(nr)) {
                            return ResultNode.NOOP;
                        } else {
                            return context.execute(next.section, context.resolutionContext());
                        }
                    });
                } else {
                    return context.execute(block.section, context.resolutionContext());
                }
            });
        }

    }

    static final class ListContext implements IfContext {

        private final List<ConditionBlock> blocks;

        ListContext(List<ConditionBlock> blocks) {
            this.blocks = blocks;
        }

        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            return resolveBlocks(context, blocks.iterator());
        }

        private CompletionStage<ResultNode> resolveBlocks(SectionResolutionContext context,
                Iterator<ConditionBlock> blocks) {
            ConditionBlock block = blocks.next();
            if (block.condition.isEmpty()) {
                // else without operands
                return context.execute(block.section, context.resolutionContext());
            }
            return block.condition.evaluate(context).thenCompose(r -> {
                if (isFalsy(r)) {
                    if (blocks.hasNext()) {
                        return resolveBlocks(context, blocks);
                    }
                    return ResultNode.NOOP;
                } else {
                    return context.execute(block.section, context.resolutionContext());
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
            return ParametersInfo.builder()
                    .checkNumberOfParams(false)
                    // {#if} must declare at least one condition param
                    .addParameter("condition")
                    .build();
        }

        public List<String> getBlockLabels() {
            return ImmutableList.of(ELSE);
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

    static class ConditionBlock {

        final SectionBlock section;
        final Condition condition;

        public ConditionBlock(SectionBlock block, SectionInitContext context) {
            this.section = block;
            List<Object> params = parseParams(new ArrayList<>(block.parameters.values()), block);
            if (!params.isEmpty() && !SectionHelperFactory.MAIN_BLOCK_NAME.equals(block.label)) {
                params = params.subList(1, params.size());
            }
            this.condition = createCondition(params, block, null, context);
        }

    }

    interface Condition {

        CompletionStage<Object> evaluate(SectionResolutionContext context);

        Operator getOperator();

        default boolean isEmpty() {
            return false;
        }

        default Object getLiteralValue() {
            return null;
        }

        default Boolean logicalComplement(Object val) {
            return Booleans.isFalsy(val) ? Boolean.TRUE : Boolean.FALSE;
        }

    }

    static class OperandCondition implements Condition {

        final Operator operator;
        final Expression expression;
        final Object literalValue;

        OperandCondition(Operator operator, Expression expression) {
            this.operator = operator;
            this.expression = expression;
            this.literalValue = expression.getLiteral();
        }

        @Override
        public CompletionStage<Object> evaluate(SectionResolutionContext context) {
            CompletionStage<Object> ret;
            if (expression.isLiteral()) {
                ret = expression.asLiteral();
            } else {
                ret = context.resolutionContext().evaluate(expression);
            }
            if (operator == Operator.NOT) {
                return ret.thenApply(this::logicalComplement);
            }
            return ret;
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

    static class DoubletonCondition implements Condition {

        final Condition condition1;
        final Condition condition2;
        final Operator operator;

        private DoubletonCondition(Condition condition1, Condition condition2, Operator operator) {
            this.condition1 = Objects.requireNonNull(condition1);
            this.condition2 = Objects.requireNonNull(condition2);
            this.operator = operator;
        }

        @Override
        public CompletionStage<Object> evaluate(SectionResolutionContext context) {
            CompletionStage<Object> ret = condition1.evaluate(context);
            if (ret instanceof CompletedStage completed) {
                ret = evaluateSecond(context, completed.get());
            } else {
                ret = ret.thenCompose(first -> evaluateSecond(context, first));
            }
            if (operator == Operator.NOT) {
                return ret.thenApply(this::logicalComplement);
            }
            return ret;
        }

        @Override
        public Operator getOperator() {
            return operator;
        }

        @Override
        public String toString() {
            return "DoubletonCondition [condition1=" + condition1 + ", condition2=" + condition2 + ", operator=" + operator
                    + "]";
        }

        CompletionStage<Object> evaluateSecond(SectionResolutionContext context, Object firstVal) {
            Boolean shortResult = null;
            Operator operator = condition2.getOperator();
            if (operator != null && operator.isShortCircuiting()) {
                shortResult = operator.evaluate(firstVal);
            }
            if (shortResult != null) {
                // There is no need to continue with the next operand
                return CompletedStage.of(shortResult);
            }
            CompletionStage<Object> second = condition2.evaluate(context);
            if (second instanceof CompletedStage completed) {
                return processValues(operator, firstVal, completed.get());
            } else {
                return second.thenCompose(c2 -> processValues(operator, firstVal, c2));
            }
        }

        CompletionStage<Object> processValues(Operator operator,
                Object firstVal, Object secondVal) {
            Object val;
            if (operator == null || !operator.isBinary()) {
                val = secondVal;
            } else {
                // Binary operator
                try {
                    if (Results.isNotFound(secondVal)) {
                        secondVal = null;
                    }
                    Object localValue = firstVal;
                    if (Results.isNotFound(localValue)) {
                        localValue = null;
                    }
                    val = operator.evaluate(localValue, secondVal);
                } catch (Throwable e) {
                    return CompletedStage.failure(e);
                }
            }
            return CompletedStage.of(val);
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
            CompletionStage<Object> ret = evaluateNext(context, null, conditions.iterator());
            if (operator == Operator.NOT) {
                return ret.thenApply(this::logicalComplement);
            }
            return ret;
        }

        CompletionStage<Object> evaluateNext(SectionResolutionContext context, Object previousValue,
                Iterator<Condition> iter) {
            Condition next = iter.next();
            Boolean shortResult = null;
            Operator operator = next.getOperator();
            if (operator != null && operator.isShortCircuiting()) {
                shortResult = operator.evaluate(previousValue);
            }
            if (shortResult != null) {
                // There is no need to continue with the next operand
                return CompletedStage.of(shortResult);
            } else {
                Object literalVal = next.getLiteralValue();
                if (literalVal != null) {
                    // A literal value does not need to be evaluated
                    if (operator == Operator.NOT) {
                        literalVal = logicalComplement(literalVal);
                    }
                    return processConditionValue(context, operator, previousValue, literalVal, iter);
                } else {
                    CompletionStage<Object> ret = next.evaluate(context);
                    if (ret instanceof CompletedStage completed) {
                        return processConditionValue(context, operator, previousValue, completed.get(),
                                iter);
                    } else {
                        return ret.thenCompose(r -> processConditionValue(context, operator, previousValue, r, iter));
                    }
                }
            }
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

        CompletionStage<Object> processConditionValue(SectionResolutionContext context, Operator operator,
                Object previousValue, Object conditionValue, Iterator<Condition> iter) {
            Object val;
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
                    return CompletedStage.failure(e);
                }
            }
            if (!iter.hasNext()) {
                return CompletedStage.of(val);
            } else {
                return evaluateNext(context, val, iter);
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
            this.aliases = List.of(aliases);
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
            } else if (value instanceof Integer) {
                decimal = new BigDecimal((Integer) value);
            } else if (value instanceof Long) {
                decimal = new BigDecimal((Long) value);
            } else if (value instanceof Double) {
                decimal = new BigDecimal((Double) value);
            } else if (value instanceof Float) {
                decimal = new BigDecimal((Float) value);
            } else if (value instanceof String) {
                decimal = new BigDecimal(value.toString());
            } else {
                throw new TemplateException("Not a valid number: " + value);
            }
            return decimal;
        }

    }

    static <B extends ErrorInitializer & WithOrigin> List<Object> parseParams(List<Object> params, B block) {

        replaceOperatorsAndCompositeParams(params, block);
        int highestPrecedence = getHighestPrecedence(params);

        if (!isGroupingNeeded(params)) {
            // No operators or all of the same precedence
            return params;
        }

        // Take the operators with highest precedence and form groups
        // For example "user.active && target.status == NEW && !target.voted" becomes "user.active && [target.status == NEW] && [!target.voted]"
        // The algorithm used is not very robust and should be improved later
        List<Object> highestGroup = null;
        List<Object> ret = new ArrayList<>();
        int lastGroupdIdx = 0;

        for (ListIterator<Object> iterator = params.listIterator(); iterator.hasNext();) {
            int prevIdx = iterator.previousIndex();
            Object param = iterator.next();
            if (param instanceof Operator) {
                Operator op = (Operator) param;
                if (op.precedence == highestPrecedence) {
                    if (highestGroup == null) {
                        highestGroup = new ArrayList<>();
                        if (op.isBinary()) {
                            highestGroup.add(params.get(prevIdx));
                        }
                    }
                    highestGroup.add(param);
                    // Add non-grouped elements
                    if (prevIdx > lastGroupdIdx) {
                        int from = lastGroupdIdx > 0 ? lastGroupdIdx + 1 : 0;
                        int to = op.isBinary() ? prevIdx : prevIdx + 1;
                        ret.addAll(params.subList(from, to));
                    }
                } else if (op.precedence < highestPrecedence) {
                    if (highestGroup != null) {
                        ret.add(highestGroup);
                        lastGroupdIdx = prevIdx;
                        highestGroup = null;
                    }
                } else {
                    throw new IllegalStateException();
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
                ret.addAll(params.subList(lastGroupdIdx + 1, params.size()));
            }
        }
        return parseParams(ret, block);
    }

    private static boolean isGroupingNeeded(List<Object> params) {
        Integer lastPrecedence = null;
        for (Object param : params) {
            if (param instanceof Operator) {
                Operator op = (Operator) param;
                if (lastPrecedence == null) {
                    lastPrecedence = op.getPrecedence();
                } else if (!lastPrecedence.equals(op.getPrecedence())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static <B extends ErrorInitializer & WithOrigin> void replaceOperatorsAndCompositeParams(List<Object> params,
            B block) {
        for (ListIterator<Object> iterator = params.listIterator(); iterator.hasNext();) {
            Object param = iterator.next();
            if (param instanceof String) {
                String stringParam = param.toString();
                Operator operator = Operator.from(stringParam);
                if (operator != null) {
                    if (operator.isBinary() && !iterator.hasNext()) {
                        throw block.error(
                                "binary operator [{operator}] set but the second operand not present for \\{#if\\} section")
                                .argument("operator", operator)
                                .code(Code.BINARY_OPERATOR_MISSING_SECOND_OPERAND)
                                .origin(block.getOrigin())
                                .build();
                    }
                    iterator.set(operator);
                } else {
                    if (stringParam.length() > 1 && stringParam.startsWith(LOGICAL_COMPLEMENT)) {
                        // !item.active
                        iterator.set(Operator.NOT);
                        stringParam = stringParam.substring(1);
                        if (stringParam.charAt(0) == Parser.START_COMPOSITE_PARAM) {
                            iterator.add(processCompositeParam(stringParam, block));
                        } else {
                            iterator.add(stringParam);
                        }
                    } else {
                        if (stringParam.charAt(0) == Parser.START_COMPOSITE_PARAM) {
                            iterator.set(processCompositeParam(stringParam, block));
                        }
                    }
                }
            }
        }
    }

    private static int getHighestPrecedence(List<Object> params) {
        int highestPrecedence = 0;
        for (Object param : params) {
            if (param instanceof Operator) {
                Operator op = (Operator) param;
                if (op.precedence > highestPrecedence) {
                    highestPrecedence = op.precedence;
                }
            }
        }
        return highestPrecedence;
    }

    static <B extends ErrorInitializer & WithOrigin> List<Object> processCompositeParam(String stringParam, B block) {
        // Composite params
        if (!stringParam.endsWith("" + Parser.END_COMPOSITE_PARAM)) {
            throw new TemplateException("Invalid composite parameter found: " + stringParam);
        }
        List<Object> split = new ArrayList<>();
        Parser.splitSectionParams(stringParam.substring(1, stringParam.length() - 1),
                block)
                .forEachRemaining(split::add);
        return parseParams(split, block);
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

            if (operator == null && conditions.size() == 1) {
                condition = conditions.get(0);
            } else if (conditions.size() == 2) {
                condition = new DoubletonCondition(conditions.get(0), conditions.get(1), operator);
            } else {
                condition = new CompositeCondition(operator, ImmutableList.copyOf(conditions));
            }
        } else {
            throw new TemplateException("Unsupported param type: " + param);
        }
        return condition;
    }

    enum Code implements ErrorCode {

        /**
         * <code>{#if foo >}{/}</code>
         */
        BINARY_OPERATOR_MISSING_SECOND_OPERAND,

        ;

        @Override
        public String getName() {
            return "IF_" + name();
        }

    }

}
