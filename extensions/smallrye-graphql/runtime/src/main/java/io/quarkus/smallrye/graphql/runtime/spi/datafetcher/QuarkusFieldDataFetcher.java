package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import graphql.schema.DataFetchingEnvironment;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.smallrye.graphql.execution.datafetcher.FieldDataFetcher;
import io.smallrye.graphql.schema.model.Field;
import io.smallrye.graphql.schema.model.Reference;
import io.smallrye.graphql.schema.model.Type;

public class QuarkusFieldDataFetcher<T> extends FieldDataFetcher<T> {

    public QuarkusFieldDataFetcher(final Field field, final Type type, final Reference owner) {
        super(field, type, owner);
    }

    @Override
    public T get(DataFetchingEnvironment dfe) throws Exception {
        ManagedContext requestContext = Arc.container().requestContext();
        try {
            RequestContextHelper.reactivate(requestContext, dfe);
            return super.get(dfe);
        } finally {
            requestContext.deactivate();
        }
    }
}
