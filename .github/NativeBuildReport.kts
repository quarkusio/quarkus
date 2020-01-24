#!/usr/bin/env kscript

@file:MavenRepository("jcenter","https://jcenter.bintray.com/")
@file:MavenRepository("maven-central","https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.kohsuke:github-api:1.101")


import org.kohsuke.github.*
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

val token = args[0]; 
val status = args[1];
val issueNumber = args[2].toInt();

val REPO = "quarkusio/quarkus"

// Handle status. Possible values are success, failure, or cancelled.
val succeed = status.equals("success", ignoreCase = true);
if (status.equals("cancelled", ignoreCase = true)) {
    println("Job status is `cancelled` - exiting")
    System.exit(0)
}
println("The CI build had status ${status}.")

val github = GitHubBuilder().withOAuthToken(token).build()
val repository = github.getRepository(REPO)

val issue = repository.getIssue(issueNumber)
if (issue == null) {
    println("Unable to find the issue ${issueNumber} in project ${REPO}")
    System.exit(-1)
} else {
    println("Report issue found: ${issue.getTitle()} - ${issue.getHtmlUrl()}")
    println("The issue is currently ${issue.getState()}")
}

val quarkusCommit = getRepositoryCommit(".")
if (succeed) {
    if (issue != null  && isOpen(issue)) {
        // close issue with a comment
        val comment = issue.comment("""
            Build fixed with:

            * Quarkus commit: ${quarkusCommit}
            * Link to build: https://github.com/quarkusio/quarkus/actions

        """.trimIndent())        
        issue.close()
        println("Comment added on issue ${issue.getHtmlUrl()} - ${comment.getHtmlUrl()}, the issue has also been closed")
    } else {
        println("Nothing to do - the build passed and the issue is already closed")
    }
} else  {
    if (isOpen(issue)) {
        val comment = issue.comment("""
        The build is still failing with:

        * Quarkus commit: ${quarkusCommit}
        * Link to build: https://github.com/quarkusio/quarkus/actions

    """.trimIndent())
        println("Comment added on issue ${issue.getHtmlUrl()} - ${comment.getHtmlUrl()}")
    } else {
        issue.reopen()
        val comment = issue.comment("""
        Unfortunately, the build failed:

        * Quarkus commit: ${quarkusCommit} 
        * Link to build: https://github.com/quarkusio/quarkus/actions

    """.trimIndent())
        println("Comment added on issue ${issue.getHtmlUrl()} - ${comment.getHtmlUrl()}, the issue has been re-opened.")

    }
}


fun getRepositoryCommit(workingDir: String) : String {
    return "git rev-parse HEAD".runCommand(workingDir)
}

fun String.runCommand(workingDir: String): String {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(File(workingDir))
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(1, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        return ""
    }
}

fun isOpen(issue : GHIssue) : Boolean {
    return issue.getState() == GHIssueState.OPEN
}

