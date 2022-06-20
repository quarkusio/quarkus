package io.quarkus.qute.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.debug.client.RemoteDebuggerClient;
import io.quarkus.qute.debug.server.RemoteDebuggerServer;

public class DebuggingQuteTemplateTest {

	@Test
	public void debuggingTemplate() throws Exception {

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
				"</html>\r\n");

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
		client.setBreakpoint(template.getId(), 4);

		// Render template with breakpoint on line 4
		renderResult.setLength(0);
		renderTemplateThread(template, renderResult);
		// Result here is empty
		assertEquals("", renderResult.toString());

		// Collect debuggee Thread (one thread)
		io.quarkus.qute.debug.Thread[] threads = client.getThreads();
		assertEquals(1, threads.length);

		io.quarkus.qute.debug.Thread thread = threads[0];
		long threadId = thread.getId();
		assertEquals("Qute render thread", thread.getName());

		// Get stack trace of the debuggee Thread
		StackTrace stackTrace = client.getStackTrace(threadId);
		StackFrame currentFrame = stackTrace.getStackFrames().get(0);
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
		Scope globalsScope = scopes[0];
		assertEquals("Globals", globalsScope.getName());

		// Get variables of the Globals scope
		// [name=Quarkus, ..]
		int variablesReference = globalsScope.getVariablesReference();
		Variable[] variables = client.getVariables(variablesReference);
		assertEquals(2, variables.length);
		
		Variable firstVar = variables[0];
		assertEquals("name", firstVar.getName());
		assertEquals("Quarkus", firstVar.getValue());
		assertEquals("String", firstVar.getType());

		Variable secondVar = variables[1];
		assertEquals("io.quarkus.qute.dataMap", secondVar.getName());
		assertEquals("true", secondVar.getValue());
		assertEquals("Boolean", secondVar.getType());

		// Step the breakpoint
		client.stepOver(threadId);
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
		server.terminate();
		while(true) {
			
		}
	}

	private static void renderTemplateThread(Template template, final StringBuilder renderResult)
			throws InterruptedException {
		java.lang.Thread httpRequest = new java.lang.Thread(() -> {
			template.instance() //
					.data("name", "Quarkus") //
					.consume(renderResult::append);
		});
		httpRequest.setName("Qute render thread");
		httpRequest.start();
		// Wait for render processs...
		java.lang.Thread.sleep(1000);
	}
}
