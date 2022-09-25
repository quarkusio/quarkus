package io.quarkus.qute.debug;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.debug.client.RemoteDebuggerClient;
import io.quarkus.qute.debug.server.RemoteDebuggerServer;
import java.lang.Thread;

public class DebuggingQuteTemplate {

    public static void main(String[] args) throws Exception {

        // Server side :
        // - create a Qute engine
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        // - and attach it to a remote debugger server on port 1234
        RemoteDebuggerServer server = RemoteDebuggerServer.createDebugger(1234);
        server.track(engine);
        // - create a Qute template
        Template template = engine.parse("<!DOCTYPE html>\r\n" + //
                "<html>\r\n" + //
                "<body>\r\n" + //
                "<h1>Hello <b>{name ?: \"Qute\"}</b></h1>\r\n" + //
                "</body>\r\n" + //
                "</html>\r\n" + "");

        // Client side
        // - connect the remote debugger client on port 1234
        RemoteDebuggerClient client = RemoteDebuggerClient.connect(1234);

        // Render template with no breakpoint
        final StringBuilder renderResult = new StringBuilder(1028);
        renderTemplateThread(template, renderResult);
        // Result here is:
        // <!DOCTYPE html>
        // <html>
        // <body>
        // <h1>Hello <b>Qute</b></h1>
        // </body>
        // </html>
        System.err.println(renderResult.toString());

        // Set a breakpoint on line 4: --> <h1>Hello <b>Qute</b></h1>
        client.setBreakpoint(template.getId(), 4);

        // Render template with breakpoint on line 4
        renderResult.setLength(0);
        renderTemplateThread(template, renderResult);
        // Result here is empty
        System.err.println(renderResult.toString());

        // Collect debuggee Thread (one thread)
        io.quarkus.qute.debug.Thread[] threads = client.getThreads();
        System.err.println("Number of threads=" + threads.length);
        io.quarkus.qute.debug.Thread thread = threads[0];
        long threadId = thread.getId();

        // Get stack trace of the debuggee Thread
        StackTrace stackTrace = client.getStackTrace(threadId);
        StackFrame currentFrame = stackTrace.getStackFrames().get(0);
        // Stack frame:
        // name = ExpressionNode [expression=Expression [namespace=null, parts=[name,
        // ?:("Qute")], literal=null]]
        int frameId = currentFrame.getId();
        String frameName = currentFrame.getName();
        System.err.println("Frame: id=" + frameId + ", name=" + frameName);

        // Get scope (Globals, Locals, etc) of the current stack frame
        Scope[] scopes = client.getScopes(frameId);
        Scope globalsScope = scopes[0];
        int variablesReference = globalsScope.getVariablesReference();
        String scopeName = globalsScope.getName();
        System.err.println("Scope: variablesReference= " + variablesReference + ", name=" + scopeName);

        // Get variables of the current scope
        // [name=Quarkus, ..]
        Variable[] variables = client.getVariables(variablesReference);
        for (Variable variable : variables) {
            System.err.println(
                    "Variable: name=" + variable.getName() + ", value=" + variable.getValue() + ", type=" + variable.getType());
        }

        // Step the breakpoint
        client.stepOver(threadId);
        // Result here is:
        // <!DOCTYPE html>
        // <html>
        // <body>
        // <h1>Hello <b>Qute</b></h1>
        // </body>
        // </html>
        Thread.sleep(1000);
        System.err.println(renderResult.toString());

        // On client side, disconnect the client
        client.terminate();
        // On server side, terminate the server
        server.terminate();

    }

    private static void renderTemplateThread(Template template, final StringBuilder renderResult)
            throws InterruptedException {
        Thread httpRequest = new Thread(() -> {
            System.err.println("Stating template render...");
            template.instance() //
                    .data("name", "Quarkus") //
                    .consume(renderResult::append);
            System.err.println("End template render");
        });
        httpRequest.setName("Qute render thread");
        httpRequest.start();
        Thread.sleep(1000);
    }
}
