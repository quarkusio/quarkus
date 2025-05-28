package io.quarkus.cli.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

import io.quarkus.devtools.exec.Executable;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.utils.Prompt;
import io.quarkus.fs.util.ZipUtils;

public class JBangSupport {

    private static final boolean IS_OS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");
    private static final String JBANG_EXECUTABLE = IS_OS_WINDOWS ? "jbang.cmd" : "jbang";

    private static final Predicate<Path> EXISTS_AND_WRITABLE = p -> p != null && p.toFile().exists() && p.toFile().canWrite();

    private static final String[] windowsWrapper = { "jbang.cmd", "jbang.ps1" };
    private static final String otherWrapper = "jbang";

    private final boolean interactiveMode;
    private final MessageWriter output;
    private Path workingDirectory;

    private Optional<Boolean> installed = Optional.empty();
    private Optional<String> version = Optional.empty();

    public JBangSupport(boolean interactiveMode, MessageWriter output) {
        this(interactiveMode, output, Paths.get(System.getProperty("user.dir")));
    }

    public JBangSupport(boolean interactiveMode, MessageWriter output, Path workingDirectory) {
        this.interactiveMode = interactiveMode;
        this.output = output;
        this.workingDirectory = workingDirectory;
    }

    public Optional<File> findWrapper() {
        return Optional.ofNullable(Executable.findWrapper(workingDirectory, windowsWrapper, otherWrapper));
    }

    public Optional<File> findExecutableInPath() {
        try {
            return Optional.ofNullable(Executable.findExecutableFile(otherWrapper));
        } catch (Exception e) {
            output.warn("jbang not found in PATH");
            return Optional.empty();
        }
    }

    public Optional<File> findExecutableInLocalJbang() {
        try {
            return Optional.ofNullable(getInstallationDir()).map(d -> d.resolve("bin").resolve(JBANG_EXECUTABLE))
                    .map(Path::toFile).filter(File::exists);
        } catch (Exception e) {
            output.warn("jbang not found in .jbang");
            return Optional.empty();
        }
    }

    public Optional<File> getOptionalExecutable() {
        return getOptionalExecutable(true);
    }

    public Optional<File> getOptionalExecutable(boolean shouldEnsureInstallation) {
        return findWrapper()
                .or(() -> findExecutableInPath())
                .or(() -> findExecutableInLocalJbang())
                .or(() -> shouldEnsureInstallation && ensureJBangIsInstalled() ? findExecutableInLocalJbang()
                        : Optional.empty())
                .map(e -> {
                    if (!e.canExecute()) {
                        e.setExecutable(true);
                    }
                    return e;
                });
    }

    public File getExecutable() {
        return getOptionalExecutable().orElseThrow(() -> new IllegalStateException("Unable to find and install JBang"));
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public List<String> getCommand() {
        return List.of(getExecutable().getAbsolutePath());
    }

    public List<String> execute(String... args) {
        try {
            List<String> command = new ArrayList<>(args.length + 1);
            command.add(getExecutable().getAbsolutePath());
            command.addAll(Arrays.asList(args));
            List<String> lines = new ArrayList<>();
            try {
                Process process = new ProcessBuilder()
                        .directory(workingDirectory.toFile())
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .command(command)
                        .start();
                // make sure the process does not block waiting for input
                process.getOutputStream().close();

                try (InputStreamReader isr = new InputStreamReader(process.getInputStream());
                        BufferedReader reader = new BufferedReader(isr)) {
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        //Remove ansi escape codes
                        lines.add(line.replaceAll("\u001B\\[[;\\d]*m", ""));
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                process.waitFor();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return lines;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<String> version() {
        if (version.isPresent()) {
            return version;
        } else {
            version = execute("version").stream().findFirst();
        }
        return version;
    }

    public boolean isAvailable() {
        return getOptionalExecutable(false).isPresent() && version().isPresent();
    }

    public boolean isInstallable() {
        return interactiveMode; //installation requires interaction
    }

    public boolean promptForInstallation() {
        // We don't want to prompt users for input when running tests.
        if (interactiveMode
                && Prompt.yesOrNo(true, "JBang is needed to list/run JBang plugins, would you like to install it now?")) {
            return true;
        }
        return false;
    }

    /**
     * Checks id jbang is installed and prompot user for installation.
     * Remembers choice so that we don't prompt user multiple times per action.
     */
    public boolean ensureJBangIsInstalled() {
        if (!installed.isPresent()) {
            installed = Optional.of(doEnsureJBangIsInstalledInternal());
        }
        return installed.orElseThrow();
    }

    private boolean doEnsureJBangIsInstalledInternal() {
        if (isAvailable()) {
            return true;
        }
        if (!isInstallable()) {
            output.warn("JBang is not installable");
            return false;
        }
        if (promptForInstallation()) {
            try {
                installJBang();
                return true;
            } catch (Exception e) {
                output.warn("Failed to install JBang");
                return false;
            }
        } else {
            return false;
        }
    }

    private Path getInstallationDir() {
        Path currentDir = workingDirectory;
        Optional<Path> dir = Optional.ofNullable(currentDir).filter(EXISTS_AND_WRITABLE);
        while (dir.map(Path::getParent).filter(EXISTS_AND_WRITABLE).isPresent()) {
            dir = dir.map(Path::getParent);
        }
        return dir.map(d -> d.resolve(".jbang"))
                .orElseThrow(() -> new IllegalStateException("Failed to determine .jbang directory"));
    }

    private void installJBang() {
        try {
            String uri = "https://www.jbang.dev/releases/latest/download/jbang.zip";
            Path downloadDir = Files.createTempDirectory("jbang-download-");

            if (!downloadDir.toFile().exists() && !downloadDir.toFile().mkdirs()) {
                throw new IOException("Failed to create JBang download directory: " + downloadDir.toAbsolutePath().toString());
            }

            Path downloadFile = downloadDir.resolve("jbang.zip");
            Path installDir = getInstallationDir();
            if (!installDir.toFile().exists() && !installDir.toFile().mkdirs()) {
                throw new IOException("Failed to create JBang install directory: " + installDir.toAbsolutePath().toString());
            }
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .GET()
                    .build();
            HttpResponse<Path> response = client.send(request, BodyHandlers.ofFile(downloadFile));
            ZipUtils.unzip(downloadFile, downloadDir);
            ZipUtils.copyFromZip(downloadDir.resolve("jbang"), installDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
