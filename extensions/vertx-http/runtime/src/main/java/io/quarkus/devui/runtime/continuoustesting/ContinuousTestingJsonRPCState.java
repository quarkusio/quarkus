package io.quarkus.devui.runtime.continuoustesting;

public class ContinuousTestingJsonRPCState {

    private boolean inProgress;
    private Config config;
    private Result result;

    public boolean isInProgress() {
        return inProgress;
    }

    public ContinuousTestingJsonRPCState setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
        return this;
    }

    public Config getConfig() {
        return config;
    }

    public ContinuousTestingJsonRPCState setConfig(Config config) {
        this.config = config;
        return this;
    }

    public Result getResult() {
        return result;
    }

    public ContinuousTestingJsonRPCState setResult(Result result) {
        this.result = result;
        return this;
    }

    public static class Config {

        private boolean enabled;
        private boolean brokenOnly;

        public boolean isEnabled() {
            return enabled;
        }

        public Config setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public boolean isBrokenOnly() {
            return brokenOnly;
        }

        public Config setBrokenOnly(boolean brokenOnly) {
            this.brokenOnly = brokenOnly;
            return this;
        }
    }

    public static class Result {

        private Counts counts;
        private long totalTime;
        private String[] tags;
        private Item[] passed;
        private Item[] failed;
        private Item[] skipped;

        public Counts getCounts() {
            return counts;
        }

        public Result setCounts(Counts counts) {
            this.counts = counts;
            return this;
        }

        public long getTotalTime() {
            return totalTime;
        }

        public Result setTotalTime(long totalTime) {
            this.totalTime = totalTime;
            return this;
        }

        public String[] getTags() {
            return tags;
        }

        public Result setTags(String[] tags) {
            this.tags = tags;
            return this;
        }

        public Item[] getPassed() {
            return passed;
        }

        public Result setPassed(Item[] passed) {
            this.passed = passed;
            return this;
        }

        public Item[] getFailed() {
            return failed;
        }

        public Result setFailed(Item[] failed) {
            this.failed = failed;
            return this;
        }

        public Item[] getSkipped() {
            return skipped;
        }

        public Result setSkipped(Item[] skipped) {
            this.skipped = skipped;
            return this;
        }

        public static class Counts {

            private long passed;
            private long failed;
            private long skipped;

            public long getPassed() {
                return passed;
            }

            public Counts setPassed(long passed) {
                this.passed = passed;
                return this;
            }

            public long getFailed() {
                return failed;
            }

            public Counts setFailed(long failed) {
                this.failed = failed;
                return this;
            }

            public long getSkipped() {
                return skipped;
            }

            public Counts setSkipped(long skipped) {
                this.skipped = skipped;
                return this;
            }

            public long getTotal() {
                return passed + failed + skipped;
            }
        }

        public static class Item {

            private String[] tags;
            private String className;
            private String displayName;
            private long time;
            private Throwable[] problems;

            public String[] getTags() {
                return tags;
            }

            public Item setTags(String[] tags) {
                this.tags = tags;
                return this;
            }

            public String getClassName() {
                return className;
            }

            public Item setClassName(String className) {
                this.className = className;
                return this;
            }

            public String getDisplayName() {
                return displayName;
            }

            public Item setDisplayName(String displayName) {
                this.displayName = displayName;
                return this;
            }

            public long getTime() {
                return time;
            }

            public Item setTime(long time) {
                this.time = time;
                return this;
            }

            public Throwable[] getProblems() {
                return problems;
            }

            public Item setProblems(Throwable[] problems) {
                this.problems = problems;
                return this;
            }
        }

    }

}
