package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.SamplePapers
import com.example.ui.viewmodel.PaperViewModel
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: PaperViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingStep by viewModel.processingStep.collectAsState()
    val errorMsg by viewModel.error.collectAsState()
    
    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Mathematics") }
    var selectedModel by remember { mutableStateOf("gemini-3.1-pro-preview") } // Default recommended
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // For sample preset testing
    var selectedSampleIndex by remember { mutableStateOf<Int?>(null) }

    var showCameraPreview by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showCameraPreview = true
        } else {
            Toast.makeText(context, "Camera permission is required to snap papers directly.", Toast.LENGTH_SHORT).show()
        }
    }

    val subjectsList = listOf("Mathematics", "Physics", "Chemistry", "Generic English/Bengali")
    val modelsList = listOf(
        "gemini-3.1-pro-preview" to "Pro Model (Highly Recommended for Math/LaTeX)",
        "gemini-3.5-flash" to "Flash Model (Faster OCR for plain text)"
    )

    // Photo picker launcher (zero permission, Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedSampleIndex = null
            viewModel.clearError()
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Back navigation support
    androidx.activity.compose.BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = modifier.testTag("scan_screen_scaffold"),
        topBar = {
            TopAppBar(
                title = { Text("Digitize New Paper", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning about API Key
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Gemini API Key Missing",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "To run custom OCR, you must add your API key in the Secrets panel in AI Studio. However, you can instantly test pre-transcribed samples below!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    placeholder = { Text("e.g. Physics HSC Board 2024") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_title_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Subject Selector
                Text(
                    text = "Select Academic Subject",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjectsList.take(3).forEach { subject ->
                        val isSelected = selectedSubject == subject
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedSubject = subject }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = subject,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Image Source Selector Box
                Text(
                    text = "Document Image Input",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected question paper",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable {
                                    selectedBitmap = null
                                    selectedImageUri = null
                                    selectedSampleIndex = null
                                }
                        ) {
                            Text("Clear Image", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Add Photo",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select or Capture Exam Paper",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Choose a direct camera frame or pick an image from your device gallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Dual Action Picker Buttons (Gallery vs CameraX direct)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Gallery Picker
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery Upload", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    // Option 2: Direct CameraX
                    Button(
                        onClick = {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                showCameraPreview = true
                            } else {
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Direct Camera", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Preset Question Paper Selection (for rapid testing in virtual environment)
                Text(
                    text = "Or choose an Exam Template to test:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SamplePapers.list.forEachIndexed { index, sample ->
                        val isSelected = selectedSampleIndex == index
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSampleIndex = index
                                    selectedBitmap = generateMockPaperBitmap(sample.title, sample.subject)
                                    selectedImageUri = null
                                    selectedSubject = sample.subject
                                    title = "ScribeEdu - Digitized ${sample.title}"
                                    viewModel.clearError()
                                }
                                .testTag("template_card_$index"),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = sample.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Preset ${sample.subject} containing LaTeX formulas and Bengali text.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Model Selection Dropdown
                Text(
                    text = "Gemini OCR Model",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    modelsList.forEach { (modelId, label) ->
                        val isSelected = selectedModel == modelId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { selectedModel = modelId }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedModel = modelId }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = if (modelId.contains("pro")) "Recommended for high STEM reasoning." else "Better for simple documents.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OCR Error Alert
                if (errorMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Digitizer Notification",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = errorMsg ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            // Elegant Sandbox bypass option for API rate-limiting
                            HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Are you facing API rate limits or lack of keys? You can load a beautifully pre-transcribed high-fidelity OCR template matching your selected subject to test ScribeEdu's full workspaces, editing toolbars, and docx exports instantly!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                                Button(
                                    onClick = {
                                        val sample = com.example.data.SamplePapers.list.firstOrNull { it.subject.equals(selectedSubject, ignoreCase = true) }
                                            ?: com.example.data.SamplePapers.list.first()
                                        viewModel.loadSample(sample)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                        contentColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("sandbox_fallback_button"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load Preloaded $selectedSubject OCR (Bypass Limit)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Digitize Trigger Button
                Button(
                    onClick = {
                        val finalBitmap = selectedBitmap
                        if (finalBitmap != null) {
                            // If user selected a preset sample, we can bypass the network cost or call the real endpoint!
                            // If the API key is missing or not configured, and they picked a template, we load the structured preloaded template instantly.
                            // Otherwise, we send the generated bitmap to the actual API for a real OCR!
                            if (selectedSampleIndex != null && (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY")) {
                                viewModel.loadSample(SamplePapers.list[selectedSampleIndex!!])
                            } else {
                                val docTitle = title.ifBlank { "Digitized $selectedSubject Paper" }
                                viewModel.digitizeCustomPaper(
                                    bitmap = finalBitmap,
                                    title = docTitle,
                                    subject = selectedSubject,
                                    modelName = selectedModel
                                )
                            }
                        }
                    },
                    enabled = selectedBitmap != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_ocr_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start LaTeX OCR Digitization", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Custom High-Fidelity Processing Overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f))
                        .clickable(enabled = false) {}, // absorb clicks
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            
                            Text(
                                text = "ScribeEdu OCR Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = processingStep,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Text(
                                text = "Gemini is performing layout analysis, character recognition, and math translation. Please wait.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                // CameraX Direct Preview Overlay
                if (showCameraPreview) {
                    CameraXCaptureView(
                        onImageCaptured = { bitmap ->
                            selectedBitmap = bitmap
                            selectedImageUri = null
                            selectedSampleIndex = null
                            showCameraPreview = false
                            viewModel.clearError()
                        },
                        onCancel = {
                            showCameraPreview = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Dynamically draws a realistic visual representation of a question paper as a fallback Bitmap
 * to simulate physical OCR input inside the virtual browser emulator perfectly!
 */
fun generateMockPaperBitmap(title: String, subject: String): Bitmap {
    val width = 600
    val height = 800
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Draw white paper background
    canvas.drawColor(android.graphics.Color.WHITE)
    
    val paint = Paint().apply {
        isAntiAlias = true
    }
    
    // Draw margins
    paint.color = android.graphics.Color.argb(40, 244, 67, 54) // Red lines for margin
    canvas.drawLine(50f, 0f, 50f, height.toFloat(), paint)
    canvas.drawLine(0f, 60f, width.toFloat(), 60f, paint)
    
    // Title/Header
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 24f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
    canvas.drawText(title, 80f, 110f, paint)
    
    paint.textSize = 18f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
    canvas.drawText("Subject: $subject | Time: 2h 30m", 80f, 140f, paint)
    
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
    
    // Render horizontal rules
    paint.color = android.graphics.Color.argb(30, 0, 0, 0)
    for (y in 170..750 step 40) {
        canvas.drawLine(50f, y.toFloat(), width.toFloat() - 30f, y.toFloat(), paint)
    }
    
    // Add realistic text and equations drawn on the mockup
    paint.color = android.graphics.Color.BLUE
    paint.textSize = 16f
    canvas.drawText("১. নিচের প্রশ্নগুলোর সমাধান কর:", 80f, 230f, paint)
    canvas.drawText("(ক) মান নির্ণয় কর: ∫ sin(x) / (sin(x) + cos(x)) dx [২]", 100f, 270f, paint)
    canvas.drawText("(খ) পরাবৃত্তের উপকেন্দ্র S(3, -2) হলে সমীকরণটি কি? [৪]", 100f, 310f, paint)
    canvas.drawText("২. নিম্নের উদ্দীপকটি লক্ষ্য কর এবং উত্তর দাও:", 80f, 430f, paint)
    canvas.drawText("(ক) তড়িৎ চালক শক্তি E = 12V এবং r = 1 Ohm। [১]", 100f, 470f, paint)
    
    // Draw custom circuit diagram visual placeholder
    paint.color = android.graphics.Color.DKGRAY
    canvas.drawRect(350f, 450f, 500f, 530f, paint)
    paint.color = android.graphics.Color.WHITE
    canvas.drawRect(352f, 452f, 498f, 528f, paint)
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 12f
    canvas.drawText("[ Circuit Diagram ]", 370f, 495f, paint)
    
    return bitmap
}

@Composable
fun CameraXCaptureView(
    onImageCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    
                    imageCapture = capture
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("CameraXCaptureView", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Circular guidelines overlay to help frame documents
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        )
        
        // Top instruction bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Cancel", tint = Color.White)
            }
            Text(
                "Align Exam Paper inside Frame",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(48.dp)) // balancing back button
        }
        
        // Bottom control shutter bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circle Shutter Button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White)
                    .clickable {
                        val capture = imageCapture ?: return@clickable
                        
                        // Create a temp file to store image
                        val photoFile = File(
                            context.cacheDir,
                            "scribeedu_capture_${System.currentTimeMillis()}.jpg"
                        )
                        
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        
                        capture.takePicture(
                            outputOptions,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                    if (bitmap != null) {
                                        // Post back on the main UI thread
                                        ContextCompat.getMainExecutor(context).execute {
                                            onImageCaptured(bitmap)
                                        }
                                    }
                                }
                                
                                override fun onError(exception: ImageCaptureException) {
                                    android.util.Log.e("CameraXCaptureView", "Capture failed", exception)
                                }
                            }
                        )
                    }
                    .padding(4.dp)
                    .border(4.dp, Color.Black, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
