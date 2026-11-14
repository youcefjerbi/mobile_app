package com.example.data.agent

/** Provider choice is separate from connection state. No credentials belong in this model. */
enum class AgentProvider(val label: String) {
    CODEX("Codex"), CLAUDE_CODE("Claude Code"), GEMINI("Gemini"), CUSTOM("Custom / DC")
}

data class AgentConfiguration(
    val provider: AgentProvider = AgentProvider.CODEX,
    val model: String = "",
    val customName: String = ""
) {
    val displayName: String get() = if (provider == AgentProvider.CUSTOM) customName.ifBlank { "Custom agent" } else provider.label
    companion object {
        fun fromStored(provider: String?, model: String?, name: String?) = AgentConfiguration(
            AgentProvider.entries.firstOrNull { it.name == provider } ?: AgentProvider.CODEX,
            model.orEmpty().take(100), name.orEmpty().take(60)
        )
    }
}
