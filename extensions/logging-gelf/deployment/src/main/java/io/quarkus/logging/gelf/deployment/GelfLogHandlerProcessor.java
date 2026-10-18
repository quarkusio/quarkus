package io.quarkus.logging.gelf.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.logging.gelf.GelfLogHandlerRecorder;

class GelfLogHandlerProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.LOGGING_GELF);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem build(GelfLogHandlerRecorder recorder) {
        return new LogHandlerBuildItem(recorder.initializeHandler());
    }

    @BuildStep
    RuntimeInitializedClassBuildItem nativeBuild() {
        return new RuntimeInitializedClassBuildItem(
                "biz.paluch.logging.gelf.jboss7.JBoss7GelfLogHandler");
    }

    @BuildStep()
    SystemPropertyBuildItem sysProp() {
        //FIXME we change the order ot the Hostname resolution for native image
        // see https://logging.paluch.biz/hostname-lookup.html
        // if not, we have the following error
        /*
         * java.lang.NullPointerException
         * at java.net.InetAddress.getHostFromNameService(InetAddress.java:615)
         * at java.net.InetAddress.getCanonicalHostName(InetAddress.java:589)
         * at biz.paluch.logging.RuntimeContainer.isQualified(RuntimeContainer.java:20)
         * at biz.paluch.logging.RuntimeContainer.getInetAddressWithHostname(RuntimeContainer.java:137)
         * at biz.paluch.logging.RuntimeContainer.lookupHostname(RuntimeContainer.java:90)
         * at biz.paluch.logging.RuntimeContainer.initialize(RuntimeContainer.java:67)
         * at biz.paluch.logging.gelf.jul.GelfLogHandler.<init>(GelfLogHandler.java:54)
         * at biz.paluch.logging.gelf.jboss7.JBoss7GelfLogHandler.<init>(JBoss7GelfLogHandler.java:75)
         * at io.quarkus.logging.gelf.GelfLogHandlerRecorder.initializeHandler(GelfLogHandlerRecorder.java:17)
         * at io.quarkus.deployment.steps.GelfLogHandlerProcessor$build27.deploy_0(GelfLogHandlerProcessor$build27.zig:76)
         * at io.quarkus.deployment.steps.GelfLogHandlerProcessor$build27.deploy(GelfLogHandlerProcessor$build27.zig:36)
         * at io.quarkus.runner.ApplicationImpl.doStart(ApplicationImpl.zig:68)
         * at io.quarkus.runtime.Application.start(Application.java:87)
         * at io.quarkus.runtime.Application.run(Application.java:210)
         * at io.quarkus.runner.GeneratedMain.main(GeneratedMain.zig:41)
         */
        return new SystemPropertyBuildItem("logstash-gelf.resolutionOrder", "localhost,network");
    }

}
