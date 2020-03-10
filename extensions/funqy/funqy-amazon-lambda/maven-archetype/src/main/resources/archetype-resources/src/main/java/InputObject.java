package ${package};

public class InputObject {

    private String name;
    private String greeting;

    public String getName() {
        return name;
    }

    public InputObject setName(String name) {
        this.name = name;
        return this;
    }

    public String getGreeting() {
        return greeting;
    }

    public InputObject setGreeting(String greeting) {
        this.greeting = greeting;
        return this;
    }
}
