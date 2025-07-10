package io.quarkus.qute.debug.breakpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
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

public class BreakpointTest {

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

        // Render template with no breakpoint
        final StringBuilder renderResult = new StringBuilder(1028);
        var renderThread = new RenderTemplateInThread(template, renderResult, instance -> {
            instance.data("name", "Quarkus");
        });
        // Result here is:
        // <!DOCTYPE html>
        // <html>
        // <body>
        // <h1>Hello <b>Quarkus</b></h1>
        // </body>
        // </html>
        assertEquals("<!DOCTYPE html>\r\n" + //
                "<html>\r\n" + //
                "<body>\r\n" + //
                "<h1>Hello <b>Quarkus</b></h1>\r\n" + //
                "</body>\r\n" + //
                "</html>\r\n", renderResult.toString());

        // Set a breakpoint on line 4: --> <h1>Hello <b>Qute</b></h1>
        client.setBreakpoint("src/main/resources/templates/" + TEMPLATE_ID, 4);

        // Render template with breakpoint on line 4
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
        // Stack frame:
        // name = ExpressionNode [expression=Expression [namespace=null, parts=[name,
        // ?:("Qute")], literal=null]]
        int frameId = currentFrame.getId();
        String frameName = currentFrame.getName();
        assertEquals(
                "ExpressionNode [expression=Expression [namespace=null, parts=[name, ?:(\"Qute\")], literal=null]]",
                frameName);

        // Test stack frames pagination
        assertEquals(2, stackFrames.length);
        StackFrame[] paginatedFrames = client.getStackFrames(threadId, 0, 1).getStackFrames();
        assertEquals(1, paginatedFrames.length);
        assertEquals(0, Arrays.asList(stackFrames).indexOf(paginatedFrames[0]));

        paginatedFrames = client.getStackFrames(threadId, 1, 1).getStackFrames();
        assertEquals(1, paginatedFrames.length);
        assertEquals(1, Arrays.asList(stackFrames).indexOf(paginatedFrames[0]));

        paginatedFrames = client.getStackFrames(threadId, 0, 2).getStackFrames();
        assertEquals(2, paginatedFrames.length);

        paginatedFrames = client.getStackFrames(threadId, 0, 3).getStackFrames();
        assertEquals(2, paginatedFrames.length);

        paginatedFrames = client.getStackFrames(threadId, 1, 2).getStackFrames();
        assertEquals(1, paginatedFrames.length);
        // Get scope (Globals, Locals, etc) of the current stack frame
        Scope[] scopes = client.getScopes(frameId);
        assertFalse(scopes.length == 0);
        Scope globalsScope = scopes[1];
        assertEquals("Globals", globalsScope.getName());

        // Get variables of the Globals scope
        // [name=Quarkus, ..]
        int variablesReference = globalsScope.getVariablesReference();
        Variable[] variables = client.getVariables(variablesReference);
        assertEquals(1, variables.length);

        Variable firstVar = variables[0];
        assertEquals("name", firstVar.getName());
        assertEquals("Quarkus", firstVar.getValue());
        assertEquals("java.lang.String", firstVar.getType());

        // Resume (_continue) the breakpoint
        client.resume(threadId);
        // Result here is:
        // <!DOCTYPE html>
        // <html>
        // <body>
        // <h1>Hello <b>Qute</b></h1>
        // </body>
        // </html>
        java.lang.Thread.sleep(1000);
        assertEquals("<!DOCTYPE html>\r\n" + //
                "<html>\r\n" + //
                "<body>\r\n" + //
                "<h1>Hello <b>Quarkus</b></h1>\r\n" + //
                "</body>\r\n" + //
                "</html>\r\n", renderResult.toString());

        // On client side, disconnect the client
        client.terminate();
        // On server side, terminate the server
        // server.terminate();

    }

}