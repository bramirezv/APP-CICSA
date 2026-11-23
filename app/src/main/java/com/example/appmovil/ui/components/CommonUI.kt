package com.example.appmovil.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.appmovil.ui.theme.DeepAzure
import com.example.appmovil.ui.theme.SoftAzure
import com.example.appmovil.ui.theme.LightAzure
import com.example.appmovil.ui.theme.SoftGray
import com.example.appmovil.ui.theme.TextDark
import com.example.appmovil.ui.theme.TextLight
import com.example.appmovil.ui.theme.SoftWhite
import com.example.appmovil.ui.theme.BackgroundLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElegantTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {},
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (value.isNotEmpty()) SoftAzure else TextLight.copy(alpha = 0.5f)
            )
        },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = TextLight
                    )
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions ?: KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text
        ),
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SoftAzure,
            unfocusedBorderColor = LightAzure,
            cursorColor = SoftAzure,
            focusedLabelColor = SoftAzure,
            unfocusedLabelColor = TextLight,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = SoftWhite,
            focusedTextColor = TextDark,
            unfocusedTextColor = TextDark
        ),
        enabled = enabled
    )
}
