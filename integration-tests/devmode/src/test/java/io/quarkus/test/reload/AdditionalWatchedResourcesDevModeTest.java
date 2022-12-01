package io.quarkus.test.reload;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@DisabledOnOs(OS.WINDOWS)
public class AdditionalWatchedResourcesDevModeTest {

    private static final String RES_WATCHED = "watched.txt";
    private static final String RES_WATCHED_SUBPATH = "sub/watched.txt";
    private static final String RES_NOT_WATCHED = "not-watched.txt";
    private static final String RES_GLOB_WATCHED = "*.sample";
    private static final String SAMPLE_FILE = "data.sample";
    private static final String APPLICATION_DEV_PROPERTIES = "application-dev.properties";

    private static final String PROPERTY = "quarkus.live-reload.watched-resources="
            + RES_WATCHED + "," + RES_WATCHED_SUBPATH + "," + RES_GLOB_WATCHED;

    private static final String MODIFIED = "modified";
    private static final String INITIAL = "initial";
    private static final String INITIAL_PROPERTY = "greeting.message=initial";
    private static final String MODIFIED_PROPERTY = "greeting.message=modified";

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(AdditionalWatchedResourcesResource.class)
                    .addAsResource(new StringAsset(INITIAL), RES_WATCHED)
                    .addAsResource(new StringAsset(INITIAL), RES_WATCHED_SUBPATH)
                    .addAsResource(new StringAsset(INITIAL), RES_NOT_WATCHED)
                    .addAsResource(new StringAsset(INITIAL), SAMPLE_FILE)
                    .addAsResource(new StringAsset(INITIAL_PROPERTY), "application-dev.properties")
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

    @Test
    public void globWatch() {
        RestAssured.get("/content/{name}", SAMPLE_FILE).then().body(is(INITIAL));

        TEST.modifyResourceFile(SAMPLE_FILE, oldSource -> MODIFIED);

        RestAssured.get("/content/{name}", SAMPLE_FILE).then().body(is(MODIFIED));
    }

    @Test
    public void watchedProfileSpecificConfigFile() {
        RestAssured.get("/content/{name}", APPLICATION_DEV_PROPERTIES).then().body(is(INITIAL_PROPERTY));

        TEST.modifyResourceFile(APPLICATION_DEV_PROPERTIES, oldSource -> MODIFIED_PROPERTY);

        RestAssured.get("/content/{name}", APPLICATION_DEV_PROPERTIES).then().body(is(MODIFIED_PROPERTY));
    }

}
