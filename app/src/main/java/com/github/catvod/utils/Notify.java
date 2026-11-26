package com.github.catvod.utils;

/** Silent parent-first stub for third-party CatVod spiders inside :spider process.
 * Compose UI owns user-visible playback and source status notices. */
public final class Notify {
    private static final Notify INSTANCE = new Notify();
    private Notify() {}
    public static Notify get() { return INSTANCE; }
    public static void show(String text) {}
    public static void show(int resId) {}
    public static void dismiss() {}
}
