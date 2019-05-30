package io.quarkus.smallrye.faulttolerance.runtime;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.metric.HystrixCollapserEventStream;
import com.netflix.hystrix.metric.HystrixCommandCompletionStream;
import com.netflix.hystrix.metric.HystrixCommandStartStream;
import com.netflix.hystrix.metric.HystrixThreadEventStream;
import com.netflix.hystrix.metric.HystrixThreadPoolCompletionStream;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;

@Template
public class SmallryeFaultToleranceTemplate {

    public void resetCommandContextOnUndeploy(ShutdownContext context) {
        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                HystrixCommandCompletionStream.reset();
                HystrixCollapserEventStream.reset();
                HystrixCommandStartStream.reset();
                HystrixThreadPoolCompletionStream.reset();
                HystrixCommandStartStream.reset();
                HystrixThreadEventStream.getInstance().shutdown();
                Hystrix.reset();
            }
        });
    }
}
