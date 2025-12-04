module io.quarkus.qute.debug {
    requires io.quarkus.qute;

    requires org.eclipse.lsp4j.debug;
    requires org.eclipse.lsp4j.jsonrpc;
    requires org.eclipse.lsp4j.jsonrpc.debug;

    exports io.quarkus.qute.debug;
    exports io.quarkus.qute.debug.adapter;
    exports io.quarkus.qute.debug.agent;
    exports io.quarkus.qute.debug.agent.breakpoints;
    exports io.quarkus.qute.debug.agent.completions;
    exports io.quarkus.qute.debug.agent.evaluations;
    exports io.quarkus.qute.debug.agent.frames;
    exports io.quarkus.qute.debug.agent.resolvers;
    exports io.quarkus.qute.debug.agent.scopes;
    exports io.quarkus.qute.debug.agent.source;
    exports io.quarkus.qute.debug.agent.variables;
    exports io.quarkus.qute.debug.client;
}
