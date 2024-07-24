import kotlinx.browser.window

class WebSessionManager : SessionManager {
    override fun saveUserSession(userSession: UserSession) {
        window.localStorage.setItem("isAdmin", userSession.isAdmin.toString())
        window.localStorage.setItem("token", userSession.token)
    }

    override fun getUserSession(): UserSession {
        // window.localStorage.getItem("isAdmin")?.toBoolean() ?: false
        return if (window.localStorage.getItem("token")?.isNotEmpty() == true && window.localStorage.getItem("isAdmin")?.isNotEmpty() == true) {
            UserSession(
                isAdmin = window.localStorage.getItem("isAdmin").toBoolean(),
                token = window.localStorage.getItem("token").toString(),
                isAuthenticated = true
            )
        } else {
            UserSession()
        }
    }
}
