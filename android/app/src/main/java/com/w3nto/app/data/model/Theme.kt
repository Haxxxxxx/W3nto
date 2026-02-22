package com.w3nto.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Theme(
    val backgroundColor: String = "#0a0a0a",
    val cardBackground: String = "#1a1a2e",
    val primaryColor: String = "#6c5ce7",
    val secondaryColor: String = "#a29bfe",
    val textColor: String = "#ffffff",
    val borderRadius: Int = 16,
    val fontFamily: String = "Inter",
    val buttonStyle: String = "rounded",
    val buttonBackground: String = "#6c5ce7",
    val buttonText: String = "#ffffff"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "backgroundColor" to backgroundColor,
        "cardBackground" to cardBackground,
        "primaryColor" to primaryColor,
        "secondaryColor" to secondaryColor,
        "textColor" to textColor,
        "borderRadius" to borderRadius,
        "fontFamily" to fontFamily,
        "buttonStyle" to buttonStyle,
        "buttonBackground" to buttonBackground,
        "buttonText" to buttonText
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Theme = Theme(
            backgroundColor = map["backgroundColor"] as? String ?: "#0a0a0a",
            cardBackground = map["cardBackground"] as? String ?: "#1a1a2e",
            primaryColor = map["primaryColor"] as? String ?: "#6c5ce7",
            secondaryColor = map["secondaryColor"] as? String ?: "#a29bfe",
            textColor = map["textColor"] as? String ?: "#ffffff",
            borderRadius = (map["borderRadius"] as? Number)?.toInt()
                ?: (map["borderRadius"] as? String)?.filter { it.isDigit() }?.toIntOrNull()
                ?: 16,
            fontFamily = map["fontFamily"] as? String ?: "Inter",
            buttonStyle = map["buttonStyle"] as? String ?: "rounded",
            buttonBackground = map["buttonBackground"] as? String ?: "#6c5ce7",
            buttonText = map["buttonText"] as? String ?: "#ffffff"
        )

        val LIGHT = Theme(
            backgroundColor = "#f5f5f5",
            cardBackground = "#ffffff",
            primaryColor = "#6c5ce7",
            secondaryColor = "#a29bfe",
            textColor = "#1a1a2e",
            buttonBackground = "#6c5ce7",
            buttonText = "#ffffff"
        )

        val DARK = Theme()

        val PINK = Theme(
            backgroundColor = "#1a0a1a",
            cardBackground = "#2d1a2d",
            primaryColor = "#e84393",
            secondaryColor = "#fd79a8",
            textColor = "#ffffff",
            buttonBackground = "#e84393",
            buttonText = "#ffffff"
        )
    }
}
