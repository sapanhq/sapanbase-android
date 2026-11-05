package com.sapanbase.sdk

/**
 * Callbacks for SapanBase chat widget events. All methods are invoked on the
 * main thread and have no-op default implementations, so implementors only
 * override the events they care about.
 */
interface SapanBaseListener {
    /** The chat panel was opened. */
    fun onOpen() {}

    /** The chat panel was closed. */
    fun onClose() {}

    /**
     * The assistant sent a reply.
     *
     * @param content the assistant's reply text (markdown)
     */
    fun onMessageReceived(content: String) {}

    /**
     * The widget reported an error.
     *
     * @param message a human-readable description of the error
     */
    fun onError(message: String) {}
}
