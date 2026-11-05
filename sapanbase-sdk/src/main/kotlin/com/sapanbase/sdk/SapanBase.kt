package com.sapanbase.sdk

import android.content.Context
import android.content.Intent
import java.lang.ref.WeakReference

/** Identity captured by [SapanBase.identify], reapplied to each chat page load. */
internal data class SapanBaseIdentity(
    val userId: String,
    val email: String?,
    val name: String?,
    val hmac: String?,
)

/**
 * Public entry point for the SapanBase Android SDK.
 *
 * The SDK renders no native chat UI: [open] launches a full-screen [SapanBaseChatActivity]
 * containing a WebView that loads the SapanBase web chat widget. All UI updates ship
 * server-side with no SDK release required.
 *
 * [init] must be called once (e.g. from [android.app.Application.onCreate]) before any other
 * method. [identify] and [clearUser] may be called at any time — before [open], before the
 * chat page finishes loading, or while it is showing — the identity is queued and (re)applied
 * to every chat page load.
 */
object SapanBase {

    internal const val DEFAULT_SCRIPT_URL = "https://app.sapanbase.com/widget.js"

    internal var companyId: String? = null
        private set
    internal var agentId: String? = null
        private set
    internal var scriptUrl: String = DEFAULT_SCRIPT_URL
        private set

    private var appContext: Context? = null
    private var listener: SapanBaseListener? = null
    private var pendingIdentity: SapanBaseIdentity? = null
    private var activityRef: WeakReference<SapanBaseChatActivity>? = null
    private var visible = false

    /**
     * Configure the SDK. Must be called before [identify], [open], or [clearUser].
     *
     * @param context any app context; only [Context.getApplicationContext] is retained
     * @param companyId your SapanBase company ID (Settings > Widget Embed)
     * @param agentId target a specific support agent; defaults to the company's default agent
     * @param scriptUrl override where widget.js loads from. Defaults to the production URL.
     *   Override only for local development against the Firebase emulators — the widget derives
     *   its API base URL from this script's origin, so it must be a real http(s) URL, never a
     *   file bundled into the app.
     */
    @JvmStatic
    @JvmOverloads
    fun init(context: Context, companyId: String, agentId: String? = null, scriptUrl: String? = null) {
        require(companyId.isNotBlank()) { "SapanBase.init: companyId is required" }
        this.appContext = context.applicationContext
        this.companyId = companyId
        this.agentId = agentId
        this.scriptUrl = scriptUrl ?: DEFAULT_SCRIPT_URL
    }

    /**
     * Identify the logged-in user. Safe to call before [open] or before the chat page has
     * finished loading — the identity is stored and applied once the page is ready, and
     * reapplied to every subsequent chat page load (the Activity may be recreated).
     *
     * @param userId unique identifier for the user
     * @param email user's email address
     * @param name user's display name
     * @param hmac HMAC-SHA256 signature generated on YOUR server, required for the user to
     *   show as verified on support requests — see docs/widget-integration.md
     */
    @JvmStatic
    @JvmOverloads
    fun identify(userId: String, email: String? = null, name: String? = null, hmac: String? = null) {
        check(companyId != null) { "SapanBase.identify: call init() first" }
        pendingIdentity = SapanBaseIdentity(userId, email, name, hmac)
        activityRef?.get()?.onIdentityChanged()
    }

    /** Clear the identified user (call on logout). Safe to call at any time. */
    @JvmStatic
    fun clearUser() {
        pendingIdentity = null
        activityRef?.get()?.onIdentityChanged()
    }

    /**
     * Open the chat: starts [SapanBaseChatActivity], which loads the widget and opens its
     * panel as soon as it is ready.
     */
    @JvmStatic
    fun open(context: Context) {
        checkNotNull(companyId) { "SapanBase.open: call init() first" }
        val intent = Intent(context, SapanBaseChatActivity::class.java)
        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Close the chat if it is currently showing. No-op otherwise. */
    @JvmStatic
    fun close() {
        activityRef?.get()?.requestClose()
    }

    /** Full teardown: closes the chat if showing and clears all state. [init] may be called again. */
    @JvmStatic
    fun destroy() {
        close()
        appContext = null
        companyId = null
        agentId = null
        scriptUrl = DEFAULT_SCRIPT_URL
        pendingIdentity = null
        listener = null
        activityRef = null
        visible = false
    }

    /** Whether the chat Activity is currently visible to the user. */
    @JvmStatic
    val isOpen: Boolean
        get() = visible

    /** Set (or clear, with `null`) the listener notified of chat events. */
    @JvmStatic
    fun setListener(listener: SapanBaseListener?) {
        this.listener = listener
    }

    internal fun pendingIdentity(): SapanBaseIdentity? = pendingIdentity

    internal fun attachActivity(activity: SapanBaseChatActivity) {
        activityRef = WeakReference(activity)
    }

    internal fun detachActivity(activity: SapanBaseChatActivity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    internal fun setVisible(isVisible: Boolean) {
        visible = isVisible
    }

    internal fun notifyOpen() {
        listener?.onOpen()
    }

    internal fun notifyClose() {
        listener?.onClose()
    }

    internal fun notifyMessage(content: String) {
        listener?.onMessageReceived(content)
    }

    internal fun notifyError(message: String) {
        listener?.onError(message)
    }
}
