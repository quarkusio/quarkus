package io.quarkus.funqy.gcp.functions.event;

import java.util.Map;

/**
 * Background function event for Pubsub
 *
 * @see <a href="https://cloud.google.com/pubsub/docs/reference/rest/v1/PubsubMessage">PubsubMessage</a>
 */
public class PubsubMessage {
    public String data;
    public Map<String, String> attributes;
    public String messageId;
    public String publishTime;
}
