package com.example.data

import com.example.data.api.VerificationItem
import com.example.data.api.DiagramItem
import com.example.data.model.DigitizedPaper

private const val d = "$" // Constant to represent literal dollar sign safely in Kotlin templates

data class SamplePaper(
    val title: String,
    val subject: String,
    val description: String,
    val needsVerification: List<VerificationItem>,
    val extractedDiagrams: List<DiagramItem>,
    val transcribedText: String,
    val imageResName: String, // String ID for drawing or mock visual
    val infoText: String
)

object SamplePapers {
    val list = listOf(
        SamplePaper(
            title = "HSC Mathematics 2nd Paper (Board Exam)",
            subject = "Mathematics",
            description = "Algebra, Calculus, Geometry with complex equations & Bengali text.",
            infoText = "Contains Bengali stem text, matrices, integrals \\\\int \\\\sin(x) dx, and square root expressions.",
            needsVerification = listOf(
                VerificationItem(
                    questionNumber = "২ (ক)",
                    uncertainText = "${d}\\int_{0}^{\\pi/2}${d}",
                    candidateText = "${d}\\int_{0}^{\\pi/2} \\\\frac{\\\\sin x}{\\\\sin x + \\\\cos x} dx${d}",
                    clarificationRequest = "Limit value is slightly blurry. Please check if the upper limit is ${d}\\pi/2${d} or ${d}\\pi/4${d}."
                ),
                VerificationItem(
                    questionNumber = "৪ (খ)",
                    uncertainText = "${d}y^2 = 4ax${d}",
                    candidateText = "${d}y^2 = 16x${d}",
                    clarificationRequest = "Co-efficient value is faded. Verify if the parabola equation is ${d}y^2 = 16x${d} or ${d}y^2 = 12x${d}."
                )
            ),
            extractedDiagrams = listOf(
                DiagramItem(
                    diagramNumber = 1,
                    description = "Figure showing a parabola ${d}y^2 = 16x${d} with its focus S and directrix MZ."
                ),
                DiagramItem(
                    diagramNumber = 2,
                    description = "A coordinate plane representing the bounded area under the curve ${d}y = x^2${d} and the line ${d}y = 4${d}."
                )
            ),
            transcribedText = """## উচ্চ মাধ্যমিক পরীক্ষা ২০২৫
### গণিত দ্বিতীয় পত্র (সৃজনশীল)
সময়: ২ ঘণ্টা ৩০ মিনিট | পূর্ণমান: ৫০

**[দ্রষ্টব্য: ডান পাশের সংখ্যা প্রশ্নের পূর্ণমান জ্ঞাপন করে। যেকোনো পাঁচটি প্রশ্নের উত্তর দাও।]**

---

#### ১. দৃশ্যকল্প-১: একটি পরাবৃত্তের সমীকরণ ${d}y^2 - 4y - 8x + 12 = 0${d}
#### দৃশ্যকল্প-২: একটি পরাবৃত্তের উপকেন্দ্র ${d}S(3, -2)${d} এবং দ্বিকাক্ষের সমীকরণ ${d}x - y + 5 = 0${d}

(ক) দৃশ্যকল্প-১ এর পরাবৃত্তটির শীর্ষবিন্দুর স্থানাঙ্ক নির্ণয় কর। [২]
(খ) দৃশ্যকল্প-২ হতে পরাবৃত্তটির সমীকরণ নির্ণয় কর। [৪]
(গ) দৃশ্যকল্প-১ হতে পরাবৃত্তটির উপকেন্দ্রিক লম্বের দৈর্ঘ্য এবং দ্বিকাক্ষের সমীকরণ নির্ণয় কর। [৪]

---

#### ২. ক বিভাগ (বীজগণিত ও ত্রিকোণমিতি)
(ক) দেখাও যে, ${d}\cos\left(2 \tan^{-1}\frac{1}{5}\right) = \sin\left(4 \tan^{-1}\frac{1}{3}\right)${d} [২]
(খ) যদি ${d}\tan^{-1} x + \tan^{-1} y + \tan^{-1} z = \pi${d} হয়, তবে প্রমাণ কর যে, ${d}x + y + z = xyz${d} [৪]
(গ) সমাধান কর: ${d}\sin \theta + \cos \theta = \sqrt{2}${d}, যখন ${d}0 \le \theta \le 2\pi${d}. [৪]

---

#### ৩. খ বিভাগ (ক্যালকুলাস)
(ক) মান নির্ণয় কর: ${d}\lim_{x \to 0} \frac{1 - \cos 5x}{x^2}${d} [২]
(খ) ${d}x${d} এর সাপেক্ষে অন্তরীকরণ কর: ${d}\ln(\sin x^2)${d} [৪]
(গ) মান নির্ণয় কর: 
[INSERT_DIAGRAM_1: Coordinate area representation]
${d}\int_{0}^{\pi/2} \frac{\sin x}{\sin x + \cos x} dx${d} [৪]

---

#### ৪. গ বিভাগ (স্থিতিবিদ্যা ও গতিবিদ্যা)
(ক) ${d}P${d} এবং ${d}2P${d} মানের দুটি বলের লব্ধি ${d}3P${d}। বল দুটির মধ্যবর্তী কোণ কত? [২]
(খ) কোন কণার উপর ক্রিয়ারত ${d}P${d} এবং ${d}Q${d} বলদ্বয়ের মধ্যবর্তী কোণ ${d}\alpha${d}। এদের লব্ধি ${d}R${d}, ${d}P${d} এর ক্রিয়ারেখার সাথে ${d}30^\circ${d} কোণ উৎপন্ন করে। প্রমাণ কর যে, ${d}Q = R${d}. [৪]
[INSERT_DIAGRAM_2: Force Vector Diagram]
(গ) একটি কণা ${d}u${d} আদিবেগে অণুভূমিকের সাথে ${d}\alpha${d} কোণে নিক্ষিপ্ত হলো। প্রমাণ কর যে, কণাটির সর্বোচ্চ উচ্চতা ${d}H = \frac{u^2 \sin^2 \alpha}{2g}${d} [৪]
""".trimIndent(),
            imageResName = "math_sample"
        ),
        SamplePaper(
            title = "HSC Physics 1st Paper (Circuit & Vector)",
            subject = "Physics",
            description = "Electricity, Vector mechanics, diagrams with circuit symbols and math formulas.",
            infoText = "Contains circuit diagram with resistors, ohm value, capacitor, vector cross-product \\\\vec{A} \\\\times \\\\vec{B}.",
            needsVerification = listOf(
                VerificationItem(
                    questionNumber = "২ (খ)",
                    uncertainText = "R = 10 ${d}\\Omega${d}",
                    candidateText = "${d}R = 10 \\\\Omega${d}",
                    clarificationRequest = "Resistor R2 value is partially smudged. Confirm if it is ${d}10 \\\\Omega${d} or ${d}18 \\\\Omega${d}."
                ),
                VerificationItem(
                    questionNumber = "৩ (গ)",
                    uncertainText = "v = 15 m/s",
                    candidateText = "${d}v = 15 \\\\text{ m/s}${d}",
                    clarificationRequest = "Velocity value of the swimmer is blurry. Verify if the speed is ${d}15 \\\\text{ m/s}${d} or ${d}12 \\\\text{ m/s}${d}."
                )
            ),
            extractedDiagrams = listOf(
                DiagramItem(
                    diagramNumber = 1,
                    description = "Electrical circuit diagram showing three resistors R1, R2, R3 in parallel-series combination connected to a 12V battery and a switch."
                ),
                DiagramItem(
                    diagramNumber = 2,
                    description = "A vector diagram showing two forces F1 and F2 acting on a point body at an angle of 120 degrees."
                )
            ),
            transcribedText = """## উচ্চ মাধ্যমিক পরীক্ষা ২০২৫
### পদার্থবিজ্ঞান প্রথম পত্র (সৃজনশীল)
সময়: ২ ঘণ্টা ৩০ মিনিট | পূর্ণমান: ৫০

---

#### ১. উদ্দীপক-১: ২ কেজি ভরের একটি বস্তুকে ২০ মিটার উচ্চতা থেকে ছেড়ে দেওয়া হলো।
#### উদ্দীপক-২:
[INSERT_DIAGRAM_1: Electrical Circuit Diagram]
চিত্রে প্রদর্শিত বর্তনীতে ব্যাটারির তড়িৎচালক শক্তি ${d}E = 12\text{ V}${d} এবং অভ্যন্তরীণ রোধ ${d}r = 1 \Omega${d}।

(ক) শক্তির সংরক্ষণশীলতা নীতিটি বিবৃত কর। [১]
(খ) কোনো বস্তুর মুক্তিবেগ এর ভরের ওপর নির্ভর করে না কেন? ব্যাখ্যা কর। [২]
(গ) উদ্দীপক-১ অনুযায়ী, বস্তুটির পতনের কত সেকেন্ড পর গতিশক্তি বিভবশক্তির ৩ গুণ হবে? [৩]
(গ) উদ্দীপক-২ অনুযায়ী, বর্তনীর তুল্য রোধ এবং তড়িৎ প্রবাহের মান নির্ণয় কর। [৪]

---

#### ২. উদ্দীপক: দুটি ভেক্টর রাশি যথাক্রমে:
${d}\vec{A} = 2\hat{i} + 3\hat{j} - \hat{k}${d}
${d}\vec{B} = \hat{i} - \hat{j} + 2\hat{k}${d}

(ক) ডট গুণন ও ক্রস গুণনের মধ্যে পার্থক্য কী? [১]
(খ) দেখাও যে, ভেক্টরদ্বয় পরস্পর লম্ব নয়। [২]
(গ) ভেক্টর ${d}\vec{A}${d} এবং ${d}\vec{B}${d} দ্বারা গঠিত সামান্তরিকের ক্ষেত্রফল নির্ণয় কর। [৩]
(ঘ) একটি ভেক্টর ${d}\vec{C} = m\hat{i} + 2\hat{j} + \hat{k}${d} হলে, ${d}m${d} এর মান কত হলে ভেক্টর ${d}\vec{A}${d} এবং ${d}\vec{C}${d} পরস্পর লম্ব হবে? গাণিতিকভাবে বিশ্লেষণ কর। [৪]

---

#### ৩. উদ্দীপক: একটি নদী পারাপারের ক্ষেত্রে একজন সাঁতারু নদী পার হতে সোজা যাত্রা শুরু করে।
[INSERT_DIAGRAM_2: Vector Flow Diagram]
নদীর প্রস্থ ${d}d = 500\text{ m}${d}। স্রোতের বেগ ${d}v = 3\text{ m/s}${d} এবং সাঁতারুর বেগ ${d}u = 6\text{ m/s}${d}।

(ক) স্রোতের অনূকূলে নৌকার বেগ বলতে কী বোঝায়? [১]
(খ) কেন নদী পার হওয়ার জন্য লম্বভাবে রওনা দিলে সময় কম লাগে? [২]
(গ) সাঁতারুটি নদী পার হতে কত সময় নেবে? [৩]
(ঘ) সাঁতারুটি যদি নদীর ঠিক বিপরীত বিন্দুতে পৌঁছাতে চায়, তবে তাকে কত কোণে রওনা দিতে হবে? গাণিতিক বিশ্লেষণ দাও। [৪]
""".trimIndent(),
            imageResName = "physics_sample"
        ),
        SamplePaper(
            title = "HSC Chemistry Midterm Exam",
            subject = "Chemistry",
            description = "Organic structures, equations, chemical symbols with formulas.",
            infoText = "Contains chemical equations, Benzene ring drawings, pH calculations.",
            needsVerification = listOf(
                VerificationItem(
                    questionNumber = "১ (গ)",
                    uncertainText = "${d}0.1\\\\text{ M}${d}",
                    candidateText = "${d}0.1\\\\text{ M}${d}",
                    clarificationRequest = "Acid concentration value is blurry. Verify if it is ${d}0.1\\\\text{ M}${d} or ${d}0.05\\\\text{ M}${d}."
                )
            ),
            extractedDiagrams = listOf(
                DiagramItem(
                    diagramNumber = 1,
                    description = "Chemical structure diagram showing the conversion of Benzene to Nitrobenzene in presence of concentrated Nitric acid and Sulphuric acid."
                )
            ),
            transcribedText = """## অর্ধ-বার্ষিক পরীক্ষা ২০২৫
### রসায়ন দ্বিতীয় পত্র
সময়: ২ ঘণ্টা ৩০ মিনিট | পূর্ণমান: ৫০

---

#### ১. উদ্দীপক-১: পাত্র-এ তে রয়েছে ${d}50\text{ mL } 0.1\text{ M } HCl${d} দ্রবণ এবং পাত্র-বি তে রয়েছে ${d}100\text{ mL } 0.05\text{ M } NaOH${d} দ্রবণ।
#### উদ্দীপক-২:
[INSERT_DIAGRAM_1: Organic Chemistry Conversion]
${d}\text{Benzene (C}_6\text{H}_6) \xrightarrow{\text{HNO}_3 / \text{H}_2\text{SO}_4} A \xrightarrow{\text{Sn / HCl}} B${d}

(ক) বাফার দ্রবণ কাকে বলে? [১]
(খ) ${d}HCl${d} একটি তীব্র এসিড কিন্তু ${d}CH_3COOH${d} একটি দুর্বল এসিড কেন? ব্যাখ্যা কর। [২]
(গ) উদ্দীপক-১ এর পাত্র-এ এর দ্রবণের pH এবং pOH কত? [৩]
(ঘ) উদ্দীপক-২ এর রাসায়নিক বিক্রিয়া সম্পন্ন করে ${d}A${d} এবং ${d}B${d} জৈব যৌগ দুটির গাঠনিক সংকেত ও নাম সনাক্ত কর। [৪]

---

#### ২. উদ্দীপক: হাইড্রোজেন পরমাণুর বোর মডেল সম্পর্কিত তথ্য।
(ক) প্লাঙ্কের ধ্রুবকের মান কত? [১]
(খ) হাইজেনবার্গের অনিশ্চয়তা নীতিটি ব্যাখ্যা কর। [২]
(গ) বোর মডেল অনুসারে একটি ইলেকট্রন ৩য় কক্ষপথে থাকলে তার কৌণিক ভরবেগ কত? [৩]
(ঘ) ইলেকট্রনটি ৩য় কক্ষপথ হতে ২য় কক্ষপথে লাফিয়ে পড়লে যে শক্তি বিকিরিত হয়, তার কম্পাঙ্ক এবং তরঙ্গদৈর্ঘ্য নির্ণয় কর। [৪]
""".trimIndent(),
            imageResName = "chemistry_sample"
        )
    )

