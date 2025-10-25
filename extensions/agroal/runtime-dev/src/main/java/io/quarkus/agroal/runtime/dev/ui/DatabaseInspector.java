package io.quarkus.agroal.runtime.dev.ui;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.quarkus.agroal.runtime.AgroalDataSourceSupport;
import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;

public final class DatabaseInspector {

    private static final Logger LOG = Logger.getLogger(DatabaseInspector.class);

    @Inject
    Instance<AgroalDataSourceSupport> agroalDataSourceSupports;

    @Inject
    Optional<Assistant> assistant;

    private final Map<String, AgroalDataSource> checkedDataSources = new HashMap<>();

    private boolean isDev = false;
    private boolean allowSql = false;
    private String allowedHost = null;

    public DatabaseInspector() {
        LaunchMode currentMode = LaunchMode.current();
        this.isDev = currentMode.isDev() && !currentMode.isRemoteDev();

        Config config = ConfigProvider.getConfig();
        this.allowSql = config.getOptionalValue("quarkus.datasource.dev-ui.allow-sql", Boolean.class)
                .orElse(false);

        this.allowedHost = config.getOptionalValue("quarkus.datasource.dev-ui.allowed-db-host", String.class)
                .orElse(null);

    }

    @PostConstruct
    protected void init() {
        if (!agroalDataSourceSupports.isResolvable()) {
            // No configured Agroal datasource at build time.
            return;
        }

        if (isDev) {
            AgroalDataSourceSupport agroalSupport = agroalDataSourceSupports.get();
            for (String name : agroalSupport.entries.keySet()) {
                AgroalDataSourceSupport.Entry entry = agroalSupport.entries.get(name);
                if (entry != null) {
                    InjectableInstance<AgroalDataSource> dataSourceInstance = AgroalDataSourceUtil.dataSourceInstance(name);
                    if (dataSourceInstance.isResolvable()) {
                        AgroalDataSource ads = dataSourceInstance.get();
                        if (isAllowedDatabase(ads)) {
                            checkedDataSources.put(name, ads);
                        }
                    }
                }
            }
        }
    }

    @JsonRpcDescription("Get all available datasources for the Database")
    @DevMCPEnableByDefault
    public List<Datasource> getDataSources() {
        if (isDev) {
            List<Datasource> datasources = new ArrayList<>();

            for (String ds : checkedDataSources.keySet()) {
                datasources.add(getDatasource(ds));
            }

            return datasources;
        }
        return List.of();
    }

    @JsonRpcDescription("Get a spesific datasource for the Database by name")
    @DevMCPEnableByDefault
    private Datasource getDatasource(@JsonRpcDescription("Datasource name") String datasource) {
        if (isDev) {
            AgroalDataSource ads = checkedDataSources.get(datasource);
            if (isAllowedDatabase(ads)) {
                AgroalDataSourceConfiguration configuration = ads.getConfiguration();

                String jdbcUrl = configuration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl();
                boolean isDefault = DataSourceUtil.isDefault(datasource);

                return new Datasource(datasource, jdbcUrl, isDefault);
            }
        }
        return null;
    }

