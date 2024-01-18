
// Configure build scan publication
boolean publish = true
if(session?.getRequest()?.getBaseDirectory() != null) {
    def testBuildPaths = [
        File.separator + 'target' + File.separator + 'codestart-test' + File.separator,
        File.separator + 'target' + File.separator + 'it' + File.separator,
        File.separator + 'target' + File.separator + 'test-classes' + File.separator,
        File.separator + 'target' + File.separator + 'test-project' + File.separator
    ]
    publish = testBuildPaths.every {testBuildPath -> !session.getRequest().getBaseDirectory().contains(testBuildPath) }
    if(!publish) {
        // do not publish a build scan for test builds
        log.debug("Disabling build scan publication for " + session.getRequest().getBaseDirectory())

        // change storage location on CI to avoid Develocity scan dumps with disabled publication to be captured for republication
        if (System.env.GITHUB_ACTIONS) {
            try {
                def storageLocationTmpDir = java.nio.file.Files.createTempDirectory(java.nio.file.Paths.get(System.env.RUNNER_TEMP), "buildScanTmp").toAbsolutePath()
                log.debug('Update storage location to ' + storageLocationTmpDir)
                gradleEnterprise.setStorageDirectory(storageLocationTmpDir)
            } catch (IOException e) {
                log.error('Temporary storage location directory cannot be created, the Build Scan will be published', e)
            }
        }
    }
}
buildScan.publishAlwaysIf(publish)
buildScan.publishIfAuthenticated()

// Add mvn command line
def mvnCommand = ''
if (System.env.MAVEN_CMD_LINE_ARGS) {
    mvnCommand = "mvn ${System.env.MAVEN_CMD_LINE_ARGS}".toString()
    buildScan.value('mvn command line', mvnCommand)
}

//Add github action information
if (System.env.GITHUB_ACTIONS) {
    def jobId = System.env.GITHUB_JOB

    buildScan.value('gh-job-id', jobId)
    buildScan.value('gh-event-name', System.env.GITHUB_EVENT_NAME)
    buildScan.value('gh-ref-name', System.env.GITHUB_REF_NAME)
    buildScan.value('gh-actor', System.env.GITHUB_ACTOR)
    buildScan.value('gh-workflow', System.env.GITHUB_WORKFLOW)
    String jobCustomValues = System.env.GE_CUSTOM_VALUES
    if (jobCustomValues != null && !jobCustomValues.isBlank()) {
        for (String jobCustomValue : jobCustomValues.split(",")) {
            int index = jobCustomValue.indexOf('=')
            if (index <= 0) {
                continue
            }
            buildScan.value(jobCustomValue.substring(0, index).trim(), jobCustomValue.substring(index + 1).trim())
        }
    }

    List<String> similarBuildsTags = new ArrayList<>()

    buildScan.tag(jobId)
    similarBuildsTags.add(jobId)

    buildScan.tag(System.env.GITHUB_EVENT_NAME)
    similarBuildsTags.add(System.env.GITHUB_EVENT_NAME)

    buildScan.tag(System.env.GITHUB_WORKFLOW)
    similarBuildsTags.add(System.env.GITHUB_WORKFLOW)

    String jobTags = System.env.GE_TAGS
    if (jobTags != null && !jobTags.isBlank()) {
        for (String tag : jobTags.split(",")) {
            buildScan.tag(tag.trim())
            similarBuildsTags.add(tag.trim())
        }
    }

    buildScan.link('Workflow run', System.env.GITHUB_SERVER_URL + '/' + System.env.GITHUB_REPOSITORY + '/actions/runs/' + System.env.GITHUB_RUN_ID)

    def prNumber = System.env.PULL_REQUEST_NUMBER
    if (prNumber != null && !prNumber.isBlank()) {
        buildScan.value('gh-pr', prNumber)
        buildScan.tag('pr-' + prNumber)
        similarBuildsTags.add('pr-' + prNumber)

        buildScan.link('Pull request', System.env.GITHUB_SERVER_URL + '/' + System.env.GITHUB_REPOSITORY + '/pull/' + prNumber )
    }

    similarBuildsTags.add(System.env.RUNNER_OS)

    buildScan.link('Similar builds', 'https://ge.quarkus.io/scans?search.tags=' + java.net.URLEncoder.encode(String.join(",", similarBuildsTags), "UTF-8").replace("+", "%20"))

    buildScan.buildScanPublished {  publishedBuildScan -> {
            File target = new File("target")
            if (!target.exists()) {
                target.mkdir()
            }
            File gradleBuildScanUrlFile = new File("target/gradle-build-scan-url.txt")
            if (!gradleBuildScanUrlFile.exists()) {
                gradleBuildScanUrlFile.withWriter { out ->
                    out.print(publishedBuildScan.buildScanUri)
                }
            }
            new File(System.env.GITHUB_STEP_SUMMARY).withWriterAppend { out ->
                out.println("\n[Build scan](${publishedBuildScan.buildScanUri})\n<sup>`${mvnCommand}`</sup>\n\n")
            }
        }
    }
}

// Check runtime Maven version and Maven Wrapper version are aligned
def runtimeInfo = (org.apache.maven.rtinfo.RuntimeInformation) session.lookup("org.apache.maven.rtinfo.RuntimeInformation")
def runtimeMavenVersion = runtimeInfo?.getMavenVersion()
Properties mavenWrapperProperties = new Properties()
File mavenWrapperPropertiesFile = new File(".mvn/wrapper/maven-wrapper.properties")
if(mavenWrapperPropertiesFile.exists()) {
    mavenWrapperPropertiesFile.withInputStream {
        mavenWrapperProperties.load(it)
    }
    // assuming the wrapper properties contains:
    // distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/VERSION/apache-maven-VERSION-bin.zip
    if(regexp = mavenWrapperProperties."distributionUrl" =~ /.*\/apache-maven-(.*)-bin\.zip/) {
        def wrapperMavenVersion = regexp.group(1)
        if (runtimeMavenVersion && wrapperMavenVersion && wrapperMavenVersion != runtimeMavenVersion) {
            log.warn("Maven Wrapper is configured with a different version (" + wrapperMavenVersion + ") than the runtime version (" + runtimeMavenVersion + "). This will negatively impact build consistency and build caching.")
            buildScan.tag("misaligned-maven-version")
            buildScan.value("wrapper-maven-version", wrapperMavenVersion)
        }
    }
}
