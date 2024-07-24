import androidx.compose.foundation.*
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager

import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.sp
import ingressouapp.composeapp.generated.resources.Res
import ingressouapp.composeapp.generated.resources.logo
import ingressouapp.composeapp.generated.resources.qrcode
import kotlinx.coroutines.delay

import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource

@Serializable
enum class ResponsiveSize(val size: Float) {
    SM(0.3f),
    MD(0.5f),
    LG(0.8f)
}

@Composable
fun getResponsiveMaxWidth(responsiveSize: ResponsiveSize): Modifier {
    val isWeb = LocalDensity.current.density < 2.0
    val cardWidthModifier = if (isWeb) {
        Modifier.fillMaxWidth(responsiveSize.size)
    } else {
        Modifier.fillMaxWidth()
    }
    return cardWidthModifier
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(onLogout: () -> Unit, onScreen: (currentScreen: String) -> Unit, sessionManager: SessionManager) {
    TopAppBar(
        title = { Text("Ingressou") },
        actions = {
            if (sessionManager.getUserSession().isAdmin) {
                IconButton(onClick = { onScreen("usuarios") }) {
                    Icon(Icons.Filled.Person, contentDescription = "Usuários")
                }
                IconButton(onClick = { onScreen("ingressos") }) {
                    Icon(Icons.Filled.Star, contentDescription = "Ingressos")
                }
                IconButton(onClick = { onScreen("validar_ingresso") }) {
                    Icon(Icons.Filled.Check, contentDescription = "Validar Ingresso")
                }
            } else {
                IconButton(onClick = { onScreen("meus_ingressos") }) {
                    Icon(Icons.Filled.Star, contentDescription = "Meus Ingressos")
                }
                IconButton(onClick = { onScreen("comprar_ingressos") }) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Comprar Ingressos")
                }
            }
            IconButton(onClick = onLogout) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}


@Composable
@Preview
fun App(sessionManager: SessionManager) {
    var userSession by remember { mutableStateOf(sessionManager.getUserSession()) }
    var currentScreen by remember { mutableStateOf(if (userSession.isAdmin) "usuarios" else "comprar_ingressos") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.LightGray) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (userSession.token.isNotEmpty()) {
                    TopBar(onLogout = {
                        userSession = UserSession()
                        sessionManager.saveUserSession(userSession)
                    }, onScreen = { screen ->
                        currentScreen = screen
                    }, sessionManager)
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (userSession.token.isNotEmpty()) {
                    if (userSession.isAdmin) {
                        when (currentScreen) {
                            "usuarios" -> UsuariosScreen(userSession)
                            "ingressos" -> IngressosScreen(userSession)
                            "validar_ingresso" -> ValidarIngressoScreen(userSession)
                        }
                    } else {
                        when (currentScreen) {
                            "meus_ingressos" -> MeusIngressosScreen(userSession)
                            "comprar_ingressos" -> PurchaseScreen(userSession)
                        }
                    }
                } else {
                    LoginScreen(userAuthentication = { authentication: UserSession ->
                        userSession = authentication
                        sessionManager.saveUserSession(authentication)
                    })
                }
            }
        }
    }
}

