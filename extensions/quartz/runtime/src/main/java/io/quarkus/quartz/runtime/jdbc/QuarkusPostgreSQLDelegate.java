package io.quarkus.quartz.runtime.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QuarkusPostgreSQLDelegate extends org.quartz.impl.jdbcjobstore.PostgreSQLDelegate {
    /**
     * See the javadoc in {@link QuarkusObjectInputStream#resolveClass(ObjectStreamClass)} and
     * {@link DBDelegateUtils#getObjectFromInput(InputStream)} on why this is needed
     */
    @Override
    protected Object getObjectFromBlob(ResultSet rs, String colName)
            throws ClassNotFoundException, IOException, SQLException {
        byte[] bytes = rs.getBytes(colName);
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        InputStream binaryInput = new ByteArrayInputStream(bytes);
        return DBDelegateUtils.getObjectFromInput(binaryInput);
    }
}
