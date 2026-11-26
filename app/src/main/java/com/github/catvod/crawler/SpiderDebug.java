package com.github.catvod.crawler;
/** Host logging ABI. Plugin text can contain cookies or URLs, so never forward it to app logs. */
public final class SpiderDebug {
    private static final java.util.concurrent.atomic.AtomicReference<String> LAST = new java.util.concurrent.atomic.AtomicReference<>("");
    public static void log(Throwable error) { if (error != null) { String name=error.getClass().getSimpleName(); StackTraceElement[] frames=error.getStackTrace(); LAST.set(name+(frames.length>0?" @ "+frames[0].getClassName()+"."+frames[0].getMethodName():"")); } }
    public static void log(String message) { }
    public static void log(String tag, String message, Object... args) { }
    public static String takeError() { return LAST.getAndSet(""); }
}
