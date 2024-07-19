import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import io.ktor.http.HttpHeaders.Date


import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun getResponsiveMaxWidth(): Modifier {
    val isWeb = LocalDensity.current.density < 2.0
    val cardWidthModifier = if (isWeb) {
        Modifier.fillMaxWidth(0.3f)
    } else {
        Modifier.fillMaxWidth()
    }
    return cardWidthModifier
}

@Composable
@Preview
fun App() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.LightGray) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoggedIn) {
                    if (isAdmin) {
                        DashboardScreen()
                    } else {
                        PurchaseScreen()
                    }
                } else {
                    LoginScreen(onLoginSuccess = { isAdminUser ->
                        isLoggedIn = true
                        isAdmin = isAdminUser
                    })
                }
            }
        }
    }
}

@Composable
@Preview
fun LoginScreen(onLoginSuccess: (Boolean) -> Unit) {
    var cpf by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFirstAccess by remember { mutableStateOf(false) }
    var isStepOneComplete by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .padding(16.dp)
            .then(getResponsiveMaxWidth())
            .wrapContentHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Ingressou", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Login", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (!isStepOneComplete) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = cpf,
                        onValueChange = { cpf = it },
                        label = { Text("CPF") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                validateCPF(cpf) { result, isFirst ->
                                    if (result) {
                                        isFirstAccess = isFirst
                                        isStepOneComplete = true
                                    } else {
                                        errorMessage = "CPF inválido"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Validar CPF")
                    }
                    errorMessage?.let {
                        Text(text = it, color = Color.Red)
                    }
                }
            } else {
                if (isFirstAccess) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirmar Senha") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (password == confirmPassword) {
                                    coroutineScope.launch {
                                        performRegistration(email, password) { success ->
                                            if (success) onLoginSuccess(false)
                                            else errorMessage = "Falha no registro"
                                        }
                                    }
                                } else {
                                    errorMessage = "As senhas não coincidem"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Registrar e Entrar")
                        }
                        errorMessage?.let {
                            Text(text = it, color = Color.Red)
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    performLogin(cpf, password) { success, isAdmin ->
                                        if (success) {
                                            onLoginSuccess(isAdmin)
                                        } else {
                                            errorMessage = "Login falhou"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Entrar")
                        }
                        errorMessage?.let {
                            Text(text = it, color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    // Implementação da tela de Dashboard
    Text(text = "Bem-vindo ao Dashboard")
}

@Composable
@Preview
fun PurchaseScreen() {
    var currentStep by remember { mutableStateOf(0) }
    var users by remember { mutableStateOf(listOf<User>()) }
    var newUser by remember { mutableStateOf(User()) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.LightGray) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                when (currentStep) {
                    0 -> PurchaseStepOne(
                        newUser = newUser,
                        users = users,
                        onUserChange = { newUser = it },
                        onAddUser = { users = users + newUser; newUser = User() },
                        onRemoveUser = { users = users - it },
                        onNext = { if (users.isNotEmpty()) currentStep++ }
                    )

                    1 -> PurchaseStepTwo(users = users, onBack = { currentStep-- }, onNext = { currentStep++ })
                    2 -> PurchaseStepThree(users = users, onBack = { currentStep-- })
                }
            }
        }
    }
}

@Composable
fun PurchaseStepOne(
    newUser: User,
    users: List<User>,
    onUserChange: (User) -> Unit,
    onAddUser: () -> Unit,
    onRemoveUser: (User) -> Unit,
    onNext: () -> Unit
) {

    fun validateForm(): Boolean {
        //val cpfRegex = Regex("^[0-9]{11}\$")
        val nameRegex = Regex("^[\\p{L} ]+\$")
        val birthDateRegex = Regex("^\\d{2}/\\d{2}/\\d{4}\$")
        val validMaritalStatuses = listOf("Solteiro(a)", "Comprometido(a)")

        return when {
            /*newUser.cpf.isEmpty() -> {
                onUserChange(newUser.copy(errorMessage = "CPF não pode estar vazio"))
                false
            }
            !cpfRegex.matches(newUser.cpf) -> {
                onUserChange(newUser.copy(errorMessage = "CPF inválido"))
                false
            }*/
            newUser.name.isEmpty() -> {
                onUserChange(newUser.copy(errorMessage = "Nome não pode estar vazio"))
                false
            }

            !nameRegex.matches(newUser.name) -> {
                onUserChange(newUser.copy(errorMessage = "Nome inválido"))
                false
            }

            newUser.birthDate.isEmpty() -> {
                onUserChange(newUser.copy(errorMessage = "Data de nascimento não pode estar vazia"))
                false
            }

            !birthDateRegex.matches(newUser.birthDate) -> {
                onUserChange(newUser.copy(errorMessage = "Data de nascimento inválida"))
                false
            }

            newUser.maritalStatus.isEmpty() -> {
                onUserChange(newUser.copy(errorMessage = "Situação não pode estar vazio"))
                false
            }

            !validMaritalStatuses.contains(newUser.maritalStatus) -> {
                onUserChange(newUser.copy(errorMessage = "Situação inválido"))
                false
            }

            else -> true
        }
    }

    fun validateListTickets(): Boolean {
        return when {
            users.isEmpty() -> {
                onUserChange(newUser.copy(errorMessage = "Adicione ingressos na lista para poder avançar"))
                false
            }

            else -> true
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.LightGray) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentWidth()
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .then(getResponsiveMaxWidth())
                        .height(400.dp) // max card
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "Adicione os ingressos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        UserForm(user = newUser, onUserChange = onUserChange)

                        Spacer(modifier = Modifier.weight(1f)) // Preenche o espaço restante disponível
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp) // Adiciona espaçamento superior se necessário
                        ) {
                            IconButton(
                                onClick = {
                                    if (validateForm()) {
                                        onUserChange(newUser.copy(id = users.size + 1))
                                        onAddUser()
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                    .size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Adicionar usuário",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (validateListTickets()) {
                                        onNext()
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                    .size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Avançar",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .then(getResponsiveMaxWidth())
                        .height(650.dp) // max card
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "Seus Ingressos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(
                            modifier = Modifier.weight(1f) // Preenche o espaço restante
                        ) {
                            items(users) { user ->
                                UserCard(user = user, onRemove = { onRemoveUser(user) })
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = "R$${users.size * 100},00",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

            }
        }
    }
}

fun Long.toBrazilianDateFormat(
    pattern: String = "dd/MM/yyyy"
): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val dateTime = instant.toLocalDateTime(TimeZone.UTC)

    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
    val month = dateTime.monthNumber.toString().padStart(2, '0')
    val year = dateTime.year.toString()

    return pattern
        .replace("dd", day)
        .replace("MM", month)
        .replace("yyyy", year)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInputField(
    selectedDate: String,
    setSelectedDate: (String) -> Unit,
    onUserChange: (User) -> Unit,
    user: User
) {
    val focusManager = LocalFocusManager.current
    var showDatePickerDialog by remember {
        mutableStateOf(false)
    }
    val datePickerState = rememberDatePickerState()

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = millis.toBrazilianDateFormat()
                            setSelectedDate(newDate)
                            onUserChange(user.copy(birthDate = newDate))
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text(text = "Escolher data")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    TextField(
        value = selectedDate,
        onValueChange = { },
        Modifier
            .fillMaxWidth()
            .onFocusEvent {
                if (it.isFocused) {
                    showDatePickerDialog = true
                    focusManager.clearFocus(force = true)
                }
            },
        label = { Text("Date") },
        readOnly = true
    )
}

@Composable
fun UserForm(user: User, onUserChange: (User) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("Solteiro(a)") }
    val options = listOf("Solteiro(a)", "Comprometido(a)")

    Column {
        /*TextField(
            value = user.cpf,
            onValueChange = {
                onUserChange(user.copy(cpf = it))
            },
            label = { Text("CPF") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))*/
        TextField(
            value = user.name,
            onValueChange = { onUserChange(user.copy(name = it)) },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        DateInputField(
            selectedDate = user.birthDate,
            setSelectedDate = { newDate -> onUserChange(user.copy(birthDate = newDate)) },
            onUserChange = onUserChange,
            user = user
        )

        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = user.maritalStatus,
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
                        onUserChange(user.copy(maritalStatus = selectionOption))
                        expanded = false
                    },
                    text = { Text(selectionOption) }
                )
            }
        }
        user.errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun UserCard(user: User, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp)
        ) {
            Column {
                Text(text = "#${user.id}", color = Color.White, fontWeight = FontWeight.Bold)
                //Text(text = "CPF: ${user.cpf}", color = Color.White)
                Text(text = "Nome: ${user.name}", color = Color.White)
                Text(text = "Data de nascimento: ${user.birthDate}", color = Color.White)
                Text(text = "Situação: ${user.maritalStatus}", color = Color.White)
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remover usuário",
                    tint = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }
}

