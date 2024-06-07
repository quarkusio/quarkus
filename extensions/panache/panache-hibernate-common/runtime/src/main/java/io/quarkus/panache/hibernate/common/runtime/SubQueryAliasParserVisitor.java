package io.quarkus.panache.hibernate.common.runtime;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.hibernate.grammars.hql.HqlParser.JoinContext;
import org.hibernate.grammars.hql.HqlParser.QueryOrderContext;
import org.hibernate.grammars.hql.HqlParser.SelectionContext;
import org.hibernate.grammars.hql.HqlParser.SimpleQueryGroupContext;
import org.hibernate.grammars.hql.HqlParserBaseVisitor;

public class SubQueryAliasParserVisitor extends HqlParserBaseVisitor<String> {

    private int inSimpleQueryGroup;
    private StringBuilder sb = new StringBuilder();
    private int counter;

    @Override
    public String visitSimpleQueryGroup(SimpleQueryGroupContext ctx) {
        inSimpleQueryGroup++;
        try {
            return super.visitSimpleQueryGroup(ctx);
        } finally {
            inSimpleQueryGroup--;
        }
    }

    @Override
    public String visitSelection(SelectionContext ctx) {
        super.visitSelection(ctx);
        if (inSimpleQueryGroup == 1) {
            if (ctx.variable() == null) {
                sb.append(" as __v" + counter++);
            }
        }
        return null;
    }

    @Override
    public String visitJoin(JoinContext ctx) {
        if (inSimpleQueryGroup == 1 && ctx.FETCH() != null) {
            // ignore fetch joins for main query
            return null;
        }
        return super.visitJoin(ctx);
    }

    @Override
    public String visitQueryOrder(QueryOrderContext ctx) {
        if (inSimpleQueryGroup == 1) {
            // ignore order/limit/offset for main query
            return null;
        }
        return super.visitQueryOrder(ctx);
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        append(node.getText());
        return null;
    }

    @Override
    protected String defaultResult() {
        return null;
    }

    @Override
    protected String aggregateResult(String aggregate, String nextResult) {
        if (nextResult != null) {
            append(nextResult);
        }
        return null;
    }

    private void append(String nextResult) {
        // don't add space at start, or around dots, commas
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '.' && !nextResult.equals(".") && !nextResult.equals(",")) {
            sb.append(" ");
        }
        sb.append(nextResult);
    }

    public String result() {
        return sb.toString();
    }
}