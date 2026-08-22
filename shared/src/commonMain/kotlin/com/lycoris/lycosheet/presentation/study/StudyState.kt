package com.lycoris.lycosheet.presentation.study

import com.lycoris.lycosheet.data.model.StudySession

data class StudyState(
    val session: StudySession? = null,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false
)
