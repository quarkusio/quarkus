package io.quarkus.websockets.next.test.telemetry;

import java.util.Arrays;

public interface ExpectedServerEndpointResponse {

    String[] NO_RESPONSE = new String[] {};
    EchoExpectedServerEndpointResponse ECHO_RESPONSE = new EchoExpectedServerEndpointResponse();
    DoubleEchoExpectedServerEndpointResponse DOUBLE_ECHO_RESPONSE = new DoubleEchoExpectedServerEndpointResponse();

    /**
     * Received message is prefixed with 'echo 0: ' and returned.
     */
    final class EchoExpectedServerEndpointResponse implements ExpectedServerEndpointResponse {

        public String[] getExpectedResponse(String[] sentMessages) {
            return Arrays.stream(sentMessages).map(msg -> "echo 0: " + msg).toArray(String[]::new);
        }

    }

    /**
     * For each received message 'msg' endpoint returns 'echo 0: msg' and 'echo 1: msg'
     */
    final class DoubleEchoExpectedServerEndpointResponse implements ExpectedServerEndpointResponse {

        public String[] getExpectedResponse(String[] sentMessages) {
            return Arrays.stream(sentMessages)
                    .mapMulti((msg, consumer) -> {
                        consumer.accept("echo 0: " + msg);
                        consumer.accept("echo 1: " + msg);
                    })
                    .toArray(String[]::new);
        }

    }

}
