package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.SamplePaper
import com.example.data.SamplePapers
import com.example.data.api.Content
import com.example.data.api.DigitizerResponse
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.model.DigitizedPaper
import com.example.data.repository.PaperRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

sealed class Screen {
    object Home : Screen()
    object Scan : Screen()
    data class Detail(val paperId: Int) : Screen()
    data class Edit(val paperId: Int) : Screen()
}

class PaperViewModel(private val repository: PaperRepository) : ViewModel() {

    val allPapers: StateFlow<List<DigitizedPaper>> = repository.allPapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val selectedPaper = MutableStateFlow<DigitizedPaper?>(null)
    val isProcessing = MutableStateFlow(false)
    val processingStep = MutableStateFlow("")
    val error = MutableStateFlow<String?>(null)

    // Backstack management for simple custom navigation
    private val navigationBackStack = mutableListOf<Screen>()

    fun navigateTo(screen: Screen) {
        navigationBackStack.add(currentScreen.value)
        currentScreen.value = screen
        if (screen is Screen.Detail) {
            loadPaper(screen.paperId)
        } else if (screen is Screen.Edit) {
            loadPaper(screen.paperId)
        }
    }

    fun navigateBack(): Boolean {
        if (navigationBackStack.isNotEmpty()) {
            val prev = navigationBackStack.removeAt(navigationBackStack.size - 1)
            currentScreen.value = prev
            if (prev is Screen.Detail) {
                loadPaper(prev.paperId)
            } else if (prev is Screen.Edit) {
                loadPaper(prev.paperId)
            } else {
                selectedPaper.value = null
            }
            return true
        }
        return false
    }

    private fun loadPaper(id: Int) {
        viewModelScope.launch {
            val paper = repository.getPaperById(id)
            selectedPaper.value = paper
        }
    }

    fun deletePaper(id: Int) {
        viewModelScope.launch {
            repository.deletePaperById(id)
            if (selectedPaper.value?.id == id) {
                selectedPaper.value = null
            }
            if (currentScreen.value is Screen.Detail && (currentScreen.value as Screen.Detail).paperId == id) {
                currentScreen.value = Screen.Home
            }
        }
    }

    fun savePaper(paper: DigitizedPaper) {
        viewModelScope.launch {
            repository.insertPaper(paper)
            // Reload
            selectedPaper.value = repository.getPaperById(paper.id)
        }
    }

    fun updateTranscription(newText: String) {
        val current = selectedPaper.value ?: return
        val updated = current.copy(transcribedText = newText)
        savePaper(updated)
    }

    fun setVerifiedStatus(isVerified: Boolean) {
        val current = selectedPaper.value ?: return
        val updated = current.copy(isVerified = isVerified)
        savePaper(updated)
    }

    fun resolveVerificationItem(updatedNeedsVerificationText: String) {
        val current = selectedPaper.value ?: return
        val updated = current.copy(needsVerificationText = updatedNeedsVerificationText)
        savePaper(updated)
    }

    fun loadSample(sample: SamplePaper) {
        viewModelScope.launch {
            isProcessing.value = true
            processingStep.value = "Preparing sample paper template..."
            kotlinx.coroutines.delay(600)
            
            processingStep.value = "Inserting into local Room database..."
            val paper = SamplePapers.toDigitizedPaper(sample)
            val newId = repository.insertPaper(paper)
            isProcessing.value = false
            navigateTo(Screen.Detail(newId.toInt()))
        }
    }

