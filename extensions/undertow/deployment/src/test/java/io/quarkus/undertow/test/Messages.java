package io.quarkus.undertow.test;

import java.util.concurrent.LinkedBlockingDeque;

public class Messages {

    public static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();
}
