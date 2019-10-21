package io.quarkus.it.amazon.dynamodb;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context.ModifyResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

public class DynamoDBModifyResponse implements ExecutionInterceptor {

    @Override
    public SdkResponse modifyResponse(ModifyResponse context, ExecutionAttributes executionAttributes) {
        if (context.response() instanceof GetItemResponse) {
            GetItemResponse response = (GetItemResponse) context.response();
            return response.copy(r -> r.item(modifyItem(response.item())));
        }
        return context.response();
    }

    private Map<String, AttributeValue> modifyItem(Map<String, AttributeValue> item) {
        Map<String, AttributeValue> modifiedItem = new HashMap<>(item);

        modifiedItem.put(DynamoDBUtils.PAYLOAD_NAME, modifyPayload(item.get(DynamoDBUtils.PAYLOAD_NAME)));

        return modifiedItem;
    }

    private AttributeValue modifyPayload(AttributeValue payload) {
        return AttributeValue.builder().s("INTERCEPTED " + payload.s()).build();
    }
}
