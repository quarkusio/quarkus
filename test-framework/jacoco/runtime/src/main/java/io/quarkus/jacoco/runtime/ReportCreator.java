package io.quarkus.jacoco.runtime;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.MultiReportVisitor;
import org.jacoco.report.MultiSourceFileLocator;
import org.jacoco.report.csv.CSVFormatter;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;
import org.jboss.logging.Logger;

public class ReportCreator implements Runnable {

    private static final Logger LOG = Logger.getLogger(ReportCreator.class);

    private final ReportInfo reportInfo;
    private final JacocoConfig config;
    private final DataFileWatch dataFileWatch;

    public ReportCreator(ReportInfo reportInfo, JacocoConfig config) {
        this.reportInfo = reportInfo;
        this.config = config;
        this.dataFileWatch = new DataFileWatch(reportInfo.dataFile, () -> !jacocoRunning(), reportInfo::emitError);
    }

    @Override
    public void run() {
        // Ugly workaround:
        // Multiple ReportCreator shutdown hooks might run concurrently, possibly corrupting the report file(s) - e.g. when using @TestProfile.
        // By locking on a class from the parent CL, all hooks are "serialized", one after another.
        // In the long run there should only be as many hooks as there are different Jacoco configs...usually there will be only one config anyway!
        synchronized (ExecFileLoader.class) {
            doRun();
        }
    }

    private void doRun() {
        Path targetDir = Paths.get(reportInfo.reportDir);
        try {
            Files.createDirectories(targetDir);

            if (!dataFileWatch.waitForDataFile()) {
                reportInfo.emitError("Jacoco data file " + reportInfo.dataFile
                        + " may not have been updated, because either the file has never been written or the file " +
                        "has not been updated or the jacoco shutdown hook did not finish after 10 seconds.\n" +
                        "If you experience EOFExceptions and/or delays after the last test run, the recommended " +
                        "work around is to disable report generation by setting `quarkus.jacoco.report=false` and " +
                        "add report generation tasks to your Maven/Gradle builds.");
            }

            ExecFileLoader loader = new ExecFileLoader();
            for (String i : reportInfo.savedData) {
                File file = new File(i);
                if (file.exists()) {
                    LOG.debugf("JaCoCo is loading data for report from: %s", file);
                    loader.load(file);
                }
            }
            final CoverageBuilder builder = new CoverageBuilder();
            final Analyzer analyzer = new Analyzer(
                    loader.getExecutionDataStore(), builder);
            for (String i : reportInfo.classFiles) {
                File file = new File(i);
                if (file.exists()) {
                    analyzer.analyzeAll(file);
                }
            }

            List<IReportVisitor> formatters = new ArrayList<>();
            addXmlFormatter(targetDir.resolve("jacoco.xml"), config.outputEncoding(), formatters);
            addCsvFormatter(targetDir.resolve("jacoco.csv"), config.outputEncoding(), formatters);
            addHtmlFormatter(targetDir, config.outputEncoding(), config.footer().orElse(""), Locale.getDefault(),
                    formatters);

            //now for the hacky bit

            final IReportVisitor visitor = new MultiReportVisitor(formatters);
            visitor.visitInfo(loader.getSessionInfoStore().getInfos(),
                    loader.getExecutionDataStore().getContents());
            MultiSourceFileLocator sourceFileLocator = new MultiSourceFileLocator(4);
            for (String i : reportInfo.sourceDirectories) {
                sourceFileLocator.add(new DirectorySourceFileLocator(new File(i), config.sourceEncoding(), 4));
            }
            final IBundleCoverage bundle = builder.getBundle(config.title().orElse(reportInfo.artifactId));
            visitor.visitBundle(bundle, sourceFileLocator);
            visitor.visitEnd();
            System.out.println("Generated Jacoco reports in " + targetDir);
            System.out.flush();
        } catch (Exception e) {
            reportInfo.emitError("Failed to generate Jacoco reports", e);
        }
    }

    private static boolean jacocoRunning() {
        try {
            ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName("org.jacoco:type=Runtime"));
            return true;
        } catch (InstanceNotFoundException e) {
            return false;
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    public void addXmlFormatter(final Path targetFile, final String encoding, List<IReportVisitor> formatters)
            throws IOException {
        final XMLFormatter xml = new XMLFormatter();
        xml.setOutputEncoding(encoding);
        formatters.add(xml.createVisitor(Files.newOutputStream(targetFile)));
    }

    public void addCsvFormatter(final Path targetFile, final String encoding, List<IReportVisitor> formatters)
            throws IOException {
        final CSVFormatter csv = new CSVFormatter();
        csv.setOutputEncoding(encoding);
        formatters.add(csv.createVisitor(Files.newOutputStream(targetFile)));
    }

    public void addHtmlFormatter(final Path targetDir, final String encoding,
            final String footer, final Locale locale, List<IReportVisitor> formatters) throws IOException {
        final HTMLFormatter htmlFormatter = new HTMLFormatter();
        htmlFormatter.setOutputEncoding(encoding);
        htmlFormatter.setLocale(locale);
        if (footer != null) {
            htmlFormatter.setFooterText(footer);
        }
        formatters.add(htmlFormatter
                .createVisitor(new FileMultiReportOutput(targetDir.toFile())));
    }

}