@Composable
@Preview
fun LoginScreen(userAuthentication: (UserSession) -> Unit) {
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
            .then(getResponsiveMaxWidth(ResponsiveSize.SM))
            .wrapContentHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painterResource(Res.drawable.logo), "logo", modifier = Modifier.width(300.dp))
            }
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
                                validateCPF(cpf) { result: CpfValidationDTO ->
                                    if (result.validate) {
                                        isFirstAccess = result.firstAccess
                                        isStepOneComplete = true
                                        errorMessage = ""
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
                                        performRegistration(cpf, email, password) { success ->
                                            if (success) {
                                                coroutineScope.launch {
                                                    performLogin(cpf, password) { success, result: LoginResponse? ->
                                                        if (success && result != null) {
                                                            errorMessage = ""
                                                            userAuthentication(
                                                                UserSession(
                                                                    isAdmin = result.tipo == UserType.ADMIN,
                                                                    token = result.token,
                                                                    isAuthenticated = true
                                                                )
                                                            )
                                                        } else {
                                                            errorMessage = "Login falhou"
                                                        }
                                                    }
                                                }
                                            } else errorMessage = "Falha no registro"
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
                                    performLogin(cpf, password) { success, result: LoginResponse? ->
                                        if (success && result != null) {
                                            errorMessage = ""
                                            userAuthentication(
                                                UserSession(
                                                    isAdmin = result.tipo == UserType.ADMIN,
                                                    token = result.token,
                                                    isAuthenticated = true
                                                )
                                            )
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
@Preview
fun PurchaseScreen(userSession: UserSession) {
    var currentStep by remember { mutableStateOf(0) }
    var users by remember { mutableStateOf(listOf<User>()) }
    var newUser by remember { mutableStateOf(User()) }

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
            2 -> PurchaseStepThree(users = users, userSession = userSession, onBack = { currentStep-- })
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
    val usersState = remember { mutableStateListOf<User>().apply { addAll(users) } }

    fun validateForm(): Boolean {
        val nameRegex = Regex("^[\\p{L} ]+\$")
        val birthDateRegex = Regex("^\\d{2}/\\d{2}/\\d{4}\$")
        val validMaritalStatuses = listOf("Solteiro(a)", "Comprometido(a)")

        return when {
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
            usersState.isEmpty() -> {
                onUserChange(newUser.copy(errorMessage = "Adicione ingressos na lista para poder avançar"))
                false
            }

            else -> true
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 65.dp)
    ) {
        if (LocalDensity.current.density < 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentWidth()
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .then(getResponsiveMaxWidth(ResponsiveSize.SM))
                        .height(400.dp)
                ) {
                    AdicioneIngressosContent(
                        newUser = newUser,
                        onUserChange = onUserChange,
                        onAddUser = {
                            onAddUser()
                            usersState.add(newUser.copy(id = usersState.size + 1))
                        },
                        validateForm = ::validateForm,
                        validateListTickets = ::validateListTickets,
                        onNext = onNext,
                        users = usersState
                    )
                }
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .then(getResponsiveMaxWidth(ResponsiveSize.SM))
                        .height(650.dp)
                ) {
                    SeusIngressosContent(users = usersState, onRemoveUser = { user ->
                        onRemoveUser(user)
                        usersState.remove(user)
                    })
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    AdicioneIngressosContent(
                        newUser = newUser,
                        onUserChange = onUserChange,
                        onAddUser = {
                            onAddUser()
                            usersState.add(newUser.copy(id = usersState.size + 1))
                        },
                        validateForm = ::validateForm,
                        validateListTickets = ::validateListTickets,
                        onNext = onNext,
                        users = usersState
                    )
                }
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(650.dp)
                ) {
                    SeusIngressosContent(users = usersState, onRemoveUser = { user ->
                        onRemoveUser(user)
                        usersState.remove(user)
                    })
                }
            }
        }
    }
}


@Composable
fun AdicioneIngressosContent(
    newUser: User,
    onUserChange: (User) -> Unit,
    onAddUser: () -> Unit,
    validateForm: () -> Boolean,
    validateListTickets: () -> Boolean,
    onNext: () -> Unit,
    users: List<User>
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

        Spacer(modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
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

@Composable
fun SeusIngressosContent(users: List<User>, onRemoveUser: (User) -> Unit) {
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
            modifier = Modifier.weight(1f)
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
                .then(getResponsiveMaxWidth(ResponsiveSize.SM))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseStepThree(users: List<User>, userSession: UserSession, onBack: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Surface(color = Color.LightGray) {
        if (showDialog) {
            BasicAlertDialog(
                onDismissRequest = { showDialog = false },
                content = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSuccess) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Ingresso pago com sucesso!",
                                tint = Color.Green,
                                modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Ocorreu algum problema no pagamento!",
                                tint = Color.Red,
                                modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = dialogMessage, fontSize = 18.sp)
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color.Gray))
                    .padding(16.dp)
            )
        }
        Card(
            modifier = Modifier
                .padding(16.dp)
                .then(getResponsiveMaxWidth(ResponsiveSize.SM))
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
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = Color.White,
                            contentDescription = "Voltar"
                        )
                    }
                    Button(onClick = {
                        coroutineScope.launch {
                            val ingressos = mutableListOf<SimpleIngresso>()
                            users.forEach { user ->
                                ingressos.add(SimpleIngresso(user.name, user.birthDate, user.maritalStatus))
                            }
                            paymentIngresso(ingressos = PaymentIngressoRequest(ingressos), token = userSession.token) { result ->
                                if (result) {
                                    dialogMessage = "Ingresso pago e criado com sucesso!"
                                    isSuccess = true
                                } else {
                                    dialogMessage = "Ocorreu algum problema no pagamento!"
                                    isSuccess = false
                                }
                            }
                            showDialog = true
                            delay(2000)
                            showDialog = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Pagar")
                    }
                }
            }
        }
    }
}

