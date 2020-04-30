package io.quarkus.mongodb.panache.binder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.panacheql.internal.HqlParser;
import io.quarkus.panacheql.internal.HqlParserBaseVisitor;

class MongoParserVisitor extends HqlParserBaseVisitor<String> {
    private Map<String, String> replacementMap;
    private Map<String, Object> parameterMaps;
    private Set<String> optionalPredicates = new HashSet<>();

    public MongoParserVisitor(Map<String, String> replacementMap, Map<String, Object> parameterMaps) {
        this.replacementMap = replacementMap;
        this.parameterMaps = parameterMaps;
    }

    @Override
    public String visitAndPredicate(HqlParser.AndPredicateContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (HqlParser.PredicateContext predicate : ctx.predicate()) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(predicate.accept(this));
        }
        return sb.toString();
    }

    @Override
    public String visitOrPredicate(HqlParser.OrPredicateContext ctx) {
        StringBuilder sb = new StringBuilder("'$or':[");
        for (HqlParser.PredicateContext predicate : ctx.predicate()) {
            if (sb.length() > 7)
                sb.append(",");
            sb.append('{').append(predicate.accept(this)).append('}');
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String visitEqualityPredicate(HqlParser.EqualityPredicateContext ctx) {
        if (optionalPredicateWithNullValue(ctx.expression(0), ctx.expression(1))) {
            return "";
        }
        return ctx.expression(0).accept(this) + ":" + ctx.expression(1).accept(this);
    }

    @Override
    public String visitInequalityPredicate(HqlParser.InequalityPredicateContext ctx) {
        if (optionalPredicateWithNullValue(ctx.expression(0), ctx.expression(1))) {
            return "";
        }
        return ctx.expression(0).accept(this) + ":{'$ne':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitLessThanOrEqualPredicate(HqlParser.LessThanOrEqualPredicateContext ctx) {
        if (optionalPredicateWithNullValue(ctx.expression(0), ctx.expression(1))) {
            return "";
        }
        return ctx.expression(0).accept(this) + ":{'$lte':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitLikePredicate(HqlParser.LikePredicateContext ctx) {
        if (optionalPredicateWithNullValue(ctx.expression(0), ctx.expression(1))) {
            return "";
        }
        return ctx.expression(0).accept(this) + ":{'$regex':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitGreaterThanPredicate(HqlParser.GreaterThanPredicateContext ctx) {
        if (optionalPredicateWithNullValue(ctx.expression(0), ctx.expression(1))) {
            return "";
        }
        return ctx.expression(0).accept(this) + ":{'$gt':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitLessThanPredicate(HqlParser.LessThanPredicateContext ctx) {
        if (optionalPredicateWithNullValue(ctx.expression(0), ctx.expression(1))) {
            return "";
        }
        return ctx.expression(0).accept(this) + ":{'$lt':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitGreaterThanOrEqualPredicate(HqlParser.GreaterThanOrEqualPredicateContext ctx) {
        if (optionalPredicateWithNullValue(ctx.expression(0), ctx.expression(1))) {
            return "";
        }
        return ctx.expression(0).accept(this) + ":{'$gte':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitIsNullPredicate(HqlParser.IsNullPredicateContext ctx) {
        boolean exists = ctx.NOT() != null;
        return ctx.expression().accept(this) + ":{'$exists':" + exists + "}";
    }

    @Override
    public String visitLiteralExpression(HqlParser.LiteralExpressionContext ctx) {
        return CommonQueryBinder.escape(ctx.getText());
    }

    @Override
    public String visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
        // this will match parameters used by PanacheQL : '?1' for index based or ':key' for named one.
        if (parameterMaps.containsKey(ctx.getText())) {
            Object value = parameterMaps.get(ctx.getText());
            if (value == null) {
                return "null";
            }
            return CommonQueryBinder.escape(value);
        } else {
            // we return the parameter to avoid an exception but the query will be invalid
            return ctx.getText();
        }
    }

    @Override
    public String visitPathExpression(HqlParser.PathExpressionContext ctx) {
        String path = ctx.getText();
        if (ctx.getText().indexOf('?') == ctx.getText().length() - 1) {
            // handle optional predicates
            path = ctx.getText().substring(0, ctx.getText().length() - 1);
            String predicate = "'" + replacementMap.getOrDefault(path, path) + "'";
            optionalPredicates.add(predicate);
            return predicate;
        }
        // this is the name of the field, we apply replacement and escape with '
        return "'" + replacementMap.getOrDefault(path, path) + "'";
    }

    private boolean optionalPredicateWithNullValue(HqlParser.ExpressionContext left, HqlParser.ExpressionContext right) {
        String leftHand = left.accept(this);
        String rightHand = right.accept(this);
        return optionalPredicates.contains(leftHand) && "null".equals(rightHand);
    }
}
