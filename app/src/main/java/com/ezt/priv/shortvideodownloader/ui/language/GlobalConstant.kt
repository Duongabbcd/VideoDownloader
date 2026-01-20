package com.ezt.priv.shortvideodownloader.ui.language

import com.ezt.priv.shortvideodownloader.ui.language.adapter.Language
import com.ezt.priv.shortvideodownloader.R


object GlobalConstant {
    fun getListLocation(): ArrayList<Language> {
        val listLanguage: ArrayList<Language> = ArrayList()
        listLanguage.add(Language(R.drawable.english, "English", "en"))
        listLanguage.add(Language(R.drawable.arabic, "العربية", "ar"))
        listLanguage.add(Language(R.drawable.bengali, "বাংলা", "bn"))
        listLanguage.add(Language(R.drawable.german, "Deutsch", "de"))
        listLanguage.add(Language(R.drawable.spanish, "Español", "es"))
        listLanguage.add(Language(R.drawable.french, "Français", "fr"))
        listLanguage.add(Language(R.drawable.hindi, "हिन्दी", "hi"))
        listLanguage.add(Language(R.drawable.indonesian, "Bahasa", "in"))
        listLanguage.add(Language(R.drawable.portuguese, "Português", "pt"))
        listLanguage.add(Language(R.drawable.italia, "Italiano", "it"))
        listLanguage.add(Language(R.drawable.russia, "Русский", "ru"))
        listLanguage.add(Language(R.drawable.korean, "한국어", "ko"))

        return listLanguage
    }
}