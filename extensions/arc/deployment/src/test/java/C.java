import jakarta.inject.Inject;

@jakarta.inject.Singleton
public class C {

    private B b;

    @Inject
    void setB(B b) {
        this.b = b;
    }

    String ping() {
        return b.ping();
    }

}