    @JsonRpcDescription("Get all the tables for a certain datasource")
    @DevMCPEnableByDefault
    public List<Table> getTables(@JsonRpcDescription("Datasource name") String datasource) {
        if (isDev) {
            List<Table> tableList = new ArrayList<>();
            try {
                AgroalDataSource ads = checkedDataSources.get(datasource);
                if (isAllowedDatabase(ads)) {
                    try (Connection connection = ads.getConnection()) {
                        DatabaseMetaData metaData = connection.getMetaData();

                        // Get all tables
                        try (ResultSet tables = metaData.getTables(null, null, "%", new String[] { "TABLE" })) {
                            while (tables.next()) {
                                String tableName = tables.getString("TABLE_NAME");
                                String tableSchema = tables.getString("TABLE_SCHEM");
                                if (tableSchema == null) {
                                    tableSchema = tables.getString("TABLE_CAT"); // fallback for MySQL
                                }

                                // Get the Primary Keys
                                List<String> primaryKeyList = getPrimaryKeys(metaData, tableSchema, tableName);

                                // Get columns for each table
                                List<Column> columnList = new ArrayList<>();
                                try (ResultSet columns = metaData.getColumns(null, tableSchema, tableName, "%")) {
                                    while (columns.next()) {
                                        String columnName = columns.getString("COLUMN_NAME");
                                        String columnType = columns.getString("TYPE_NAME");
                                        int columnSize = columns.getInt("COLUMN_SIZE");
                                        String nullable = columns.getString("IS_NULLABLE");
                                        int dataType = columns.getInt("DATA_TYPE");
                                        columnList
                                                .add(new Column(columnName, columnType, columnSize, nullable,
                                                        isBinary(dataType)));

                                    }
                                }
                                List<ForeignKey> foreignKeyList = new ArrayList<>();
                                try (ResultSet fks = metaData.getImportedKeys(null, tableSchema, tableName)) {
                                    while (fks.next()) {
                                        String fkColumn = fks.getString("FKCOLUMN_NAME");
                                        String pkTable = fks.getString("PKTABLE_NAME");
                                        String pkColumn = fks.getString("PKCOLUMN_NAME");
                                        foreignKeyList.add(new ForeignKey(fkColumn, pkTable, pkColumn));
                                    }
                                }

                                tableList.add(new Table(tableSchema, tableName, primaryKeyList, columnList, foreignKeyList));
                            }
                        }
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }

            return tableList;
        }
        return null;
    }

    @JsonRpcDescription("Generate an ER Diagram in dot (graphviz) format for a certain datasource")
    public String generateDot(@JsonRpcDescription("Datasource name") String datasource) {
        if (isDev) {
            List<Table> tables = getTables(datasource);

            StringBuilder dot = new StringBuilder();
            dot.append("digraph ER {\n");
            dot.append("  graph [splines=ortho, nodesep=1, ranksep=2];\n");
            dot.append("  node [shape=record, fontname=Helvetica];\n\n");

            for (Table table : tables) {
                StringBuilder fields = new StringBuilder();
                for (Column col : table.columns()) {
                    boolean isPK = table.primaryKeys().contains(col.columnName());
                    fields.append(col.columnName())
                            .append(": ")
                            .append(col.columnType())
                            .append(" (")
                            .append(col.columnSize())
                            .append(")")
                            .append(isPK ? " (PK)" : "")
                            .append("\\l");
                }

                dot.append("  ")
                        .append(escape(table.tableName()))
                        .append(" [label=\"{")
                        .append(table.tableName())
                        .append("|")
                        .append(fields)
                        .append("}\"];\n");

                for (ForeignKey fk : table.foreignKeys()) {
                    dot.append("  ")
                            .append(escape(table.tableName()))
                            .append(" -> ")
                            .append(escape(fk.referencedTable()))
                            .append(" [label=\"")
                            .append(fk.columnName())
                            .append(" → ")
                            .append(fk.referencedColumn())
                            .append("\"];\n");
                }
            }

            dot.append("}\n");
            return dot.toString();
        }
        return null;
    }

    @JsonRpcDescription("Execute SQL against a certain datasource")
    @DevMCPEnableByDefault
    public DataSet executeSQL(@JsonRpcDescription("Datasource name") String datasource,
            @JsonRpcDescription("Valid SQL to execute") String sql,
            @JsonRpcDescription("Page number for pagable rusults, starting at 1") Integer pageNumber,
            @JsonRpcDescription("Number of rows in a page, example 10") Integer pageSize) {
        if (isDev && sqlIsValid(sql)) {
            try {
                AgroalDataSource ads = checkedDataSources.get(datasource);
                if (isAllowedDatabase(ads)) {
                    try (Connection connection = ads.getConnection()) {
                        // Create a scrollable ResultSet
                        try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                ResultSet.CONCUR_READ_ONLY)) {
                            boolean hasResultSet = statement.execute(sql);
                            if (hasResultSet) {
                                try (ResultSet resultSet = statement.executeQuery(sql)) {

                                    // Get the total number of rows
                                    resultSet.last();
                                    int totalNumberOfElements = resultSet.getRow();

                                    // Get the column metadata
                                    List<String> cols = new ArrayList<>();
                                    ResultSetMetaData metaData = resultSet.getMetaData();
                                    int columnCount = metaData.getColumnCount();
                                    for (int i = 1; i <= columnCount; i++) {
                                        String columnName = metaData.getColumnName(i);
                                        cols.add(columnName);
                                    }

                                    int startRow = (pageNumber - 1) * pageSize + 1;

                                    List<Map<String, String>> rows = new ArrayList<>();

                                    if (resultSet.absolute(startRow)) {
                                        int rowCount = 0;

                                        do {
                                            Map<String, String> row = new HashMap<>();
                                            for (int i = 1; i <= columnCount; i++) {
                                                String columnName = metaData.getColumnName(i);
                                                boolean isBinary = isBinary(metaData.getColumnType(i),
                                                        metaData.getColumnClassName(i));
                                                if (!isBinary) {
                                                    Object columnValue = resultSet.getObject(i);
                                                    row.put(columnName, String.valueOf(columnValue));
                                                } else {
                                                    byte[] binary = resultSet.getBytes(i);
                                                    if (!resultSet.wasNull()) {
                                                        row.put(columnName, Base64.getEncoder().encodeToString(binary));
                                                    } else {
                                                        row.put(columnName, null);
                                                    }
                                                }
                                            }
                                            rows.add(row);
                                            rowCount++;
                                        } while (resultSet.next() && rowCount < pageSize);
                                    }
                                    return new DataSet(cols, rows, null, null, totalNumberOfElements);
                                }
                            } else {
                                // Query did not return a ResultSet (e.g., DELETE, UPDATE)
                                int updateCount = statement.getUpdateCount();
                                String message = "Query executed successfully. Rows affected: " + updateCount;
                                return new DataSet(null, null, null, message, -1);
                            }
                        } catch (Exception e) {
                            return new DataSet(null, null, e.getMessage(), null, -1);
                        }
                    }
                } else {
                    return new DataSet(null, null,
                            "Datasource access not allowed. By default only local databases are enabled; you can use the"
                                    + " 'quarkus.datasource.dev-ui.allowed-db-host' configuration property to configure allowed hosts ('*' to allow all).",
                            null, -1);
                }
            } catch (SQLException ex) {
                return new DataSet(null, null, ex.getMessage(), null, -1);
            }
        } else {
            return new DataSet(null, null, "Unknown Error", null, -1);
        }
    }

