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

import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.quarkus.agroal.runtime.AgroalDataSourceSupport;
import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.Arc;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;

public final class DatabaseInspector {

    @Inject
    Instance<DataSources> dataSources;

    private final Map<String, DataSource> checkedDataSources = new HashMap<>();

    private boolean isDev = false;
    private boolean allowSql = false;
    private String allowedHost = null;

    public DatabaseInspector() {
        LaunchMode currentMode = LaunchMode.current();
        this.isDev = currentMode == LaunchMode.DEVELOPMENT && !LaunchMode.isRemoteDev();

        Config config = ConfigProvider.getConfig();
        this.allowSql = config.getOptionalValue("quarkus.datasource.dev-ui.allow-sql", Boolean.class)
                .orElse(false);

        this.allowedHost = config.getOptionalValue("quarkus.datasource.dev-ui.allowed-db-host", String.class)
                .orElse(null);

    }

    @PostConstruct
    protected void init() {
        if (!dataSources.isResolvable()) {
            // No configured Agroal datasource at build time.
            return;
        }

        if (isDev) {
            AgroalDataSourceSupport agroalSupport = Arc.container().instance(AgroalDataSourceSupport.class)
                    .get();
            for (String name : agroalSupport.entries.keySet()) {
                DataSource ds = dataSources.get().getDataSource(name);
                if (ds != null) {
                    checkedDataSources.put(name, ds);
                }
            }
        }
    }

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

    private Datasource getDatasource(String datasource) {
        if (isDev) {
            AgroalDataSource ads = (AgroalDataSource) checkedDataSources.get(datasource);
            if (isAllowedDatabase(ads)) {
                AgroalDataSourceConfiguration configuration = ads.getConfiguration();

                String jdbcUrl = configuration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl();
                boolean isDefault = DataSourceUtil.isDefault(datasource);

                return new Datasource(datasource, jdbcUrl, isDefault);
            }
        }
        return null;
    }

    public List<Table> getTables(String datasource) {
        if (isDev) {
            List<Table> tableList = new ArrayList<>();
            try {
                AgroalDataSource ads = (AgroalDataSource) checkedDataSources.get(datasource);
                if (isAllowedDatabase(ads)) {
                    try (Connection connection = ads.getConnection()) {
                        DatabaseMetaData metaData = connection.getMetaData();

                        // Get all tables
                        try (ResultSet tables = metaData.getTables(null, null, "%", new String[] { "TABLE" })) {
                            while (tables.next()) {
                                String tableName = tables.getString("TABLE_NAME");
                                String tableSchema = tables.getString("TABLE_SCHEM");

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
                                tableList.add(new Table(tableSchema, tableName, primaryKeyList, columnList));
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

    public DataSet executeSQL(String datasource, String sql, Integer pageNumber, Integer pageSize) {
        if (isDev && sqlIsValid(sql)) {
            try {
                AgroalDataSource ads = (AgroalDataSource) checkedDataSources.get(datasource);
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
                                                boolean isBinary = isBinary(metaData.getColumnType(i));
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
                    return new DataSet(null, null, "Only supported for Local Databases", null, -1);
                }
            } catch (SQLException ex) {
                return new DataSet(null, null, ex.getMessage(), null, -1);
            }
        } else {
            return new DataSet(null, null, "Unknown Error", null, -1);
        }
    }

    public String getInsertScript(String datasource) {
        if (isDev) {
            try {
                AgroalDataSource ads = (AgroalDataSource) checkedDataSources.get(datasource);
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
        AgroalDataSourceConfiguration configuration = ads.getConfiguration();
        String jdbcUrl = configuration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl();

        try {
            if (jdbcUrl.startsWith("jdbc:h2:mem:") || jdbcUrl.startsWith("jdbc:h2:file:")
                    || jdbcUrl.startsWith("jdbc:h2:tcp://localhost")
                    || (this.allowedHost != null && !this.allowedHost.isBlank()
                            && jdbcUrl.startsWith("jdbc:h2:tcp://" + this.allowedHost))
                    || jdbcUrl.startsWith("jdbc:derby:memory:")) {
                return true;
            }

            String cleanUrl = jdbcUrl.replace("jdbc:", "");
            URI uri = new URI(cleanUrl);

            String host = uri.getHost();

            return host != null && ((host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")) ||
                    (this.allowedHost != null && !this.allowedHost.isBlank() && host.equalsIgnoreCase(this.allowedHost)));

        } catch (URISyntaxException e) {
            Log.warn(e.getMessage());
        }

        return false;
    }

    private boolean isBinary(int dataType) {
        return dataType == Types.BLOB ||
                dataType == Types.VARBINARY ||
                dataType == Types.LONGVARBINARY ||
                dataType == Types.BINARY ||
                dataType == Types.JAVA_OBJECT ||
                dataType == Types.OTHER;
    }

    private static record Column(String columnName, String columnType, int columnSize, String nullable, boolean binary) {
    }

    private static record Table(String tableSchema, String tableName, List<String> primaryKeys, List<Column> columns) {
    }

    private static record Datasource(String name, String jdbcUrl, boolean isDefault) {
    }

    private static record DataSet(List<String> cols, List<Map<String, String>> data, String error, String message,
            int totalNumberOfElements) {
    }
}
