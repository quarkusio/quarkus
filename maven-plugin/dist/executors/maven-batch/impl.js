"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.batchMavenExecutor = exports.default = void 0;
// Re-export the executor for Nx
var executor_1 = require("./executor");
Object.defineProperty(exports, "default", { enumerable: true, get: function () { return __importDefault(executor_1).default; } });
Object.defineProperty(exports, "batchMavenExecutor", { enumerable: true, get: function () { return executor_1.batchMavenExecutor; } });
