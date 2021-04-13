package ilove.quark.us.picocli;

import javax.enterprise.context.Dependent;

@Dependent
public class GreetingService {

    public void sayHello(String name){
        System.out.printf("Hello dear %s!\n", name);
    }
}
