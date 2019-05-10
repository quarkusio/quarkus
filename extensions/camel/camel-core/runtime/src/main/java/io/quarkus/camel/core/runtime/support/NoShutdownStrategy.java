package io.quarkus.camel.core.runtime.support;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Service;
import org.apache.camel.spi.RouteStartupOrder;
import org.apache.camel.spi.ShutdownStrategy;

public class NoShutdownStrategy implements ShutdownStrategy {

    @Override
    public void shutdownForced(CamelContext context, List<RouteStartupOrder> routes) throws Exception {

    }

    @Override
    public void shutdown(CamelContext context, List<RouteStartupOrder> routes) throws Exception {

    }

    @Override
    public void suspend(CamelContext context, List<RouteStartupOrder> routes) throws Exception {

    }

    @Override
    public void shutdown(CamelContext context, List<RouteStartupOrder> routes, long timeout, TimeUnit timeUnit)
            throws Exception {

    }

    @Override
    public boolean shutdown(CamelContext context, RouteStartupOrder route, long timeout, TimeUnit timeUnit,
            boolean abortAfterTimeout) throws Exception {
        return false;
    }

    @Override
    public void suspend(CamelContext context, List<RouteStartupOrder> routes, long timeout, TimeUnit timeUnit)
            throws Exception {

    }

    @Override
    public void setTimeout(long timeout) {

    }

    @Override
    public long getTimeout() {
        return 0;
    }

    @Override
    public void setTimeUnit(TimeUnit timeUnit) {

    }

    @Override
    public TimeUnit getTimeUnit() {
        return null;
    }

    @Override
    public void setSuppressLoggingOnTimeout(boolean suppressLoggingOnTimeout) {

    }

    @Override
    public boolean isSuppressLoggingOnTimeout() {
        return false;
    }

    @Override
    public void setShutdownNowOnTimeout(boolean shutdownNowOnTimeout) {

    }

    @Override
    public boolean isShutdownNowOnTimeout() {
        return false;
    }

    @Override
    public void setShutdownRoutesInReverseOrder(boolean shutdownRoutesInReverseOrder) {

    }

    @Override
    public boolean isShutdownRoutesInReverseOrder() {
        return false;
    }

    @Override
    public void setLogInflightExchangesOnTimeout(boolean logInflightExchangesOnTimeout) {

    }

    @Override
    public boolean isLogInflightExchangesOnTimeout() {
        return false;
    }

    @Override
    public boolean forceShutdown(Service service) {
        return false;
    }

    @Override
    public boolean hasTimeoutOccurred() {
        return false;
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }
}
