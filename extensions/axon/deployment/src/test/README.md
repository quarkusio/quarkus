The unit tests for this extension are ignored because the Axon framework can't be run in embedded mode.

To run this tests its required to start an Axon server instance on localhost.

You can start that with Docker:
```
docker run -d --name my-axon-server -p 8024:8024 -p 8124:8124 axoniq/axonserver
```

The test results also depends on output to the console and needs a human to validate ;-)
