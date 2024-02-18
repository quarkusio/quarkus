package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.http.AbstractHttpEvent;
import jdk.jfr.*;

@Label("REST")
@Category({ "Quarkus", "HTTP" })
@Name("quarkus.Rest")
@Description("REST Server has been processing during this period")
@StackTrace(false)
public class RestPeriodEvent extends AbstractHttpEvent {
}
