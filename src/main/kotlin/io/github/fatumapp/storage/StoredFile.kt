package io.github.fatumapp.storage

data class StoredFile(
    val id: String?,
    val key: String,
    val bucket: String?,
    val originalFilename: String,
    val contentType: String,
    val size: Long
)