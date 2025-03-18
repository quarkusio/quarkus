package io.quarkus.liquibase.common.runtime.graal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import liquibase.Scope;
import liquibase.util.LiquibaseUtil;

@TargetClass(liquibase.util.LiquibaseUtil.class)
final class SubstituteLiquibaseUtil {

    @Alias
    private static Properties liquibaseBuildProperties;

    @Substitute
    private static String getBuildInfo(String propertyId) {
        // this is a bit of a mess: we have to get rid of the entire first part that accesses osgi classes
        // and only retain the second part
        // taken from: https://github.com/liquibase/liquibase/blob/v4.7.1/liquibase-core/src/main/java/liquibase/util/LiquibaseUtil.java#L57-L91
        if (liquibaseBuildProperties == null) {
            try {
                liquibaseBuildProperties = new Properties();
                final Enumeration<URL> propertiesUrls = Scope.getCurrentScope().getClassLoader()
                        .getResources("liquibase.build.properties");
                while (propertiesUrls.hasMoreElements()) {
                    final URL url = propertiesUrls.nextElement();
                    try (InputStream buildProperties = url.openStream()) {
                        if (buildProperties != null) {
                            liquibaseBuildProperties.load(buildProperties);
                        }
                    }
                }
            } catch (IOException e) {
                Scope.getCurrentScope().getLog(LiquibaseUtil.class).severe("Cannot read liquibase.build.properties", e);
            }
        }

        String value;
        value = liquibaseBuildProperties.getProperty(propertyId);
        if (value == null) {
            value = "UNKNOWN";
        }
        return value;
    }
}
