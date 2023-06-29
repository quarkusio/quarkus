import groovy.json.JsonSlurper

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

    if (System.env.GITHUB_EVENT_NAME == "pull_request" && System.env.GITHUB_EVENT_PATH != null) {
        File eventJsonFile = new File(System.env.GITHUB_EVENT_PATH)
        if (eventJsonFile.exists()) {
            def eventJson = new JsonSlurper().parse(eventJsonFile)
            def prNumber = eventJson.pull_request?.number

            if (prNumber != null) {
                buildScan.value('gh-pr', prNumber)
                buildScan.tag('pr-' + prNumber)
            }
        }
    }

    buildScan.buildScanPublished {  publishedBuildScan ->
        new File(System.env.GITHUB_STEP_SUMMARY).withWriterAppend { out ->
            out.println("\n[Build scan for '${mvnCommand}' in ${jobName}](${publishedBuildScan.buildScanUri})\n")
        }
    }
}

