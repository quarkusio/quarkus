# Troubleshooting performance issues

Performance is at the heart of Quarkus.

If you are facing performance issues (runtime or startup issues), and would like to discuss them with the Quarkus Team,
you are more than welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev),
[Zulip chat](https://quarkusio.zulipchat.com) or [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).

To help us troubleshoot your issues, we will need some performance insights from your application.

On Linux or macOS, one of the best way to gather performance insights would be to generate CPU and allocation [FlameGraphs](https://github.com/brendangregg/FlameGraph)
via [Async Profiler](https://github.com/jvm-profiling-tools/async-profiler).

If you want a deeper introduction to Async Profiler, do checkout [this article](https://hackernoon.com/profiling-java-applications-with-async-profiler-049s2790).

## Installing Async Profiler

To install Async Profiler, go to the [release page](https://github.com/jvm-profiling-tools/async-profiler/releases) and download the latest release.

Async Profiler depends on `perf_events`.
To allow capturing kernel call stacks using `perf_events` from a non-root process,
you must first apply a couple OS configuration options.

For your terminal session:

```shell script
echo 1 | sudo tee /proc/sys/kernel/perf_event_paranoid
echo 0 | sudo tee /proc/sys/kernel/kptr_restrict
```

Or permanently using `sysctl`:

```shell script
sudo sysctl -w kernel.perf_event_paranoid=1
sudo sysctl -w kernel.kptr_restrict=0
```

For allocation profiling, you also need to install HotSpot debug symbol (unless you use Oracle JDK that embeds them already).

Depending on your Linux and Java distribution this can be done via:

```shell script
# Ubuntu/Debian - Java 17
apt install openjdk-17-dbg

# Ubuntu/Debian - Java 21
 apt install openjdk-21-dbg

# On CentOS, RHEL and some other RPM-based distributions - Java 17
debuginfo-install java-17-openjdk
```

You can also use a __fastdebug__ build of OpenJdk, this kind of build is not for production use (JVM as assertions are enabled), but it includes debug symbols

If needed, see [this](https://github.com/jvm-profiling-tools/async-profiler#allocation-profiling) section in the Async Profiler site for details.

## Profiling application runtime with Async Profiler

Async Profiler comes with a Java agent, and a command line.

To profile application while it is running, it is recommended to use the command line as you can choose when to start the profiler and prevent your profile data from being bloated with startup events.
This can be important as any application performs a lot of bootstrapping operation upon startup that won't occur at any other during the application lifecycle.
By starting the profiling on demand, you prevent these bootstrap instructions from being part of the profile data.

When you use the command line, it is advised to use `-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints` JVM flags to have more accurate results.

It is usually advised to profile an application under load,
and to start profiling only after some warmup time to allow Java's Just In Time compiler to optimize your application code (not to mention giving the opportunity for database caches to warmup, etc...).
Such load could be created by a load generator tool ([ab](https://httpd.apache.org/docs/2.4/programs/ab.html), [wrk2](https://github.com/giltene/wrk2), [Gatling](https://gatling.io/), [Apache JMeter](https://jmeter.apache.org/), ...).

### CPU profiling

To start CPU profiling, execute the following command:

```shell script
/path/to/async-profiler/profiler.sh start -b 4000000 <pid>
```

`-b 4000000` is used to increase the frame buffer size as the default is often too small.

To end profiling and gather the results you can launch the same command with the `stop` subcommand, this will tell you if the buffer frame was too small.
The output is a text file that is not really usable, so let's use our preferred performance representation: the  flame graph.

```shell script
/path/to/async-profiler/profiler.sh stop -f /tmp/cpu-profile.html <pid>
```

It will create an HTML flame graph (Async Profiler automatically detect that you ask for a  flame graph thanks to the `html` file extension)
that you can open in your browser (and even zoom inside it by clicking on a frame).

One very useful option is `-s` (or `--simple`) that results in simple class names being used instead of fully qualified class names,
thus making the  flame graph more readable (at cost of not showing the package names of classes).
You can also limit the profiling duration by using `-d` (or `--duration`) followed by the duration in seconds.
If you use the `--duration` option, the output file will be created automatically at the end of the duration period. You do not need to explicitly start and stop the profiler.

### Allocation profiling

To start allocation profiling, execute the following command:

```shell script
/path/to/async-profiler/profiler.sh start -b 4000000 -e alloc <pid>
```

`-e` (or `--event`) allow to specify the type of event to profile. The default profile type is CPU, but in this case as we are interested in allocation profiling, we specify `alloc` as the `-e` value.

Stopping allocation profiling is done in the same way as for the previously shown CPU profiling.

```shell script
/path/to/async-profiler/profiler.sh stop -f /tmp/alloc-profile.html <pid>
```

## Profiling application startup with Async Profiler

When you want to profile application startup, you cannot use the command line tool as you need a way to start the profiler with your application.

For this case, the Java agent is the best tool.
It will start profiling when you start the application, then record the profiling data when the application exits.

Some example usages are:

```shell script
PATH_TO_ASYNC_PROFILER=...

# profile CPU startup
java -agentpath:${PATH_TO_ASYNC_PROFILER}/lib/libasyncProfiler.so=start,event=cpu,file=startup-cpu-profile.html,interval=1000000,simple\
    -jar target/quarkus-app/quarkus-run.jar

# profile allocation startup
java -agentpath:${PATH_TO_ASYNC_PROFILER}/lib/libasyncProfiler.so=start,alloc=1,total,event=alloc,file=startup-alloc-profile.jfr -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:-UseTLAB -Xmx1G -Xms1G -XX:+AlwaysPreTouch -jar target/quarkus-app/quarkus-run.jar
```

Stop the application with `CTRL+C` once you have gathered what you want (e.g. just after startup, after the first request).
It will dump the profiling information.
The name of the file is in the `file=` parameter.

For the allocation case, you obtain a JFR file, you can convert it to your typical Async Profiler flamegraph HTML output with:

```shell script
java -cp ${PATH_TO_ASYNC_PROFILER}/lib/converter.jar jfr2flame startup-alloc-profile.jfr --alloc --total startup-alloc-profile.html
```

Note that short options are not supported inside the agent, you need to use their long versions.

By default, Async Profiler sample events every 10ms.
When it comes to profiling / debugging a Quarkus startup issue, this value is often too high as Quarkus starts very fast.
For that reason, it is not uncommon to configure the profiling interval to 1000000ns (i.e. 1ms).

## Profiling application dev mode with Async Profiler

For profiling Quarkus dev mode, the Java agent is again necessary.
It can be used in the same way as for the production application with the exception that `agentpath` option needs to be set via the `jvm.args` system property.

```shell script
PATH_TO_ASYNC_PROFILER=...

# profile CPU startup
mvn quarkus:dev -Djvm.args="-agentpath:${PATH_TO_ASYNC_PROFILER}/lib/libasyncProfiler.so=start,event=cpu,file=startup-cpu-profile.html,interval=1000000,simple"

# profile allocation startup
mvn quarkus:dev -Djvm.args="-agentpath:${PATH_TO_ASYNC_PROFILER}/lib/libasyncProfiler.so=start,alloc=1,total,event=alloc,file=startup-alloc-profile.jfr -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:-UseTLAB -Xmx1G -Xms1G -XX:+AlwaysPreTouch"
```

Stop the application with `CTRL+C` once you have gathered what you want (e.g. just after startup, after the first request).
It will dump the profiling information.
The name of the file is in the `file=` parameter.

For the allocation case, you obtain a JFR file, you can convert it to your typical Async Profiler flamegraph HTML output with:

```shell script
java -cp ${PATH_TO_ASYNC_PROFILER}/lib/converter.jar jfr2flame startup-alloc-profile.jfr --alloc --total startup-alloc-profile.html
```

You can also configure the `jvm.args` system property directly inside the `quarkus-maven-plugin` section of your `pom.xml`.

## Analysing build steps execution time

When trying to debug startup performance, it is convenient to log build steps execution time.
This can be achieved by adding the following system property: `-Dquarkus.debug.print-startup-times=true` in dev mode or when launching the JAR.

There is also a nice visualization of build steps available in the Dev UI located here: <http://localhost:8080/q/dev/build-steps>.

If you want to have the same visualization of build steps processing when building your application, you can use the `quarkus.debug.dump-build-metrics=true` property.
For example using `mvn package -Dquarkus.debug.dump-build-metrics=true`, will generate a `build-metrics.json` in your `target` repository that you can process via the quarkus-build-report application available here <https://github.com/mkouba/quarkus-build-report>.
This application will generate a `report.html` that you can open in your browser.

## What about Windows?

If you are on Windows, you can still get useful performance insights using JFR - Java Flight Recorder.

The following Java options will enable JFR to record profiling data inside a `myrecording.jfr` file that can then be used by JMC - Java Mission Control for analysis.

```shell script
-XX:StartFlightRecording=filename=myrecording.jfr,settings=profile -XX:FlightRecorderOptions=stackdepth=64
```

Here we configure JFR with a deeper stack depth as the default is usually not enough.

## What about native executables?

If you are having performance issues with native builds of your application first make sure that these issues only manifest in native mode.
If so, please consult the [native reference guide](https://quarkus.io/guides/native-reference) and more specifically the [profiling section](https://quarkus.io/guides/native-reference#profiling).
