package ilove.quark.us.googlecloudfunctionshttp;

import io.quarkus.funqy.Funq;
import io.smallrye.mutiny.Uni;

public class GreetingFunqy {

    @Funq
    public String funqy() {
        return "Make it funqy";
    }

    @Funq
    public Uni<String> funqyAsync() {
        return Uni.createFrom().item(() -> "Make it funqy asynchronously");
    }
}
