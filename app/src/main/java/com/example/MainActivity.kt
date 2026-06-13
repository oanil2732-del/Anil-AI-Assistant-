package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        ChatScreen()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
  val messages by viewModel.messages.collectAsStateWithLifecycle()
  val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
  var currentInput by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  // Scroll to bottom when messages change
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
            title = { Text("AI Assistant") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
      }
  ) { innerPadding ->
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
      LazyColumn(
          state = listState,
          modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
          contentPadding = PaddingValues(vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(messages, key = { it.id }) { msg ->
          ChatBubble(message = msg)
        }
        if (isLoading) {
          item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
          }
        }
      }

      Row(
          modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
            value = currentInput,
            onValueChange = { currentInput = it },
            modifier = Modifier
                .weight(1f)
                .testTag("chat_input"),
            placeholder = { Text("Type a message...") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = 4,
            shape = RoundedCornerShape(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        FloatingActionButton(
            onClick = {
              if (currentInput.isNotBlank()) {
                val text = currentInput
                currentInput = ""
                viewModel.sendMessage(text)
              }
            },
            modifier = Modifier.testTag("send_button"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send Message"
          )
        }
      }
    }
  }
}

@Composable
fun ChatBubble(message: ChatMessage) {
  val backgroundColor = when {
    message.isError -> MaterialTheme.colorScheme.errorContainer
    message.isUser -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.secondaryContainer
  }
  
  val textColor = when {
    message.isError -> MaterialTheme.colorScheme.onErrorContainer
    message.isUser -> MaterialTheme.colorScheme.onPrimaryContainer
    else -> MaterialTheme.colorScheme.onSecondaryContainer
  }

  val shape = when {
    message.isUser -> RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    else -> RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
  }

  Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
  ) {
    Surface(
        color = backgroundColor,
        shape = shape,
        modifier = Modifier.widthIn(max = 280.dp)
    ) {
      Text(
          text = message.text,
          modifier = Modifier.padding(12.dp),
          color = textColor,
          style = MaterialTheme.typography.bodyLarge
      )
    }
  }
}
