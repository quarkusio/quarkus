/*
 *  Copyright (c) 2019 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package org.jboss.shamrock.maven.it.verifier;


import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static org.jboss.shamrock.maven.it.ExtensionListIT.installPluginToLocalRepository;

/**
 * Implementation of verifier using a forked process that is still running while verifying. The process is stop when
 * {@link RunningInvoker#stop()} is called.
 */
public class RunningInvoker extends MavenProcessInvoker {

    private final boolean debug;
    private MavenProcessInvocationResult result;
    private final File log;
    private final PrintStreamHandler logHandler;

    public RunningInvoker(File basedir, boolean debug) throws FileNotFoundException {
        this.debug = debug;
        setWorkingDirectory(basedir);
        String repo = System.getProperty("maven.repo");
        if (repo == null) {
            repo = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
        }
        installPluginToLocalRepository(new File(repo));
        setLocalRepositoryDirectory(new File(repo));
        log = new File(basedir, "build-" + basedir.getName() + ".log");
        PrintStream stream = new PrintStream(log);
        logHandler = new PrintStreamHandler(stream, true);
        setErrorHandler(logHandler);
        setOutputHandler(logHandler);
        setLogger(new PrintStreamLogger(stream, InvokerLogger.DEBUG));
    }

    public MavenProcessInvocationResult execute(List<String> goals, Map<String, String> envVars) throws MavenInvocationException {
        DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(goals);
        request.setDebug(debug);
        request.setLocalRepositoryDirectory(getLocalRepositoryDirectory());
        request.setBaseDirectory(getWorkingDirectory());
        request.setPomFile(new File(getWorkingDirectory(), "pom.xml"));

        if (System.getProperty("mavenOpts") != null) {
            request.setMavenOpts(System.getProperty("mavenOpts"));
        }

        request.setShellEnvironmentInherited(true);
        envVars.forEach(request::addShellEnvironment);
        request.setOutputHandler(logHandler);
        request.setErrorHandler(logHandler);
        this.result = (MavenProcessInvocationResult) execute(request);
        return result;
    }

    @Override
    public InvocationResult execute(InvocationRequest request) throws MavenInvocationException {
        return super.execute(request);
    }

    public String log() throws IOException {
        return FileUtils.readFileToString(log, "UTF-8");
    }
}
