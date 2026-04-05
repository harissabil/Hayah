package id.harissabil.hayah.service

/**
 * Predefined keywords that trigger Quranic reminders when detected in
 * on-screen text or notifications.
 *
 * Keywords are lowercase and checked via case-insensitive word-boundary matching.
 */
object TriggerKeywords {

    /** Islamic and life-context keywords that warrant a Quranic reminder. */
    val ISLAMIC_KEYWORDS: Set<String> = setOf(
        // Life & Death
        "death", "dying", "funeral", "grave", "afterlife",

        // Virtues
        "patience", "grateful", "gratitude", "kindness", "charity",
        "forgiveness", "mercy", "humility", "honesty", "justice",

        // Struggles
        "anger", "jealousy", "pride", "greed", "arrogance",
        "anxiety", "depression", "fear", "stress", "loneliness",

        // Worship
        "prayer", "fasting", "hajj", "zakat", "worship",

        // Wealth & Provision
        "money", "wealth", "rizq", "provision", "debt",

        // Family & Relations
        "family", "parents", "children", "marriage", "orphan",

        // Spiritual
        "sin", "sins", "repentance", "tawbah", "heaven", "hell",
        "judgment", "hereafter", "trust", "tawakkul", "sabr",
        "shukr", "dua", "quran", "iman", "taqwa",

        // Knowledge & Guidance
        "knowledge", "wisdom", "guidance", "truth", "falsehood",

        // Nature & Signs
        "nature", "creation", "rain", "earth", "sky",
    )

    /** Activity-to-keyword mapping for Activity Recognition transitions. */
    val ACTIVITY_KEYWORDS: Map<String, String> = mapOf(
        "IN_VEHICLE" to "travel",
        "ON_BICYCLE" to "exercise",
        "RUNNING" to "running",
        "WALKING" to "walking",
    )
}
