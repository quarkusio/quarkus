
// Configure build scan publication
boolean publish = true
if(session?.getRequest()?.getBaseDirectory() != null) {
    def testBuildPaths = [ '/target/codestart-test/', '/target/it/', '/target/test-classes/', '/target/test-project/']
    publish = testBuildPaths.every {testBuildPath -> !session.getRequest().getBaseDirectory().contains(testBuildPath) }
    if(!publish) {
        // do not publish a build scan for test builds
        log.debug("Disabling build scan publication for " + session.getRequest().getBaseDirectory())
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
    def jobName = System.env.GITHUB_JOB

    buildScan.value('gh-job-name', jobName)
    buildScan.value('gh-event-name', System.env.GITHUB_EVENT_NAME)
    buildScan.value('gh-ref-name', System.env.GITHUB_REF_NAME)
    buildScan.value('gh-actor', System.env.GITHUB_ACTOR)
    buildScan.value('gh-workflow', System.env.GITHUB_WORKFLOW)


    def prnumber = System.env.PULL_REQUEST_NUMBER
    if (prnumber != null) {
        buildScan.value('gh-pr', prnumber)
        buildScan.tag('pr-' + prnumber)
    }

    buildScan.buildScanPublished {  publishedBuildScan ->
        new File(System.env.GITHUB_STEP_SUMMARY).withWriterAppend { out ->
            out.println("\n[Build scan for '${mvnCommand}' in ${jobName}](${publishedBuildScan.buildScanUri})\n")
        }
    }
}

