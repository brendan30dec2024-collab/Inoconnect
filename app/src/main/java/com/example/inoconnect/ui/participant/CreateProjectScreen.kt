package com.example.inoconnect.ui.participant

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.auth.LightGrayInput
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    onProjectCreated: () -> Unit,
    onBackClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Form State
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var targetTeamSize by remember { mutableStateOf("") }

    // Tags
    var currentTagInput by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }

    // Image
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    // Helper: Date Picker
    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, day: Int ->
                deadline = "$year-${month + 1}-$day"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- 1. Blue Curved Header ---
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - 80)
                quadraticBezierTo(
                    size.width / 2, size.height + 40,
                    0f, size.height - 80
                )
                close()
            }
            drawPath(path = path, color = BrandBlue)
        }

        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(top = 40.dp, start = 16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // --- 2. Main Content Card ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "New Project",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Image Upload Area ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(LightGrayInput)
                            .clickable { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                                Text("Upload Cover Photo", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- Form Fields ---

                    // 1. Title
                    CustomStyledInput(
                        value = title,
                        onValueChange = { title = it },
                        label = "Project Title",
                        icon = Icons.Default.Info
                    )

                    Spacer(Modifier.height(16.dp))

                    // 2. Description
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Description", fontWeight = FontWeight.SemiBold, color = Color.Black, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Describe your project...", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = LightGrayInput,
                                unfocusedContainerColor = LightGrayInput,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // 3. Team Size & Deadline
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomStyledInput(
                                value = targetTeamSize,
                                onValueChange = { if (it.all { char -> char.isDigit() }) targetTeamSize = it },
                                label = "Team Size",
                                icon = Icons.Default.Person,
                                keyboardType = KeyboardType.Number
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text(text = "Deadline", fontWeight = FontWeight.SemiBold, color = Color.Black, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                                TextField(
                                    value = deadline,
                                    onValueChange = {},
                                    placeholder = { Text("Select", color = Color.Gray) },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = LightGrayInput,
                                        unfocusedContainerColor = LightGrayInput,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePicker() }) {
                                            Icon(Icons.Default.DateRange, null, tint = BrandBlue)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 4. Tags Input
                    Text(text = "Looking For (Skills/Roles)", fontWeight = FontWeight.SemiBold, color = Color.Black, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp).align(Alignment.Start))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = currentTagInput,
                            onValueChange = { currentTagInput = it },
                            placeholder = { Text("e.g. Developer") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = LightGrayInput,
                                unfocusedContainerColor = LightGrayInput,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (currentTagInput.isNotBlank() && !tags.contains(currentTagInput)) {
                                    tags.add(currentTagInput.trim())
                                    currentTagInput = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null)
                        }
                    }

                    // Chips Display
                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            items(tags) { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { tags.remove(tag) },
                                    label = { Text(tag) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = BrandBlue.copy(alpha = 0.1f),
                                        selectedLabelColor = BrandBlue
                                    ),
                                    border = InputChipDefaults.inputChipBorder(
                                        enabled = true,
                                        selected = true,
                                        selectedBorderColor = BrandBlue.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // --- Submit Button ---
                    if (isUploading) {
                        CircularProgressIndicator(color = BrandBlue)
                    } else {
                        Button(
                            onClick = {
                                val teamSizeInt = targetTeamSize.toIntOrNull()

                                if (title.isBlank() || deadline.isBlank() || targetTeamSize.isBlank()) {
                                    Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                                } else if (teamSizeInt == null || teamSizeInt <= 0) {
                                    Toast.makeText(context, "Invalid team size", Toast.LENGTH_SHORT).show()
                                } else if (teamSizeInt > 50) { // [FIX] Hard Limit Validation
                                    Toast.makeText(context, "Team size cannot exceed 50 members", Toast.LENGTH_LONG).show()
                                } else {
                                    scope.launch {
                                        isUploading = true
                                        val finalUrl = selectedImageUri?.let { repository.uploadImage(it) } ?: ""

                                        repository.createProject(
                                            title,
                                            description,
                                            finalUrl,
                                            tags.toList(),
                                            deadline,
                                            teamSizeInt
                                        )

                                        isUploading = false
                                        onProjectCreated()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Publish Project", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun CustomStyledInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.SemiBold, color = Color.Black, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(label, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LightGrayInput,
                unfocusedContainerColor = LightGrayInput,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}