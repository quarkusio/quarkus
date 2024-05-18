package io.quarkus.mongodb.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

class MongoReactiveContextProviderTest {

    @Test
    void getContext() {
        var provider = new MongoReactiveContextProvider();
        assertThat(provider.getContext(mock())).isInstanceOf(MongoRequestContext.class);
    }

}
