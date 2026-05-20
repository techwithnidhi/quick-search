package com.tk.quicksearch.app

/**
 * Default-launcher entry point for the home gesture.
 *
 * This is a thin subclass of [MainActivity] that exists solely so the manifest
 * can bind an opaque theme (`Theme.QuickSearch.Opaque`) to the `CATEGORY_HOME`
 * intent filter.
 *
 * [MainActivity] itself keeps its translucent theme for non-launcher launches
 * (share intents, web search, ASSIST, app-icon taps from another launcher,
 * etc.) where seeing through to the previous app is desirable.
 *
 * Splitting the HOME entry point into its own activity avoids the Pixel
 * home-gesture flash that occurs when a translucent window is treated as the
 * current home surface and the system reveals the previous launcher behind it
 * during the transition animation.
 */
class HomeActivity : MainActivity()
