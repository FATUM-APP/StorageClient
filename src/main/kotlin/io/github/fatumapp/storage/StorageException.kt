package fatum.io.github.fatumapp.storage

class StorageException (
    message: String,
    cause: Throwable?,
    val kind: Kind
) : RuntimeException(message, cause) {
    enum class Kind { REJECTED, UNAVAILABLE, UNEXPECTED }
    companion object {
        @JvmStatic
        fun rejected(message: String): StorageException = StorageException(message, null, Kind.REJECTED)
        @JvmStatic
        fun unavailable(message: String, cause: Throwable? = null): StorageException =
            StorageException(message, cause, Kind.UNAVAILABLE)
        @JvmStatic
        fun unexpected(message: String, cause: Throwable? = null): StorageException =
            StorageException(message, cause, Kind.UNEXPECTED)
    }
}