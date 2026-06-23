///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus.platform:quarkus-bom:3.36.1@pom
//DEPS io.quarkus:quarkus-jackson
//DEPS io.quarkus:quarkus-picocli

//JAVAC_OPTIONS -parameters
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager
//JAVA_OPTIONS -Dquarkus.log.level=WARN
//JAVA_OPTIONS -Dquarkus.banner.enabled=false

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "CheckBackports",
        description = "Check that all commits from pull requests in a backport project column have been cherry-picked")
public class CheckBackports implements Runnable {

    private static final String ORG = "quarkusio";
    private static final String REPO = "quarkus";
    private static final String GRAPHQL_URL = "https://api.github.com/graphql";

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @Parameters(index = "0", description = "The version to check, e.g. 3.33.2")
    String version;

    private String token;

    @Override
    public void run() {
        try {
            doRun();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void doRun() throws Exception {
        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Version must be in X.Y.Z format, got: " + version);
        }
        String majorMinor = parts[0] + "." + parts[1];
        String projectName = "Backports for " + majorMinor;
        String branch = "upstream/" + majorMinor;

        token = resolveToken();

        System.out.println("Checking backport cherry-picks for " + version);
        System.out.println("  Project: " + projectName);
        System.out.println("  Branch:  " + branch);
        System.out.println();

        // Step 1: fetch the upstream branch
        fetchUpstream(majorMinor);

        // Step 2: find the project
        String projectId = findProject(projectName);
        if (projectId == null) {
            throw new IllegalStateException("Could not find project '" + projectName + "' in org " + ORG);
        }

        // Step 3: find pull requests in the target column
        List<PullRequest> pullRequests = findPullRequestsInColumn(projectId, version);
        if (pullRequests.isEmpty()) {
            System.out.println("No pull requests found in column '" + version + "'.");
            return;
        }
        System.out.println("Found " + pullRequests.size() + " pull request" + plural(pullRequests.size()) + " in column '" + version + "'.");
        System.out.println();

        // Step 4: filter out pull requests targeting the branch directly
        List<PullRequest> toCheck = pullRequests.stream()
                .filter(pr -> !pr.baseRefName.equals(majorMinor))
                .toList();

        // Step 5: for each remaining pull request, check commits
        List<PullRequestReport> reports = new ArrayList<>();
        int checked = 0;
        for (PullRequest pr : toCheck) {
            checked++;
            System.out.print("\rChecking pull requests... [" + checked + "/" + toCheck.size() + "]");

            List<CommitInfo> commits = getPullRequestCommits(pr.number);
            List<CommitInfo> missing = new ArrayList<>();
            for (CommitInfo c : commits) {
                if (!isCherryPicked(c.sha, branch)) {
                    missing.add(c);
                }
            }
            if (!missing.isEmpty()) {
                reports.add(new PullRequestReport(pr, missing));
            }
        }
        System.out.println();
        System.out.println();

        // Step 6: report
        if (reports.isEmpty()) {
            System.out.println("All commits from all pull requests have been cherry-picked to " + branch + ".");
        } else {
            System.out.println("=== Missing cherry-picks for " + version + " ===");
            System.out.println();
            System.out.println(reports.size() + " pull request" + plural(reports.size()) + " with missing cherry-picks");
            System.out.println();
            for (PullRequestReport report : reports) {
                PullRequest pr = report.pullRequest;
                System.out.println("#" + pr.number + " · " + pr.title);
                System.out.println("  -> " + pr.url);
                for (CommitInfo c : report.missingCommits) {
                    System.out.println("  " + c.sha.substring(0, 12) + " " + c.firstLine);
                }
                System.out.println();
            }
        }
    }

    private void fetchUpstream(String branch) throws Exception {
        System.out.println("Fetching upstream/" + branch + "...");
        ProcessBuilder pb = new ProcessBuilder("git", "fetch", "upstream", branch);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        if (p.waitFor() != 0) {
            throw new IllegalStateException("Failed to fetch upstream/" + branch + ". Make sure the 'upstream' remote is configured.");
        }
        System.out.println();
    }

    private String resolveToken() throws Exception {
        String t = System.getenv("GITHUB_TOKEN");
        if (t != null && !t.isBlank()) {
            return t;
        }
        t = System.getenv("GH_TOKEN");
        if (t != null && !t.isBlank()) {
            return t;
        }
        ProcessBuilder pb = new ProcessBuilder("gh", "auth", "token");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new BufferedReader(new InputStreamReader(p.getInputStream()))
                .lines().collect(Collectors.joining("\n")).trim();
        if (p.waitFor() == 0 && !output.isBlank()) {
            return output;
        }
        throw new IllegalStateException("No GitHub token found. Set GITHUB_TOKEN or authenticate with 'gh auth login'.");
    }

    private String findProject(String projectName) throws Exception {
        String query = """
                query($org: String!, $search: String!) {
                  organization(login: $org) {
                    projectsV2(first: 20, query: $search) {
                      nodes { id title }
                    }
                  }
                }""";

        JsonNode response = graphql(query, Map.of("org", ORG, "search", projectName));
        JsonNode nodes = response.at("/data/organization/projectsV2/nodes");
        for (JsonNode node : nodes) {
            if (projectName.equals(node.get("title").asText())) {
                return node.get("id").asText();
            }
        }
        return null;
    }

    private List<PullRequest> findPullRequestsInColumn(String projectId, String columnName) throws Exception {
        List<PullRequest> result = new ArrayList<>();
        String cursor = null;

        String query = """
                query($projectId: ID!, $after: String) {
                  node(id: $projectId) {
                    ... on ProjectV2 {
                      items(first: 100, after: $after) {
                        nodes {
                          fieldValues(first: 20) {
                            nodes {
                              ... on ProjectV2ItemFieldSingleSelectValue {
                                name
                              }
                            }
                          }
                          content {
                            ... on PullRequest { number title url baseRefName }
                          }
                        }
                        pageInfo { hasNextPage endCursor }
                      }
                    }
                  }
                }""";

        while (true) {
            Map<String, Object> variables = new java.util.LinkedHashMap<>();
            variables.put("projectId", projectId);
            variables.put("after", cursor);

            JsonNode response = graphql(query, variables);
            JsonNode items = response.at("/data/node/items");
            JsonNode nodes = items.get("nodes");

            for (JsonNode item : nodes) {
                // Check if any field value matches the column name
                boolean matches = false;
                for (JsonNode fv : item.at("/fieldValues/nodes")) {
                    if (fv.has("name") && columnName.equals(fv.get("name").asText())) {
                        matches = true;
                        break;
                    }
                }

                if (matches) {
                    JsonNode content = item.get("content");
                    if (content != null && content.has("number")) {
                        result.add(new PullRequest(
                                content.get("number").asInt(),
                                content.get("title").asText(),
                                content.get("url").asText(),
                                content.get("baseRefName").asText()));
                    }
                }
            }

            JsonNode pageInfo = items.get("pageInfo");
            if (pageInfo.get("hasNextPage").asBoolean()) {
                cursor = pageInfo.get("endCursor").asText();
            } else {
                break;
            }
        }

        return result;
    }

    private List<CommitInfo> getPullRequestCommits(int prNumber) throws Exception {
        List<CommitInfo> commits = new ArrayList<>();
        String cursor = null;

        String query = """
                query($owner: String!, $repo: String!, $prNumber: Int!, $after: String) {
                  repository(owner: $owner, name: $repo) {
                    pullRequest(number: $prNumber) {
                      commits(first: 100, after: $after) {
                        nodes {
                          commit { oid message }
                        }
                        pageInfo { hasNextPage endCursor }
                      }
                    }
                  }
                }""";

        while (true) {
            Map<String, Object> variables = new java.util.LinkedHashMap<>();
            variables.put("owner", ORG);
            variables.put("repo", REPO);
            variables.put("prNumber", prNumber);
            variables.put("after", cursor);

            JsonNode response = graphql(query, variables);
            JsonNode commitsNode = response.at("/data/repository/pullRequest/commits");

            for (JsonNode node : commitsNode.get("nodes")) {
                JsonNode commit = node.get("commit");
                String sha = commit.get("oid").asText();
                String message = commit.get("message").asText();
                String firstLine = message.split("\\n")[0];
                if (!firstLine.startsWith("Merge branch ") && !firstLine.startsWith("Merge pull request ")) {
                    commits.add(new CommitInfo(sha, firstLine));
                }
            }

            JsonNode pageInfo = commitsNode.get("pageInfo");
            if (pageInfo.get("hasNextPage").asBoolean()) {
                cursor = pageInfo.get("endCursor").asText();
            } else {
                break;
            }
        }

        return commits;
    }

    private boolean isCherryPicked(String sha, String branch) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "git", "log", branch, "--grep=cherry picked from commit " + sha, "--oneline", "-1");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new BufferedReader(new InputStreamReader(p.getInputStream()))
                .lines().collect(Collectors.joining("\n")).trim();
        return p.waitFor() == 0 && !output.isBlank();
    }

    private JsonNode graphql(String query, Map<String, Object> variables) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("query", query);
        body.set("variables", objectMapper.valueToTree(variables));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPHQL_URL))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("GraphQL HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode json = objectMapper.readTree(response.body());
        if (json.has("errors")) {
            throw new RuntimeException("GraphQL error: " + json.get("errors"));
        }
        return json;
    }

    private static String plural(int count) {
        return count > 1 ? "s" : "";
    }

    record PullRequest(int number, String title, String url, String baseRefName) {}
    record CommitInfo(String sha, String firstLine) {}
    record PullRequestReport(PullRequest pullRequest, List<CommitInfo> missingCommits) {}
}
