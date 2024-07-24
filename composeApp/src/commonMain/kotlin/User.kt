import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch


data class Usuario(
    val id: String = "",
    val cpf: String,
    val nome: String,
    val sobrenome: String,
    val email: String? = null,
    val dataNascimento: String? = null,
    val tipo: String = "COMUM",
    val isPrimeiroAcesso: Boolean = true
)

suspend fun getUsers(callback: (List<UserResponse>) -> Unit) {
    try {
        val response: HttpResponse = client.get("${ApiConfig.BASE_URL}/user/") {
            contentType(ContentType.Application.Json)
        }
        if (response.status == HttpStatusCode.OK) {
            val result: List<UserResponse> = response.body()
            println(result)
            println(result.size)
            callback(result)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

suspend fun createUser(
    request: UserCreateRequest,
    token: String,
    callback: (UserResponse) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val response: HttpResponse = client.post("${ApiConfig.BASE_URL}/user/") {
            contentType(ContentType.Application.Json)
            setBody(request)
            headers {
                append("Authorization", "Token $token")
            }
        }
        if (response.status == HttpStatusCode.Created) {
            val result: UserResponse = response.body()
            callback(result)
        } else {
            onError("Ocorreu um problema ao tentar criar um usuário, tente novamente")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onError("Serviço indisponível, contate um administrador.")
    }
}

suspend fun updateUser(
    request: UserCreateRequest,
    userId: String,
    token: String,
    callback: (UserResponse) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val response: HttpResponse = client.put("${ApiConfig.BASE_URL}/user/${userId}/") {
            contentType(ContentType.Application.Json)
            setBody(request)
            headers {
                append("Authorization", "Token $token")
            }
        }
        if (response.status == HttpStatusCode.OK) {
            val result: UserResponse = response.body()
            callback(result)
        } else {
            onError("Ocorreu um problema ao tentar alterar o usuário, tente novamente")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onError("Serviço indisponível, contate um administrador.")
    }
}

suspend fun deleteUser(userId: String, token: String, callback: () -> Unit, onError: (String) -> Unit) {
    try {
        val response: HttpResponse = client.delete("${ApiConfig.BASE_URL}/user/${userId}/") {
            contentType(ContentType.Application.Json)
            headers {
                append("Authorization", "Token $token")
            }
        }
        if (response.status == HttpStatusCode.NoContent) {
            callback()
        } else {
            onError("Erro ao deletar usuário")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onError("Serviço indisponível, contate um administrador.")
    }
}

@Composable
fun UsuariosScreen(userSession: UserSession) {
    val usuarios = remember { mutableStateListOf<Usuario>() }
    val coroutineScope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        getUsers { result ->
            if (result.isNotEmpty()) {
                usuarios.clear()
                val usuariosConverted = result.toUsuarioList()
                usuarios.addAll(usuariosConverted)
            }
        }
    }

    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
    val (currentUsuario, setCurrentUsuario) = remember { mutableStateOf<Usuario?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    val itemsPerPage = 3

    val pagedUsuarios by derivedStateOf {
        usuarios.chunked(itemsPerPage)
    }

    fun updateCurrentPage() {
        val maxPage = if (pagedUsuarios.isNotEmpty()) pagedUsuarios.size - 1 else 0
        if (currentPage > maxPage) {
            currentPage = maxPage
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                text = "Usuários",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.then(getResponsiveMaxWidth(ResponsiveSize.MD)).padding(start = 10.dp)
            )
            Card(
                modifier = Modifier
                    .padding(10.dp)
                    .then(getResponsiveMaxWidth(ResponsiveSize.MD).fillMaxHeight(0.8f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxHeight()
                ) {
                    Button(onClick = { setCurrentUsuario(null); setShowDialog(true) }) {
                        Text("Adicionar Usuário")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier
                                .align(alignment = Alignment.Center)
                                .fillMaxWidth()
                        ) {
                            UsuarioTableHeader()
                            HorizontalDivider()
                            if (pagedUsuarios.isNotEmpty() && currentPage < pagedUsuarios.size) {
                                UsuarioTable(
                                    usuarios = pagedUsuarios[currentPage],
                                    onEdit = {
                                        setCurrentUsuario(it)
                                        setShowDialog(true)
                                    },
                                    onDelete = { user ->
                                        coroutineScope.launch {
                                            deleteUser(
                                                userId = user.id,
                                                token = userSession.token,
                                                callback = {
                                                    usuarios.remove(user)
                                                    updateCurrentPage()
                                                },
                                                onError = { message ->
                                                    errorMessage = message
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            10.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        if (currentPage > 0) {
                            IconButton(
                                onClick = { if (currentPage > 0) currentPage-- },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text("Página ${currentPage + 1} de ${if (pagedUsuarios.isNotEmpty()) pagedUsuarios.size else 1}")
                        if (currentPage < pagedUsuarios.size - 1) {
                            IconButton(
                                onClick = { if (currentPage < pagedUsuarios.size - 1) currentPage++ },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Próxima",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    if (showDialog) {
                        UsuarioDialog(
                            usuario = currentUsuario,
                            onSave = { user ->
                                if (currentUsuario == null) {
                                    coroutineScope.launch {
                                        createUser(
                                            request = UserCreateRequest(
                                                firstName = user.nome,
                                                lastName = user.sobrenome,
                                                cpf = user.cpf,
                                                email = user.email,
                                                tipo = "COMUM",
                                                dataNascimento = user.dataNascimento,
                                                isPrimeiroAcesso = true
                                            ),
                                            token = userSession.token,
                                            callback = { result ->
                                                usuarios.add(user.copy(id = result.id))
                                                errorMessage = null
                                                setShowDialog(false)
                                                updateCurrentPage()
                                            },
                                            onError = { message ->
                                                errorMessage = message
                                            }
                                        )

                                    }
                                } else {
                                    coroutineScope.launch {
                                        updateUser(
                                            request = UserCreateRequest(
                                                firstName = user.nome,
                                                lastName = user.sobrenome,
                                                cpf = user.cpf,
                                                email = user.email,
                                                tipo = user.tipo,
                                                dataNascimento = user.dataNascimento,
                                                isPrimeiroAcesso = user.isPrimeiroAcesso
                                            ),
                                            userId = user.id,
                                            token = userSession.token,
                                            callback = { result ->
                                                val index = usuarios.indexOf(currentUsuario)
                                                if (index != -1) {
                                                    usuarios[index] = user
                                                }
                                                errorMessage = null
                                                setShowDialog(false)
                                                updateCurrentPage()
                                            },
                                            onError = { message ->
                                                errorMessage = message
                                            }
                                        )
                                    }
                                }
                            },
                            onClose = { setShowDialog(false) },
                            errorMessage = errorMessage,
                            setErrorMessage = { errorMessage = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UsuarioTableHeader() {
    Row(modifier = Modifier.requiredWidth(800.dp).padding(8.dp)) {
        Text(text = "CPF", fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = "Nome", fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = "Email", fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = "Data de Nascimento", fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = "Ações", fontSize = 14.sp, modifier = Modifier.weight(0.5f))
    }
}

@Composable
fun UsuarioTable(
    usuarios: List<Usuario>,
    onEdit: (Usuario) -> Unit,
    onDelete: (Usuario) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        LazyColumn {
            items(usuarios) { usuario ->
                Column {
                    Row(modifier = Modifier.requiredWidth(800.dp).padding(8.dp)) {
                        Text(text = usuario.cpf, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            text = if (usuario.nome.isNotEmpty()) "${usuario.nome} ${usuario.sobrenome}" else "-",
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(text = usuario.email ?: "-", fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(text = usuario.dataNascimento ?: "-", fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Row {
                            IconButton(onClick = { onEdit(usuario) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDelete(usuario) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}


@Composable
fun UsuarioDialog(
    usuario: Usuario?,
    onSave: (Usuario) -> Unit,
    onClose: () -> Unit,
    errorMessage: String?,
    setErrorMessage: (String?) -> Unit
) {
    var id by remember { mutableStateOf(usuario?.id ?: "") }
    var cpf by remember { mutableStateOf(usuario?.cpf ?: "") }
    var nome by remember { mutableStateOf(usuario?.nome ?: "") }
    var sobrenome by remember { mutableStateOf(usuario?.sobrenome ?: "") }
    var email by remember { mutableStateOf(usuario?.email) }
    var dataNascimento by remember { mutableStateOf(usuario?.dataNascimento) }
    var tipo by remember { mutableStateOf(usuario?.tipo ?: "") }
    var isPrimeiroAcesso by remember { mutableStateOf(usuario?.isPrimeiroAcesso ?: true) }

    Dialog(onDismissRequest = { onClose() }) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                TextField(
                    value = cpf,
                    onValueChange = { cpf = it },
                    label = { Text("CPF") }
                )
                TextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") }
                )
                TextField(
                    value = sobrenome,
                    onValueChange = { sobrenome = it },
                    label = { Text("Sobrenome") }
                )
                TextField(
                    value = email ?: "",
                    onValueChange = { email = it },
                    label = { Text("Email") }
                )
                TextField(
                    value = dataNascimento  ?: "",
                    onValueChange = { dataNascimento = it },
                    label = { Text("Data de Nascimento") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                errorMessage?.let {
                    Text(text = it, color = Color.Red)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End) {
                    Button(onClick = { onClose(); setErrorMessage(null) }) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onSave(Usuario(id, cpf, nome, sobrenome, email, dataNascimento, tipo, isPrimeiroAcesso))
                    }) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