    fun digitizeCustomPaper(
        bitmap: Bitmap,
        title: String,
        subject: String,
        modelName: String
    ) {
        viewModelScope.launch {
            isProcessing.value = true
            error.value = null
            
            try {
                processingStep.value = "Converting image to high-fidelity Base64..."
                val base64Image = withContext(Dispatchers.Default) {
                    bitmap.toBase64()
                }

                processingStep.value = "Connecting to Gemini OCR Assistant ($modelName)..."
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw Exception("Gemini API Key is empty or placeholder! Please configure your key in the AI Studio Secrets panel.")
                }

                val prompt = """
                    You are an expert academic OCR assistant and document digitizer specialized in Bengali/English exam papers containing complex mathematical symbols, scientific equations, and technical diagrams.
                    Your core goal is to transcribe the input image of the question paper into highly accurate, structured Markdown with LaTeX, preserving both the exact language characters (including Bangla conjuncts/যুক্তবর্ণ) and the original layout of the document.

                    STRICT RULES:
                    1. BENGALI LANGUAGE INTEGRITY (বাংলা ভাষা ও যুক্তবর্ণ):
                       - Support full Bangla printed/handwritten script transcription. 
                       - Accurately transcribe all Bengali conjunct characters (e.g., ক্ষ, জ্ঞ, শ্র, ত্ত, দ্ধ, চ্ছ, ল্প, ন্দ, ইত্যাদি) exactly as printed.
                       - Maintain Bengali numbers (১, ২, ৩, ক, খ, গ, ঘ) and punctuation exactly.
                       
                    2. EXACT LAYOUT & ZERO OMISSION (হুবহু লেআউট ও কোনো কিছু বাদ না দেওয়া):
                       - Do NOT skip any lines, serial numbers, options, or mark distributions (e.g., [২], [৪], [৫], [Marks: 10]).
                       - Preserve the spacing, line breaks, indentation, and structure of questions and sub-questions exactly as they are laid out in the image.
                       - Transcribe all text as-is. Do NOT summarize, correct grammar, translate, or rewrite.

                    3. MATHEMATICAL EQUATIONS (গণিত ও সমীকরণ):
                       - Convert all inline and block mathematical expressions, fractions, square roots, integrals, matrices, vectors, superscripts, subscripts, and variables into clean LaTeX or standardized Unicode math notation (e.g., ${'$'}E = mc^2${'$'}, ${'$'}\frac{a}{b}${'$'}, ${'$'}\sqrt{x}${'$'}).
                       - Format all math expressions cleanly so they can be parsed into native Microsoft Word Equations (OMML).                       
                    3. DIAGRAMS, SCHEMATICS & FIGURES:
                       - When an image contains a geometry figure, circuit diagram, graph, chart, vector diagram, or any visual illustration, mark its exact location in the transcription text using a clear placeholder tag:
                         `[INSERT_DIAGRAM_X: Description of diagram/figure]` (where X is the sequential diagram number).
                       - Provide a brief description of what the diagram contains so the application can extract/crop that image area for the final document.
                       
                    4. UNCERTAINTY & AMBIGUITY FLAGGING (CRITICAL):
                       - If any word, symbol, number, fraction, handwriting, or formula is blurry, partially cropped, distorted, or ambiguous, DO NOT GUESS OR ASSUME SILENTLY.
                       - Mark the uncertain portion inline as: `[UNCERTAIN: suspected_text?]`.
                       - Create a dedicated section in the JSON response listing all questionable items, their respective question numbers, and your best candidate reading, so the user can be prompted to verify or correct them.

                    You MUST return the output as a valid JSON object matching the following JSON Schema:
                    {
                      "type": "object",
                      "properties": {
                        "needsVerification": {
                          "type": "array",
                          "items": {
                            "type": "object",
                            "properties": {
                              "questionNumber": { "type": "string" },
                              "uncertainText": { "type": "string" },
                              "candidateText": { "type": "string" },
                              "clarificationRequest": { "type": "string" }
                            },
                            "required": ["questionNumber", "uncertainText", "candidateText", "clarificationRequest"]
                          }
                        },
                        "extractedDiagrams": {
                          "type": "array",
                          "items": {
                            "type": "object",
                            "properties": {
                              "diagramNumber": { "type": "integer" },
                              "description": { "type": "string" }
                            },
                            "required": ["diagramNumber", "description"]
                          }
                        },
                        "transcribedText": { "type": "string" }
                      },
                      "required": ["needsVerification", "extractedDiagrams", "transcribedText"]
                    }

                    Respond ONLY with this JSON. Do not wrap it in markdown codeblocks like ```json ... ```, just output raw JSON.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = prompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.1f
                    )
                )

                processingStep.value = "Gemini is performing OCR & structuring LaTeX equations..."
                var response: com.example.data.api.GenerateContentResponse? = null
                var attempt = 0
                val maxAttempts = 3
                var currentDelay = 2000L // 2 seconds initial delay
                
                while (attempt < maxAttempts) {
                    try {
                        response = RetrofitClient.service.generateContent(modelName, apiKey, request)
                        break // Success! Break the loop
                    } catch (httpEx: retrofit2.HttpException) {
                        if (httpEx.code() == 429 && attempt < maxAttempts - 1) {
                            attempt++
                            processingStep.value = "Rate limit hit (429). Retrying in ${currentDelay / 1000}s (Attempt $attempt of $maxAttempts)..."
                            kotlinx.coroutines.delay(currentDelay)
                            currentDelay *= 2 // Exponential backoff
                        } else {
                            throw httpEx // Propagate other errors or if we exceeded max attempts
                        }
                    }
                }
                
                if (response == null) {
                    throw Exception("Failed to receive response from Gemini API after retries.")
                }
                
                processingStep.value = "Parsing transcribed document..."
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("No response received from Gemini API.")

                // Clean the output if the model wrapped it in markdown codeblock
                val jsonString = if (rawText.trim().startsWith("```json")) {
                    rawText.trim().removePrefix("```json").removeSuffix("```").trim()
                } else if (rawText.trim().startsWith("```")) {
                    rawText.trim().removePrefix("```").removeSuffix("```").trim()
                } else {
                    rawText.trim()
                }

                // Attempt to parse structured response
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(DigitizerResponse::class.java)
                
                var finalNeedsVerifText = "None"
                var finalExtDiagText = "None"
                var finalTranscribedText = ""

                try {
                    val parsed = adapter.fromJson(jsonString)
                    if (parsed != null) {
                        finalTranscribedText = parsed.transcribedText
                        
                        // Structure needs verification
                        if (parsed.needsVerification.isNotEmpty()) {
                            val sb = java.lang.StringBuilder()
                            parsed.needsVerification.forEach {
                                sb.append("Question: ${it.questionNumber}\n")
                                sb.append("Uncertain: ${it.uncertainText}\n")
                                sb.append("Candidate: ${it.candidateText}\n")
                                sb.append("Clarification: ${it.clarificationRequest}\n\n")
                            }
                            finalNeedsVerifText = sb.toString().trim()
                        }
                        
                        // Structure diagrams
                        if (parsed.extractedDiagrams.isNotEmpty()) {
                            val sb = java.lang.StringBuilder()
                            parsed.extractedDiagrams.forEach {
                                sb.append("Diagram ${it.diagramNumber}: ${it.description}\n\n")
                            }
                            finalExtDiagText = sb.toString().trim()
                        }
                    } else {
                        throw Exception("Failed to parse JSON schema.")
                    }
                } catch (pe: Exception) {
                    Log.e("PaperViewModel", "JSON parsing failed, falling back to raw output", pe)
                    // Fallback: put raw text in transcription
                    finalTranscribedText = rawText
                    finalNeedsVerifText = "Failed to parse automated verification checks. Please audit transcription manually."
                    finalExtDiagText = "Failed to parse diagrams lists automatically."
                }

                processingStep.value = "Saving to history..."
                val paper = DigitizedPaper(
                    title = title.ifBlank { "Digitized Paper - " + System.currentTimeMillis() },
                    subject = subject,
                    needsVerificationText = finalNeedsVerifText,
                    extractedDiagramsText = finalExtDiagText,
                    transcribedText = finalTranscribedText,
                    modelUsed = modelName,
                    imageUri = null, // Custom scans don't have static drawables
                    isVerified = false
                )

                val newId = repository.insertPaper(paper)
                isProcessing.value = false
                navigateTo(Screen.Detail(newId.toInt()))

            } catch (e: retrofit2.HttpException) {
                Log.e("PaperViewModel", "OCR HTTP Failed", e)
                val friendlyMessage = when (e.code()) {
                    429 -> "Rate limit exceeded (HTTP 429). ScribeEdu is receiving too many requests. Please wait a moment and try again."
                    401, 403 -> "API Key Authentication failed (HTTP ${e.code()}). Please check if your Gemini API Key is entered correctly in the AI Studio Secrets panel."
                    500, 503 -> "Gemini Server error (HTTP ${e.code()}). Google's servers are temporarily busy. Please try again in a few seconds."
                    else -> "Network request failed (HTTP ${e.code()}). Please verify your internet connection."
                }
                error.value = friendlyMessage
                isProcessing.value = false
            } catch (e: Exception) {
                Log.e("PaperViewModel", "OCR Failed", e)
                error.value = e.message ?: "An unknown error occurred during transcription."
                isProcessing.value = false
            }
        }
    }

    fun clearError() {
        error.value = null
    }

    // Helper to extract bitmap from local resource or URI if needed
    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}

fun Bitmap.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

class PaperViewModelFactory(private val repository: PaperRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaperViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaperViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
