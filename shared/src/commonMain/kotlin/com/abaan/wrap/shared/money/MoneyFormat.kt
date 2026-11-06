package com.abaan.wrap.shared.money

/** Converts between minor units (fils) and text. All supported currencies have two decimals. */
object MoneyFormat {
    private const val MINOR_PER_MAJOR = 100L
    private val inputPattern = Regex("""^\d+(\.\d{0,2})?$|^\.\d{1,2}$""")

    /** "AED 1,250.50" */
    fun format(minor: Long, currency: String): String {
        val sign = if (minor < 0) "-" else ""
        return "$sign$currency ${formatAmount(kotlin.math.abs(minor))}"
    }

    /** "1,250.50" without currency; negative values get a leading minus. */
    fun formatAmount(minor: Long): String {
        val sign = if (minor < 0) "-" else ""
        val abs = kotlin.math.abs(minor)
        val major = abs / MINOR_PER_MAJOR
        val fraction = (abs % MINOR_PER_MAJOR).toString().padStart(2, '0')
        return "$sign${groupThousands(major)}.$fraction"
    }

    /** "1250.5" style text for editing a stored amount; drops ".00". */
    fun toInput(minor: Long): String {
        val major = minor / MINOR_PER_MAJOR
        val fraction = minor % MINOR_PER_MAJOR
        return if (fraction == 0L) major.toString() else "$major.${fraction.toString().padStart(2, '0')}"
    }

    /**
     * Parses user input such as "1,250.5", " 300 " or ".75" into minor units.
     * Returns null for anything that is not a non-negative amount with at most two decimals.
     */
    fun parse(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace(" ", "")
        if (cleaned.isEmpty() || !inputPattern.matches(cleaned)) return null
        val parts = cleaned.split('.')
        val major = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        val fraction = parts.getOrNull(1).orEmpty().padEnd(2, '0').ifEmpty { "00" }.toLong()
        if (major > Long.MAX_VALUE / MINOR_PER_MAJOR - 1) return null
        return major * MINOR_PER_MAJOR + fraction
    }

    private fun groupThousands(value: Long): String {
        val digits = value.toString()
        val firstGroup = digits.length % 3
        return buildString {
            digits.forEachIndexed { index, c ->
                if (index > 0 && (index - firstGroup) % 3 == 0) append(',')
                append(c)
            }
        }
    }
}
