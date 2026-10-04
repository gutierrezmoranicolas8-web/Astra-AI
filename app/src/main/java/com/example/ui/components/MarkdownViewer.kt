package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownViewer(
    markdownText: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val sections = parseMarkdownSections(markdownText)

    Column(modifier = modifier) {
        for (sec in sections) {
            when (sec) {
                is MarkdownSection.Header -> {
                    val fontSize = when (sec.level) {
                        1 -> 20.sp
                        2 -> 18.sp
                        else -> 16.sp
                    }
                    Text(
                        text = sec.text,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                is MarkdownSection.CodeBlock -> {
                    CodeBlockCard(
                        code = sec.code,
                        language = sec.language,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Código", sec.code))
                            Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is MarkdownSection.BulletPoint -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "•",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = formatInlineMarkdown(sec.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
                is MarkdownSection.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(sec.text),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    code: String,
    language: String,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A), // Dark slate editor background
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "código" }.uppercase(),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar código",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFF38BDF8), // Electric cyan
                    lineHeight = 18.sp
                )
            }
        }
    }
}

sealed class MarkdownSection {
    data class Header(val text: String, val level: Int) : MarkdownSection()
    data class Paragraph(val text: String) : MarkdownSection()
    data class BulletPoint(val text: String) : MarkdownSection()
    data class CodeBlock(val code: String, val language: String) : MarkdownSection()
}

fun parseMarkdownSections(markdown: String): List<MarkdownSection> {
    val sections = mutableListOf<MarkdownSection>()
    val lines = markdown.lines()
    var inCodeBlock = false
    val codeBuilder = StringBuilder()
    var codeLang = ""

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                sections.add(MarkdownSection.CodeBlock(codeBuilder.toString().trimEnd(), codeLang))
                codeBuilder.clear()
                codeLang = ""
                inCodeBlock = false
            } else {
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeBuilder.append(line).append("\n")
            continue
        }

        if (trimmed.startsWith("### ")) {
            sections.add(MarkdownSection.Header(trimmed.removePrefix("### "), 3))
        } else if (trimmed.startsWith("## ")) {
            sections.add(MarkdownSection.Header(trimmed.removePrefix("## "), 2))
        } else if (trimmed.startsWith("# ")) {
            sections.add(MarkdownSection.Header(trimmed.removePrefix("# "), 1))
        } else if (trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            val bulletText = trimmed.substring(2)
            sections.add(MarkdownSection.BulletPoint(bulletText))
        } else if (trimmed.isNotBlank()) {
            sections.add(MarkdownSection.Paragraph(trimmed))
        }
    }

    if (inCodeBlock) {
        sections.add(MarkdownSection.CodeBlock(codeBuilder.toString().trimEnd(), codeLang))
    }

    return sections
}

fun formatInlineMarkdown(text: String) = buildAnnotatedString {
    var currentIndex = 0
    val regex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`)")
    val matches = regex.findAll(text)

    for (match in matches) {
        val start = match.range.first
        val end = match.range.last + 1
        if (start > currentIndex) {
            append(text.substring(currentIndex, start))
        }
        val matchValue = match.value
        when {
            matchValue.startsWith("**") && matchValue.endsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(matchValue.removeSurrounding("**"))
                }
            }
            matchValue.startsWith("*") && matchValue.endsWith("*") -> {
                withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(matchValue.removeSurrounding("*"))
                }
            }
            matchValue.startsWith("`") && matchValue.endsWith("`") -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x33888888),
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(" " + matchValue.removeSurrounding("`") + " ")
                }
            }
            else -> append(matchValue)
        }
        currentIndex = end
    }
    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}
