import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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


data class Ingresso(
    val id: String,
    val usuario: UserResponse,
    val nome: String,
    val dataNascimento: String,
    val situacao: String,
    val createdAt: String,
    val utilizadoEm: String?
)

suspend fun getIngressos(token: String, callback: (List<IngressoResponse>) -> Unit) {
    try {
        val response: HttpResponse = client.get("${ApiConfig.BASE_URL}/ingresso/") {
            contentType(ContentType.Application.Json)
            headers {
                append("Authorization", "Token $token")
            }
        }
        if (response.status == HttpStatusCode.OK) {
            val result: List<IngressoResponse> = response.body()
            callback(result)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

suspend fun createIngresso(
    request: IngressoGenerateRequest,
    token: String,
    callback: (IngressoResponse) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val response: HttpResponse = client.post("${ApiConfig.BASE_URL}/ingresso/generate/") {
            contentType(ContentType.Application.Json)
            setBody(request)
            headers {
                append("Authorization", "Token $token")
            }
        }
        if (response.status == HttpStatusCode.Created) {
            val result: IngressoResponse = response.body()
            callback(result)
        } else {
            onError("Ocorreu um problema ao tentar criar um ingresso para este cpf, tente novamente")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onError("Serviço indisponível, contate um administrador.")
    }
}

suspend fun updateIngresso(
    request: IngressoGenerateRequest,
    ingressoId: String,
    token: String,
    callback: (IngressoResponse) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val response: HttpResponse = client.put("${ApiConfig.BASE_URL}/ingresso/${ingressoId}/") {
            contentType(ContentType.Application.Json)
            setBody(request)
            headers {
                append("Authorization", "Token $token")
            }
        }
        if (response.status == HttpStatusCode.OK) {
            val result: IngressoResponse = response.body()
            callback(result)
        } else {
            onError("Ocorreu um problema ao tentar alterar o ingresso, tente novamente")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onError("Serviço indisponível, contate um administrador.")
    }
}

suspend fun deleteIngresso(
    ingressoId: String,
    token: String,
    callback: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val response: HttpResponse = client.delete("${ApiConfig.BASE_URL}/ingresso/${ingressoId}/") {
            contentType(ContentType.Application.Json)
            headers {
                append("Authorization", "Token $token")
            }
        }
        if (response.status == HttpStatusCode.NoContent) {
            callback()
        } else {
            onError("Erro ao deletar ingresso")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onError("Erro ao deletar ingresso: ${e.message}")
    }
}


@Composable
fun IngressosScreen(userSession: UserSession) {
    val ingressos = remember { mutableStateListOf<Ingresso>() }
    val coroutineScope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        getIngressos(token = userSession.token) { result ->
            if (result.isNotEmpty()) {
                ingressos.clear()
                val ingressosConverted = result.toIngressoList()
                ingressos.addAll(ingressosConverted)
            }
        }
    }

    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
    val (currentIngresso, setCurrentIngresso) = remember { mutableStateOf<Ingresso?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    val itemsPerPage = 3

    val pagedIngressos by derivedStateOf {
        ingressos.chunked(itemsPerPage)
    }

    fun updateCurrentPage() {
        val maxPage = if (pagedIngressos.isNotEmpty()) pagedIngressos.size - 1 else 0
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
                text = "Ingressos",
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
                    Button(onClick = { setCurrentIngresso(null); setShowDialog(true) }) {
                        Text("Adicionar Ingresso")
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
                            IngressoTableHeader()
                            HorizontalDivider()
                            if (pagedIngressos.isNotEmpty() && currentPage < pagedIngressos.size) {
                                IngressoTable(
                                    ingressos = pagedIngressos[currentPage],
                                    onEdit = {
                                        setCurrentIngresso(it)
                                        setShowDialog(true)
                                    },
                                    onDelete = { ingresso ->
                                        coroutineScope.launch {
                                            deleteIngresso(
                                                ingressoId = ingresso.id,
                                                token = userSession.token,
                                                callback = {
                                                    ingressos.remove(ingresso)
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
                        Text("Página ${currentPage + 1} de ${if (pagedIngressos.isNotEmpty()) pagedIngressos.size else 1}")
                        if (currentPage < pagedIngressos.size - 1) {
                            IconButton(
                                onClick = { if (currentPage < pagedIngressos.size - 1) currentPage++ },
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
                        IngressoDialog(
                            ingresso = currentIngresso,
                            onSave = { ingresso ->
                                if (currentIngresso == null) {
                                    coroutineScope.launch {
                                        createIngresso(
                                            request = IngressoGenerateRequest(
                                                usuario = ingresso.usuario.cpf,
                                                nome = ingresso.nome,
                                                dataNascimento = ingresso.dataNascimento,
                                                situacao = ingresso.situacao
                                            ),
                                            token = userSession.token,
                                            callback = { result ->
                                                ingressos.add(ingresso.copy(id = result.id, usuario = result.usuario))
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
                                        updateIngresso(
                                            request = IngressoGenerateRequest(
                                                usuario = ingresso.usuario.cpf,
                                                nome = ingresso.nome,
                                                dataNascimento = ingresso.dataNascimento,
                                                situacao = ingresso.situacao
                                            ),
                                            ingressoId = ingresso.id,
                                            token = userSession.token,
                                            callback = { result ->
                                                val index = ingressos.indexOf(currentIngresso)
                                                if (index != -1) {
                                                    ingressos[index] = ingresso
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
fun IngressoTableHeader() {
    Row(modifier = Modifier.requiredWidth(800.dp).padding(8.dp)) {
        Text(text = "Responsável", fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = "Nome", fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = "Nascimento", fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = "Situação", fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = "Utilizado em", fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = "Ações", fontSize = 14.sp, modifier = Modifier.weight(0.5f))
    }
}

@Composable
fun IngressoTable(
    ingressos: List<Ingresso>,
    onEdit: (Ingresso) -> Unit,
    onDelete: (Ingresso) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        LazyColumn {
            items(ingressos) { ingresso ->
                Column {
                    Row(modifier = Modifier.requiredWidth(800.dp).padding(8.dp)) {
                        Text(
                            text = "${ingresso.usuario.firstName} ${ingresso.usuario.lastName}",
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = ingresso.nome,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(text = ingresso.dataNascimento, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(text = ingresso.situacao, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(text = ingresso.utilizadoEm ?: "N/A", fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Row {
                            IconButton(onClick = { onEdit(ingresso) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDelete(ingresso) }) {
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
fun IngressoDialog(
    ingresso: Ingresso?,
    onSave: (Ingresso) -> Unit,
    onClose: () -> Unit,
    errorMessage: String?,
    setErrorMessage: (String?) -> Unit
) {
    var id by remember { mutableStateOf(ingresso?.id ?: "") }
    var usuario by remember { mutableStateOf(ingresso?.usuario ?: UserResponse("", "", "", "", "", "", "", true)) }
    var nome by remember { mutableStateOf(ingresso?.nome ?: "") }
    var dataNascimento by remember { mutableStateOf(ingresso?.dataNascimento ?: "") }
    var situacao by remember { mutableStateOf(ingresso?.situacao ?: "") }
    var createdAt by remember { mutableStateOf(ingresso?.createdAt ?: "") }
    var utilizadoEm by remember { mutableStateOf(ingresso?.utilizadoEm ?: "") }

    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("Solteiro(a)") }
    val options = listOf("Solteiro(a)", "Comprometido(a)")

    Dialog(onDismissRequest = { onClose() }) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                TextField(
                    value = usuario.cpf,
                    onValueChange = { usuario = usuario.copy(cpf = it) },
                    label = { Text("CPF do responsável") }
                )
                TextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") }
                )
                TextField(
                    value = dataNascimento,
                    onValueChange = { dataNascimento = it },
                    label = { Text("Data de nascimento") }
                )
                TextField(
                    value = situacao,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Situação") },
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                ) {
                    options.forEach { selectionOption ->
                        DropdownMenuItem(
                            onClick = {
                                selectedOption = selectionOption
                                situacao = selectionOption
                                expanded = false
                            },
                            text = { Text(selectionOption) }
                        )
                    }
                }
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
                        onSave(Ingresso(id, usuario, nome, dataNascimento, situacao, createdAt, utilizadoEm))
                    }) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

@Composable
fun ValidarIngressoScreen(userSession: UserSession) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 65.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QrScannerCompose(userSession)
    }
}
