package id.harissabil.hayah.service

/**
 * Predefined keywords that trigger Quranic reminders when detected in
 * on-screen text or notifications.
 *
 * Keywords are lowercase and checked via case-insensitive word-boundary matching.
 */
object TriggerKeywords {
    /** Islamic and life-context keywords that warrant a Quranic reminder. */
    val ISLAMIC_KEYWORDS: Set<String> =
        setOf(
            // Life & Death
            "death",
            "dying",
            "funeral",
            "grave",
            "afterlife",
            // Virtues
            "patience",
            "grateful",
            "gratitude",
            "kindness",
            "charity",
            "forgiveness",
            "mercy",
            "humility",
            "honesty",
            "justice",
            // Struggles
            "anger",
            "jealousy",
            "pride",
            "greed",
            "arrogance",
            "anxiety",
            "depression",
            "fear",
            "stress",
            "loneliness",
            // Worship
            "prayer",
            "fasting",
            "hajj",
            "zakat",
            "worship",
            // Wealth & Provision
            "money",
            "wealth",
            "rizq",
            "provision",
            "debt",
            // Family & Relations
            "family",
            "parents",
            "children",
            "marriage",
            "orphan",
            // Spiritual
            "sin",
            "sins",
            "repentance",
            "tawbah",
            "heaven",
            "hell",
            "judgment",
            "hereafter",
            "trust",
            "tawakkul",
            "sabr",
            "shukr",
            "dua",
            "quran",
            "iman",
            "taqwa",
            // Knowledge & Guidance
            "knowledge",
            "wisdom",
            "guidance",
            "truth",
            "falsehood",
            // Nature & Signs
            "nature",
            "creation",
            "rain",
            "earth",
            "sky",
        )

    /** Activity-to-keyword mapping for Activity Recognition transitions. */
    val ACTIVITY_KEYWORDS: Map<String, String> =
        mapOf(
            "IN_VEHICLE" to "travel",
            "ON_BICYCLE" to "exercise",
            "RUNNING" to "running",
            "WALKING" to "walking",
        )

