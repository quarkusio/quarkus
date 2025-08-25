package io.quarkus.qute.debug.completions;

import static io.quarkus.qute.debug.QuteAssert.assertCompletion;
import static io.quarkus.qute.debug.QuteAssert.c;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;
import org.eclipse.lsp4j.debug.StackFrame;
import org.junit.jupiter.api.Test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.debug.RenderTemplateInThread;
import io.quarkus.qute.debug.adapter.RegisterDebugServerAdapter;
import io.quarkus.qute.debug.client.DAPClient;
import io.quarkus.qute.debug.client.DebuggerUtils;
import io.quarkus.qute.debug.data.Item;

public class CompletionArrayListTest {

    private static final String TEMPLATE_ID = "hello.qute";

    @Test
    public void debuggingTemplate() throws Exception {
        int port = DebuggerUtils.findAvailableSocketPort();

        // Server side :
        // - create a Qute engine and set the debugging port as 1234
        Engine engine = Engine.builder() //
                .enableTracing(true) // enable tracing required by debugger
                .addEngineListener(new RegisterDebugServerAdapter(port, false)) // debug engine on the given port
                .addDefaults()//
                .addValueResolver(new ReflectionValueResolver()) //
                .build();

        // - create a Qute template
        Template template = engine.parse("<html>\n" + //
                "   Hello {name}!\n" + //
                "   {#for item in items}\n" + //
                "        {item.price}\n" + //
                "        {item_count}\n" + //
                "   {/for}\n" + //
                "</html>", null, TEMPLATE_ID);

        // Client side
        // - connect the remote debugger client on the given port
        DAPClient client = new DAPClient();
        client.connectToServer(port) //
                .get(10000, TimeUnit.MILLISECONDS);

        // Set a breakpoint on line 5: --> {item_count}
        client.setBreakpoint("src/main/resources/templates/" + TEMPLATE_ID, 5);

        // Render template with breakpoint on line 5
        final StringBuilder renderResult = new StringBuilder(1028);
        new RenderTemplateInThread(template, renderResult, instance -> {
            instance.data("name", "Quarkus");
            instance.data("items", List.of(new Item(10), new Item(20), new Item(30)).toArray());
        });

        // Collect debuggee Thread (one thread)
        var threads = client.getThreads();
        assertEquals(1, threads.length);

        var thread = threads[0];
        int threadId = thread.getId();
        assertEquals("Qute render thread", thread.getName());

        // Get stack trace of the debuggee Thread
        StackFrame[] stackFrames = client.getStackFrames(threadId);
        StackFrame currentFrame = stackFrames[0];

        int frameId = currentFrame.getId();
        String frameName = currentFrame.getName();
        assertEquals("ExpressionNode [expression=Expression [namespace=null, parts=[item_count], literal=null]]",
                frameName);

        // Execute completion on current frame context

        // completion with data model root
        CompletionItem[] items = client.completion("", 1, 1, frameId);
        assertNotNull(items, "");
        assertCompletion(new CompletionItem[] { //
                c("name", CompletionItemType.REFERENCE), //
                c("items", CompletionItemType.REFERENCE), //
                c("item", CompletionItemType.REFERENCE), //
                c("item_count", CompletionItemType.REFERENCE), //
                c("item_index", CompletionItemType.REFERENCE) }, items);

        // completion with value resolvers
        items = client.completion("items.", 1, 7, frameId);
        assertNotNull(items, "");
        assertCompletion(new CompletionItem[] { //
                c("size", CompletionItemType.PROPERTY), //
                c("length", CompletionItemType.PROPERTY), //
                c("|index|", CompletionItemType.PROPERTY), //
                c("take(|index|)", CompletionItemType.FUNCTION), //
                c("takeLast(|index|)", CompletionItemType.FUNCTION), //
                c("get(|index|)", CompletionItemType.FUNCTION) }, items);

        // completion with reflection
        items = client.completion("item.", 1, 6, frameId);
        assertNotNull(items, "");
        assertCompletion(new CompletionItem[] { //
                c("price", CompletionItemType.FIELD), //
                c("toString()", CompletionItemType.METHOD) }, items);

        // On client side, disconnect the client
        client.terminate();
        // On server side, terminate the server
        // server.terminate();

    }

}