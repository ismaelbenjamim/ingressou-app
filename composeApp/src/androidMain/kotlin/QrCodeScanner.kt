import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ingressouapp.composeapp.generated.resources.Res
import ingressouapp.composeapp.generated.resources.logo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import qrscanner.QrScanner


@Composable
fun QrScannerCompose2() {
    var platformName = "Mobile"
    var qrCodeURL by remember { mutableStateOf("") }
    var startBarCodeScan by remember { mutableStateOf(false) }
    var flashlightOn by remember { mutableStateOf(false) }
    var openImagePicker by remember { mutableStateOf(value = false) }
    val snackBarHostState = SnackbarHostState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .background(color = Color.White)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (qrCodeURL.isEmpty() && startBarCodeScan) {
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
                                qrCodeURL = it
                                startBarCodeScan = false
                            },
                            imagePickerHandler = {
                                openImagePicker = it
                            },
                            onFailure = {
                                coroutineScope.launch {
                                    if (it.isEmpty()) {
                                        snackBarHostState.showSnackbar("Invalid qr code")
                                    } else {
                                        snackBarHostState.showSnackbar(it)
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
                                modifier = Modifier.background(Color.Transparent)
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
                            qrCodeURL = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    ) {
                        Text(
                            text = "Scan Qr",
                            modifier = Modifier.background(Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = qrCodeURL,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 12.dp)
                    )
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
                    }.align(Alignment.TopEnd),
                tint = Color.White
            )
        }
    }
}