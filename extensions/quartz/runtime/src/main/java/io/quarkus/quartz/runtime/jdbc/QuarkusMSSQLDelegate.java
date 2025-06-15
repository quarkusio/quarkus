package io.quarkus.quartz.runtime.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QuarkusMSSQLDelegate extends org.quartz.impl.jdbcjobstore.MSSQLDelegate {
    /**
     * See the javadoc in {@link QuarkusObjectInputStream#resolveClass(ObjectStreamClass)} and
     * {@link DBDelegateUtils#getObjectFromInput(InputStream)} on why this is needed
     */
    @Override
    protected Object getObjectFromBlob(ResultSet rs, String colName)
            throws ClassNotFoundException, IOException, SQLException {
        InputStream binaryInput = rs.getBinaryStream(colName);
        return DBDelegateUtils.getObjectFromInput(binaryInput);
    }
}
