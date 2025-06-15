package io.quarkus.quartz.runtime.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QuarkusStdJDBCDelegate extends org.quartz.impl.jdbcjobstore.StdJDBCDelegate {
    /**
     * See the javadoc in {@link QuarkusObjectInputStream#resolveClass(ObjectStreamClass)} and
     * {@link DBDelegateUtils#getObjectFromInput(InputStream)} on why this is needed
     */
    @Override
    protected Object getObjectFromBlob(ResultSet rs, String colName)
            throws ClassNotFoundException, IOException, SQLException {
        Blob blobLocator = rs.getBlob(colName);
        if (blobLocator == null || blobLocator.length() == 0) {
            return null;
        }
        InputStream binaryInput = blobLocator.getBinaryStream();
        return DBDelegateUtils.getObjectFromInput(binaryInput);
    }
}
