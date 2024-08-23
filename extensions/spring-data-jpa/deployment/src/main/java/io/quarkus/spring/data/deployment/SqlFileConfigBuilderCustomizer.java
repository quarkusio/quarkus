package io.quarkus.spring.data.deployment;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class SqlFileConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(SmallRyeConfigBuilder builder) {
        List<String> supportedSqlFiles = List.of("import.sql", "data.sql");
        List<String> sqlFilesThatExist = new ArrayList<>();

        for (String sqlFile : supportedSqlFiles) {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(sqlFile);
            // we only check for files that are part of the application itself,
            // this is done as to follow what the HibernateOrmProcessor does
            if ((resource != null) && !resource.getProtocol().equals("jar")) {
                sqlFilesThatExist.add(sqlFile);
            }
        }

        // use a priority of 50 to make sure that this is overridable by any of the standard methods
        if (!sqlFilesThatExist.isEmpty()) {
            builder.withSources(
                    new PropertiesConfigSource(
                            Map.of("quarkus.hibernate-orm.sql-load-script", String.join(",", sqlFilesThatExist)),
                            "quarkus-spring-data-jpa", 50));
        }

    }
}
