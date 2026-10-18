package io.github.fatumapp.storage

import fatum.io.github.fatumapp.storage.FileStorageClient
import fatum.io.github.fatumapp.storage.HttpFileStorageClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.http.HttpClient

@AutoConfiguration
@EnableConfigurationProperties(FileStorageProperties::class)
@ConditionalOnClass(RestClient::class)
@ConditionalOnProperty(
    prefix = "fatum.storage",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class FileStorageAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(FileStorageClient::class)
    fun fileStorageClient(properties: FileStorageProperties): FileStorageClient {
        val baseUri = validBaseUri(properties.baseUrl)
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout)
            .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(properties.readTimeout)
        }

        // Builder local e independiente: no sustituye ni reutiliza el RestClient del consumidor.
        val builder = RestClient.builder()
            .requestFactory(requestFactory)
            .baseUrl(baseUri.toString().trimEnd('/'))
        if (properties.internalSecret.isNotBlank()) {
            builder.defaultHeader("X-Storage-Key", properties.internalSecret)
        }
        return HttpFileStorageClient(builder.build(), properties)
    }

    private fun validBaseUri(value: String): URI {
        val uri = runCatching { URI.create(value.trim()) }
            .getOrElse { throw IllegalArgumentException("fatum.storage.base-url no es una URL válida", it) }
        require(uri.scheme == "http" || uri.scheme == "https") {
            "fatum.storage.base-url debe usar http o https"
        }
        require(!uri.host.isNullOrBlank()) {
            "fatum.storage.base-url debe incluir host"
        }
        return uri
    }
}
