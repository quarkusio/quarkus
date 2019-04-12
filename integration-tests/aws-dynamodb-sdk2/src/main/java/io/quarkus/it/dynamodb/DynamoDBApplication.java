package io.quarkus.it.dynamodb;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/test")
public class DynamoDBApplication extends Application {
}