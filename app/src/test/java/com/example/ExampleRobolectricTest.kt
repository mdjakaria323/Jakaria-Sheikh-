package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SamplePapers
import com.example.data.model.DigitizedPaper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Use a highly stable target SDK level for Robolectric testing
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ScribeEdu", appName)
    }

    @Test
    fun `verify sample papers loading and converters`() {
        // Retrieve the mathematics test sample
        val mathSample = SamplePapers.list.first { it.subject == "Mathematics" }
        assertNotNull(mathSample)
        assertEquals("Mathematics", mathSample.subject)
        
        // Convert to Room digitized entity
        val digitizedPaper = SamplePapers.toDigitizedPaper(mathSample)
        assertNotNull(digitizedPaper)
        assertEquals("HSC Mathematics 2nd Paper (Board Exam)", digitizedPaper.title)
        assertEquals("Mathematics", digitizedPaper.subject)
        assertNotNull(digitizedPaper.needsVerificationText)
        assertNotNull(digitizedPaper.extractedDiagramsText)
        
        // Assert that LaTeX content is preserved correctly
        assertTrue(digitizedPaper.transcribedText.contains("গণিত দ্বিতীয় পত্র"))
        assertTrue(digitizedPaper.transcribedText.contains("INSERT_DIAGRAM_1"))
    }

    @Test
    fun `verify paper model properties`() {
        val paper = DigitizedPaper(
            id = 42,
            title = "Test Physics Paper",
            subject = "Physics",
            needsVerificationText = "None",
            extractedDiagramsText = "None",
            transcribedText = "Calculus limits: $\\lim_{x \\to 0}$",
            isVerified = true
        )
        
        assertEquals(42, paper.id)
        assertEquals("Test Physics Paper", paper.title)
        assertEquals("Physics", paper.subject)
        assertEquals("None", paper.needsVerificationText)
        assertTrue(paper.isVerified)
        assertTrue(paper.transcribedText.contains("\\lim"))
    }
}
