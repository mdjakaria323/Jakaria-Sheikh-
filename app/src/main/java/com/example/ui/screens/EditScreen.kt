package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.PaperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: PaperViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val paper by viewModel.selectedPaper.collectAsState()
    
    val activePaper = paper
    if (activePaper == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Use TextFieldValue to track selection and cursor position for toolbar insertion
    var textFieldValue by remember(activePaper.id) {
        mutableStateOf(TextFieldValue(activePaper.transcribedText))
    }
    
    var showUnsavedDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = textFieldValue.text != activePaper.transcribedText

    // Intercept back actions
    androidx.activity.compose.BackHandler {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            viewModel.navigateBack()
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have made edits to the transcription. Do you want to discard your changes and go back?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        viewModel.navigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.testTag("edit_screen_scaffold"),
        topBar = {
            TopAppBar(
                title = { Text("Edit LaTeX Transcription", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasUnsavedChanges) {
                                showUnsavedDialog = true
                            } else {
                                viewModel.navigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.updateTranscription(textFieldValue.text)
                            Toast.makeText(context, "Transcription saved successfully!", Toast.LENGTH_SHORT).show()
                            viewModel.navigateBack()
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("save_trans_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Toolbar title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Insert LaTeX & Formatting Templates",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // LaTeX Helper Formatting Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Formatting Shortcut Button
                ToolbarButton(label = "Fraction", formula = "\\frac{a}{b}") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Sqrt", formula = "\\sqrt{x}") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Integral", formula = "\\int_{a}^{b} f(x) dx") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Limit", formula = "\\lim_{x \\to 0}") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Sum", formula = "\\sum_{i=1}^{n}") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Vector", formula = "\\vec{A}") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Theta", formula = "\\theta") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Alpha", formula = "\\alpha") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Beta", formula = "\\beta") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Omega", formula = "\\Omega") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Diagram Tag", formula = "[INSERT_DIAGRAM_X: Description]") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Uncertainty Tag", formula = "[UNCERTAIN: text?]") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Bold", formula = "**bold_text**") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
                ToolbarButton(label = "Sub-Heading", formula = "### ") {
                    insertAtCursor(it, textFieldValue) { updated -> textFieldValue = updated }
                }
            }

            // Main Editor Field
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .testTag("editor_text_field"),
                placeholder = { Text("Transcribe question paper here using LaTeX and Markdown...") },
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 22.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
fun ToolbarButton(
    label: String,
    formula: String,
    onClick: (String) -> Unit
) {
    AssistChip(
        onClick = { onClick(formula) },
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("toolbar_$label")
    )
}

// Inserts text at the current cursor position or replaces selected text safely
private fun insertAtCursor(
    textToInsert: String,
    currentValue: TextFieldValue,
    onValueUpdated: (TextFieldValue) -> Unit
) {
    val selection = currentValue.selection
    val text = currentValue.text
    
    val before = text.substring(0, selection.start)
    val after = text.substring(selection.end)
    
    val newText = before + textToInsert + after
    val newSelectionIndex = selection.start + textToInsert.length
    
    onValueUpdated(
        TextFieldValue(
            text = newText,
            selection = TextRange(newSelectionIndex, newSelectionIndex)
        )
    )
}
