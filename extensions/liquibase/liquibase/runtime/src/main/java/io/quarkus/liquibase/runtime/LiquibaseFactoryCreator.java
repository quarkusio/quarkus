package io.quarkus.liquibase.runtime;

import javax.sql.DataSource;

import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.liquibase.LiquibaseFactory;

public class LiquibaseFactoryCreator implements BeanCreator<LiquibaseFactory> {

    public static final String DATA_SOURCE_NAME = "dataSourceName";

    @Override
    public LiquibaseFactory create(SyntheticCreationalContext<LiquibaseFactory> context) {
        String dataSourceName = (String) context.getParams().get(DATA_SOURCE_NAME);
        DataSource dataSource = context.getInjectedReference(DataSource.class,
                AgroalDataSourceUtil.qualifier(dataSourceName));
        LiquibaseFactoryProducer liquibaseProducer = context.getInjectedReference(LiquibaseFactoryProducer.class);
        return liquibaseProducer.createLiquibaseFactory(dataSource, dataSourceName);
    }
}
