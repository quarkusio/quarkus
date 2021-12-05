package io.quarkus.arc.deployment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Split package (same package coming from multiple app archives) is considered a bad practice and
 * this processor tries to detect it and log a warning listing out the offending packages.
 * <p>
 * Note that this processor is a best-effort because it only operates on {@code ApplicationArchivesBuildItem} which
 * means that if a 3rd party library isn't indexed, we aren't able to detect it even though it can still be a part of
 * resulting application. See also {@code io.quarkus.arc.processor.BeanArchives.IndexWrapper}.
 * <p>
 * This processor can be configured by users and extensions to skip validation for certain packages hence avoiding the
 * warning in logs.
 * See also {@link ArcConfig#ignoredSplitPackages} and {@link IgnoreSplitPackageBuildItem}
 */
public class SplitPackageProcessor {

    private static final Logger LOGGER = Logger.getLogger(SplitPackageProcessor.class);

    private static final Predicate<String> IGNORE_PACKAGE = new Predicate<>() {

        @Override
        public boolean test(String packageName) {
            // Remove the elements from this list when the original issue is fixed
            // so that we can detect further issues.
            return packageName.startsWith("io.fabric8.kubernetes");
        }
    };

    @BuildStep
    void splitPackageDetection(ApplicationArchivesBuildItem archivesBuildItem,
            ArcConfig config,
            List<IgnoreSplitPackageBuildItem> excludedPackages,
            // Dummy producer to make sure the build step executes
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> dummy) {
        Map<String, Set<ApplicationArchive>> packageToArchiveMap = new HashMap<>();

        // build up exclusion predicates from user defined config and extensions
        List<Predicate<String>> packageSkipPredicates = new ArrayList<>();
        if (config.ignoredSplitPackages.isPresent()) {
            packageSkipPredicates.addAll(initPredicates(config.ignoredSplitPackages.get()));
        }
        for (IgnoreSplitPackageBuildItem exclusionBuildItem : excludedPackages) {
            packageSkipPredicates.addAll(initPredicates(exclusionBuildItem.getExcludedPackages()));
        }

        // for all app archives
        for (ApplicationArchive archive : archivesBuildItem.getAllApplicationArchives()) {
            // and for each known class in each archive
            for (ClassInfo classInfo : archive.getIndex().getKnownClasses()) {
                String packageName = DotNames.packageName(classInfo.name());
                packageToArchiveMap.compute(packageName, (key, val) -> {
                    Set<ApplicationArchive> returnValue = val == null ? new HashSet<>() : val;
                    returnValue.add(archive);
                    return returnValue;
                });
            }
        }
        // for each split package, create something like
        // - "com.me.app.sub" found in [archiveA, archiveB]
        StringBuilder splitPackagesWarning = new StringBuilder();
        for (String packageName : packageToArchiveMap.keySet()) {
            if (IGNORE_PACKAGE.test(packageName)) {
                continue;
            }

            // skip packages based on pre-built predicates
            boolean skipEvaluation = false;
            for (Predicate<String> predicate : packageSkipPredicates) {
                if (predicate.test(packageName)) {
                    skipEvaluation = true;
                    break;
                }
            }
            if (skipEvaluation) {
                continue;
            }

            Set<ApplicationArchive> applicationArchives = packageToArchiveMap.get(packageName);
            if (applicationArchives.size() > 1) {
                splitPackagesWarning.append("\n- \"" + packageName + "\" found in ");
                Iterator<ApplicationArchive> iterator = applicationArchives.iterator();
                Set<String> splitPackages = new TreeSet<>();
                while (iterator.hasNext()) {
                    final ApplicationArchive next = iterator.next();
                    final ArtifactKey a = next.getKey();
                    // can be null for instance in test mode where all application classes go under target/classes
                    if (a == null) {
                        if (archivesBuildItem.getRootArchive().equals(next)) {
                            // the archive we found is a root archive, e.g. application classes
                            splitPackages.add("application classes");
                        } else {
                            // as next best effort, we try to take first path from archive paths collection
                            Iterator<Path> pathIterator = next.getResolvedPaths().iterator();
                            if (pathIterator.hasNext()) {
                                splitPackages.add(pathIterator.next().toString());
                            } else {
                                // if all else fails, we mark it as unknown archive
                                splitPackages.add("unknown archive");
                            }
                        }
                    } else {
                        // Generates an app archive information in form of groupId:artifactId:classifier:type
                        splitPackages.add(a.toString());
                    }
                }
                splitPackagesWarning.append(splitPackages.stream().collect(Collectors.joining(", ", "[", "]")));
            }
        }
        // perform logging if needed
        if (splitPackagesWarning.length() > 0) {
            LOGGER.warnf("Detected a split package usage which is considered a bad practice and should be avoided. " +
                    "Following packages were detected in multiple archives: %s", splitPackagesWarning.toString());
        }
    }

    private List<Predicate<String>> initPredicates(Collection<String> exclusions) {
        final String packMatch = ".*";
        List<Predicate<String>> predicates = new ArrayList<>();
        for (String exclusionExpression : exclusions) {
            if (exclusionExpression.endsWith(packMatch)) {
                // Package ends with ".*"
                final String pack = exclusionExpression.substring(0, exclusionExpression.length() - packMatch.length());
                predicates.add(new Predicate<String>() {
                    @Override
                    public boolean test(String packageName) {
                        return packageName.equals(pack) || packageName.startsWith(pack + ".");
                    }
                });
            } else {
                // Standard full package name
                predicates.add(new Predicate<String>() {
                    @Override
                    public boolean test(String packageName) {
                        return packageName.equals(exclusionExpression);
                    }
                });
            }
        }
        return predicates;
    }
}
