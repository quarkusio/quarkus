import { QwcServerLog } from "qwc-server-log";

/**
 * This component filter the log to only show LGTM related entries.
 */
export class QwcLgtmLog extends QwcServerLog {
  doLogEntry(entry) {
    if (entry.loggerName && entry.loggerName.includes("LgtmContainer")) {
      return true;
    }
    return false;
  }
}

customElements.define("qwc-lgtm-log", QwcLgtmLog);
