package com.example.appmovil.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmovil.data.remote.MessageItem
import com.example.appmovil.data.remote.UserContact
import com.example.appmovil.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _contacts = MutableStateFlow<List<UserContact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageItem>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _incomingMessageEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val incomingMessageEvent = _incomingMessageEvent.asSharedFlow()

    private val _selectedContact = MutableStateFlow<UserContact?>(null)
    val selectedContact = _selectedContact.asStateFlow()

    private var pollingJob: Job? = null

    // Cargar lista de personas con las que hablar
    fun fetchContacts(miId: Int, miRol: String) {
        viewModelScope.launch {
            _contacts.value = repository.getContacts(miId, miRol)
        }
    }

    // Seleccionar a alguien y empezar a escuchar mensajes
    fun selectContact(contact: UserContact, miId: Int) {
        _selectedContact.value = contact
        _messages.value = emptyList()
        
        // Marcar como leídos inmediatamente
        viewModelScope.launch {
            repository.markMessagesRead(miId, contact.id)
            // Refrescar contactos usando el rol que ya tenemos en el contacto seleccionado (que es el rol de los demas)
            // O mejor, el poller de HomeScreen ya se encarga, pero esto ayuda a la inmediatez
            fetchContacts(miId, if(contact.rol == "satelite") "supervisor" else "satelite") 
        }
        
        startPolling(miId, contact.id)
    }

    // Volver a la lista de contactos
    fun clearSelection() {
        _selectedContact.value = null
        stopPolling()
    }

    private fun startPolling(miId: Int, otroId: Int) {
        stopPolling()
        pollingJob = viewModelScope.launch {
            while (true) {
                // Si estamos en un chat específico, marcamos como leídos periódicamente también
                repository.markMessagesRead(miId, otroId)
                
                val newMessages = repository.getMessages(miId, otroId)
                
                // Si hay mensajes nuevos
                if (newMessages.size > _messages.value.size) {
                    val lastMessage = newMessages.lastOrNull()
                    // Si el último mensaje es del otro, disparamos evento de sonido
                    if (lastMessage != null && !lastMessage.esMio) {
                        _incomingMessageEvent.emit(Unit)
                    }
                    _messages.value = newMessages
                } else if (newMessages != _messages.value) {
                    _messages.value = newMessages
                }
                
                delay(5000) // Cada 5 segundos
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun sendMessage(miId: Int, receptorId: Int, texto: String) {
        if (texto.isBlank()) return
        viewModelScope.launch {
            val success = repository.sendPrivateMessage(miId, receptorId, texto)
            if (success) {
                // Forzar actualización inmediata
                val updated = repository.getMessages(miId, receptorId)
                _messages.value = updated
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
