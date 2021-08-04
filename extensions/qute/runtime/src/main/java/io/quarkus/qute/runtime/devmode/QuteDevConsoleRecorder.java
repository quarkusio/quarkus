package io.quarkus.qute.runtime.devmode;

import java.util.function.BiFunction;

import io.quarkus.arc.Arc;
import io.quarkus.dev.devui.DevConsoleManager;
import io.quarkus.qute.Engine;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuteDevConsoleRecorder {

    public static final String RENDER_HANDLER = QuteDevConsoleRecorder.class.getName() + ".RENDER_HANDLER";

    public void setupRenderer() {
        //setup the render handler that is used to handle the template
        //this is invoked from the deployment side
        DevConsoleManager.setGlobal(RENDER_HANDLER, new BiFunction<String, Object, String>() {
            @Override
            public String apply(String template, Object data) {
                Engine engine = Arc.container().instance(Engine.class).get();
                return engine.getTemplate(template).render(data);
            }
        });
    }

}
