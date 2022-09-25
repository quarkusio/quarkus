package toremove2;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.debug.server.RemoteDebuggerServer;

public class QuteRender {

    public static final String HELLO_TEMPLATE = "hello";

    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(() -> {

            Engine engine0 = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
            Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
            Template template = engine.parse("{abcd}! \r\n {abcd}", null, HELLO_TEMPLATE);
            System.err.println(engine.getTemplate(HELLO_TEMPLATE));

            try {
                RemoteDebuggerServer debugger = RemoteDebuggerServer.createDebugger();
                if (debugger != null) {
                    debugger.track(engine);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            while (true) {

                String s = template.data("abcd", HELLO_TEMPLATE).render();
                System.err.println(s);

            }
        });
        t.start();
        t.join();
    }

}
