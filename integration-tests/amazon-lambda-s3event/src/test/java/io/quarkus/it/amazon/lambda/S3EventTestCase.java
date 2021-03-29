package io.quarkus.it.amazon.lambda;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class S3EventTestCase {

    static String json = "{\n" +
            "  \"Records\":[\n" +
            "    {\n" +
            "      \"eventVersion\":\"2.0\",\n" +
            "      \"eventSource\":\"aws:s3\",\n" +
            "      \"awsRegion\":\"us-west-2\",\n" +
            "      \"eventTime\":\"1970-01-01T00:00:00.000Z\",\n" +
            "      \"eventName\":\"ObjectCreated:Put\",\n" +
            "      \"userIdentity\":{\n" +
            "        \"principalId\":\"AIDAJDPLRKLG7UEXAMPLE\"\n" +
            "      },\n" +
            "      \"requestParameters\":{\n" +
            "        \"sourceIPAddress\":\"127.0.0.1\"\n" +
            "      },\n" +
            "      \"responseElements\":{\n" +
            "        \"x-amz-request-id\":\"C3D13FE58DE4C810\",\n" +
            "        \"x-amz-id-2\":\"FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD\"\n" +
            "      },\n" +
            "      \"s3\":{\n" +
            "        \"s3SchemaVersion\":\"1.0\",\n" +
            "        \"configurationId\":\"testConfigRule\",\n" +
            "        \"bucket\":{\n" +
            "          \"name\":\"sourcebucket\",\n" +
            "          \"ownerIdentity\":{\n" +
            "            \"principalId\":\"A3NL1KOZZKExample\"\n" +
            "          },\n" +
            "          \"arn\":\"arn:aws:s3:::sourcebucket\"\n" +
            "        },\n" +
            "        \"object\":{\n" +
            "          \"key\":\"HappyFace.jpg\",\n" +
            "          \"size\":1024,\n" +
            "          \"eTag\":\"d41d8cd98f00b204e9800998ecf8427e\",\n" +
            "          \"versionId\":\"096fKKXTRTtl3on89fVO.nfljtsv6qko\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Test
    public void testS3() throws Exception {
        String out = LambdaClient.invokeJson(String.class, json);
        Assertions.assertEquals("Ok", out);
    }

}
