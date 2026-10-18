package io.quarkus.qute.debug.breakpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.Variable;
import org.junit.jupiter.api.Test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.debug.RenderTemplateInThread;
import io.quarkus.qute.debug.adapter.RegisterDebugServerAdapter;
import io.quarkus.qute.debug.client.DAPClient;
import io.quarkus.qute.debug.client.DebuggerUtils;

public class ConditionalBreakpointTest {

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

        // Render template without breakpoint
        final StringBuilder renderResult = new StringBuilder(1028);
        var renderThread = new RenderTemplateInThread(template, renderResult, instance -> {
            instance.data("name", "Quarkus") //
                    .data("items", List.of("foo", "bar", "baz"));
        });

        assertEquals("<html>\n" + //
                "   Hello Quarkus!\n" + //
                "        foo\n" + //
                "        1\n" + //
                "        bar\n" + //
                "        2\n" + //
                "        baz\n" + //
                "        3\n" + //
                "</html>", renderResult.toString());

        // Set a breakpoint on line 5: --> {item_count}
        client.setBreakpoint("src/main/resources/templates/" + TEMPLATE_ID, 5);

        // Render template with a breakpoint
        renderResult.setLength(0);
        renderThread.render();

        // Result here is empty
        assertEquals("", renderResult.toString());

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

        // Render template with breakpoint on line 4
        renderResult.setLength(0);
        renderThread.render();

        // Result here is empty
        assertEquals("", renderResult.toString());

        // Collect debuggee Thread (two threads)
        threads = client.getThreads();
        assertEquals(2, threads.length);

        thread = threads[1];
        threadId = thread.getId();
        assertEquals("Qute render thread", thread.getName());

        // Get stack trace of the debuggee Thread
        stackFrames = client.getStackFrames(threadId);
        currentFrame = stackFrames[0];
        // Stack frame on item_count
        frameId = currentFrame.getId();
        frameName = currentFrame.getName();
        assertEquals(
                "ExpressionNode [expression=Expression [namespace=null, parts=[item_count], literal=null]]",
                frameName);

        // Evaluate item_count
        var evalResult = client.evaluateSync(frameId, "item_count");
        assertEquals("1", evalResult.getResult());

        // Get scope (Globals, Locals, etc) of the current stack frame
        Scope[] scopes = client.getScopes(frameId);
        assertFalse(scopes.length == 0);
        Scope globalsScope = scopes[1];
        assertEquals("Globals", globalsScope.getName());

        // Get variables of the Globals scope
        // [name=Quarkus, ..]
        int variablesReference = globalsScope.getVariablesReference();
        Variable[] variables = client.getVariables(variablesReference);
        assertEquals(2, variables.length);

        Variable firstVar = variables[0];
        assertEquals("name", firstVar.getName());
        assertEquals("Quarkus", firstVar.getValue());
        assertEquals("java.lang.String", firstVar.getType());

        Variable secondVar = variables[1];
        assertEquals("items", secondVar.getName());
        assertEquals("[foo, bar, baz]", secondVar.getValue());
        assertEquals("java.util.ImmutableCollections$ListN", secondVar.getType());

        // Set a breakpoint on line 5: --> {item_count} with condition
        client.setBreakpoint("src/main/resources/templates/" + TEMPLATE_ID, 5, "item_count > 2");

        // Resume (_continue) the breakpoint
        client.resume(threadId);
        java.lang.Thread.sleep(1000);

        stackFrames = client.getStackFrames(threadId);
        currentFrame = stackFrames[0];
        // Stack frame on item_count
        frameId = currentFrame.getId();
        frameName = currentFrame.getName();
        assertEquals(
                "ExpressionNode [expression=Expression [namespace=null, parts=[item_count], literal=null]]",
                frameName);

        // Evaluate item_count
        evalResult = client.evaluateSync(frameId, "item_count");
        assertEquals("3", evalResult.getResult());

        client.resume(threadId);
        java.lang.Thread.sleep(1000);

        // Result here is:
        // <!DOCTYPE html>
        // <html>
        // <body>
        // <h1>Hello <b>Qute</b></h1>
        // </body>
        // </html>
        java.lang.Thread.sleep(1000);
        assertEquals("<html>\n" + //
                "   Hello Quarkus!\n" + //
                "        foo\n" + //
                "        1\n" + //
                "        bar\n" + //
                "        2\n" + //
                "        baz\n" + //
                "        3\n" + //
                "</html>", renderResult.toString());

        // On client side, disconnect the client
        client.terminate();
        // On server side, terminate the server
        // server.terminate();

    }

}