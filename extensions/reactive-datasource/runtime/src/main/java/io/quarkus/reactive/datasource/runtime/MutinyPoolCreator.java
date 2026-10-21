package io.quarkus.reactive.datasource.runtime;

import static io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil.qualifier;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.vertx.sqlclient.Pool;

public class MutinyPoolCreator implements BeanCreator<io.vertx.mutiny.sqlclient.Pool> {

    public static final String DATA_SOURCE_NAME = "dataSourceName";

    public static ActiveResult checkActive(String dataSourceName) {
        return ReactiveDataSourceUtil.dataSourceInstance(dataSourceName).getHandle().getBean().checkActive();
    }

    @Override
    public io.vertx.mutiny.sqlclient.Pool create(SyntheticCreationalContext<io.vertx.mutiny.sqlclient.Pool> context) {
        String dataSourceName = (String) context.getParams().get(DATA_SOURCE_NAME);
        return io.vertx.mutiny.sqlclient.Pool.newInstance(
                context.getInjectedReference(Pool.class, qualifier(dataSourceName)));
    }
}
