package io.github.fatumapp.storage

import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.multipart.MultipartFile

/** Las rutas identifican propósitos en FileService, no paths HTTP ni buckets. */
interface FileStorageClient {
    fun upload(file: MultipartFile, route: String): StoredFile
    fun upload(content: ByteArray, filename: String, contentType: String?, route: String): StoredFile
    fun delete(route: String, key: String?)
    fun presignedUrl(route: String, key: String?): String?
}

