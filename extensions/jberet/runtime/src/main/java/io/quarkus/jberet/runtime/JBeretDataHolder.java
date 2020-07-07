package io.quarkus.jberet.runtime;

import java.util.List;

import org.jberet.job.model.Job;

class JBeretDataHolder {
    private static volatile JBeretData data;

    static void registerJobs(final List<Job> jobs) {
        data = new JBeretData(jobs);
    }

    static List<Job> getJobs() {
        return data.jobs;
    }

    private static class JBeretData {
        private final List<Job> jobs;

        public JBeretData(final List<Job> jobs) {
            this.jobs = jobs;
        }
    }
}
