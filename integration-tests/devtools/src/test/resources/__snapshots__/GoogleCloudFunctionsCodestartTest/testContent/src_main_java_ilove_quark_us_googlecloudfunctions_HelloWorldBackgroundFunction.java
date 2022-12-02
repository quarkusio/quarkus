package ilove.quark.us.googlecloudfunctions;

import javax.enterprise.context.ApplicationScoped;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;

@ApplicationScoped
public class HelloWorldBackgroundFunction implements BackgroundFunction<HelloWorldBackgroundFunction.StorageEvent> {

    @Override
    public void accept(StorageEvent event, Context context) throws Exception {
        System.out.println("Receive event on file: " + event.name);
    }

    public static class StorageEvent {
        public String name;
    }
}