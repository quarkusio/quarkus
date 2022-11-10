package org.acme;

import java.util.Arrays;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class EntryPoint {

    public static void main(String[] args) {
        System.out.println("basic-java-main-application-project: args.length: " + args.length);
        System.out.println("basic-java-main-application-project: args: " + Arrays.toString(args));
    }
}
