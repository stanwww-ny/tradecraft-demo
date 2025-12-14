package io.tradecraft.common.log;

import io.tradecraft.common.meta.Component;
import io.tradecraft.common.meta.Flow;
import io.tradecraft.common.meta.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public final class LogUtils {
    // --- Logger cache ---
    private static final ConcurrentHashMap<Class<?>, Logger> CACHE = new ConcurrentHashMap<>();

    private LogUtils() {
    }

    private static Logger loggerFor(Class<?> c) {
        return CACHE.computeIfAbsent(c, LoggerFactory::getLogger);
    }

    // Resolve owner class for instance OR static contexts
    private static Class<?> ownerClass(Object owner) {
        if (owner == null) return LogUtils.class;
        return (owner instanceof Class<?> c) ? c : owner.getClass();
    }

    private static String shortName(Object o) {
        if (o == null) return "null";
        return (o instanceof Class<?> c) ? c.getSimpleName() : o.getClass().getSimpleName();
    }

    private static String prefix(Component m, MessageType k, Flow f) {
        return "[" + m + "][" + k + "][" + f + "]";
    }

    private static String safeFmt(String fmt) {
        return (fmt == null || fmt.isBlank()) ? "{}" : fmt;
    }

    // --- level routing helpers (keeps call sites tidy) ---
    private static void route(Logger log, MessageType k, String fmt, Object... args) {
        switch (k) {
            case CMD -> {
                if (log.isInfoEnabled()) log.info(fmt, args);
            }
            case EV, ER -> {
                if (log.isInfoEnabled()) log.info(fmt, args);
            }
            default -> {
                if (log.isInfoEnabled()) log.info(fmt, args);
            }
        }
    }

    private static void routeObj(Logger log, MessageType k, String head, Object msg) {
        final String type = shortName(msg);
        switch (k) {
            case CMD -> {
                if (log.isInfoEnabled()) log.info("{} {} {}", head, type, msg);
            }
            case EV, ER -> {
                if (log.isInfoEnabled()) log.info("{} {} {}", head, type, msg);
            }
            default -> {
                if (log.isInfoEnabled()) log.info("{} {} {}", head, type, msg);
            }
        }
    }

    // When a Throwable is present and there are also args, be portable:
    //  - emit the formatted line with args
    //  - then emit stacktrace on a separate line (works on SLF4J 1.7 & 2.x)
    private static void errorWithThrowablePortable(Logger log, String headPlusFmt, Throwable t, Object... args) {
        if (args == null || args.length == 0) {
            log.error(headPlusFmt, t);
        } else {
            log.error(headPlusFmt, args);
            log.error(headPlusFmt + " (stacktrace below)", t);
        }
    }

    /**
     * Instance/static owner; logs object via toString(), level chosen by kind.
     */
    public static void log(Component m, MessageType k, Flow f, Object owner, Object msg) {
        final Class<?> cls = ownerClass(owner);
        final Logger log = loggerFor(cls);
        final String head = prefix(m, k, f) + " " + shortName(owner) + " -";
        routeObj(log, k, head, msg);
    }

    /**
     * Instance/static owner; object message + Throwable emitted at error level (portable).
     */
    public static void log(Component m, MessageType k, Flow f, Object owner, Object msg, Throwable t) {
        final Class<?> cls = ownerClass(owner);
        final Logger log = loggerFor(cls);
        final String head = prefix(m, k, f) + " " + shortName(owner) + " -";
        // First the normal line at the k-mapped level
        routeObj(log, k, head, msg);
        // Then ensure stacktrace shows
        log.error(head + " {}", shortName(msg) + ": " + msg, t);
    }

    /**
     * Instance/static owner; SLF4J fmt. If fmt is null/blank, falls back to "{}" and renders the first arg (so
     * `log(..., "", er)` works).
     */
    public static void log(Component m, MessageType k, Flow f, Object owner, String fmt, Object... args) {
        // Handle blank/null fmt by routing to object overload (first arg) or empty string
        if (fmt == null || fmt.isBlank()) {
            if (args != null && args.length > 0) {
                log(m, k, f, owner, args[0]);
            } else {
                log(m, k, f, owner, "");
            }
            return;
        }

        final Class<?> cls = ownerClass(owner);
        final Logger log = loggerFor(cls);
        final String head = prefix(m, k, f) + " " + shortName(owner) + " - ";
        route(log, k, head + fmt, args);
    }

    // ========= Object message APIs =========

    /**
     * Instance/static owner; SLF4J fmt + Throwable (portable across SLF4J versions).
     */
    public static void logEx(Component m, MessageType k, Flow f, Object owner, Throwable t, String fmt, Object... args) {
        final Class<?> cls = ownerClass(owner);
        final Logger log = loggerFor(cls);
        final String headFmt = prefix(m, k, f) + " " + shortName(owner) + " - " + safeFmt(fmt);
        errorWithThrowablePortable(log, headFmt, t, args);
    }

    /**
     * Instance/static owner; Throwable only (no explicit message).
     */
    public static void logEx(Component m, MessageType k, Flow f, Object owner, Throwable t) {
        final Class<?> cls = ownerClass(owner);
        final Logger log = loggerFor(cls);
        final String headFmt = prefix(m, k, f) + " " + shortName(owner) + " - {}";
        errorWithThrowablePortable(log, headFmt, t, "");
    }

    // ========= SLF4J-style fmt/args APIs =========

    public static void log(Component m, MessageType k, Flow f, Class<?> cls, String fmt, Object... args) {
        if (fmt == null || fmt.isBlank()) {
            if (args != null && args.length > 0) {
                log(m, k, f, cls, args[0]);
            } else {
                log(m, k, f, cls, "");
            }
            return;
        }

        final Logger log = loggerFor(cls);
        final String head = prefix(m, k, f) + " " + cls.getSimpleName() + " - ";
        route(log, k, head + fmt, args);
    }

    public static void log(Component m, MessageType k, Flow f, Class<?> cls, Object msg) {
        final Logger log = loggerFor(cls);
        final String head = prefix(m, k, f) + " " + cls.getSimpleName() + " -";
        routeObj(log, k, head, msg);
    }

    public static void logEx(Component m, MessageType k, Flow f, Class<?> cls, Throwable t, String fmt, Object... args) {
        final Logger log = loggerFor(cls);
        final String headFmt = prefix(m, k, f) + " " + cls.getSimpleName() + " - " + safeFmt(fmt);
        errorWithThrowablePortable(log, headFmt, t, args);
    }

    // ========= Class<?> overloads for static call sites =========

    public static void logEx(Component m, MessageType k, Flow f, Class<?> cls, Throwable t) {
        final Logger log = loggerFor(cls);
        final String headFmt = prefix(m, k, f) + " " + cls.getSimpleName() + " - {}";
        errorWithThrowablePortable(log, headFmt, t, "");
    }


}