@Composable
fun MeusIngressosScreen(userSession: UserSession) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 65.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(Res.drawable.qrcode), "qrcode", modifier = Modifier.width(300.dp))
    }
}

val client = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

suspend fun validateCPF(cpf: String, callback: (CpfValidationDTO) -> Unit) {
    try {
        val response: HttpResponse = client.get("${ApiConfig.BASE_URL}/user/validate_cpf/${cpf}/") {
            contentType(ContentType.Application.Json)
        }
        if (response.status == HttpStatusCode.OK) {
            val result: CpfValidationResponse = response.body()
            callback(
                CpfValidationDTO(
                    validate = true,
                    firstAccess = result.firstAccess
                )
            )
        } else {
            callback(CpfValidationDTO(validate = false, firstAccess = false))
        }
    } catch (e: Exception) {
        callback(CpfValidationDTO(validate = false, firstAccess = false))
    }
}

suspend fun performRegistration(cpf: String, email: String, password: String, callback: (Boolean) -> Unit) {
    try {
        val response: HttpResponse = client.post("${ApiConfig.BASE_URL}/user/primeiro_acesso/") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(cpf, password, email))
        }
        if (response.status == HttpStatusCode.OK) {
            callback(true)
        } else {
            callback(false)
        }
    } catch (e: Exception) {
        callback(false)
    }
}

suspend fun performLogin(cpf: String, password: String, callback: (Boolean, LoginResponse?) -> Unit) {
    try {
        val response: HttpResponse = client.post("${ApiConfig.BASE_URL}/user/login/") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(cpf, password))
        }
        if (response.status == HttpStatusCode.OK) {
            val result: LoginResponse = response.body()
            callback(true, result)
        } else {
            callback(false, null)
        }
    } catch (e: Exception) {
        callback(false, null)
    }
}

suspend fun validateQRCode(ingressoId: String, token: String, callback: (Boolean) -> Unit) {
    try {
        println(ingressoId)
        val response: HttpResponse = client.post("${ApiConfig.BASE_URL}/ingresso/validate/") {
            contentType(ContentType.Application.Json)
            setBody(ValidateQRCodeRequest(ingressoId))
            headers {
                append("Authorization", "Token $token")
            }
        }
        println(response.status)
        if (response.status == HttpStatusCode.OK) {
            callback(true)
        } else {
            callback(false)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        callback(false)
    }
}

suspend fun paymentIngresso(ingressos: PaymentIngressoRequest, token: String, callback: (Boolean) -> Unit) {
    try {
        println(ingressos)
        val response: HttpResponse = client.post("${ApiConfig.BASE_URL}/ingresso/payment/") {
            contentType(ContentType.Application.Json)
            setBody(ingressos)
            headers {
                append("Authorization", "Token $token")
            }
        }
        println(response.status)
        if (response.status == HttpStatusCode.Created) {
            callback(true)
        } else {
            callback(false)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        callback(false)
    }
}


@Serializable
data class RegistrationRequest(val email: String, val password: String)


data class User(
    val id: Int = 1,
    val name: String = "",
    val birthDate: String = "",
    val maritalStatus: String = "",
    val errorMessage: String? = null
)
