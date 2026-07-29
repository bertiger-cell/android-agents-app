package com.agents.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun IntroScreen(onFinished: () -> Unit) {
    // Agent walks from left to right
    val infiniteTransition = rememberInfiniteTransition(label = "walk")
    val walkProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "walkProgress"
    )

    // Bobbing up and down while walking
    val bob by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    // Thought bubble fade-in after a delay
    var showBubble by remember { mutableStateOf(false) }
    var quoteIndex by remember { mutableStateOf(0) }

    val quotes = listOf(
        "Wo sind wir stehen geblieben?",
        "Ich hab euch vermisst!",
        "Bereit fuer neue Abenteuer?",
        " Hoffentlich die API-Keys nicht vergessen...",
        "Lade KI-Intelligenz... 42%... noch Geduld...",
        "Zeit was Cooles zu bauen!"
    )

    LaunchedEffect(Unit) {
        delay(800)
        showBubble = true
        while (true) {
            quoteIndex = (quoteIndex + 1) % quotes.size
            delay(2500)
        }
    }

    // Navigate away after full walk
    LaunchedEffect(Unit) {
        delay(4200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Thought bubble
        if (showBubble) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 60.dp)
                    .alpha(animateFloatAsState(
                        targetValue = if (showBubble) 1f else 0f,
                        animationSpec = tween(600),
                        label = "bubbleAlpha"
                    ).value)
            ) {
                ThoughtBubble(text = quotes[quoteIndex])
            }
        }

        // Agent figure
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(
                    x = (walkProgress * LocalConfiguration.current.screenWidthDp.dp.value).dp - 40.dp,
                    y = (100 + bob).dp
                ),
            contentAlignment = Alignment.Center
        ) {
            AgentFigure()
        }

        // App title at bottom
        Text(
            text = "Android Agents",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .alpha(animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(1000, delayMillis = 500),
                    label = "titleAlpha"
                ).value)
        )
    }
}

@Composable
private fun AgentFigure() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Head
        Text(
            text = "\uD83E\uDD16", // Robot emoji
            fontSize = 48.sp
        )
    }
}

@Composable
private fun ThoughtBubble(text: String) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = surfaceColor,
            tonalElevation = 8.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
        // Bubble tail (small triangle)
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            color = surfaceColor,
            modifier = Modifier
                .size(12.dp)
                .offset(y = (-4).dp)
        ) {}
    }
}