    fun toDigitizedPaper(sample: SamplePaper): DigitizedPaper {
        // Construct the NEEDS_VERIFICATION section
        val needsVerifStr = if (sample.needsVerification.isEmpty()) "None" else {
            val sb = java.lang.StringBuilder()
            sample.needsVerification.forEach {
                sb.append("Question: ${it.questionNumber}\n")
                sb.append("Uncertain: ${it.uncertainText}\n")
                sb.append("Candidate: ${it.candidateText}\n")
                sb.append("Clarification: ${it.clarificationRequest}\n\n")
            }
            sb.toString().trim()
        }

        // Construct the EXTRACTED_DIAGRAMS section
        val extDiagStr = if (sample.extractedDiagrams.isEmpty()) "None" else {
            val sb = java.lang.StringBuilder()
            sample.extractedDiagrams.forEach {
                sb.append("Diagram ${it.diagramNumber}: ${it.description}\n\n")
            }
            sb.toString().trim()
        }

        return DigitizedPaper(
            title = sample.title,
            subject = sample.subject,
            needsVerificationText = needsVerifStr,
            extractedDiagramsText = extDiagStr,
            transcribedText = sample.transcribedText,
            modelUsed = "gemini-3.1-pro-preview",
            imageUri = "sample://" + sample.imageResName,
            isVerified = false
        )
    }
}