    /**
     * Rich descriptions for semantic embedding mode.
     * Each theme maps a keyword to a natural-language description that produces
     * a higher-quality embedding than a single word.
     */
    val THEME_DEFINITIONS: List<ThemeDefinition> =
        listOf(
            // Life & Death
            ThemeDefinition("death", "death, dying, mortality, loss of life, bereavement"),
            ThemeDefinition("funeral", "funeral, burial, mourning, grief over someone who passed away"),
            ThemeDefinition("afterlife", "the afterlife, life after death, what comes after this world"),
            // Virtues
            ThemeDefinition("patience", "being patient during hardship, difficulty, and trials in life"),
            ThemeDefinition("gratitude", "feeling grateful, thankful, and blessed for what one has"),
            ThemeDefinition("kindness", "showing kindness, compassion, and generosity to others"),
            ThemeDefinition("charity", "giving charity, helping the poor, donating, and generosity"),
            ThemeDefinition("forgiveness", "forgiving others, letting go of grudges, and seeking forgiveness"),
            ThemeDefinition("mercy", "mercy, compassion, and being gentle with others"),
            ThemeDefinition("humility", "being humble, avoiding arrogance, modesty"),
            ThemeDefinition("honesty", "being honest, truthful, and trustworthy"),
            ThemeDefinition("justice", "justice, fairness, standing up for what is right"),
            // Struggles
            ThemeDefinition("anger", "feeling angry, frustrated, losing temper, rage"),
            ThemeDefinition("jealousy", "feeling jealous, envious, comparing oneself to others"),
            ThemeDefinition("pride", "excessive pride, arrogance, thinking oneself superior"),
            ThemeDefinition("greed", "greed, wanting more, never being satisfied with what one has"),
            ThemeDefinition("anxiety", "feeling anxious, worried, stressed, or overwhelmed"),
            ThemeDefinition("depression", "feeling depressed, sad, hopeless, or emotionally low"),
            ThemeDefinition("fear", "feeling afraid, fearful, scared about the future"),
            ThemeDefinition("stress", "feeling stressed, pressured, burned out, or overburdened"),
            ThemeDefinition("loneliness", "feeling lonely, isolated, alone, or disconnected from others"),
            // Worship
            ThemeDefinition("prayer", "praying, salah, worship, communicating with God"),
            ThemeDefinition("fasting", "fasting, Ramadan, self-discipline through abstaining from food"),
            ThemeDefinition("hajj", "pilgrimage, hajj, umrah, visiting the holy places"),
            ThemeDefinition("zakat", "zakat, obligatory charity, purifying wealth"),
            ThemeDefinition("worship", "worship, devotion, acts of worship and obedience to God"),
            // Wealth & Provision
            ThemeDefinition("money", "money, finances, financial concerns, earning a living"),
            ThemeDefinition("wealth", "wealth, being rich, abundance, material possessions"),
            ThemeDefinition("rizq", "rizq, provision from God, sustenance and livelihood"),
            ThemeDefinition("debt", "debt, financial burden, owing money, financial difficulty"),
            // Family & Relations
            ThemeDefinition("family", "family relationships, bonds between relatives, family love"),
            ThemeDefinition("parents", "parents, honoring mother and father, filial duty"),
            ThemeDefinition("children", "children, raising kids, parenting, caring for offspring"),
            ThemeDefinition("marriage", "marriage, spouse, marital relationship, love between partners"),
            ThemeDefinition("orphan", "orphans, caring for children without parents"),
            // Spiritual
            ThemeDefinition("sin", "committing sin, wrongdoing, transgression, guilt"),
            ThemeDefinition("repentance", "repentance, turning back to God, seeking forgiveness for sins"),
            ThemeDefinition("heaven", "heaven, paradise, the reward of the righteous"),
            ThemeDefinition("hell", "hell, punishment, consequences of wrongdoing"),
            ThemeDefinition("judgment", "the Day of Judgment, accountability, being judged for deeds"),
            ThemeDefinition("trust", "trusting in God, relying on God, tawakkul"),
            ThemeDefinition("dua", "making dua, supplication, asking God for help"),
            ThemeDefinition("quran", "reading Quran, the holy book, scripture, divine guidance"),
            ThemeDefinition("iman", "faith, belief in God, strengthening one's iman"),
            ThemeDefinition("taqwa", "God-consciousness, piety, being mindful of God"),
            // Knowledge & Guidance
            ThemeDefinition("knowledge", "seeking knowledge, learning, education, understanding"),
            ThemeDefinition("wisdom", "wisdom, deep understanding, insight, discernment"),
            ThemeDefinition("guidance", "guidance, being guided, finding the right path, direction in life"),
            ThemeDefinition("truth", "truth, honesty, distinguishing truth from falsehood"),
            // Nature & Signs
            ThemeDefinition("nature", "nature, the natural world, God's creation, beauty of the earth"),
            ThemeDefinition("creation", "creation, the origin of life, how everything was made"),
            ThemeDefinition("rain", "rain, water from the sky, sustenance for the earth"),
            ThemeDefinition("earth", "the earth, the land, the planet, the world we live on"),
            ThemeDefinition("sky", "the sky, the heavens above, stars, the cosmos"),
            // Activity-based
            ThemeDefinition("travel", "traveling, journeying, being on the road, commuting"),
            ThemeDefinition("exercise", "exercising, physical activity, working out, staying healthy"),
            ThemeDefinition("running", "running, jogging, moving quickly on foot"),
            ThemeDefinition("walking", "walking, strolling, moving on foot, taking a walk"),
        )
}

/**
 * A theme with a keyword identifier and a rich description for embedding.
 */
data class ThemeDefinition(
    val keyword: String,
    val description: String,
)
