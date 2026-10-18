package io.quarkus.bootstrap.forkjoin;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public class QuarkusForkJoinWorkerThread extends ForkJoinWorkerThread {

    private static volatile ClassLoader qClassloader;

    protected QuarkusForkJoinWorkerThread(ForkJoinPool pool) {
        super(pool);
    }

    public static synchronized void setQuarkusAppClassloader(ClassLoader runnerClassLoader) {
        qClassloader = runnerClassLoader;
    }

    protected void onStart() {
        super.onStart();
        if (qClassloader != null) {
            super.setContextClassLoader(qClassloader);
        }
        //When null: it means the task which is being scheduled was scheduled
        //before the application was started - possibly by some parallel
        //task during the preparation of the application classloader itself:
        //should be safe to run with the existing (system) classsloader.
    }

}
