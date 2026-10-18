package io.github.fatumapp.storage

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuración deliberadamente genérica: no conoce rutas de User, Catalog ni Lambda.
 * Las var públicas generan getters/setters JavaBeans para el binder de Spring Boot.
 */
@ConfigurationProperties(prefix = "fatum.storage")
class FileStorageProperties {
    var enabled: Boolean = true
    var baseUrl: String = "http://localhost:8081"
    var internalSecret: String = ""
    var connectTimeout: Duration = Duration.ofSeconds(3)
    var readTimeout: Duration = Duration.ofSeconds(30)

    var uploadPath: String = "/files"
    var deletePath: String = "/files"
    var presignPath: String = "/files/url"
}
