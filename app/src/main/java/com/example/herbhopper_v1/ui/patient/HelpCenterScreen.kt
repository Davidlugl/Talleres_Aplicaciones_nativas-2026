package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import com.example.herbhopper_v1.R

/**
 * Pantalla del Centro de Ayuda para el paciente.
 * Presenta una lista de Preguntas Frecuentes (FAQ) y proporciona canales directos de soporte
 * como chat en vivo (Live Chat) y envío de correo electrónico a soporte técnico.
 *
 * @param onBack Función de retorno (callback) que se ejecuta al presionar el botón de regresar.
 * @param onChatClick Función de retorno (callback) que se ejecuta al presionar el botón de Chat en Vivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(onBack: () -> Unit, onChatClick: () -> Unit = {}) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.help_center), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            item {
                Text(
                    stringResource(id = R.string.faq_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            val faqs = listOf(
                R.string.faq_q1 to R.string.faq_a1,
                R.string.faq_q2 to R.string.faq_a2,
                R.string.faq_q3 to R.string.faq_a3,
                R.string.faq_q4 to R.string.faq_a4
            )

            items(faqs) { (questionRes, answerRes) ->
                FaqItem(
                    question = stringResource(id = questionRes),
                    answer = stringResource(id = answerRes)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    stringResource(id = R.string.need_more_help),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onChatClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Chat, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.live_chat_btn))
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:soporte@herbhopper.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Soporte Técnico")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No hay cliente de correo instalado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Email, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.send_email_btn))
                }
            }
        }
    }
}

/**
 * Componente visual que representa un elemento individual de Pregunta Frecuente (FAQ).
 * Muestra la pregunta en un formato destacado y la respuesta descriptiva dentro de una tarjeta con bordes redondeados.
 *
 * @param question El texto de la pregunta a mostrar.
 * @param answer El texto de la respuesta correspondiente.
 */
@Composable
fun FaqItem(question: String, answer: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(question, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
