package io.quarkus.arc.test.contexts.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.ConversationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Conversation context is not supported in ArC by default; users can still supply their own context impl though.
 * We just want to test that it is recognized as a scope and properly fails instead of falling back to @Dependent
 */
public class ConversationContextTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(ConversationBean.class);

    @Test
    public void testContexts() {
        InstanceHandle<ConversationBean> handle = Arc.container().select(ConversationBean.class).getHandle();
        assertEquals(ConversationScoped.class, handle.getBean().getScope());
        ConversationBean conversationBean = handle.get();
        assertNotNull(conversationBean);
        // actual attempt to use the bean should result in ContextNotActiveException as we don't provide any impl
        assertThrows(ContextNotActiveException.class, () -> conversationBean.ping());
    }
}
