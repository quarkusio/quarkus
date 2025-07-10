package io.quarkus.qute.debug.variables;

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
import io.quarkus.qute.debug.data.Item;

public class VariablesTest {

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
            instance.data("items", List.of(new Item(10), new Item(20), new Item(30)));
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

        // [name=Quarkus, ..]
        Variable nameVar = variables[0];
        assertEquals("name", nameVar.getName());
        assertEquals("Quarkus", nameVar.getValue());
        assertEquals("java.lang.String", nameVar.getType());

        // [items=, ..]
        Variable itemsVar = variables[1];
        assertEquals("items", itemsVar.getName());
        assertEquals("[Item(10), Item(20), Item(30)]", itemsVar.getValue());
        assertEquals("java.util.ImmutableCollections$ListN", itemsVar.getType());

        // Get variables of the items
        Variable[] itemsVariables = client.getVariables(itemsVar.getVariablesReference());
        assertEquals(3, itemsVariables.length);

        Variable firstItem = itemsVariables[0];
        assertEquals("0", firstItem.getName());
        assertEquals("Item(10)", firstItem.getValue());
        assertEquals("io.quarkus.qute.debug.data.Item", firstItem.getType());

        // Get variables of the first item
        Variable[] firstItemVariables = client.getVariables(firstItem.getVariablesReference());
        assertEquals(1, firstItemVariables.length);

        firstItem = firstItemVariables[0];
        assertEquals("price", firstItem.getName());
        assertEquals("10", firstItem.getValue());
        assertEquals("java.math.BigDecimal", firstItem.getType());

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