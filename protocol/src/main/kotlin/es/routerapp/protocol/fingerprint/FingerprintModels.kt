package es.routerapp.protocol.fingerprint

enum class FingerprintCategory {
    PHONE,
    TABLET,
    COMPUTER,
    TV,
    CONSOLE,
    IOT,
    PRINTER,
    ROUTER,
    OTHER,
}

data class FingerprintMatch(
    val exactModel: String,
    val vendor: String,
    val category: FingerprintCategory,
    val discoveryMethod: String,
)
