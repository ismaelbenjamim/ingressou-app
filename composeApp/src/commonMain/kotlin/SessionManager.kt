interface SessionManager {
    fun saveUserSession(userSession: UserSession)
    fun getUserSession(): UserSession
}
