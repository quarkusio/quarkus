package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.http.AbstractHttpEvent;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Label("REST End")
@Category({ "Quarkus", "HTTP" })
@Name("quarkus.RestEnd")
@Description("REST Server processing has completed")
@StackTrace(false)
@Enabled(false)
public class RestEndEvent extends AbstractHttpEvent {
}
