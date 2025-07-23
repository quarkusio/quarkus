package io.quarkus.kotlin.arc

import io.quarkus.arc.Arc
import io.quarkus.arc.InjectableContext
import io.quarkus.arc.ManagedContext
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

/**
 * This function extends the CoroutineContext to include the Quarkus Request Context if it is
 * active.
 *
 * If the caller finalizes the request context before the coroutine resumes, it results in undefined
 * behavior.
 *
 * Will not start a request context if there is none active at the time of invocation.
 */
fun CoroutineContext.withCdiContext(): CoroutineContext {
    val requestContext: ManagedContext? = Arc.container()?.requestContext()
    return if (requestContext == null) {
        this
    } else {
        this + RequestContextCoroutineContext(requestContext = requestContext)
    }
}

/**
 * A CoroutineContext.Element to propagate the Quarkus Request Context.
 *
 * This element captures the active request context when a coroutine is launched and ensures it is
 * activated whenever the coroutine resumes on a thread.
 *
 * @param requestContext The Quarkus ManagedContext for the request scope.
 */
class RequestContextCoroutineContext(private val requestContext: ManagedContext) :
    ThreadContextElement<RequestContextCoroutineContext.ContextSnapshot> {

    private val state: InjectableContext.ContextState? = requestContext.stateIfActive
    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader

    fun InjectableContext.ContextState?.isNullOrInvalid(): Boolean {
        return this == null || !this.isValid
    }

    /** A companion object to act as the Key for this context element. */
    companion object Key : CoroutineContext.Key<RequestContextCoroutineContext>

    /** The key that identifies this element in a CoroutineContext. */
    override val key: CoroutineContext.Key<*>
        get() = Key

    /**
     * This function is invoked when the coroutine resumes execution on a thread. It activates the
     * captured request context.
     *
     * @param context The coroutine context.
     * @return The state of the request context *before* this element activated its captured state.
     *   This is used by `restoreThreadContext` to correctly reset the context later.
     */
    override fun updateThreadContext(context: CoroutineContext): ContextSnapshot {
        // Capture the state of the current thread's context before we change it.
        val oldState = requestContext.stateIfActive

        val oldClassLoader = Thread.currentThread().contextClassLoader

        Thread.currentThread().contextClassLoader = classLoader

        // If the coroutine was launched from a thread without an active request context,
        // we should deactivate any context that might be active on the current thread.
        if (state.isNullOrInvalid()) {
            requestContext.deactivate()
        } else {
            // Activate the request context that we captured when the coroutine was created.
            requestContext.activate(state)
        }

        return ContextSnapshot(oldState, oldClassLoader)
    }

    /**
     * This function is invoked when the coroutine suspends or completes. It restores the request
     * context of the thread to its original state.
     *
     * @param context The coroutine context.
     * @param oldState The state that was returned by `updateThreadContext`.
     */
    override fun restoreThreadContext(context: CoroutineContext, oldState: ContextSnapshot) {

        Thread.currentThread().contextClassLoader = oldState.classLoader

        // We must restore the request context on the thread to whatever it was before
        // this coroutine resumed.
        val oldContext = oldState.contextState
        if (oldContext.isNullOrInvalid()) {
            requestContext.deactivate()
        } else {
            requestContext.activate(oldContext)
        }
    }

    data class ContextSnapshot(
        val contextState: InjectableContext.ContextState? = null,
        val classLoader: ClassLoader,
    )
}
