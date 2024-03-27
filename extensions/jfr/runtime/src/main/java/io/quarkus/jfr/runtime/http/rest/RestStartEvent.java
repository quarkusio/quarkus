package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.http.AbstractHttpEvent;
import jdk.jfr.*;

@Label("REST Start")
@Category({ "Quarkus", "HTTP" })
@Name("quarkus.RestStart")
@Description("REST Server processing has started")
@StackTrace(false)
@Enabled(false)
public class RestStartEvent extends AbstractHttpEvent {
}
