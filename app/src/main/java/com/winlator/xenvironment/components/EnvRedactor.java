package com.winlator.xenvironment.components;

import com.winlator.core.envvars.EnvVars;

/**
 * Masks credentials before environment dumps hit logcat. Unredacted
 * environment is never written anywhere.
 */
final class EnvRedactor {
    private static final String[] SENSITIVE_KEY_PARTS = {
        "TOKEN", "PASSWORD", "SECRET", "AUTH", "COOKIE", "SESSION", "KEY",
        "STEAMID", "STEAMUSER", "REFRESH",
    };

    private EnvRedactor() {}

    static boolean isSensitiveKey(String key) {
        String upper = key.toUpperCase();
        for (String part : SENSITIVE_KEY_PARTS) {
            if (upper.contains(part)) return true;
        }
        return false;
    }

    static String redact(EnvVars envVars) {
        StringBuilder sb = new StringBuilder();
        for (String name : envVars) {
            String value = isSensitiveKey(name) ? "<redacted>" : envVars.get(name);
            if (sb.length() > 0) sb.append(' ');
            sb.append(name).append('=').append(value);
        }
        return sb.toString();
    }

    /** Single-quote shell escaping: safe for spaces, quotes, cyrillic, &, ;, $. */
    static String shellQuote(String arg) {
        if (arg.matches("[A-Za-z0-9_\\-./:=,+%@]+")) return arg;
        return "'" + arg.replace("'", "'\\''") + "'";
    }
}
