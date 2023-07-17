@jakarta.inject.Singleton
public class L {

    private final C c;

    L(C c) {
        this.c = c;
    }

    String ping() {
        return c.ping();
    }

}
