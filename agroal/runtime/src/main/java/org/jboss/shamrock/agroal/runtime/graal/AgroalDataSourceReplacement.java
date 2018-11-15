package org.jboss.shamrock.agroal.runtime.graal;

import java.sql.SQLException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceListener;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.agroal.pool.DataSource;

@TargetClass(AgroalDataSource.class)
final class AgroalDataSourceReplacement {

    @Substitute
    static AgroalDataSource from(AgroalDataSourceConfiguration configuration, AgroalDataSourceListener... listeners) throws SQLException {
        return new DataSource(configuration, listeners);

    }

}
