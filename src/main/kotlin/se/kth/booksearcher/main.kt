package se.kth.booksearcher

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import se.kth.booksearcher.ui.App

fun main() = application {
    // Create the connector
    val esConnector = ElasticsearchConnector()
    
    var connectionStatus by remember { mutableStateOf("Not tested") }
    
    Window(
        onCloseRequest = {
            // Make sure to close the connection when closing the app
            esConnector.close()
            exitApplication()
        },
        title = "BookSearcher",
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Test connection button
            Button(
                onClick = {
                    connectionStatus = if (esConnector.testConnection()) {
                        "Connected successfully to Elasticsearch!"
                    } else {
                        "Failed to connect to Elasticsearch"
                    }
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Test Elasticsearch Connection")
            }
            
            // Display connection status
            Text(
                text = "Connection status: $connectionStatus",
                modifier = Modifier.padding(8.dp)
            )
            
            // Original App UI
            App()
        }
    }
}