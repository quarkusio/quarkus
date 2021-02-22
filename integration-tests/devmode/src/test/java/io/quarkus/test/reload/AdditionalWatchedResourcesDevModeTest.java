package io.quarkus.test.reload;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class AdditionalWatchedResourcesDevModeTest {

    private static final String RES_WATCHED = "watched.txt";
    private static final String RES_WATCHED_SUBPATH = "sub/watched.txt";
    private static final String RES_NOT_WATCHED = "not-watched.txt";

    private static final String PROPERTY = "quarkus.live-reload.watched-resources="
            + RES_WATCHED + "," + RES_WATCHED_SUBPATH;

    private static final String MODIFIED = "modified";
    private static final String INITIAL = "initial";

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(AdditionalWatchedResourcesResource.class)
                    .addAsResource(new StringAsset(INITIAL), RES_WATCHED)
                    .addAsResource(new StringAsset(INITIAL), RES_WATCHED_SUBPATH)
                    .addAsResource(new StringAsset(INITIAL), RES_NOT_WATCHED)
                    .addAsResource(new StringAsset(PROPERTY), "application.properties"));

    @Test
    public void watched() {
        RestAssured.get("/content/{name}", RES_WATCHED).then().body(is(INITIAL));

        TEST.modifyResourceFile(RES_WATCHED, oldSource -> MODIFIED);

        RestAssured.get("/content/{name}", RES_WATCHED).then().body(is(MODIFIED));
    }

    @Test
    public void watchedInSubPath() {
        RestAssured.get("/content/{name}", RES_WATCHED_SUBPATH).then().body(is(INITIAL));

        TEST.modifyResourceFile(RES_WATCHED_SUBPATH, oldSource -> MODIFIED);

        RestAssured.get("/content/{name}", RES_WATCHED_SUBPATH).then().body(is(MODIFIED));
    }

    @Test
    public void notWatched() {
        RestAssured.get("/content/{name}", RES_NOT_WATCHED).then().body(is(INITIAL));

        TEST.modifyResourceFile(RES_WATCHED_SUBPATH, oldSource -> MODIFIED);

        RestAssured.get("/content/{name}", RES_NOT_WATCHED).then().body(is(INITIAL));
    }
}
