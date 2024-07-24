import android.content.Context
import android.content.SharedPreferences

class AndroidSessionManager(private val context: Context) : SessionManager {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    override fun saveUserSession(userSession: UserSession) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("isAdmin", userSession.isAdmin)
        editor.putString("token", userSession.token)
        editor.apply()
    }

    override fun getUserSession(): UserSession {
        // sharedPreferences.getBoolean("isAdmin", false)
        return if (sharedPreferences.getString("token", null)?.isNotEmpty() == true) {
            UserSession(
                token = sharedPreferences.getString("token", null)!!,
                isAdmin = sharedPreferences.getBoolean("isAdmin", false),
                isAuthenticated = true
            )
        } else {
            UserSession()
        }
    }
}