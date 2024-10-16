package io.quarkus.panacheql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.panacheql.internal.HqlLexer;
import io.quarkus.panacheql.internal.HqlParser;
import io.quarkus.panacheql.internal.HqlParser.AndPredicateContext;
import io.quarkus.panacheql.internal.HqlParser.ComparisonPredicateContext;
import io.quarkus.panacheql.internal.HqlParser.GeneralPathExpressionContext;
import io.quarkus.panacheql.internal.HqlParser.IsNullPredicateContext;
import io.quarkus.panacheql.internal.HqlParser.LiteralExpressionContext;
import io.quarkus.panacheql.internal.HqlParser.PredicateContext;
import io.quarkus.panacheql.internal.HqlParserBaseVisitor;

/**
 * Unit test for simple App.
 */
public class LexerTest {

    @Test
    public void test() {
        HqlLexer lexer = new HqlLexer(CharStreams.fromString("bar = 1 AND gee is not null"));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HqlParser parser = new HqlParser(tokens);
        PredicateContext predicate = parser.predicate();
        HqlParserBaseVisitor<String> visitor = new HqlParserBaseVisitor<String>() {
            @Override
            public String visitAndPredicate(AndPredicateContext ctx) {
                StringBuilder sb = new StringBuilder();
                for (PredicateContext predicate : ctx.predicate()) {
                    if (sb.length() > 0)
                        sb.append(" && ");
                    sb.append(predicate.accept(this));
                }
                return sb.toString();
            }

            @Override
            public String visitIsNullPredicate(IsNullPredicateContext ctx) {
                String expr = ctx.expression().accept(this);
                if (ctx.NOT() != null)
                    return expr + " != null";
                return expr + " == null";
            }

            @Override
            public String visitComparisonPredicate(ComparisonPredicateContext ctx) {
                if (ctx.comparisonOperator().EQUAL() != null) {
                    return ctx.expression(0).accept(this) + " == " + ctx.expression(1).accept(this);
                }
                return super.visitComparisonPredicate(ctx);
            }

            @Override
            public String visitLiteralExpression(LiteralExpressionContext ctx) {
                return ctx.getText();
            }

            @Override
            public String visitGeneralPathExpression(GeneralPathExpressionContext ctx) {
                return ctx.getText();
            }
        };
        Assertions.assertEquals("bar == 1 && gee != null", predicate.accept(visitor));
    }
}
