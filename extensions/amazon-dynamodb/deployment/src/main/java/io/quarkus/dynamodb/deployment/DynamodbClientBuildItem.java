package io.quarkus.dynamodb.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DynamodbClientBuildItem extends SimpleBuildItem {

    private final boolean createSyncClient;
    private final boolean createAsyncClient;

    public DynamodbClientBuildItem(boolean createSyncClient, boolean createAsyncClient) {
        this.createSyncClient = createSyncClient;
        this.createAsyncClient = createAsyncClient;
    }

    public boolean isCreateSyncClient() {
        return createSyncClient;
    }

    public boolean isCreateAsyncClient() {
        return createAsyncClient;
    }
}
