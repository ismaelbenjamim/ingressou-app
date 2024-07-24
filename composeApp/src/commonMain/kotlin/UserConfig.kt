import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserType {
    @SerialName("COMUM")
    COMUM,

    @SerialName("ADMIN")
    ADMIN
}

@Serializable
data class UserSession(
    var isAdmin: Boolean = false,
    var isAuthenticated: Boolean = false,
    var token: String = "",
)
