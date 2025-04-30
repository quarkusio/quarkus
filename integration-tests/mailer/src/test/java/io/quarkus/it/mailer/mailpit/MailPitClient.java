package io.quarkus.it.mailer.mailpit;

import java.util.ArrayList;
import java.util.List;

import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

/**
 * Very simple MailPit API client to verify the tests
 */
public class MailPitClient {

    private final String root;

    public MailPitClient(String root) {
        this.root = root;
    }

    public MessageSummary getMessages() {
        return RestAssured.get(root + "/api/v1/messages")
                .then().statusCode(200)
                .extract().body().as(MessageSummary.class);
    }

    public String getMessageTextContent(Message message) {
        return RestAssured.get(root + "/view/" + message.id + ".txt")
                .then().statusCode(200)
                .extract().asString();
    }

    public String getMessageHtmlContent(Message message) {
        return RestAssured.get(root + "/view/" + message.id + ".html")
                .then().statusCode(200)
                .extract().asString();
    }

    public JsonObject getMessageHeaders(Message message) {
        String string = RestAssured.get(root + "/api/v1/message/" + message.id + "/headers")
                .then().statusCode(200)
                .extract().body().asString();

        return new JsonObject(string);
    }

    public String getRawSource(Message message) {
        return RestAssured.get(root + "/api/v1/message/" + message.id + "/raw")
                .then().statusCode(200)
                .extract().body().asString();
    }

    public void deleteAllMessages() {
        RestAssured.delete(root + "/api/v1/messages")
                .then().statusCode(200);
    }

    public List<Attachment> getAttachments(Message message) {
        DetailedMessage details = RestAssured.get(root + "/api/v1/message/" + message.id)
                .then().statusCode(200)
                .extract().body().as(DetailedMessage.class);

        List<Attachment> attachments = new ArrayList<>();
        for (Attachment attachment : details.attachments) {
            byte[] binary = RestAssured.get(root + "/api/v1/message/" + message.id + "/part/" + attachment.partId)
                    .then().statusCode(200)
                    .extract().body().asByteArray();
            attachments.add(attachment.addBinary(binary));
        }

        return attachments;
    }
}
