package io.quarkus.qute.debug.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.debug.StackFrame;
import org.junit.jupiter.api.Test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.debug.adapter.RegisterDebugServerAdapter;
import io.quarkus.qute.debug.client.DAPClient;
import io.quarkus.qute.debug.client.DebuggerUtils;

/**
 * Test class validating Qute debugger behavior for nested sections (#for / #if).
 * <p>
 * This test ensures that:
 * <ul>
 * <li>Stack frames are correctly grouped and ordered for nested Qute sections (e.g. nested {@code #for} and {@code #if}).</li>
 * <li>Each iteration of a loop updates the section frame stack as expected.</li>
 * <li>Conditional sections such as {@code #if} produce consistent frame entries during evaluation.</li>
 * <li>The {@link io.quarkus.qute.debug.client.DAPClient} correctly steps through template nodes using {@code next()}.</li>
 * </ul>
 * <p>
 * The tested template structure:
 *
 * <pre>
 * &lt;html&gt;
 *   {#for user in users}
 *     {user.name}
 *     {#for task in user.tasks}
 *       {task}
 *       {#if task_done}
 *         Done
 *       {/if}
 *     {/for}
 *   {/for}
 * &lt;/html&gt;
 * </pre>
 * <p>
 * For each step in the rendering process, the test sets a breakpoint and uses {@link DAPClient#next(int)}
 * to verify that the reported stack frames correspond to the correct Qute AST nodes (HTML, section, or expression).
 * <p>
 * The test demonstrates that the debugger maintains proper frame grouping
 * even with nested {@code #for} and {@code #if} constructs.
 *
 * @see io.quarkus.qute.debug.adapter.RegisterDebugServerAdapter
 * @see io.quarkus.qute.debug.client.DAPClient
 * @see io.quarkus.qute.debug.RenderTemplateInThread
 * @see io.quarkus.qute.debug.agent.SectionFrameStack
 */

public class NestedLoopIfBreakpointTest {

    private static final String TEMPLATE_ID = "nested.qute";

    @Test
    public void testNestedLoopsAndIfFrames() throws Exception {
        int port = DebuggerUtils.findAvailableSocketPort();

        Engine engine = Engine.builder()
                .enableTracing(true)
                .addEngineListener(new RegisterDebugServerAdapter(port, false))
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .build();

        Template template = engine.parse("<html>\n" +
                "    {#for user in users}\n" +
                "        {user.name}\n" +
                "        {#for task in user.tasks}\n" +
                "            {task}\n" +
                "            {#if user.task_done}\n" +
                "                Done\n" +
                "            {/if}\n" +
                "        {/for}\n" +
                "    {/for}\n" +
                "</html>", null, TEMPLATE_ID);

        DAPClient client = new DAPClient();
        client.connectToServer(port).get(10, TimeUnit.SECONDS);

        // Set breakpoint at the start of the template
        client.setBreakpoint("src/main/resources/templates/" + TEMPLATE_ID, 1);

        final StringBuilder renderResult = new StringBuilder(1024);

        // Start rendering in a thread
        new io.quarkus.qute.debug.RenderTemplateInThread(template, renderResult, instance -> {
            instance.data("users", List.of(
                    new User("Alice", List.of("Task1", "Task2")),
                    new User("Bob", List.of("Task3"))));
        });

        int threadId = client.getThreads()[0].getId();

        // --- Step through template using next ---
        // Each step, we check the full list of stack frames

        // <html>
        assertFrames(client, threadId,
                "TextNode [value=<html>\n]");

        // {#for user in users}
        client.next(threadId);
        assertFrames(client, threadId,
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:2]]",
                "TextNode [value=<html>\n]");

        // Alice user...

        // {user.name} with Alice
        client.next(threadId);
        assertFrames(client, threadId,
                "ExpressionNode [expression=Expression [namespace=null, parts=[user, name], literal=null]]",
                "TextNode [value=        ]",
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:2]]",
                "TextNode [value=<html>\n]");

        // {#for task in user.tasks} with Alice
        client.next(threadId);
        assertFrames(client, threadId,
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:4]]",
                "TextNode [value=\n]",
                "ExpressionNode [expression=Expression [namespace=null, parts=[user, name], literal=null]]",
                "TextNode [value=        ]",
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:2]]",
                "TextNode [value=<html>\n]");

        // {task} with Alice/Task1
        client.next(threadId);
        assertFrames(client, threadId,
                "ExpressionNode [expression=Expression [namespace=null, parts=[task], literal=null]]",
                "TextNode [value=            ]",
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:4]]",
                "TextNode [value=\n]",
                "ExpressionNode [expression=Expression [namespace=null, parts=[user, name], literal=null]]",
                "TextNode [value=        ]",
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:2]]",
                "TextNode [value=<html>\n]");

        // {#if user.task_done} with Alice/Task1
        client.next(threadId);
        assertFrames(client, threadId,
                "SectionNode [helper=IfSectionHelper, origin=  template [nested.qute:6]]",
                "TextNode [value=\n]",
                "ExpressionNode [expression=Expression [namespace=null, parts=[task], literal=null]]",
                "TextNode [value=            ]",
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:4]]",
                "TextNode [value=\n]",
                "ExpressionNode [expression=Expression [namespace=null, parts=[user, name], literal=null]]",
                "TextNode [value=        ]",
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:2]]",
                "TextNode [value=<html>\n]");

        // Done with Alice/Task1
        client.stepIn(threadId); // use stepIn to suspend to the Done text node
        assertFrames(client, threadId,
                "TextNode [value=                Done\n]",
                "SectionNode [helper=IfSectionHelper, origin=  template [nested.qute:6]]",
                "TextNode [value=\n]",
                "ExpressionNode [expression=Expression [namespace=null, parts=[task], literal=null]]",
                "TextNode [value=            ]",
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:4]]",
                "TextNode [value=\n]",
                "ExpressionNode [expression=Expression [namespace=null, parts=[user, name], literal=null]]",
                "TextNode [value=        ]",
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:2]]",
                "TextNode [value=<html>\n]");

        // {task} with Alice/Task2
        client.next(threadId);
        assertFrames(client, threadId,
                "ExpressionNode [expression=Expression [namespace=null, parts=[task], literal=null]]",
                "TextNode [value=            ]",
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:4]]",
                "TextNode [value=\n]",
                "ExpressionNode [expression=Expression [namespace=null, parts=[user, name], literal=null]]",
                "TextNode [value=        ]",
                "SectionNode [helper=LoopSectionHelper, origin=  template [nested.qute:2]]",
                "TextNode [value=<html>\n]");

        client.terminate();
    }

    private void assertFrames(DAPClient client, int threadId, String... expectedFrameNames) throws Exception {
        Thread.sleep(500); // allow debugger to catch up
        StackFrame[] frames = client.getStackFrames(threadId);
        //java.util.stream.Stream.of(frames).map(sf -> "\"" + sf.getName() + "\"").toList()
        assertEquals(expectedFrameNames.length, frames.length, "Frame count mismatch");
        for (int i = 0; i < frames.length; i++) {
            assertEquals(expectedFrameNames[i], frames[i].getName(), "Frame at index " + i);
        }
    }

    // Simple helper class for the test
    static class User {
        public String name;
        public List<String> tasks;
        public boolean task_done = true;

        User(String name, List<String> tasks) {
            this.name = name;
            this.tasks = tasks;
        }

    }
}
