package io.quarkus.info.runtime;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.info.BuildInfo;
import io.quarkus.info.GitInfo;
import io.quarkus.info.JavaInfo;
import io.quarkus.info.OsInfo;
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

    public Supplier<GitInfo> gitInfoSupplier(String branch, String latestCommitId, String latestCommitTime) {
        return new Supplier<GitInfo>() {
            @Override
            public GitInfo get() {
                return new GitInfo() {
                    @Override
                    public String branch() {
                        return branch;
                    }

                    @Override
                    public String latestCommitId() {
                        return latestCommitId;
                    }

                    @Override
                    public OffsetDateTime commitTime() {
                        return OffsetDateTime.parse(latestCommitTime, ISO_OFFSET_DATE_TIME);
                    }
                };
            }
        };
    }

    public Supplier<BuildInfo> buildInfoSupplier(String group, String artifact, String version, String time,
            String quarkusVersion) {
        return new Supplier<BuildInfo>() {
            @Override
            public BuildInfo get() {
                return new BuildInfo() {
                    @Override
                    public String group() {
                        return group;
                    }

                    @Override
                    public String artifact() {
                        return artifact;
                    }

                    @Override
                    public String version() {
                        return version;
                    }

                    @Override
                    public OffsetDateTime time() {
                        return OffsetDateTime.parse(time, ISO_OFFSET_DATE_TIME);
                    }

                    @Override
                    public String quarkusVersion() {
                        return quarkusVersion;
                    }
                };
            }
        };
    }

    public OsInfoContributor osInfoContributor() {
        return new OsInfoContributor();
    }

    public Supplier<OsInfo> osInfoSupplier() {
        return new Supplier<OsInfo>() {
            @Override
            public OsInfo get() {
                return new OsInfo() {
                    @Override
                    public String name() {
                        return OsInfoContributor.getName();
                    }

                    @Override
                    public String version() {
                        return OsInfoContributor.getVersion();
                    }

                    @Override
                    public String architecture() {
                        return OsInfoContributor.getArchitecture();
                    }
                };
            }
        };
    }

    public JavaInfoContributor javaInfoContributor() {
        return new JavaInfoContributor();
    }

    public Supplier<JavaInfo> javaInfoSupplier() {
        return new Supplier<JavaInfo>() {
            @Override
            public JavaInfo get() {
                return new JavaInfo() {
                    @Override
                    public String version() {
                        return JavaInfoContributor.getVersion();
                    }
                };
            }
        };
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
            for (InstanceHandle<InfoContributor> handler : Arc.container().listAll(InfoContributor.class)) {
                InfoContributor contributor = handler.get();
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
