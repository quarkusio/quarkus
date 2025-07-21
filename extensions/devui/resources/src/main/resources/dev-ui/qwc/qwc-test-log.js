import { QwcServerLog} from 'qwc-server-log';

/**
 * This component filter the log to only show test related entries
 */
export class QwcTestLog extends QwcServerLog {

    doLogEntry(entry){
        if (entry.threadName && entry.threadName.includes("Test runner thread")) {
            return true;
        }
        return false;
    }
}

customElements.define('qwc-test-log', QwcTestLog);