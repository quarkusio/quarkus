package io.quarkus.amazon.lambda.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.Test;

public class AmazonLambdaContextTest {

    @Test
    public void testGetRemainingTimeInMillis() throws Exception {
        HttpURLConnection request = mock(HttpURLConnection.class);

        // We add 5 seconds to the current date and set this as lambda deadline, so we know there is still some remaining time left
        when(request.getHeaderField(AmazonLambdaApi.LAMBDA_RUNTIME_DEADLINE_MS))
                .thenReturn(String.valueOf(System.currentTimeMillis() + 5000L));

        AmazonLambdaContext amazonLambdaContext = new AmazonLambdaContext(request, null, null);

        assertTrue(amazonLambdaContext.getRemainingTimeInMillis() > 0, "The remaining time in millis should not be negative");
    }
}
