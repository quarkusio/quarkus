package io.quarkus.undertow.websockets.runtime;

import java.util.function.Supplier;

import org.xnio.XnioWorker;

public class WorkerSupplier implements Supplier<XnioWorker> {

    static volatile XnioWorker worker;

    @Override
    public XnioWorker get() {
        return worker;
    }
}
