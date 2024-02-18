package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.http.AbstractHttpEvent;
import jdk.jfr.*;

@Label("REST End")
@Category({ "Quarkus", "HTTP" })
@Name("quarkus.RestEnd")
@Description("REST Server processing has completed")
@StackTrace(false)
@Enabled(false)
public class RestEndEvent extends AbstractHttpEvent {
}
