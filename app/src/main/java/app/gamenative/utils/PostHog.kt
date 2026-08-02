package app.gamenative.utils

/**
 * No-op stub replacing the PostHog SDK (analytics removed from the build).
 * Call sites remain so event semantics are preserved if analytics is ever
 * reintroduced; nothing is collected or sent.
 */
object PostHog {
    fun capture(event: String, properties: Map<String, Any?>? = null) = Unit
    fun register(key: String, value: Any?) = Unit
}
