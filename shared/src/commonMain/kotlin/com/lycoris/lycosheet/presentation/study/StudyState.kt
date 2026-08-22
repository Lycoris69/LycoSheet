package com.lycoris.lycosheet.presentation.study

import com.lycoris.lycosheet.data.model.CardGrade
import com.lycoris.lycosheet.data.model.StudySession

data class StudyState(
    val session: StudySession? = null,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    // Grades recorded this session; key = card index at time of grading
    val grades: List<CardGrade> = emptyList()
)
