import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CpfValidationResponse(
    @SerialName("primeiro_acesso")
    val firstAccess: Boolean
)

@Serializable
data class CpfValidationDTO(
    val validate: Boolean = false,
    val firstAccess: Boolean
)

@Serializable
data class LoginRequest(
    val cpf: String,
    val password: String
)

@Serializable
data class ValidateQRCodeRequest(
    @SerialName("ingresso")
    val ingressoId: String
)

@Serializable
data class RegisterRequest(
    val cpf: String,
    val password: String,
    val email: String
)

@Serializable
data class LoginResponse(
    val token: String = "",
    val tipo: UserType
)

@Serializable
data class UserCreateRequest(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("cpf")
    val cpf: String,
    @SerialName("email")
    val email: String? = null,
    @SerialName("tipo")
    val tipo: String,
    @SerialName("birthday")
    val dataNascimento: String? = null,
    @SerialName("is_primeiro_acesso")
    val isPrimeiroAcesso: Boolean
)

@Serializable
data class UserResponse(
    @SerialName("id")
    val id: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("cpf")
    val cpf: String,
    @SerialName("email")
    val email: String? = null,
    @SerialName("tipo")
    val tipo: String,
    @SerialName("birthday")
    val dataNascimento: String? = null,
    @SerialName("is_primeiro_acesso")
    val isPrimeiroAcesso: Boolean
)

fun UserResponse.toUsuario(): Usuario {
    return Usuario(
        id = this.id,
        cpf = this.cpf,
        nome = this.firstName,
        sobrenome = this.lastName,
        email = this.email,
        dataNascimento = this.dataNascimento,
        tipo = this.tipo,
        isPrimeiroAcesso = this.isPrimeiroAcesso,
    )
}

fun List<UserResponse>.toUsuarioList(): List<Usuario> {
    return this.map { it.toUsuario() }
}

@Serializable
data class IngressoCreateRequest(
    @SerialName("usuario")
    val usuario: UserResponse,
    val nome: String,
    @SerialName("data_nascimento")
    val dataNascimento: String,
    val situacao: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("utilizado_em")
    val utilizadoEm: String?
)
@Serializable
data class IngressoGenerateRequest(
    val usuario: String,
    val nome: String,
    @SerialName("data_nascimento")
    val dataNascimento: String,
    val situacao: String,
)

@Serializable
data class IngressoResponse(
    @SerialName("id")
    val id: String,
    @SerialName("usuario")
    val usuario: UserResponse,
    val nome: String,
    @SerialName("data_nascimento")
    val dataNascimento: String,
    val situacao: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("utilizado_em")
    val utilizadoEm: String?
)

@Serializable
data class SimpleIngresso(
    val nome: String,
    @SerialName("data_nascimento")
    val dataNascimento: String,
    val situacao: String,
)

@Serializable
data class PaymentIngressoRequest(
    @SerialName("ingressos")
    val ingressos: List<SimpleIngresso>,
)

fun IngressoResponse.toIngresso(): Ingresso {
    return Ingresso(
        id = this.id,
        usuario = this.usuario,
        nome = this.nome,
        dataNascimento = this.dataNascimento,
        situacao = this.situacao,
        createdAt = this.createdAt,
        utilizadoEm = this.utilizadoEm
    )
}

fun List<IngressoResponse>.toIngressoList(): List<Ingresso> {
    return this.map { it.toIngresso() }
}