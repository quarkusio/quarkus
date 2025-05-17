package io.quarkus.panache.hibernate.common.runtime;

import java.util.Locale;
import java.util.regex.Pattern;

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

    public static String getEntityName(Class<?> entityClass) {
        // FIXME: not true?
        // Escape the entity name just in case some keywords are used
        // in package names that will prevent ORM from executing a query
        return "`%s`".formatted(entityClass.getName());
    }

    /**
     * Removes \n, \r and outside spaces, and turns to lower case. DO NOT USE the result to pass it on to ORM,
     * because the query is likely to be invalid since we replace newlines even if they
     * are in quoted strings. This is only useful to analyse the start of the query for
     * quick processing. NEVER use this to pass it to the DB or to replace user queries.
     */
    public static String trimForAnalysis(String query) {
        // first replace single chars \n\r\t to spaces
        // turn to lower case
        String ret = query.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').toLowerCase(Locale.ROOT);
        // if we have more than one space, replace with one
        if (ret.indexOf("  ") != -1) {
            ret = ret.replaceAll("\\s+", " ");
        }
        // replace outer spaces
        return ret.trim();
    }

    public static String createFindQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null) {
            return "FROM " + getEntityName(entityClass);
        }

        String trimmedForAnalysis = trimForAnalysis(query);
        if (trimmedForAnalysis.isEmpty()) {
            return "FROM " + getEntityName(entityClass);
        }

        if (trimmedForAnalysis.startsWith("from ")
                || trimmedForAnalysis.startsWith("select ")
                || trimmedForAnalysis.startsWith("with ")) {
            return query;
        }
        if (trimmedForAnalysis.startsWith("order by ")
                || trimmedForAnalysis.startsWith("where ")) {
            return "FROM " + getEntityName(entityClass) + " " + query;
        }
        if (trimmedForAnalysis.indexOf(' ') == -1 && trimmedForAnalysis.indexOf('=') == -1 && paramCount == 1) {
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

    public static String createQueryForCount(Class<?> entityClass, String query, int paramCount) {
        if (query == null || query.isEmpty())
            return "FROM " + getEntityName(entityClass);

        String trimmedForAnalysis = trimForAnalysis(query);
        if (trimmedForAnalysis.isEmpty())
            return "FROM " + getEntityName(entityClass);

        // assume these have valid select clauses and let them through
        if (trimmedForAnalysis.startsWith("select ")
                || trimmedForAnalysis.startsWith("with ")
                || trimmedForAnalysis.startsWith("from ")) {
            return query;
        }
        if (trimmedForAnalysis.startsWith("where ")) {
            return "FROM " + getEntityName(entityClass) + " " + query;
        }
        if (trimmedForAnalysis.startsWith("order by ")) {
            // ignore it
            return "FROM " + getEntityName(entityClass);
        }
        if (trimmedForAnalysis.indexOf(' ') == -1 && trimmedForAnalysis.indexOf('=') == -1 && paramCount == 1) {
            query += " = ?1";
        }
        return "FROM " + getEntityName(entityClass) + " WHERE " + query;
    }

    public static String createUpdateQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null) {
            throw new PanacheQueryException("Query string cannot be null");
        }

        String trimmedForAnalysis = trimForAnalysis(query);
        if (trimmedForAnalysis.isEmpty()) {
            throw new PanacheQueryException("Query string cannot be empty");
        }

        // backwards compat trying to be helpful, remove the from
        if (trimmedForAnalysis.startsWith("update from")) {
            // find the original from and skip it
            int index = query.toLowerCase(Locale.ROOT).indexOf("from");
            return "update " + query.substring(index + 4);
        }
        if (trimmedForAnalysis.startsWith("update ")) {
            return query;
        }
        if (trimmedForAnalysis.startsWith("from ")) {
            // find the original from and skip it
            int index = query.toLowerCase(Locale.ROOT).indexOf("from");
            return "UPDATE " + query.substring(index + 4);
        }
        if (trimmedForAnalysis.indexOf(' ') == -1 && trimmedForAnalysis.indexOf('=') == -1 && paramCount == 1) {
            query += " = ?1";
        }
        if (trimmedForAnalysis.startsWith("set ")) {
            return "UPDATE " + getEntityName(entityClass) + " " + query;
        }
        return "UPDATE " + getEntityName(entityClass) + " SET " + query;
    }

    public static String createDeleteQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null)
            return "DELETE FROM " + getEntityName(entityClass);

        String trimmedForAnalysis = trimForAnalysis(query);
        if (trimmedForAnalysis.isEmpty())
            return "DELETE FROM " + getEntityName(entityClass);

        if (trimmedForAnalysis.startsWith("delete ")) {
            return query;
        }
        if (trimmedForAnalysis.startsWith("from ")) {
            return "DELETE " + query;
        }
        if (trimmedForAnalysis.startsWith("order by ")) {
            // ignore it
            return "DELETE FROM " + getEntityName(entityClass);
        }
        if (trimmedForAnalysis.indexOf(' ') == -1 && trimmedForAnalysis.indexOf('=') == -1 && paramCount == 1) {
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
