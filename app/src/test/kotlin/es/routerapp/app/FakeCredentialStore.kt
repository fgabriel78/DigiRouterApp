package es.routerapp.app

class FakeCredentialStore(initialPassword: String? = null) : CredentialStore {
    private var storedPassword: String? = initialPassword

    override fun getSavedPassword(): String? = storedPassword

    override fun savePassword(password: String) {
        storedPassword = password
    }

    override fun clear() {
        storedPassword = null
    }

    override fun hasSavedPassword(): Boolean = !storedPassword.isNullOrEmpty()
}
