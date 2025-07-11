import { LitElement, html, css } from 'lit';
import '@vaadin/button';

/**
 * This component allows downloading of binary content
 */
export class QwcWorkspaceBinary extends LitElement { 
    
    static styles = css`
        :host {
            display: flex;
            justify-content: center;
            gap: 30px;
            flex-direction: column;
        }
        .text {
            display: flex;
            align-items: center;
            height: 70%;
            font-size: xxx-large;
            opacity: 0.2;
            flex-direction: column;
        }
    `;
    
    static properties = {
        base64Data: {type: String},
        filename: {type: String},
        mimeType: {type: String}
    };
    
    constructor() { 
        super();
        this.base64Data = '';
        this.filename = 'file.bin';
        this.mimeType = null;
    }
    
    render() {
        return html`
            <div class="text">Binary content.</div>
            <vaadin-button theme="secondary" @click=${() => this._downloadFile()}>Download</vaadin-button>
        `;
    }
    
    _downloadFile() {
        if (!this.base64Data) {
            console.warn('No Base64 data provided.');
            return;
        }

        const mime = this.mimeType || this._detectMimeType(this.filename);
        const byteCharacters = atob(this.base64Data);
        const byteArray = new Uint8Array(byteCharacters.length);
        for (let i = 0; i < byteCharacters.length; i++) {
          byteArray[i] = byteCharacters.charCodeAt(i);
        }

        const blob = new Blob([byteArray], { type: mime });
        const url = URL.createObjectURL(blob);

        const link = document.createElement('a');
        link.href = url;
        link.download = this.filename;
        link.click();

        URL.revokeObjectURL(url);
    }
    
    _detectMimeType(filename) {
        const ext = filename.split('.').pop()?.toLowerCase() || '';
        const map = {
            pdf: 'application/pdf',
            zip: 'application/zip',
            jar: 'application/java-archive',
            doc: 'application/msword',
            docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            xls: 'application/vnd.ms-excel',
            xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            ppt: 'application/vnd.ms-powerpoint',
            pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
            csv: 'text/csv',
            json: 'application/json',
            png: 'image/png',
            jpg: 'image/jpeg',
            jpeg: 'image/jpeg',
            gif: 'image/gif',
            txt: 'text/plain',
            html: 'text/html',
            xml: 'application/xml',
        };
        return map[ext] || 'application/octet-stream';
    }
}
customElements.define('qwc-workspace-binary', QwcWorkspaceBinary);