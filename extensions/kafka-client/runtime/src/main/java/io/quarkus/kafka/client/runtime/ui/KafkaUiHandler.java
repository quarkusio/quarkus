
package io.quarkus.kafka.client.runtime.ui;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.concurrent.ExecutionException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.arc.Arc;
import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.kafka.client.runtime.ui.model.request.KafkaCreateTopicRequest;
import io.quarkus.kafka.client.runtime.ui.model.request.KafkaMessageCreateRequest;
import io.quarkus.kafka.client.runtime.ui.model.request.KafkaMessagesRequest;
import io.quarkus.kafka.client.runtime.ui.model.request.KafkaOffsetRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class KafkaUiHandler extends AbstractHttpRequestHandler {

    @Override
    public void handlePost(RoutingContext event) {
        if (event.body() == null) {
            endResponse(event, BAD_REQUEST, "Request body is null");
            return;
        }
        var webUtils = kafkaWebUiUtils();
        var body = webUtils.fromJson(event.body().buffer());
        if (body == null) {
            endResponse(event, BAD_REQUEST, "Request JSON body is null");
            return;
        }
        var action = body.getString("action");

        var message = "OK";
        var error = "";

        var adminClient = kafkaAdminClient();

        boolean res = false;
        if (null != action) {
            try {
                switch (action) {
                    case "getInfo":
                        message = webUtils.toJson(webUtils.getKafkaInfo());
                        res = true;
                        break;
                    case "getAclInfo":
                        message = webUtils.toJson(webUtils.getAclInfo());
                        res = true;
                        break;
                    case "createTopic":
                        var topicCreateRq = webUtils.fromJson(event.body().buffer(), KafkaCreateTopicRequest.class);
                        res = adminClient.createTopic(topicCreateRq);
                        message = webUtils.toJson(webUtils.getTopics());
                        break;
                    case "deleteTopic":
                        res = adminClient.deleteTopic(body.getString("key"));
                        message = "{}";
                        res = true;
                        break;
                    case "getTopics":
                        message = webUtils.toJson(webUtils.getTopics());
                        res = true;
                        break;
                    case "topicMessages":
                        var msgRequest = webUtils.fromJson(event.body().buffer(), KafkaMessagesRequest.class);
                        message = webUtils.toJson(webUtils.getMessages(msgRequest));
                        res = true;
                        break;
                    case "getOffset":
                        var request = webUtils.fromJson(event.body().buffer(), KafkaOffsetRequest.class);
                        message = webUtils.toJson(webUtils.getOffset(request));
                        res = true;
                        break;
                    case "createMessage":
                        var rq = webUtils.fromJson(event.body().buffer(), KafkaMessageCreateRequest.class);
                        webUtils.createMessage(rq);
                        message = "{}";
                        res = true;
                        break;
                    case "getPartitions":
                        var topicName = body.getString("topicName");
                        message = webUtils.toJson(webUtils.partitions(topicName));
                        res = true;
                        break;
                    default:
                        break;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }

        if (res) {
            endResponse(event, OK, message);
        } else {
            message = "ERROR: " + error;
            endResponse(event, BAD_REQUEST, message);
        }
    }

    private void endResponse(RoutingContext event, HttpResponseStatus status, String message) {
        event.response().setStatusCode(status.code());
        event.response().end(message);
    }

    private KafkaUiUtils kafkaWebUiUtils() {
        return Arc.container().instance(KafkaUiUtils.class).get();
    }

    @Override
    public void handleGet(RoutingContext event) {
        //TODO: move pure get requests processing here
        HttpServerRequest request = event.request();
        String path = request.path();
        endResponse(event, OK, "GET method is not supported yet. Path is: " + path);
    }

    @Override
    public void handleOptions(RoutingContext event) {
        endResponse(event, OK, "OPTION method is not supported yet");
    }

    private KafkaAdminClient kafkaAdminClient() {
        return Arc.container().instance(KafkaAdminClient.class).get();
    }

}
