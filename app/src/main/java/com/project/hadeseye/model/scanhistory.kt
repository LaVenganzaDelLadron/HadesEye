package com.project.hadeseye.model

data class ScanHistory(
    val url: String,
    val status: String,
    val date: String,
    val fileName: String,
    val ip: String? = null,
    val domain: String
)
