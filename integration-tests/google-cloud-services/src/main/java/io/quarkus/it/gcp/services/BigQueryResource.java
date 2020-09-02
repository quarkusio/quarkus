package io.quarkus.it.gcp.services;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;

@Path("/bigquery")
public class BigQueryResource {
    @Inject
    BigQuery bigquery;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String bigquery() throws InterruptedException {
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(
                "SELECT "
                        + "CONCAT('https://stackoverflow.com/questions/', CAST(id as STRING)) as url, view_count "
                        + "FROM `bigquery-public-data.stackoverflow.posts_questions` "
                        + "WHERE tags like '%google-bigquery%' ORDER BY favorite_count DESC LIMIT 10")
                .setUseLegacySql(false)
                .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            throw new RuntimeException(queryJob.getStatus().getError().toString());
        }

        // Get the results and return it
        TableResult result = queryJob.getQueryResults();
        return StreamSupport.stream(result.iterateAll().spliterator(), false)
                .map(row -> row.get("url").getStringValue() + " - " + row.get("view_count").getLongValue() + "\n")
                .collect(Collectors.joining());
    }
}