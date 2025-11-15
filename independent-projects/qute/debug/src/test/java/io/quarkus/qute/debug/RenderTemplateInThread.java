package io.quarkus.qute.debug;

import java.util.function.Consumer;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

public class RenderTemplateInThread {

    private final Template template;
    private final StringBuilder renderResult;
    private final Consumer<TemplateInstance> configure;

    public RenderTemplateInThread(Template template, final StringBuilder renderResult,
            Consumer<TemplateInstance> configure) throws InterruptedException {
        this.template = template;
        this.renderResult = renderResult;
        this.configure = configure;
        render();
    }

    public void render() throws InterruptedException {
        Thread httpRequest = new Thread(() -> {
            var instance = template.instance(); //
            configure.accept(instance);
            instance.consume(renderResult::append);
        });
        httpRequest.setName("Qute render thread");
        httpRequest.start();
        // Wait for render processs...
        Thread.sleep(1000);
    }
}
