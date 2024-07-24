import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ingressouapp.composeapp.generated.resources.Res
import ingressouapp.composeapp.generated.resources.logo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import qrscanner.QrScanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun QrScannerCompose(userSession: UserSession) {
    var platformName = "Mobile"
    var codigoIngresso by remember { mutableStateOf("") }
    var startBarCodeScan by remember { mutableStateOf(false) }
    var flashlightOn by remember { mutableStateOf(false) }
    var openImagePicker by remember { mutableStateOf(value = false) }
    val coroutineScope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()) {
        Column(
            modifier = Modifier
                .background(color = Color.White)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (codigoIngresso.isEmpty() && startBarCodeScan) {
                Column(
                    modifier = Modifier
                        .background(color = Color.Black)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = if (platformName != "Desktop") {
                            Modifier
                                .size(250.dp)
                                .clip(shape = RoundedCornerShape(size = 14.dp))
                                .clipToBounds()
                                .border(2.dp, Color.Gray, RoundedCornerShape(size = 14.dp))
                        } else {
                            Modifier
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        QrScanner(
                            modifier = Modifier
                                .clipToBounds()
                                .clip(shape = RoundedCornerShape(size = 14.dp)),
                            flashlightOn = flashlightOn,
                            openImagePicker = openImagePicker,
                            onCompletion = {
                                codigoIngresso = it
                                coroutineScope.launch {
                                    validateQRCode(codigoIngresso, userSession.token) { result ->
                                        if (result) {
                                            dialogMessage = "Ingresso validado com sucesso!"
                                            isSuccess = true
                                        } else {
                                            dialogMessage = "Ingresso inválido ou já foi utilizado!"
                                            isSuccess = false
                                        }
                                    }
                                    showDialog = true
                                    delay(2000)
                                    showDialog = false
                                }
                                startBarCodeScan = false
                            },
                            imagePickerHandler = {
                                openImagePicker = it
                            },
                            onFailure = {
                                coroutineScope.launch {
                                    if (it.isEmpty()) {
                                        dialogMessage = "Invalid QR code"
                                        isSuccess = false
                                        showDialog = true
                                        delay(2000)
                                        showDialog = false
                                    } else {
                                        dialogMessage = it
                                        isSuccess = false
                                        showDialog = true
                                        delay(2000)
                                        showDialog = false
                                    }
                                }
                            }
                        )
                    }

                    if (platformName != "Desktop") {
                        Box(
                            modifier = Modifier
                                .padding(start = 20.dp, end = 20.dp, top = 30.dp)
                                .background(
                                    color = Color(0xFFF9F9F9),
                                    shape = RoundedCornerShape(25.dp)
                                )
                                .height(35.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 5.dp, horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                Icon(imageVector = if (flashlightOn) Icons.Filled.Add else Icons.Filled.Close,
                                    "flash",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            flashlightOn = !flashlightOn
                                        })

                                VerticalDivider(
                                    modifier = Modifier,
                                    thickness = 1.dp,
                                    color = Color(0xFFD8D8D8)
                                )

                                Image(
                                    painter = painterResource(Res.drawable.logo),
                                    contentDescription = "gallery",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            openImagePicker = true
                                        }
                                )
                            }
                        }
                    } else {
                        Button(
                            modifier = Modifier.padding(top = 12.dp),
                            onClick = {
                                openImagePicker = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                        ) {
                            Text(
                                text = "Select Image",
                                modifier = Modifier
                                    .background(Color.Transparent)
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            startBarCodeScan = true
                            codigoIngresso = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            text = "Leitura de QRCode de Ingresso",
                            modifier = Modifier
                                .background(Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
        if (startBarCodeScan) {
            Icon(
                imageVector = Icons.Filled.Close,
                "Close",
                modifier = Modifier
                    .padding(top = 12.dp, end = 12.dp)
                    .size(24.dp)
                    .clickable {
                        startBarCodeScan = false
                    }
                    .align(Alignment.TopEnd),
                tint = Color.White
            )
        }

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
                                contentDescription = "Ingresso validado com sucesso!",
                                tint = Color.Green,
                                modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Ingresso inválido ou já foi validado!",
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
    }
}