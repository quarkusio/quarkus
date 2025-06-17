import { ExecutorContext, TaskGraph } from '@nx/devkit';
export interface MavenBatchExecutorOptions {
    goals: string[];
    projectRoot?: string;
    verbose?: boolean;
    mavenPluginPath?: string;
    timeout?: number;
    outputFile?: string;
    failOnError?: boolean;
}
export interface MavenGoalResult {
    goal: string;
    success: boolean;
    durationMs: number;
    exitCode: number;
    output: string[];
    errors: string[];
}
export interface MavenBatchResult {
    overallSuccess: boolean;
    totalDurationMs: number;
    errorMessage?: string;
    goalResults: MavenGoalResult[];
}
export interface ExecutorResult {
    success: boolean;
    terminalOutput: string;
    output?: MavenBatchResult;
    error?: string;
}
export default function runExecutor(options: MavenBatchExecutorOptions, context: ExecutorContext): Promise<ExecutorResult>;
export declare function batchMavenExecutor(taskGraph: TaskGraph, inputs: Record<string, MavenBatchExecutorOptions>): Promise<Record<string, {
    success: boolean;
    terminalOutput: string;
}>>;
