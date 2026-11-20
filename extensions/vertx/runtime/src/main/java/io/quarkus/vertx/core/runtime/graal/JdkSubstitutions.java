package io.quarkus.vertx.core.runtime.graal;

import java.io.IOException;
import java.nio.channels.spi.AsynchronousChannelProvider;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@Substitute
@TargetClass(className = "sun.nio.ch.WindowsAsynchronousFileChannelImpl", innerClass = "DefaultIocpHolder")
@Platforms({ Platform.WINDOWS.class })
final class Target_sun_nio_ch_WindowsAsynchronousFileChannelImpl_DefaultIocpHolder {

    @Alias
    @InjectAccessors(DefaultIocpAccessor.class)
    static Target_sun_nio_ch_Iocp defaultIocp;
}

@TargetClass(className = "sun.nio.ch.Iocp")
@Platforms({ Platform.WINDOWS.class })
final class Target_sun_nio_ch_Iocp {

    @Alias
    Target_sun_nio_ch_Iocp(AsynchronousChannelProvider provider, Target_sun_nio_ch_ThreadPool pool) throws IOException {
    }

    @Alias
    Target_sun_nio_ch_Iocp start() {
        return null;
    }
}

@TargetClass(className = "sun.nio.ch.ThreadPool")
@Platforms({ Platform.WINDOWS.class })
final class Target_sun_nio_ch_ThreadPool {

    @Alias
    static Target_sun_nio_ch_ThreadPool createDefault() {
        return null;
    }
}

final class DefaultIocpAccessor {
    static Target_sun_nio_ch_Iocp get() {
        try {
            return new Target_sun_nio_ch_Iocp(null, Target_sun_nio_ch_ThreadPool.createDefault()).start();
        } catch (IOException ioe) {
            throw new InternalError(ioe);
        }
    }
}

class JdkSubstitutions {

}
