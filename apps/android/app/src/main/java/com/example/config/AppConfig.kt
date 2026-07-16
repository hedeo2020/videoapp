package com.example.config

import com.example.BuildConfig

object AppConfig {
    val API_BASE_URL: String = BuildConfig.SECURESTREAM_API_URL
    val WEB_BASE_URL: String = API_BASE_URL
        .removeSuffix("/")
        .removeSuffix("/api/v1")
}
