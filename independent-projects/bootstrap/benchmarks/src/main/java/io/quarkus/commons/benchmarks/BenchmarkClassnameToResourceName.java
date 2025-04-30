package io.quarkus.commons.benchmarks;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import io.quarkus.commons.classloading.ClassLoaderHelper;

/**
 * We benchmark this strategy with CompilerControl.Mode.EXCLUDE as this code
 * is primarily useful during bootstrap. We already know JIT will do a fantastic
 * job when compiling it even if it's written in less efficient ways, so there's
 * no much point in optimising such for compiled code: let's choose a strategy
 * that doesn't cost excessively even before JIT kicks in.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime) //!
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 20, timeUnit = TimeUnit.MILLISECONDS) //ignored in single shot mode
@Measurement(iterations = 20, time = 50, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
public class BenchmarkClassnameToResourceName {

    @Param({ "io.quarkus.commons.benchmarks.BenchmarkClassnameToResourceName" })
    public String arg;

    @Benchmark
    public String checkNewMethod() {
        return ClassLoaderHelper.fromClassNameToResourceName(arg);
    }

    @Benchmark
    public String oldMethod() {
        return oldMethod(arg);
    }

    private static String oldMethod(String name) {
        return name.replace(".", "/") + ".class";
    }

    @Benchmark
    public String oldAltMethod() {
        return oldAltMethod(arg);
    }

    private static String oldAltMethod(String name) {
        return name.replace('.', '/') + ".class";
    }

    public static void main(String[] args) throws IOException {
        org.openjdk.jmh.Main.main(new String[] { "-prof", "gc", "-prof", "perfnorm" });
    }

}
