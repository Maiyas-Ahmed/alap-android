package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatMessage
import com.example.ui.theme.LocalAlapColors

@Composable
fun QuizMessageContent(
    msg: ChatMessage,
    currentUserId: String,
    textColor: Color,
    onSelectOption: (Int) -> Unit
) {
    val extended = LocalAlapColors.current
    val options = msg.pollOptions.orEmpty()
    val votes = msg.pollVotes.orEmpty()
    val myVote = (votes[currentUserId] as? Number)?.toInt()
    val correctIndex = msg.correctOptionIndex?.toInt()
    val hasAnswered = myVote != null
    val isCorrectAnswer = hasAnswered && correctIndex != null && myVote == correctIndex

    val correctGreen = Color(0xFF157F3D)
    val wrongRed = MaterialTheme.colorScheme.error

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Quiz,
                        contentDescription = "MCQ Quiz",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "MCQ QUIZ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (hasAnswered && correctIndex != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCorrectAnswer) correctGreen.copy(alpha = 0.14f) else wrongRed.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isCorrectAnswer) "Correct!" else "Incorrect",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrectAnswer) correctGreen else wrongRed,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Text(
            text = msg.pollQuestion ?: msg.text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        options.forEachIndexed { idx, optLabel ->
            val count = votes.values.count { (it as? Number)?.toInt() == idx }
            val isMyPick = myVote == idx
            val isRightOption = correctIndex != null && idx == correctIndex

            val containerColor = when {
                !hasAnswered -> Color.White.copy(alpha = 0.85f)
                isRightOption -> correctGreen.copy(alpha = 0.14f)
                isMyPick -> wrongRed.copy(alpha = 0.12f)
                else -> Color.White.copy(alpha = 0.65f)
            }
            val borderColor = when {
                !hasAnswered -> MaterialTheme.colorScheme.outline
                isRightOption -> correctGreen
                isMyPick -> wrongRed
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = containerColor,
                border = BorderStroke(width = if (isMyPick || (hasAnswered && isRightOption)) 1.5.dp else 1.dp, color = borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectOption(idx) }
                    .testTag("quiz_option_$idx")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (hasAnswered && (isRightOption || isMyPick)) {
                            Icon(
                                imageVector = if (isRightOption) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = if (isRightOption) "Correct option" else "Wrong selection",
                                tint = if (isRightOption) correctGreen else wrongRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = optLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isMyPick || (hasAnswered && isRightOption)) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (hasAnswered) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRightOption) correctGreen else extended.mutedText
                        )
                    }
                }
            }
        }

        if (hasAnswered && !msg.quizExplanation.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Explanation: ${msg.quizExplanation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Text(
            text = "${votes.size} response(s) • ${if (hasAnswered) "Tap to switch answer" else "Tap an option to answer"}",
            style = MaterialTheme.typography.labelSmall,
            color = extended.mutedText
        )
    }
}

@Composable
fun FlashcardMessageContent(
    msg: ChatMessage,
    textColor: Color
) {
    val extended = LocalAlapColors.current
    var flipped by rememberSaveable(msg.id) { mutableStateOf(false) }
    val frontText = msg.flashcardFront.orEmpty().ifBlank { msg.text }
    val backText = msg.flashcardBack.orEmpty().ifBlank { "No answer provided" }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (flipped) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.9f),
            border = BorderStroke(
                width = 1.dp,
                color = if (flipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { flipped = !flipped }
                .testTag("flashcard_card_${msg.id}")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Style,
                            contentDescription = "Flashcard",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (flipped) "FLASHCARD • ANSWER" else "FLASHCARD • QUESTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Flip,
                        contentDescription = "Flip flashcard",
                        tint = extended.mutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (flipped) backText else frontText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (flipped) FontWeight.Medium else FontWeight.Bold,
                    color = if (flipped) MaterialTheme.colorScheme.onPrimaryContainer else textColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (flipped) "Tap card to see question" else "Tap card to flip offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.mutedText
                )
            }
        }
    }
}
