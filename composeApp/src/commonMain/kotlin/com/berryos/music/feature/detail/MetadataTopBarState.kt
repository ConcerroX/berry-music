package com.berryos.music.feature.detail

/** Controls rendered by the page-owned metadata top bar. */
data class MetadataTopBarState(
    val onSave: () -> Unit,
    val saveEnabled: Boolean,
    val tabLabels: List<String>,
    val selectedTabIndex: Int,
    val selectedTabPosition: Float,
    val onTabSelected: (Int) -> Unit,
)