@Composable
fun PurchaseStepTwo(users: List<User>, onBack: () -> Unit, onNext: () -> Unit) {
    Surface(color = Color.LightGray) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .then(getResponsiveMaxWidth())
                .height(600.dp) // max card
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Confirmação das informações",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f) // Preenche o espaço restante
                ) {
                    items(users) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 4.dp,
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Column {
                                    Text(text = "#${user.id}", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(text = "Nome: ${user.name}", color = Color.White)
                                    Text(text = "Data de nascimento: ${user.birthDate}", color = Color.White)
                                    Text(text = "Situação: ${user.maritalStatus}", color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = Color.White,
                            contentDescription = "Voltar"
                        )
                    }
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            tint = Color.White,
                            contentDescription = "Avançar"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseStepThree(users: List<User>, onBack: () -> Unit) {
    Surface(color = Color.LightGray) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .then(getResponsiveMaxWidth())
                .wrapContentHeight()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Pagamento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                users.forEach { user ->
                    Text(text = "Ingresso para ${user.name}: R$100,00")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "Total: R$${users.size * 100},00",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = Color.White,
                            contentDescription = "Voltar"
                        )
                    }
                    Button(onClick = { /* Implementar pagamento */ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Pagar")
                    }
                }
            }
        }
    }
}

val client = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

suspend fun validateCPF(cpf: String, callback: (Boolean, Boolean) -> Unit) {
    try {
        // Código para validar o CPF
        callback(true, true)
    } catch (e: Exception) {
        callback(false, false)
    }
}

suspend fun performRegistration(email: String, password: String, callback: (Boolean) -> Unit) {
    try {
        println(email)
        println(password)
        // Código para registrar o usuário
        callback(true)
    } catch (e: Exception) {
        callback(false)
    }
}

suspend fun performLogin(cpf: String, password: String, callback: (Boolean, Boolean) -> Unit) {
    try {
        val response: HttpResponse = client.post("https://seuservidor.com/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(cpf, password))
        }
        if (response.status == HttpStatusCode.OK) {
            val isAdmin = response.body<Boolean>()
            callback(true, isAdmin)
        } else {
            callback(false, false)
        }
    } catch (e: Exception) {
        callback(false, false)
    }
}

@Serializable
data class RegistrationRequest(val email: String, val password: String)

@Serializable
data class LoginRequest(val cpf: String, val password: String)

data class User(
    val id: Int = 1,
    //val cpf: String = "",
    val name: String = "",
    val birthDate: String = "",
    val maritalStatus: String = "",
    val errorMessage: String? = null
)
