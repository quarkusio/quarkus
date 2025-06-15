package io.quarkus.arc.test.shutdown;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Messages {

    public static final List<String> MESSAGES = new CopyOnWriteArrayList<>();
}
