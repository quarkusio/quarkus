package io.quarkus.scheduler.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.runtime.tui.widgets.ListView;
import io.quarkus.scheduler.runtime.dev.ui.SchedulerJsonRPCService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Shell page for the Scheduler extension showing scheduled jobs.
 */
public class SchedulerShellPage extends BaseExtensionPage {

    @Inject
    SchedulerJsonRPCService schedulerService;

    private ListView<ScheduledJob> jobList;
    private List<ScheduledJob> allJobs = new ArrayList<>();

    // No-arg constructor for CDI
    public SchedulerShellPage() {
        this.jobList = new ListView<>(job -> {
            String statusIcon = job.running ? "> " : job.paused ? "|| " : "o ";
            return statusIcon + job.identity + " (" + job.cron + ")";
        });
    }

    public SchedulerShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        allJobs.clear();
        redraw();

        try {
            JsonObject result = schedulerService.getData();
            loading = false;

            if (result != null) {
                JsonArray methodsArray = result.getJsonArray("methods");
                if (methodsArray != null) {
                    for (int i = 0; i < methodsArray.size(); i++) {
                        JsonObject methodObj = methodsArray.getJsonObject(i);
                        String methodDescription = methodObj.getString("methodDescription");
                        String methodName = methodObj.getString("methodName");

                        JsonArray schedulesArray = methodObj.getJsonArray("schedules");
                        if (schedulesArray != null) {
                            for (int j = 0; j < schedulesArray.size(); j++) {
                                JsonObject schedObj = schedulesArray.getJsonObject(j);
                                String identity = schedObj.getString("identity");
                                String cron = schedObj.getString("cron");
                                if (cron == null) {
                                    cron = schedObj.getString("every");
                                }
                                boolean running = schedObj.getBoolean("running", false);
                                boolean paused = !running;

                                if (identity != null) {
                                    allJobs.add(new ScheduledJob(identity, cron, methodDescription,
                                            methodName, running, paused));
                                }
                            }
                        }
                    }
                }
            }

            allJobs.sort((a, b) -> a.identity.compareToIgnoreCase(b.identity));
            jobList.setItems(allJobs);
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load scheduled jobs: " + ex.getMessage();
            redraw();
        }
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);
        int row = 3;
        if (loading) {
            renderLoading(buffer, row);
        } else if (jobList.isEmpty()) {
            buffer.setString(1, row, "No scheduled jobs found", Style.create().gray());
        } else {
            jobList.setVisibleRows(height - row - 8);
            jobList.setWidth(width - 4);
            jobList.render(buffer, row, 2);

            // Show selected job details
            ScheduledJob selected = jobList.getSelectedItem();
            if (selected != null) {
                KeyValuePanel detail = new KeyValuePanel();
                detail.addIfPresent("Cron", selected.cron);
                detail.addIfPresent("Method", selected.methodName);
                detail.addIfPresent("Description", selected.methodDescription);
                detail.render(buffer, height - 7, 2, width - 4);
            }
        }

        if (error != null) {
            renderError(buffer, height - 4);
        }

        renderFooter(buffer, "[Enter] Toggle pause  [T] Trigger now");
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        if (jobList.handleKey(key)) {
            redraw();
            return true;
        }

        switch (key) {
            case KeyCode.ENTER:
                togglePause();
                return true;
            case 't':
            case 'T':
                triggerJob();
                return true;
            default:
                return super.handleKey(key);
        }
    }

    private void togglePause() {
        ScheduledJob selected = jobList.getSelectedItem();
        if (selected == null)
            return;

        loading = true;
        ctx.setStatus(selected.paused ? "Resuming " + selected.identity + "..." : "Pausing " + selected.identity + "...");
        redraw();

        try {
            if (selected.paused) {
                schedulerService.resumeJob(selected.identity);
            } else {
                schedulerService.pauseJob(selected.identity);
            }
            loading = false;
            loadData();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to toggle job: " + ex.getMessage();
            redraw();
        }
    }

    private void triggerJob() {
        ScheduledJob selected = jobList.getSelectedItem();
        if (selected == null)
            return;

        loading = true;
        ctx.setStatus("Triggering " + selected.identity + "...");
        redraw();

        try {
            schedulerService.executeJob(selected.methodDescription);
            loading = false;
            ctx.setStatus("Job triggered: " + selected.identity);
            loadData();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to trigger job: " + ex.getMessage();
            redraw();
        }
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        jobList.setVisibleRows(height - 14);
        jobList.setWidth(width - 4);
    }

    @Override
    protected void renderPanelContent(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        int row = startRow;

        if (loading) {
            renderPanelLoading(buffer, row, startCol);
            return;
        }

        if (error != null) {
            renderPanelError(buffer, row, startCol, panelWidth);
            return;
        }

        row++;

        if (allJobs.isEmpty()) {
            return;
        }

        int maxJobs = Math.min(allJobs.size(), panelHeight - 3);
        for (int i = 0; i < maxJobs; i++) {
            ScheduledJob job = allJobs.get(i);
            String statusIcon = job.running ? "> " : job.paused ? "|| " : "o ";
            buffer.setString(startCol, row, truncate(statusIcon + job.identity, panelWidth - 2), Style.EMPTY);
            row++;
        }

        if (allJobs.size() > maxJobs) {
            buffer.setString(startCol, row, "+" + (allJobs.size() - maxJobs) + " more...", Style.create().gray());
        }
    }

    private static class ScheduledJob {
        final String identity;
        final String cron;
        final String methodDescription;
        final String methodName;
        final boolean running;
        final boolean paused;

        ScheduledJob(String identity, String cron, String methodDescription, String methodName,
                boolean running, boolean paused) {
            this.identity = identity != null ? identity : "unknown";
            this.cron = cron;
            this.methodDescription = methodDescription;
            this.methodName = methodName;
            this.running = running;
            this.paused = paused;
        }
    }
}
