//usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.kohsuke:github-api:1.128
//DEPS info.picocli:picocli:4.6.1

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

@Command(name = "report", mixinStandardHelpOptions = true,
		description = "Takes care of updating the appropriate issue depending on the status of the build")
class Report implements Runnable {

	@Option(names = "token", description = "Github token to use when calling the Github API")
	private String token;

	@Option(names = "status", description = "The status of the CI run")
	private String status;

	@Option(names = "issueRepo", description = "The repository where the issue resides (i.e. quarkusio/quarkus)")
	private String issueRepo;

	@Option(names = "issueNumber", description = "The issue to update")
	private Integer issueNumber;

	@Option(names = "thisRepo", description = "The repository for which we are reporting the CI status")
	private String thisRepo;

	@Option(names = "runId", description = "The ID of the Github Action run for  which we are reporting the CI status")
	private String runId;

	@Override
	public void run() {
		try {
			final boolean succeed = "success".equalsIgnoreCase(status);
			if ("cancelled".equalsIgnoreCase(status)) {
				System.out.println("Job status is `cancelled` - exiting");
				System.exit(0);
			}

			System.out.println(String.format("The CI build had status %s.", status));

			final GitHub github = new GitHubBuilder().withOAuthToken(token).build();
			final GHRepository repository = github.getRepository(issueRepo);

			final GHIssue issue = repository.getIssue(issueNumber);
			if (issue == null) {
				System.out.println(String.format("Unable to find the issue %s in project %s", issueNumber, issueRepo));
				System.exit(-1);
			} else {
				System.out.println(String.format("Report issue found: %s - %s", issue.getTitle(), issue.getHtmlUrl().toString()));
				System.out.println(String.format("The issue is currently %s", issue.getState().toString()));
			}

			if (succeed) {
				if (issue != null  && isOpen(issue)) {
					// close issue with a comment
					final GHIssueComment comment = issue.comment(String.format("Build fixed:\n* Link to latest CI run: https://github.com/%s/actions/runs/%s", thisRepo, runId));
					issue.close();
					System.out.println(String.format("Comment added on issue %s - %s, the issue has also been closed", issue.getHtmlUrl().toString(), comment.getHtmlUrl().toString()));
				} else {
					System.out.println("Nothing to do - the build passed and the issue is already closed");
				}
			} else  {
				if (isOpen(issue)) {
					final GHIssueComment comment = issue.comment(String.format("The build is still failing:\n* Link to latest CI run: https://github.com/%s/actions/runs/%s", thisRepo, runId));
					System.out.println(String.format("Comment added on issue %s - %s", issue.getHtmlUrl().toString(), comment.getHtmlUrl().toString()));
				} else {
					issue.reopen();
					final GHIssueComment comment = issue.comment(String.format("Unfortunately, the build failed:\n* Link to latest CI run: https://github.com/%s/actions/runs/%s", thisRepo, runId));
					System.out.println(String.format("Comment added on issue %s - %s, the issue has been re-opened", issue.getHtmlUrl().toString(), comment.getHtmlUrl().toString()));
				}
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static boolean isOpen(GHIssue issue) {
		return (issue.getState() == GHIssueState.OPEN);
	}

	public static void main(String... args) {
		int exitCode = new CommandLine(new Report()).execute(args);
		System.exit(exitCode);
	}
}