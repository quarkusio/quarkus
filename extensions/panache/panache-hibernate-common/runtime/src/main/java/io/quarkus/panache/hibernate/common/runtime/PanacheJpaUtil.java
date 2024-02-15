package io.quarkus.panache.hibernate.common.runtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.hibernate.grammars.hql.HqlLexer;
import org.hibernate.grammars.hql.HqlParser;
import org.hibernate.grammars.hql.HqlParser.SelectStatementContext;

import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.exception.PanacheQueryException;

public class PanacheJpaUtil {

    // match SELECT DISTINCT? id (AS id)? (, id (AS id)?)*
    static final Pattern SELECT_PATTERN = Pattern.compile(
            "^\\s*SELECT\\s+((?:DISTINCT\\s+)?\\w+(?:\\.\\w+)*)(?:\\s+AS\\s+\\w+)?(\\s*,\\s*\\w+(?:\\.\\w+)*(?:\\s+AS\\s+\\w+)?)*\\s+(.*)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // match FROM
    static final Pattern FROM_PATTERN = Pattern.compile("^\\s*FROM\\s+.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // match a FETCH
    static final Pattern FETCH_PATTERN = Pattern.compile(".*\\s+FETCH\\s+.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // match a lone SELECT
    static final Pattern LONE_SELECT_PATTERN = Pattern.compile(".*SELECT\\s+.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // match a leading WITH
    static final Pattern WITH_PATTERN = Pattern.compile("^\\s*WITH\\s+.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * This turns an HQL (already expanded from Panache-QL) query into a count query, using text manipulation
     * if we can, because it's faster, or fall back to using the ORM HQL parser in {@link #getCountQueryUsingParser(String)}
     */
    public static String getFastCountQuery(String query) {
        // try to generate a good count query from the existing query
        String countQuery;
        // there are no fast ways to get rid of fetches, or WITH
        if (FETCH_PATTERN.matcher(query).matches()
                || WITH_PATTERN.matcher(query).matches()) {
            return getCountQueryUsingParser(query);
        }
        // if it starts with select, we can optimise
        Matcher selectMatcher = SELECT_PATTERN.matcher(query);
        if (selectMatcher.matches()) {
            // this one cannot be null
            String firstSelection = selectMatcher.group(1).trim();
            if (firstSelection.toLowerCase().startsWith("distinct ")) {
                // this one can be null
                String secondSelection = selectMatcher.group(2);
                // we can only count distinct single columns
                if (secondSelection != null && !secondSelection.trim().isEmpty()) {
                    throw new PanacheQueryException("Count query not supported for select query: " + query);
                }
                countQuery = "SELECT COUNT(" + firstSelection + ") " + selectMatcher.group(3);
            } else {
                // it's not distinct, forget the column list
                countQuery = "SELECT COUNT(*) " + selectMatcher.group(3);
            }
        } else if (LONE_SELECT_PATTERN.matcher(query).matches()) {
            // a select anywhere else in there might be tricky
            return getCountQueryUsingParser(query);
        } else if (FROM_PATTERN.matcher(query).matches()) {
            countQuery = "SELECT COUNT(*) " + query;
        } else {
            throw new PanacheQueryException("Count query not supported for select query: " + query);
        }

        // remove the order by clause
        String lcQuery = countQuery.toLowerCase();
        int orderByIndex = lcQuery.lastIndexOf(" order by ");
        if (orderByIndex != -1) {
            countQuery = countQuery.substring(0, orderByIndex);
        }
        return countQuery;
    }

    /**
     * This turns an HQL (already expanded from Panache-QL) query into a count query, using the
     * ORM HQL parser. Slow version, see {@link #getFastCountQuery(String)} for the fast version.
     */
    public static String getCountQueryUsingParser(String query) {
        HqlLexer lexer = new HqlLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HqlParser parser = new HqlParser(tokens);
        SelectStatementContext statement = parser.selectStatement();
        CountParserVisitor visitor = new CountParserVisitor();
        statement.accept(visitor);
        return visitor.result();
    }

    public static String getEntityName(Class<?> entityClass) {
        // FIXME: not true?
        return entityClass.getName();
    }

    public static String createFindQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null) {
            return "FROM " + getEntityName(entityClass);
        }

        String trimmed = query.replace('\n', ' ').replace('\r', ' ').trim();
        if (trimmed.isEmpty()) {
            return "FROM " + getEntityName(entityClass);
        }

        String trimmedLc = trimmed.toLowerCase();
        if (trimmedLc.startsWith("from ")
                || trimmedLc.startsWith("select ")
                || trimmedLc.startsWith("with ")) {
            return query;
        }
        if (trimmedLc.startsWith("order by ")
                || trimmedLc.startsWith("where ")) {
            return "FROM " + getEntityName(entityClass) + " " + query;
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            query += " = ?1";
        }
        return "FROM " + getEntityName(entityClass) + " WHERE " + query;
    }

    public static boolean isNamedQuery(String query) {
        if (query == null || query.isEmpty()) {
            return false;
        }
        return query.charAt(0) == '#';
    }

    public static String createCountQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null)
            return "SELECT COUNT(*) FROM " + getEntityName(entityClass);

        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return "SELECT COUNT(*) FROM " + getEntityName(entityClass);

        String trimmedLc = trimmed.toLowerCase();
        // assume these have valid select clauses and let them through
        if (trimmedLc.startsWith("select ")
                || trimmedLc.startsWith("with ")) {
            return query;
        }
        if (trimmedLc.startsWith("from ")) {
            return "SELECT COUNT(*) " + query;
        }
        if (trimmedLc.startsWith("where ")) {
            return "SELECT COUNT(*) FROM " + getEntityName(entityClass) + " " + query;
        }
        if (trimmedLc.startsWith("order by ")) {
            // ignore it
            return "SELECT COUNT(*) FROM " + getEntityName(entityClass);
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            query += " = ?1";
        }
        return "SELECT COUNT(*) FROM " + getEntityName(entityClass) + " WHERE " + query;
    }

    public static String createUpdateQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null) {
            throw new PanacheQueryException("Query string cannot be null");
        }

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw new PanacheQueryException("Query string cannot be empty");
        }

        String trimmedLc = trimmed.toLowerCase();
        // backwards compat trying to be helpful, remove the from
        if (trimmedLc.startsWith("update from")) {
            return "update " + trimmed.substring(11);
        }
        if (trimmedLc.startsWith("update ")) {
            return query;
        }
        if (trimmedLc.startsWith("from ")) {
            return "UPDATE " + trimmed.substring(5);
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            query += " = ?1";
        }
        if (trimmedLc.startsWith("set ")) {
            return "UPDATE " + getEntityName(entityClass) + " " + query;
        }
        return "UPDATE " + getEntityName(entityClass) + " SET " + query;
    }

    public static String createDeleteQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null)
            return "DELETE FROM " + getEntityName(entityClass);

        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return "DELETE FROM " + getEntityName(entityClass);

        String trimmedLc = trimmed.toLowerCase();
        if (trimmedLc.startsWith("delete ")) {
            return query;
        }
        if (trimmedLc.startsWith("from ")) {
            return "DELETE " + query;
        }
        if (trimmedLc.startsWith("order by ")) {
            // ignore it
            return "DELETE FROM " + getEntityName(entityClass);
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            query += " = ?1";
        }
        return "DELETE FROM " + getEntityName(entityClass) + " WHERE " + query;
    }

    public static String toOrderBy(Sort sort) {
        if (sort == null) {
            return null;
        }
        if (sort.getColumns().size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" ORDER BY ");
        for (int i = 0; i < sort.getColumns().size(); i++) {
            Sort.Column column = sort.getColumns().get(i);
            if (i > 0)
                sb.append(" , ");
            if (sort.isEscapingEnabled()) {
                sb.append(escapeColumnName(column.getName()));
            } else {
                sb.append(column.getName());
            }
            if (column.getDirection() != Sort.Direction.Ascending) {
                sb.append(" DESC");
            }

            if (column.getNullPrecedence() != null) {
                if (column.getNullPrecedence() == Sort.NullPrecedence.NULLS_FIRST) {
                    sb.append(" NULLS FIRST");
                } else {
                    sb.append(" NULLS LAST");
                }
            }

        }
        return sb.toString();
    }

    private static StringBuilder escapeColumnName(String columnName) {
        StringBuilder sb = new StringBuilder();
        String[] path = columnName.split("\\.");
        for (int j = 0; j < path.length; j++) {
            if (j > 0)
                sb.append('.');
            sb.append('`').append(unquoteColumnName(path[j])).append('`');
        }
        return sb;
    }

    private static String unquoteColumnName(String columnName) {
        String unquotedColumnName;
        //Note HQL uses backticks to escape/quote special words that are used as identifiers
        if (columnName.charAt(0) == '`' && columnName.charAt(columnName.length() - 1) == '`') {
            unquotedColumnName = columnName.substring(1, columnName.length() - 1);
        } else {
            unquotedColumnName = columnName;
        }
        // Note we're not dealing with columns but with entity attributes so no backticks expected in unquoted column name
        if (unquotedColumnName.indexOf('`') >= 0) {
            throw new PanacheQueryException("Sort column name cannot have backticks");
        }
        return unquotedColumnName;
    }
}
