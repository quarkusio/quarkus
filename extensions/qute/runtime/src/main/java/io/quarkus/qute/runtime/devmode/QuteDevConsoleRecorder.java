package io.quarkus.qute.runtime.devmode;

import java.util.function.BiFunction;

import io.quarkus.arc.Arc;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.qute.Engine;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuteDevConsoleRecorder {

    public static final String RENDER_HANDLER = QuteDevConsoleRecorder.class.getName() + ".RENDER_HANDLER";

    public void setupRenderer() {
        //set up the render handler that is used to handle the template
        //this is invoked from the deployment side
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        DevConsoleManager.setGlobal(RENDER_HANDLER, new BiFunction<String, Object, String>() {
            @Override
            public String apply(String template, Object data) {
                var old = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(tccl);
                    Engine engine = Arc.container().instance(Engine.class).get();
                    return engine.getTemplate(template).render(data);
                } finally {
                    Thread.currentThread().setContextClassLoader(old);
                }
            }
        });
    }

}
