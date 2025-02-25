import {css, html, LitElement} from 'lit';
import {JsonRpc} from 'jsonrpc';
import '@vaadin/grid';
import {columnBodyRenderer} from '@vaadin/grid/lit.js';
import '@vaadin/vertical-layout';

export class QwcFaultToleranceMethods extends LitElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
      vaadin-grid {
        height: 100%;
        padding-bottom: 10px;
      }

      .method {
        color: var(--lumo-primary-text-color);
      }
    `;

    static properties = {
        _guardedMethods: {state: true},
    };

    connectedCallback() {
        super.connectedCallback();
        this._refresh();
    }

    render() {
        if (this._guardedMethods) {
            return this._renderGuardedMethods();
        } else {
            return html`<span>Loading guarded methods...</span>`;
        }
    }

    _renderGuardedMethods() {
        return html`
                <vaadin-grid .items="${this._guardedMethods}" theme="no-border">
                    <vaadin-grid-column header="Bean Class" auto-width flex-grow="0"
                                        ${columnBodyRenderer(this._renderBeanClass, [])}
                                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column header="Method" auto-width flex-grow="0"
                                        ${columnBodyRenderer(this._renderMethodName, [])}
                                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column header="Fault Tolerance Strategies" auto-width flex-grow="0"
                                        ${columnBodyRenderer(this._renderStrategies, [])}
                                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>
            `;
    }

    _renderBeanClass(guardedMethod) {
        return html`
            <code>${guardedMethod.beanClass}</code>
        `;
    }

    _renderMethodName(guardedMethod) {
        return html`
            <code class="method">${guardedMethod.method}()</code>
        `;
    }

    _renderStrategies(guardedMethod) {
        return html`
            <vaadin-vertical-layout>
                ${guardedMethod.ApplyFaultTolerance ? this._renderApplyFaultTolerance(guardedMethod.ApplyFaultTolerance) : html``}
                ${guardedMethod.ApplyGuard ? this._renderApplyGuard(guardedMethod.ApplyGuard) : html``}
                ${guardedMethod.Asynchronous ? html`<span>@Asynchronous</span>` : html``}
                ${guardedMethod.AsynchronousNonBlocking ? html`<span>@AsynchronousNonBlocking</span>` : html``}
                ${guardedMethod.Blocking ? html`<span>@Blocking</span>` : html``}
                ${guardedMethod.NonBlocking ? html`<span>@NonBlocking</span>` : html``}
                ${guardedMethod.Bulkhead ? this._renderBulkhead(guardedMethod.Bulkhead) : html``}
                ${guardedMethod.CircuitBreaker ? this._renderCircuitBreaker(guardedMethod.CircuitBreaker) : html``}
                ${guardedMethod.CircuitBreakerName ? this._renderCircuitBreakerName(guardedMethod.CircuitBreakerName) : html``}
                ${guardedMethod.Fallback ? this._renderFallback(guardedMethod.Fallback) : html``}
                ${guardedMethod.RateLimit ? this._renderRateLimit(guardedMethod.RateLimit) : html``}
                ${guardedMethod.Retry ? this._renderRetry(guardedMethod.Retry) : html``}
                ${guardedMethod.ExponentialBackoff ? this._renderExponentialBackoff(guardedMethod.ExponentialBackoff) : html``}
                ${guardedMethod.FibonacciBackoff ? this._renderFibonacciBackoff(guardedMethod.FibonacciBackoff) : html``}
                ${guardedMethod.CustomBackoff ? this._renderCustomBackoff(guardedMethod.CustomBackoff) : html``}
                ${guardedMethod.RetryWhen ? this._renderRetryWhen(guardedMethod.RetryWhen) : html``}
                ${guardedMethod.BeforeRetry ? this._renderBeforeRetry(guardedMethod.BeforeRetry) : html``}
                ${guardedMethod.Timeout ? this._renderTimeout(guardedMethod.Timeout) : html``}
            </vaadin-vertical-layout>
        `;
    }

    _renderApplyFaultTolerance(applyFaultTolerance) {
        return html`
            <span>@ApplyFaultTolerance("${applyFaultTolerance.value}")</span>
        `;
    }

    _renderApplyGuard(applyGuard) {
        return html`
            <span>@ApplyGuard("${applyGuard.value}")</span>
        `;
    }

    _renderBulkhead(bulkhead) {
        return html`
            <span>@Bulkhead(value = ${bulkhead.value}, waitingTaskQueue = ${bulkhead.waitingTaskQueue})</span>
        `;
    }

    _renderCircuitBreaker(circuitBreaker) {
        return html`
            <span>@CircuitBreaker(delay = ${circuitBreaker.delay} ${circuitBreaker.delayUnit},
                requestVolumeThreshold = ${circuitBreaker.requestVolumeThreshold},
                failureRatio = ${circuitBreaker.failureRatio},
                successThreshold = ${circuitBreaker.successThreshold},
                failOn = ${this._renderArray(circuitBreaker.failOn)},
                skipOn = ${this._renderArray(circuitBreaker.skipOn)})</span>
        `;
    }

    _renderCircuitBreakerName(circuitBreakerName) {
        return html`
            <span>
                &rarrhk;
                @CircuitBreakerName("${circuitBreakerName.value}")
            </span>
        `;
    }

    _renderFallback(fallback) {
        return html`
            <span>@Fallback(value = ${fallback.value},
                fallbackMethod = "${fallback.fallbackMethod}",
                applyOn = ${this._renderArray(fallback.applyOn)},
                skipOn = ${this._renderArray(fallback.skipOn)})</span>
        `;
    }

    _renderRateLimit(rateLimit) {
        return html`
            <span>@RateLimit(value = ${rateLimit.value},
                window = ${rateLimit.window} ${rateLimit.windowUnit},
                minSpacing = ${rateLimit.minSpacing} ${rateLimit.minSpacingUnit},
                type = ${rateLimit.type})</span>
        `;
    }

    _renderRetry(retry) {
        return html`
            <span>@Retry(maxRetries = ${retry.maxRetries},
                delay = ${retry.delay} ${retry.delayUnit},
                maxDuration = ${retry.maxDuration} ${retry.maxDurationUnit},
                jitter = ${retry.jitter} ${retry.jitterUnit},
                retryOn = ${this._renderArray(retry.retryOn)},
                abortOn = ${this._renderArray(retry.abortOn)})</span>
        `;
    }

    _renderExponentialBackoff(exponentialBackoff) {
        return html`
            <span>
                &rarrhk;
                @ExponentialBackoff(factor = ${exponentialBackoff.factor},
                    maxDelay = ${exponentialBackoff.maxDelay} ${exponentialBackoff.maxDelayUnit})
            </span>
        `;
    }

    _renderFibonacciBackoff(fibonacciBackoff) {
        return html`
            <span>
                &rarrhk;
                @FibonacciBackoff(maxDelay = ${fibonacciBackoff.maxDelay} ${fibonacciBackoff.maxDelayUnit})
            </span>
        `;
    }

    _renderCustomBackoff(customBackoff) {
        return html`
            <span>
                &rarrhk;
                @CustomBackoff(${customBackoff.value})
            </span>
        `;
    }

    _renderRetryWhen(retryWhen) {
        return html`
            <span>
                &rarrhk;
                @RetryWhen(result = ${retryWhen.result}, exception = ${retryWhen.exception})
            </span>
        `;
    }

    _renderBeforeRetry(beforeRetry) {
        return html`
            <span>
                &rarrhk;
                @BeforeRetry(value = ${beforeRetry.value}, methodName = ${beforeRetry.methodName})
            </span>
        `;
    }

    _renderTimeout(timeout) {
        return html`
            <span>@Timeout(${timeout.value} ${timeout.valueUnit})</span>
        `;
    }

    _renderArray(array) {
        return array ? html`[${array.join(', ')}]` : html`[]`;
    }

    _refresh() {
        this.jsonRpc.getGuardedMethods().then(guardedMethods => {
            this._guardedMethods = guardedMethods.result;
        });
    }
}

customElements.define('qwc-fault-tolerance-methods', QwcFaultToleranceMethods);
