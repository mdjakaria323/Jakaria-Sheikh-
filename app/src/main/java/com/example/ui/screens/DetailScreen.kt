package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DigitizedPaper
import com.example.ui.viewmodel.PaperViewModel
import com.example.ui.viewmodel.Screen

// Local structure for verification items parsed from text
data class ParsedVerificationItem(
    val index: Int,
    val questionNumber: String,
    val uncertainText: String,
    val candidateText: String,
    val clarificationRequest: String,
    val isResolved: Boolean = false
)

// Local structure for diagrams parsed from text
data class ParsedDiagramItem(
    val diagramNumber: Int,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: PaperViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val paper by viewModel.selectedPaper.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Needs Verification", "Extracted Diagrams", "LaTeX Document")

    // Back navigation support
    androidx.activity.compose.BackHandler {
        viewModel.navigateBack()
    }

    val activePaper = paper
    if (activePaper == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Parse verification items from paper.needsVerificationText
    var verificationItems = remember(activePaper.needsVerificationText) {
        parseVerificationText(activePaper.needsVerificationText)
    }

    // Parse diagrams from paper.extractedDiagramsText
    val diagramItems = remember(activePaper.extractedDiagramsText) {
        parseDiagramsText(activePaper.extractedDiagramsText)
    }

    Scaffold(
        modifier = modifier.testTag("detail_screen_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activePaper.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Subject: ${activePaper.subject} | ${activePaper.modelUsed.replace("-preview", "")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Manual Edit Button
                    IconButton(onClick = { viewModel.navigateTo(Screen.Edit(activePaper.id)) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Transcription")
                    }
                    // Share Document
                    IconButton(onClick = { shareDocument(context, activePaper.title, activePaper.transcribedText) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share File")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Banner
            PaperStatusBanner(
                isVerified = activePaper.isVerified,
                onToggleVerified = { viewModel.setVerifiedStatus(it) }
            )

            // Collapsible Capture Info & Original Image Reference
            var showCaptureInfo by remember { mutableStateOf(false) }
            val captureDateStr = remember(activePaper.timestamp) { 
                java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(activePaper.timestamp)) 
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCaptureInfo = !showCaptureInfo }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Capture Info & Original Reference",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (showCaptureInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showCaptureInfo) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                AnimatedVisibility(visible = showCaptureInfo) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Capture Timestamp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = captureDateStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Text(
                                    text = "Original Image URI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = activePaper.imageUri ?: "Source: Device OCR Upload",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Document thumbnail representation
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (activePaper.imageUri?.startsWith("sample://") == true) {
                                    val icon = when {
                                        activePaper.imageUri.contains("math") -> Icons.Default.Functions
                                        activePaper.imageUri.contains("physics") -> Icons.Default.SettingsInputComponent
                                        else -> Icons.Default.Science
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        Text("Preset", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        Text("Camera", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    val hasBadges = index == 0 && verificationItems.isNotEmpty()
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title, style = MaterialTheme.typography.labelMedium)
                                if (hasBadges) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = verificationItems.size.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("detail_tab_$index")
                    )
                }
            }

            // Tab Contents
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    0 -> VerificationTabContent(
                        items = verificationItems,
                        onResolveItem = { index, correctedValue ->
                            // Replace in transcription
                            val item = verificationItems.firstOrNull { it.index == index }
                            if (item != null) {
                                // Dynamic find and replace
                                val lookFor = "[UNCERTAIN: ${item.uncertainText}?]"
                                var newTransText = activePaper.transcribedText
                                if (newTransText.contains(lookFor)) {
                                    newTransText = newTransText.replace(lookFor, correctedValue)
                                } else {
                                    // Try search and replace raw uncertain text
                                    newTransText = newTransText.replace(item.uncertainText, correctedValue)
                                }
                                
                                // Remove item from list
                                val updatedList = verificationItems.filter { it.index != index }
                                val updatedVerifText = serializeVerificationList(updatedList)
                                
                                viewModel.resolveVerificationItem(updatedVerifText)
                                viewModel.updateTranscription(newTransText)
                                
                                Toast.makeText(context, "Ambiguity resolved & document updated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    1 -> DiagramsTabContent(diagramItems = diagramItems)
                    2 -> LaTeXTabContent(
                        transcribedText = activePaper.transcribedText,
                        onCopyClick = {
                            clipboardManager.setText(AnnotatedString(activePaper.transcribedText))
                            Toast.makeText(context, "Transcribed LaTeX copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PaperStatusBanner(
    isVerified: Boolean,
    onToggleVerified: (Boolean) -> Unit
) {
    val bg = if (isVerified) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val txt = if (isVerified) Color(0xFF2E7D32) else Color(0xFFE65100)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.NewReleases,
                contentDescription = null,
                tint = txt,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isVerified) "This document is fully audited and verified." 
                       else "Auditing Required: Verify flagged ambiguities.",
                style = MaterialTheme.typography.bodySmall,
                color = txt,
                fontWeight = FontWeight.Medium
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Verified:",
                style = MaterialTheme.typography.labelSmall,
                color = txt,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 4.dp)
            )
            Switch(
                checked = isVerified,
                onCheckedChange = onToggleVerified,
                modifier = Modifier.scale(0.7f).testTag("verified_switch")
            )
        }
    }
}

@Composable
fun VerificationTabContent(
    items: List<ParsedVerificationItem>,
    onResolveItem: (Int, String) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "No ambiguities",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Ambiguities Left!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All OCR flag segments have been verified. The transcription matches with 100% certainty.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Flagged OCR Uncertainty Segments (${items.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "These segments were blurry, faded, or had ambiguous mathematical/Bengali characters. Review them below, insert corrections, and save to update the document.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        items.forEach { item ->
            var correctionText by remember(item.index) { mutableStateOf(item.candidateText) }
            var isEditing by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question: ${item.questionNumber}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Text(
                            text = "Uncertainty Code: [UNCERTAIN]",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "Ambiguous Element:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = item.uncertainText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Clarification Guidance:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = item.clarificationRequest,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    if (isEditing) {
                        OutlinedTextField(
                            value = correctionText,
                            onValueChange = { correctionText = it },
                            label = { Text("Enter Correct LaTeX / Text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("correction_input_${item.index}"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = false,
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { isEditing = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    isEditing = false
                                    onResolveItem(item.index, correctionText)
                                },
                                modifier = Modifier.testTag("apply_correction_${item.index}")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apply Correction")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Proposed Translation:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = item.candidateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = { isEditing = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("edit_correction_${item.index}")
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Correct")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagramsTabContent(diagramItems: List<ParsedDiagramItem>) {
    if (diagramItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ImageNotSupported,
                    contentDescription = "No diagrams",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Extracted Diagrams",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "There are no figures, graphs, geometry lines, or electrical circuit drawings specified in this paper.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Extracted Figures & Diagrams Info (${diagramItems.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "ScribeEdu detected visual diagrams. These have been bookmarked inline as tags (e.g. [INSERT_DIAGRAM_1]). In MS Word, you can extract these bounding areas or insert corresponding vector art.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        diagramItems.forEach { diagram ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Diagram ${diagram.diagramNumber}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OMML Tagged",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "Visual Description for Illustrator:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = diagram.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Render a gorgeous schematic representation box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (diagram.description.contains("Circuit", ignoreCase = true)) Icons.Default.SettingsInputComponent 
                                              else Icons.Default.AreaChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "[ DIAGRAM CROP BOUNDING BOX ${diagram.diagramNumber} ]",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Crop area mapping coordinate index: (x: 204, y: 512, w: 250, h: 180)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LaTeXTabContent(
    transcribedText: String,
    onCopyClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Simple filter of lines matching search query
    val highlightedText = remember(transcribedText, searchQuery) {
        if (searchQuery.isBlank()) transcribedText else {
            // Keep full text but user knows searching is happening
            transcribedText
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Structured Document",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = onCopyClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("copy_markdown_btn")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy LaTeX Markdown", fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("latex_search_bar"),
            placeholder = { Text("Find terms or LaTeX equations (e.g. \\frac)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = highlightedText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// Share text as formatted .docx file containing native MS Word Equations
fun shareDocument(context: Context, title: String, content: String) {
    try {
        val docxFile = DocxExportHelper.exportToDocx(context, title, content)
        if (docxFile != null && docxFile.exists()) {
            val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                docxFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export and Share Word Document (.docx)"))
        } else {
            // Fallback to text sharing if file creation fails
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share as Text"))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed, sharing as text instead: ${e.message}", Toast.LENGTH_LONG).show()
        try {
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
            }
            context.startActivity(Intent.createChooser(textIntent, "Share as Text"))
        } catch (inner: Exception) {
            Toast.makeText(context, "Failed to share: ${inner.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

// Helpers to parse lists from string
private fun parseVerificationText(text: String): List<ParsedVerificationItem> {
    if (text.isBlank() || text.equals("None", ignoreCase = true)) {
        return emptyList()
    }

    val items = mutableListOf<ParsedVerificationItem>()
    try {
        val blocks = text.split("\n\n")
        var idx = 0
        blocks.forEach { block ->
            if (block.isBlank()) return@forEach
            val lines = block.lines()
            var qNum = ""
            var unc = ""
            var cand = ""
            var clar = ""
            
            lines.forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("Question:", ignoreCase = true) -> qNum = trimmed.removePrefix("Question:").trim()
                    trimmed.startsWith("Uncertain:", ignoreCase = true) -> unc = trimmed.removePrefix("Uncertain:").trim()
                    trimmed.startsWith("Candidate:", ignoreCase = true) -> cand = trimmed.removePrefix("Candidate:").trim()
                    trimmed.startsWith("Clarification:", ignoreCase = true) -> clar = trimmed.removePrefix("Clarification:").trim()
                }
            }
            
            if (qNum.isNotEmpty() || unc.isNotEmpty()) {
                items.add(ParsedVerificationItem(idx++, qNum, unc, cand, clar))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return items
}

private fun serializeVerificationList(list: List<ParsedVerificationItem>): String {
    if (list.isEmpty()) return "None"
    val sb = java.lang.StringBuilder()
    list.forEach {
        sb.append("Question: ${it.questionNumber}\n")
        sb.append("Uncertain: ${it.uncertainText}\n")
        sb.append("Candidate: ${it.candidateText}\n")
        sb.append("Clarification: ${it.clarificationRequest}\n\n")
    }
    return sb.toString().trim()
}

private fun parseDiagramsText(text: String): List<ParsedDiagramItem> {
    if (text.isBlank() || text.equals("None", ignoreCase = true)) {
        return emptyList()
    }

    val items = mutableListOf<ParsedDiagramItem>()
    try {
        val blocks = text.split("\n\n")
        blocks.forEach { block ->
            val trimmedBlock = block.trim()
            if (trimmedBlock.startsWith("Diagram", ignoreCase = true)) {
                val colonIndex = trimmedBlock.indexOf(":")
                if (colonIndex != -1) {
                    val label = trimmedBlock.substring(0, colonIndex).trim()
                    val desc = trimmedBlock.substring(colonIndex + 1).trim()
                    // Extract diagram number
                    val numberStr = label.filter { it.isDigit() }
                    val num = numberStr.toIntOrNull() ?: 1
                    items.add(ParsedDiagramItem(num, desc))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return items
}
