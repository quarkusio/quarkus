package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.*;
import static java.lang.ProcessBuilder.Redirect.PIPE;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.input.TeeInputStream;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.deployment.cmd.RunCommandActionResultBuildItem;
import io.quarkus.deployment.cmd.RunCommandHandler;
import io.quarkus.test.common.http.TestHTTPResourceManager;

public class RunCommandLauncher implements ArtifactLauncher<ArtifactLauncher.InitContext> {

    Process quarkusProcess = null;
    private List<String> args;
    private long waitTimeSeconds;
    private Path workingDir;
    private String startedExpression;
    private boolean needsLogFile;
    private Path logFilePath;

    private final Map<String, String> systemProps = new HashMap<>();

    private ExecutorService executorService = Executors.newFixedThreadPool(3);

    public static RunCommandLauncher tryLauncher(QuarkusBootstrap bootstrap, String target, Duration waitTime) {
        Map<String, List> cmds = new HashMap<>();
        try (CuratedApplication curatedApplication = bootstrap.bootstrap()) {
            AugmentAction action = curatedApplication.createAugmentor();
            action.performCustomBuild(RunCommandHandler.class.getName(), new Consumer<Map<String, List>>() {
                @Override
                public void accept(Map<String, List> accepted) {
                    cmds.putAll(accepted);
                }
            }, RunCommandActionResultBuildItem.class.getName());
        } catch (BootstrapException ex) {
            throw new RuntimeException(ex);
        }
        List cmd = null;
        if (target != null) {
            cmd = cmds.get(target);
            if (cmd == null) {
                throw new RuntimeException("quarkus.run.target \"" + target + "\" does not exist");
            }
        } else if (cmds.size() == 1) { // defaults to pure java run
            return null;
        } else if (cmds.size() == 2) { // choose not default
            for (Map.Entry<String, List> entry : cmds.entrySet()) {
                if (entry.getKey().equals("java"))
                    continue;
                cmd = entry.getValue();
                break;
            }
        } else if (cmds.size() > 2) {
            String tooMany = cmds.keySet().stream().collect(Collectors.joining(" "));
            throw new RuntimeException(
                    "Too many extensions support quarkus:run.  Set quarkus.run.target to pick one to run during integration tests: "
                            + tooMany);
        } else {
            throw new RuntimeException("Should never reach this!");
        }
        RunCommandLauncher launcher = new RunCommandLauncher();
        launcher.args = (List<String>) cmd.get(0);
        launcher.workingDir = (Path) cmd.get(1);
        launcher.startedExpression = (String) cmd.get(2);
        launcher.needsLogFile = (Boolean) cmd.get(3);
        launcher.logFilePath = (Path) cmd.get(4);
        launcher.waitTimeSeconds = waitTime.getSeconds();
        return launcher;
    }

    @Override
    public void init(InitContext initContext) {
        throw new UnsupportedOperationException("not implemented for run command yet");
    }

    @Override
    public LaunchResult runToCompletion(String[] args) {
        throw new UnsupportedOperationException("not implemented for run command yet");
    }

    @Override
    public void start() throws IOException {
        System.setProperty("test.url", TestHTTPResourceManager.getUri());

        Path logFile = logFilePath;

        System.out.println("Executing \"" + String.join(" ", args) + "\"");
        if (needsLogFile) {
            if (logFilePath == null)
                logFile = PropertyTestUtil.getLogFilePath();
            System.out.println("Creating Logfile for custom extension run: " + logFile.toString());
            Files.deleteIfExists(logFile);
            Files.createDirectories(logFile.getParent());
            FileOutputStream logOutputStream = new FileOutputStream(logFile.toFile(), true);
            quarkusProcess = new ProcessBuilder(args).directory(workingDir.toFile()).redirectError(PIPE)
                    .redirectOutput(PIPE).start();
            InputStream teo = new TeeInputStream(quarkusProcess.getInputStream(), System.out);
            executorService.submit(() -> teo.transferTo(logOutputStream));
            InputStream tee = new TeeInputStream(quarkusProcess.getErrorStream(), System.err);
            executorService.submit(() -> tee.transferTo(logOutputStream));
        } else {
            quarkusProcess = new ProcessBuilder(args).directory(workingDir.toFile()).inheritIO().start();
        }
        CountDownLatch signal = new CountDownLatch(1);
        WaitForStartReader reader = new WaitForStartReader(logFile, Duration.ofSeconds(waitTimeSeconds), signal,
                startedExpression);
        executorService.submit(reader);

        try {
            signal.await(waitTimeSeconds + 2, TimeUnit.SECONDS); // wait enough for the signal to be given by the
                                                                 // capturing thread
        } catch (Exception e) {
            // ignore
        }
        if (!reader.isStarted()) {
            LauncherUtil.destroyProcess(quarkusProcess, true);
            throw new RuntimeException("Unable to start target quarkus application " + this.waitTimeSeconds + "s");
        }
    }

    public boolean listensOnSsl() {
        return false;
    }

    public void includeAsSysProps(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    @Override
    public void close() {
        executorService.shutdown();
        if (quarkusProcess != null) {
            LauncherUtil.destroyProcess(quarkusProcess, true);
        }
    }

    /**
     * Thread that reads a process output file looking for the line that indicates the address the application is
     * listening on.
     */
    private static class WaitForStartReader implements Runnable {

        private final Path processOutput;
        private final Duration waitTime;
        private final CountDownLatch signal;
        private final Pattern startedRegex;
        private volatile boolean started;

        public WaitForStartReader(Path processOutput, Duration waitTime, CountDownLatch signal,
                String startedExpression) {
            this.processOutput = processOutput;
            this.waitTime = waitTime;
            this.signal = signal;
            this.startedRegex = Pattern.compile(startedExpression);
        }

        public boolean isStarted() {
            return started;
        }

        @Override
        public void run() {
            long bailoutTime = System.currentTimeMillis() + waitTime.toMillis();
            try (BufferedReader reader = new BufferedReader(new FileReader(processOutput.toFile()))) {
                while (true) {
                    if (reader.ready()) { // avoid blocking as the input is a file which continually gets more data
                                          // added
                        String line = reader.readLine();
                        if (startedRegex.matcher(line).find()) {
                            started = true;
                            signal.countDown();
                            return;
                        }
                    } else {
                        // wait until there is more of the file for us to read

                        long now = System.currentTimeMillis();
                        if (now > bailoutTime) {
                            signal.countDown();
                            return;
                        }

                        try {
                            Thread.sleep(LOG_CHECK_INTERVAL);
                        } catch (InterruptedException e) {
                            signal.countDown();
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception occurred while reading process output from file " + processOutput);
                e.printStackTrace();
                signal.countDown();
            }
        }
    }
}
