package io.quarkus.axon.deployment.querytest;

import org.axonframework.queryhandling.QueryHandler;

import io.quarkus.axon.deployment.aggregatetest.TestItem;

public class QueryTestHandler {

    public QueryTestHandler() {
    }

    @QueryHandler
    public TestItem on(RequestTestItemQuery itemQuery) {
        return new TestItem(itemQuery.getLookupId(), "some text from queryhandler");
    }

}
