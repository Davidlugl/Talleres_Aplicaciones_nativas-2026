package com.example.herbhopper_v1.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.PrescriptionResponse
import com.example.herbhopper_v1.data.network.PrescriptionReviewRequest
import com.example.herbhopper_v1.data.network.apiErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Fórmulas médicas: el paciente las carga (foto, imagen o PDF) y el farmacéutico
 * (vendedor) o el administrador las aprueba o rechaza.
 */
class PrescriptionViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ApiService.instance

    private val _prescriptions = MutableStateFlow<List<PrescriptionResponse>>(emptyList())
    val prescriptions: StateFlow<List<PrescriptionResponse>> = _prescriptions

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    fun refresh() {
        viewModelScope.launch {
            try {
                _prescriptions.value = api.getPrescriptions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Sube la fórmula. [onResult] recibe null si se guardó o el mensaje de error. */
    fun upload(fileUri: Uri, mimeType: String, doctorName: String, doctorRegistry: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                } ?: throw java.io.IOException("No se pudo leer el archivo")
                val extension = if (mimeType == "application/pdf") "pdf" else "jpg"
                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    "formula.$extension",
                    bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                )
                val textType = "text/plain".toMediaTypeOrNull()
                api.uploadPrescription(
                    filePart,
                    doctorName.trim().toRequestBody(textType),
                    doctorRegistry.trim().toRequestBody(textType)
                )
                refresh()
                onResult(null)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage("No se pudo subir la fórmula"))
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun review(id: Int, approve: Boolean, note: String?, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                api.reviewPrescription(id, PrescriptionReviewRequest(if (approve) "APPROVED" else "REJECTED", note))
                refresh()
                onResult(null)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage())
            }
        }
    }
}
