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

public class CompletionListTest {

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
                c("take(|index|)", CompletionItemType.FUNCTION), //
                c("get(|index|)", CompletionItemType.FUNCTION) }, items);

        // completion with reflection
        items = client.completion("item.", 1, 6, frameId);
        assertNotNull(items, "");
        assertCompletion(new CompletionItem[] { //
                c("compareTo(|arg0|)", CompletionItemType.METHOD), //
                c("length()", CompletionItemType.METHOD), //
                c("toString()", CompletionItemType.METHOD), //
                c("charAt(|arg0|)", CompletionItemType.METHOD), //
                c("isEmpty()", CompletionItemType.METHOD), //
                c("codePoints()", CompletionItemType.METHOD), //
                c("subSequence(|arg0|,arg1)", CompletionItemType.METHOD), //
                c("chars()", CompletionItemType.METHOD), //
                c("describeConstable()", CompletionItemType.METHOD), //
                c("codePoints()", CompletionItemType.METHOD), //
                c("resolveConstantDesc(|arg0|)", CompletionItemType.METHOD), //
                c("equals(|arg0|)", CompletionItemType.METHOD), //
                c("hashCode()", CompletionItemType.METHOD), //
                // c("indexOf(|arg0|,arg1,arg2)", CompletionItemType.METHOD), // Only available with Java 21
                c("indexOf(|arg0|,arg1)", CompletionItemType.METHOD), //
                c("indexOf(|arg0|)", CompletionItemType.METHOD), //
                c("codePointAt(|arg0|)", CompletionItemType.METHOD), //
                c("codePointBefore(|arg0|)", CompletionItemType.METHOD), //
                c("codePointCount(|arg0|,arg1)", CompletionItemType.METHOD), //
                c("offsetByCodePoints(|arg0|,arg1)", CompletionItemType.METHOD), //
                c("getBytes()", CompletionItemType.METHOD), //
                c("contentEquals(|arg0|)", CompletionItemType.METHOD), //
                c("regionMatches(|arg0|,arg1,arg2,arg3)", CompletionItemType.METHOD), //
                c("regionMatches(|arg0|,arg1,arg2,arg3,arg4)", CompletionItemType.METHOD), //
                c("startsWith(|arg0|)", CompletionItemType.METHOD), //
                c("startsWith(|arg0|,arg1)", CompletionItemType.METHOD), //
                c("lastIndexOf(|arg0|)", CompletionItemType.METHOD), //
                c("lastIndexOf(|arg0|,arg1)", CompletionItemType.METHOD), //
                c("substring(|arg0|,arg1)", CompletionItemType.METHOD), //
                c("substring(|arg0|)", CompletionItemType.METHOD), //
                c("replace(|arg0|,arg1)", CompletionItemType.METHOD), //
                c("matches(|arg0|)", CompletionItemType.METHOD), //
                c("replaceFirst(|arg0|,arg1)", CompletionItemType.METHOD), //
                c("replaceAll(|arg0|,arg1)", CompletionItemType.METHOD), //
                c("split(|arg0|)", CompletionItemType.METHOD), //
                c("split(|arg0|,arg1)", CompletionItemType.METHOD), //
                // c("splitWithDelimiters(|arg0|,arg1)", CompletionItemType.METHOD), // Only available with Java 21
                c("toLowerCase()", CompletionItemType.METHOD), //
                c("toLowerCase(|arg0|)", CompletionItemType.METHOD), //
                c("toUpperCase()", CompletionItemType.METHOD), //
                c("toUpperCase(|arg0|)", CompletionItemType.METHOD), //
                c("trim()", CompletionItemType.METHOD), //
                c("strip()", CompletionItemType.METHOD), //
                c("stripLeading()", CompletionItemType.METHOD), //
                c("stripTrailing()", CompletionItemType.METHOD), //
                c("lines()", CompletionItemType.METHOD), //
                c("repeat(|arg0|)", CompletionItemType.METHOD), //
                c("isBlank()", CompletionItemType.METHOD), //
                c("toCharArray()", CompletionItemType.METHOD), //
                c("equalsIgnoreCase(|arg0|)", CompletionItemType.METHOD), //
                c("compareToIgnoreCase(|arg0|)", CompletionItemType.METHOD), //
                c("endsWith(|arg0|)", CompletionItemType.METHOD), //
                c("concat(|arg0|)", CompletionItemType.METHOD), //
                c("contains(|arg0|)", CompletionItemType.METHOD), //
                c("indent(|arg0|)", CompletionItemType.METHOD), //
                c("stripIndent()", CompletionItemType.METHOD), //
                c("translateEscapes()", CompletionItemType.METHOD), //
                c("transform(|arg0|)", CompletionItemType.METHOD), //
                c("formatted(|arg0|)", CompletionItemType.METHOD), //
                c("intern()", CompletionItemType.METHOD) }, items);

        // On client side, disconnect the client
        client.terminate();
        // On server side, terminate the server
        // server.terminate();

    }

}