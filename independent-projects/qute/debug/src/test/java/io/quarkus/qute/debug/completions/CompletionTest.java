package io.quarkus.qute.debug.completions;

import static io.quarkus.qute.debug.QuteAssert.assertCompletion;
import static io.quarkus.qute.debug.QuteAssert.c;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

public class CompletionTest {

    private static final String TEMPLATE_ID = "hello.qute";

    @Test
    public void debuggingTemplate() throws Exception {
        int port = DebuggerUtils.findAvailableSocketPort();

        // Server side :
        // - create a Qute engine and set the debugging port as 1234
        Engine engine = Engine.builder() //
                .enableTracing(true) // enable tracing required by debugger
                .addEngineListener(new RegisterDebugServerAdapter(port, false)) // debug engine on the given port
                .addDefaults().addValueResolver(new ReflectionValueResolver()).build();

        // - create a Qute template
        Template template = engine.parse("<!DOCTYPE html>\r\n" + //
                "<html>\r\n" + //
                "<body>\r\n" + //
                "<h1>Hello <b>{name ?: \"Qute\"}</b></h1>\r\n" + //
                "</body>\r\n" + //
                "</html>\r\n", null, TEMPLATE_ID);

        // Client side
        // - connect the remote debugger client on the given port
        DAPClient client = new DAPClient();
        client.connectToServer(port) //
                .get(10000, TimeUnit.MILLISECONDS);

        // Set a breakpoint on line 4: --> <h1>Hello <b>Qute</b></h1>
        client.setBreakpoint("src/main/resources/templates/" + TEMPLATE_ID, 4);

        // Render template with breakpoint on line 4
        final StringBuilder renderResult = new StringBuilder(1028);
        new RenderTemplateInThread(template, renderResult, instance -> {
            instance.data("name", "Quarkus");
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
        // Stack frame:
        // name = ExpressionNode [expression=Expression [namespace=null, parts=[name,
        // ?:("Qute")], literal=null]]
        int frameId = currentFrame.getId();
        String frameName = currentFrame.getName();
        assertEquals(
                "ExpressionNode [expression=Expression [namespace=null, parts=[name, ?:(\"Qute\")], literal=null]]",
                frameName);

        // Check completion
        CompletionItem[] items = client.completion("", 0, 0, frameId);
        assertNotNull(items, "");
        assertCompletion(new CompletionItem[] { c("name", CompletionItemType.REFERENCE) }, items);

        // On client side, disconnect the client
        client.terminate();
        // On server side, terminate the server
        // server.terminate();

    }

}