package io.quarkus.qute.debug.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.junit.jupiter.api.Test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.debug.RenderTemplateInThread;
import io.quarkus.qute.debug.adapter.RegisterDebugServerAdapter;
import io.quarkus.qute.debug.client.DAPClient;
import io.quarkus.qute.debug.client.DebuggerUtils;

public class EvaluationListTest {

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
        Template template = engine.parse("<html>\n" + //
                "   Hello {name}!\n" + //
                "   {#for item in items}\n" + //
                "        {item}\n" + //
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
            instance.data("name", "Quarkus") //
                    .data("items", List.of("foo", "bar", "baz"));
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

        // Evaluate expressions on current frame context

        // String expression
        var result = client.evaluateSync(frameId, "name");
        assertNotNull(result);
        assertEquals("Quarkus", result.getResult());

        result = client.evaluateSync(frameId, "name.charAt(0)");
        assertNotNull(result);
        assertEquals("Q", result.getResult());

        // List<String> expression
        result = client.evaluateSync(frameId, "items");
        assertNotNull(result);
        assertEquals("[foo, bar, baz]", result.getResult());

        result = client.evaluateSync(frameId, "items.size");
        assertNotNull(result);
        assertEquals("3", result.getResult());

        result = client.evaluateSync(frameId, "items[1]");
        assertNotNull(result);
        assertEquals("bar", result.getResult());

        // Condition expression
        result = client.evaluateSync(frameId, "items.size >= 3");
        assertNotNull(result);
        assertEquals("true", result.getResult());

        // Invalid expression
        Exception ex = assertThrows(ResponseErrorException.class, () -> {
            client.evaluateSync(frameId, "items.XXX");
        });
        assertEquals(
                "Rendering error: Property \"XXX\" not found on the base object \"java.util.ImmutableCollections$ListN\" in expression {items.XXX}",
                ex.getMessage());

        // On client side, disconnect the client
        client.terminate();
        // On server side, terminate the server
        // server.terminate();

    }

}