    @JsonRpcDescription("Get the import.sql script for a certain datasource")
    public String getInsertScript(@JsonRpcDescription("Datasource name") String datasource) {
        if (isDev) {
            try {
                AgroalDataSource ads = checkedDataSources.get(datasource);
                if (isAllowedDatabase(ads)) {
                    try (Connection connection = ads.getConnection();
                            StringWriter writer = new StringWriter()) {
                        DatabaseMetaData metaData = connection.getMetaData();
                        try (ResultSet tables = metaData.getTables(null, null, "%", new String[] { "TABLE" })) {
                            while (tables.next()) {
                                String tableName = tables.getString("TABLE_NAME");
                                exportTable(connection, writer, tableName);
                            }
                        }

                        return writer.toString();
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

    public CompletionStage<Map<String, String>> generateTableData(String datasource, String schema, String name, int rowCount) {
        if (isDev && assistant.isPresent()) {
            List<Table> tables = getTables(datasource);
            Optional<Table> matchingTable = tables.stream()
                    .filter(t -> t.tableSchema().equals(schema) && t.tableName().equals(name))
                    .findFirst();

            if (matchingTable.isPresent()) {
                return assistant.get().assistBuilder()
                        .userMessage(generateInsertPrompt(matchingTable.get(), rowCount))
                        .responseType(InsertStatementResponse.class)
                        .assist();
            }
        }
        return CompletableFuture.failedStage(new RuntimeException("Assistant is not available"));
    }

    public CompletionStage<Map<String, String>> englishToSQL(String datasource, String schema, String name, String english) {
        if (isDev && assistant.isPresent()) {
            List<Table> tables = getTables(datasource);

            return assistant.get().assistBuilder()
                    .userMessage(englishToSqlPrompt(tables, schema, name, english))
                    .responseType(EnglishToSQLResponse.class)
                    .assist();

        }
        return CompletableFuture.failedStage(new RuntimeException("Assistant is not available"));
    }

    private void exportTable(Connection conn, StringWriter writer, String tableName) throws SQLException, IOException {
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                StringBuilder insertQuery = new StringBuilder("INSERT INTO " + tableName + " (");

                for (int i = 1; i <= columnCount; i++) {
                    insertQuery.append(metaData.getColumnName(i));
                    if (i < columnCount)
                        insertQuery.append(", ");
                }

                insertQuery.append(") VALUES (");

                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    if (value == null) {
                        insertQuery.append("NULL");
                    } else if (value instanceof String || value instanceof Date || value instanceof Timestamp) {
                        insertQuery.append("'").append(value.toString().replace("'", "''")).append("'");
                    } else {
                        insertQuery.append(value.toString());
                    }
                    if (i < columnCount)
                        insertQuery.append(", ");
                }

                insertQuery.append(");\n");
                writer.write(insertQuery.toString());
            }
        }
    }

    private String escape(String value) {
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private boolean sqlIsValid(String sql) {
        if (sql == null || sql.isEmpty())
            return false;
        if (allowSql) {
            return true;
        } else {
            String lsql = sql.toLowerCase().trim();
            return lsql.startsWith("select")
                    && !lsql.contains("update ")
                    && !lsql.contains("delete ")
                    && !lsql.contains("insert ")
                    && !lsql.contains("create ")
                    && !lsql.contains("drop "); // Having a sql with those nested is invalid anyway
        }
    }

    private List<String> getPrimaryKeys(DatabaseMetaData metaData, String tableSchema, String tableName) throws SQLException {
        List<String> primaryKeyList = new ArrayList<>();
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(null, tableSchema, tableName)) {
            while (primaryKeys.next()) {
                String primaryKeyColumn = primaryKeys.getString("COLUMN_NAME");
                primaryKeyList.add(primaryKeyColumn);
            }
        }
        return primaryKeyList;
    }

    private boolean isAllowedDatabase(AgroalDataSource ads) {
        final String allowedHost = this.allowedHost == null ? null : this.allowedHost.trim();
        if (allowedHost != null && allowedHost.equals("*")) {
            // special value indicating to allow any host
            return true;
        }

        if (ads == null)
            return false;

        try {
            AgroalDataSourceConfiguration configuration = ads.getConfiguration();
            String jdbcUrl = configuration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl();
            if (jdbcUrl.startsWith("jdbc:h2:mem:") || jdbcUrl.startsWith("jdbc:h2:file:")
                    || jdbcUrl.startsWith("jdbc:h2:tcp://localhost")
                    || (allowedHost != null && !allowedHost.isBlank()
                            && jdbcUrl.startsWith("jdbc:h2:tcp://" + allowedHost))) {
                return true;
            }

            String cleanUrl = jdbcUrl.replace("jdbc:", "").replaceFirst(";", "?").replace(";", "&");
            URI uri = new URI(cleanUrl);

            String host = uri.getHost();

            return host != null && ((host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")) ||
                    (allowedHost != null && !allowedHost.isBlank() && host.equalsIgnoreCase(allowedHost)));

        } catch (URISyntaxException e) {
            LOG.warn(e.getMessage());
        } catch (InactiveBeanException ibe) {
            // The datasource is disabled.
        }

        return false;
    }

    private boolean isBinary(int dataType, String javaClassName) {

        // Some java classes can be handled as String
        if (java.util.UUID.class.getName().equals(javaClassName)) {
            return false;
        }
        return isBinary(dataType);
    }

    private boolean isBinary(int dataType) {
        return dataType == Types.BLOB ||
                dataType == Types.VARBINARY ||
                dataType == Types.LONGVARBINARY ||
                dataType == Types.BINARY ||
                dataType == Types.JAVA_OBJECT ||
                dataType == Types.OTHER;
    }

    private String englishToSqlPrompt(List<Table> tables, String schema, String name, String english) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate valid SQL given the following english statement: \n")
                .append(english)
                .append("\n\nAnd this is the known tables in the database:\n");

        for (Table table : tables) {
            sb.append(getTableDefinitionAsString(table));
            sb.append("\n\n");
        }

        sb.append("\nIf you can not defer the schema name from the english statement, use ").append(schema)
                .append(" as the schema name");
        sb.append("\nIf you can not defer the table name from the english statement, use ").append(name)
                .append(" as the table name");

        sb.append(
                "\nReturn the output in a field called `sql` with the contents being valid SQL");

        sb.append(
                "\nIf you can not reliably create this sql, rather create an output with a field called error containing the reason");

        return sb.toString();
    }

    private String generateInsertPrompt(Table table, int rowCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a valid SQL script with ")
                .append(rowCount)
                .append(" INSERT statements for the following table:\n\n");

        sb.append(getTableDefinitionAsString(table));

        sb.append(
                "\nReturn the output in a field called `script` with the contents being a SQL script with valid INSERT INTO statements for ")
                .append(table.tableSchema())
                .append(".")
                .append(table.tableName())
                .append(".\n");

        return sb.toString();
    }

    private String getTableDefinitionAsString(Table table) {
        StringBuilder sb = new StringBuilder();
        sb.append("Table name: ")
                .append(table.tableSchema())
                .append(".")
                .append(table.tableName())
                .append("\n\n");

        sb.append("Columns:\n");
        for (Column column : table.columns()) {
            sb.append("- ")
                    .append(column.columnName())
                    .append(" (")
                    .append(column.columnType());
            if (column.columnType().equalsIgnoreCase("varchar")) {
                sb.append("(").append(column.columnSize()).append(")");
            }
            sb.append(", nullable: ").append(column.nullable());
            sb.append(")\n");
        }

        if (!table.primaryKeys().isEmpty()) {
            sb.append("\nPrimary key(s): ").append(String.join(", ", table.primaryKeys())).append("\n");
        }

        if (!table.foreignKeys().isEmpty()) {
            sb.append("\nForeign keys:\n");
            for (ForeignKey fk : table.foreignKeys()) {
                sb.append("- ")
                        .append(fk.columnName())
                        .append(" references ")
                        .append(fk.referencedTable())
                        .append("(")
                        .append(fk.referencedColumn())
                        .append(")\n");
            }
        }
        return sb.toString();
    }

    private static record Column(String columnName, String columnType, int columnSize, String nullable, boolean binary) {
    }

    private static record ForeignKey(String columnName, String referencedTable, String referencedColumn) {
    }

    private static record Table(String tableSchema, String tableName, List<String> primaryKeys, List<Column> columns,
            List<ForeignKey> foreignKeys) {
    }

    private static record Datasource(String name, String jdbcUrl, boolean isDefault) {
    }

    private static record DataSet(List<String> cols, List<Map<String, String>> data, String error, String message,
            int totalNumberOfElements) {
    }

    final record EnglishToSQLResponse(String sql, String error) {
    }

    final record InsertStatementResponse(String script) {
    }
}
