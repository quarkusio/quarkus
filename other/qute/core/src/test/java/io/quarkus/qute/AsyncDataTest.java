package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

public class AsyncDataTest {

    @Test
    public void testAsyncData() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(ValueResolver.builder().applyToBaseClass(Client.class)
                .applyToName("tokens").resolveSync(ec -> ((Client) ec.getBase()).getTokens()).build()).build();
        assertEquals("alpha:bravo:delta:",
                engine.parse("{#for token in client.tokens}{token}:{/for}").data("client", new Client()).render());
        assertEquals("alpha:bravo:delta:",
                engine.parse("{#for token in tokens}{token}:{/for}").data("tokens", new Client().getTokens()).render());
        assertEquals("alpha", engine.parse("{token}").data("token", CompletedStage.of("alpha")).render());
    }

    static class Client {

        public CompletionStage<List<String>> getTokens() {
            CompletableFuture<List<String>> tokens = new CompletableFuture<>();
            tokens.complete(Arrays.asList("alpha", "bravo", "delta"));
            return tokens;
        }

    }

}
