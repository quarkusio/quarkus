package io.quarkus.amazon.lambda.runtime;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;

import org.junit.Test;

public class AmazonLambdaContextTest {

    @Test
    public void testGetRemainingTimeInMillis() throws Exception {
        HttpURLConnection request = mock(HttpURLConnection.class);

        // We add 5 seconds to the current date and set this as lambda deadline, so we know there is still some remaining time left
        when(request.getHeaderField(AmazonLambdaApi.LAMBDA_RUNTIME_DEADLINE_MS))
                .thenReturn(String.valueOf(System.currentTimeMillis() + 5000L));

        AmazonLambdaContext amazonLambdaContext = new AmazonLambdaContext(request, null, null);

        assertTrue("The remaining time in millis should not be negative", amazonLambdaContext.getRemainingTimeInMillis() > 0);
    }
}
