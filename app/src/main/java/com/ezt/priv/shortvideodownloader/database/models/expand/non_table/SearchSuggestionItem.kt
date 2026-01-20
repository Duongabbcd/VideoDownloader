package com.ezt.priv.shortvideodownloader.database.models.expand.non_table

data class SearchSuggestionItem(
    var text: String,
    val type: SearchSuggestionType,
)

enum class SearchSuggestionType{
    SUGGESTION, HISTORY, CLIPBOARD
}