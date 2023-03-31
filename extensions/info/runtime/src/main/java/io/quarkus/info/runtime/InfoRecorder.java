package io.quarkus.info.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.info.runtime.spi.InfoContributor;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class InfoRecorder {

    public Handler<RoutingContext> handler(Map<String, Object> buildTimeInfo, List<InfoContributor> knownContributors) {
        return new InfoHandler(buildTimeInfo, knownContributors);
    }

    public OsInfoContributor osInfoContributor() {
        return new OsInfoContributor();
    }

    public JavaInfoContributor javaInfoContributor() {
        return new JavaInfoContributor();
    }

    private static class InfoHandler implements Handler<RoutingContext> {
        private final Map<String, Object> finalBuildInfo;

        public InfoHandler(Map<String, Object> buildTimeInfo, List<InfoContributor> knownContributors) {
            this.finalBuildInfo = new HashMap<>(buildTimeInfo);
            for (InfoContributor contributor : knownContributors) {
                //TODO: we might want this to be done lazily
                // also, do we want to merge information or simply replace like we are doing here?
                finalBuildInfo.put(contributor.name(), contributor.data());
            }
        }

        @Override
        public void handle(RoutingContext ctx) {
            HttpServerResponse resp = ctx.response();
            resp.headers().set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
            JsonObject jsonObject = new JsonObject(finalBuildInfo);
            ctx.end(Json.encodePrettily(jsonObject));
        }
    }
}
