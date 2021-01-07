package io.quarkus.qute.runtime.devmode;

import java.net.URLConnection;

import io.quarkus.arc.Arc;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Variant;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QuteDevConsoleRecorder {

    public Handler<RoutingContext> invokeHandler() {
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext context) {
                context.request().setExpectMultipart(true);
                context.request().endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void ignore) {
                        MultiMap form = context.request().formAttributes();
                        String templatePath = form.get("template-path");
                        String testJsonData = form.get("template-data");
                        Engine engine = Arc.container().instance(Engine.class).get();
                        String contentType = null;
                        String fileName = templatePath;
                        int slashIdx = fileName.lastIndexOf('/');
                        if (slashIdx != -1) {
                            fileName = fileName.substring(slashIdx, fileName.length());
                        }
                        int dotIdx = fileName.lastIndexOf('.');
                        if (dotIdx != -1) {
                            String suffix = fileName.substring(dotIdx + 1, fileName.length());
                            if (suffix.equalsIgnoreCase("json")) {
                                contentType = Variant.APPLICATION_JSON;
                            } else {
                                contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
                            }
                        }
                        try {
                            Object testData = Json.decodeValue(testJsonData);
                            context.response().setStatusCode(200).putHeader("Content-Type", contentType)
                                    .end(engine.getTemplate(templatePath).render(testData));
                        } catch (Throwable e) {
                            context.fail(e);
                        }
                    }
                });
            }

        };
    }

}
