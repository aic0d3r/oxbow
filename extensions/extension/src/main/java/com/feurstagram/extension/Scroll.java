package com.feurstagram.extension;

/**
 * Endless scrolling: the reel viewer's pager is created with swiping disabled, so a reel
 * opened from a DM, a profile or a search result stays a single reel.
 */
public final class Scroll {

    private Scroll() {}

    /** Hook, right after the reel viewer stores its ViewPager2. */
    public static void onReelPager(Object pager) {
        if (pager == null || !Config.isEndlessScrollBlocked()) return;
        try {
            pager.getClass().getMethod("setUserInputEnabled", boolean.class).invoke(pager, false);
        } catch (Throwable ignored) {
        }
    }
}
