package com.vocab.ielts.offline

/** Optional sample data for testing. These are added only when the user taps the sample-data button. */
object WordSamples {
    val words = listOf(
        WordEntity(term = "allocate", ipa = "/ˈæləkeɪt/", partOfSpeech = "verb", meaning = "phân bổ", example = "The city should allocate more funds to public transport.", topic = "Economy"),
        WordEntity(term = "sustainable", ipa = "/səˈsteɪnəbəl/", partOfSpeech = "adjective", meaning = "bền vững", example = "Cities need sustainable transport systems.", topic = "Environment"),
        WordEntity(term = "mitigate", ipa = "/ˈmɪtɪɡeɪt/", partOfSpeech = "verb", meaning = "giảm nhẹ", example = "Governments must mitigate the effects of climate change.", topic = "Environment")
    )
}
