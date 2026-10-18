package fatum.io.github.fatumapp.storage

import io.github.fatumapp.storage.FileStorageProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.multipart.MultipartFile

class HttpFileStorageClient(
    private val restClient: RestClient,
    private val properties: FileStorageProperties
) : FileStorageClient {
    private val mapType = object : ParameterizedTypeReference<Map<String, Any?>>() {}

    override fun upload(file: MultipartFile, route: String): StoredFile {
        requireRoute(route)
        if (file.isEmpty) throw StorageException.rejected("El archivo está vacío")
        val body = MultipartBodyBuilder().apply {
            part("file", file.resource)
                .filename(file.originalFilename?.takeIf { it.isNotBlank() } ?: "upload")
                .contentType(parseContentType(file.contentType))
        }.build()
        return execute {
            responseMap(restClient.post()
                .uri { it.path(properties.uploadPath).queryParam("route", route).build() }
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve()).toStoredFile()
        }
    }

    override fun upload(content: ByteArray, filename: String, contentType: String?, route: String): StoredFile {
        requireRoute(route)
        if (content.isEmpty()) throw StorageException.rejected("El contenido está vacío")
        val body = MultipartBodyBuilder().apply {
            part("file", ByteArrayResource(content))
                .filename(filename.takeIf { it.isNotBlank() } ?: "upload")
                .contentType(parseContentType(contentType))
        }.build()
        return execute {
            responseMap(restClient.post()
                .uri { it.path(properties.uploadPath).queryParam("route", route).build() }
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve()).toStoredFile()
        }
    }

    override fun delete(route: String, key: String?) {
        requireRoute(route)
        if (key.isNullOrBlank()) return
        execute {
            checked(restClient.delete()
                .uri { it.path(properties.deletePath).queryParam("route", route).queryParam("key", key).build() }
                .retrieve()).toBodilessEntity()
        }
    }

    override fun presignedUrl(route: String, key: String?): String? {
        requireRoute(route)
        if (key.isNullOrBlank()) return null
        return execute {
            responseMap(restClient.get()
                .uri { it.path(properties.presignPath).queryParam("route", route).queryParam("key", key).build() }
                .retrieve()).requiredString("url")
        }
    }

    private fun responseMap(response: RestClient.ResponseSpec): Map<String, Any?> =
        checked(response).body(mapType) ?: throw StorageException.unexpected("Storage devolvió una respuesta vacía")

    private fun checked(response: RestClient.ResponseSpec): RestClient.ResponseSpec = response
        .onStatus({ it.is4xxClientError }) { _, result ->
            throw StorageException.rejected("Storage rechazó la solicitud (HTTP ${result.statusCode.value()})")
        }
        .onStatus({ it.is5xxServerError }) { _, result ->
            throw StorageException.unavailable("Storage no está disponible (HTTP ${result.statusCode.value()})")
        }

    private fun <T> execute(block: () -> T): T = try {
        block()
    } catch (exception: StorageException) {
        throw exception
    } catch (exception: RestClientException) {
        throw StorageException.unavailable("Falló la comunicación HTTP con storage", exception)
    }

    private fun requireRoute(route: String) {
        if (route.isBlank()) throw StorageException.rejected("La ruta de almacenamiento es obligatoria")
    }

    private fun parseContentType(value: String?): MediaType =
        if (value.isNullOrBlank()) MediaType.APPLICATION_OCTET_STREAM else try {
            MediaType.parseMediaType(value)
        } catch (exception: IllegalArgumentException) {
            throw StorageException.rejected("El tipo MIME no es válido")
        }

    private fun Map<String, Any?>.toStoredFile(): StoredFile {
        val size = this["size"] as? Number ?: throw StorageException.unexpected("Storage no devolvió un tamaño numérico")
        val bytes = size.toLong()
        if (bytes < 0 || size.toDouble() != bytes.toDouble()) throw StorageException.unexpected("Tamaño de respuesta inválido")
        return StoredFile(
            id = this["id"] as? String,
            key = requiredString("key"),
            bucket = this["bucket"] as? String,
            originalFilename = requiredString("originalFilename"),
            contentType = requiredString("contentType"),
            size = bytes
        )
    }

    private fun Map<String, Any?>.requiredString(name: String): String =
        (this[name] as? String)?.takeIf { it.isNotBlank() }
            ?: throw StorageException.unexpected("Storage no devolvió el campo requerido: $name")
}
