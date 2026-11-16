package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CashoutEmerald
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitPanel
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThrustCrimson

@Composable
fun LoginScreen(
  authError: String?,
  onClearError: () -> Unit,
  onLogin: (phone: String, password: String) -> Unit,
  onRegister: (phone: String, password: String, pilotName: String) -> Unit,
  onDemoLogin: () -> Unit,
  onContinueGuest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isCreateMode by remember { mutableStateOf(false) }
  var phone by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var pilotName by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 520.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero Banner Header
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(160.dp)
          .clip(RoundedCornerShape(24.dp))
          .border(1.5.dp, ThrustCrimson.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_aviator_hero),
          contentDescription = "SkyCrash cockpit",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(ObsidianBg.copy(alpha = 0.25f), ObsidianBg.copy(alpha = 0.9f))
              )
            )
        )
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          verticalArrangement = Arrangement.Bottom,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.FlightTakeoff,
              contentDescription = null,
              tint = ThrustCrimson,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "SKYCRASH",
              style = MaterialTheme.typography.headlineMedium,
              color = TextPrimary,
            )
          }
          Text(
            text = "Sign in with your phone number and password to access your KSh wallet & flight log.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
      }

      // Sign In / Create Account Card
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(CockpitPanel)
          .border(1.dp, CockpitBorder, RoundedCornerShape(22.dp))
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Mode Switcher
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CockpitSurface)
            .border(1.dp, CockpitBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          AuthModeTab(
            label = "Sign In",
            selected = !isCreateMode,
            onClick = {
              isCreateMode = false
              onClearError()
            },
            tag = "auth_mode_signin",
            modifier = Modifier.weight(1f),
          )
          AuthModeTab(
            label = "Create Account",
            selected = isCreateMode,
            onClick = {
              isCreateMode = true
              onClearError()
            },
            tag = "auth_mode_register",
            modifier = Modifier.weight(1f),
          )
        }

        AnimatedVisibility(visible = isCreateMode) {
          OutlinedTextField(
            value = pilotName,
            onValueChange = {
              pilotName = it
              onClearError()
            },
            label = { Text("Name (Optional)") },
            leadingIcon = {
              Icon(Icons.Rounded.Person, contentDescription = null, tint = RadarCyan)
            },
            singleLine = true,
            colors = authFieldColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_name_input"),
          )
        }

        OutlinedTextField(
          value = phone,
          onValueChange = { raw ->
            phone = raw.filter { it.isDigit() || it == '+' || it == ' ' }
            onClearError()
          },
          label = { Text("Phone Number") },
          placeholder = { Text("0712 345 678 or +254712345678", color = TextSecondary) },
          leadingIcon = {
            Icon(Icons.Rounded.Phone, contentDescription = null, tint = CashoutEmerald)
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          colors = authFieldColors(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_phone_input"),
        )

        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            onClearError()
          },
          label = { Text("Password") },
          placeholder = { Text("Min 6 characters", color = TextSecondary) },
          leadingIcon = {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = RadarCyan)
          },
          trailingIcon = {
            IconButton(
              onClick = { passwordVisible = !passwordVisible },
              modifier = Modifier.testTag("auth_toggle_password_visibility"),
            ) {
              Icon(
                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                tint = TextSecondary,
              )
            }
          },
          singleLine = true,
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          colors = authFieldColors(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_password_input"),
        )

        if (!authError.isNullOrBlank()) {
          Text(
            text = authError,
            style = MaterialTheme.typography.bodyMedium,
            color = ThrustCrimson,
            modifier = Modifier.testTag("auth_error_text"),
          )
        }

        Button(
          onClick = {
            if (isCreateMode) {
              onRegister(phone, password, pilotName)
            } else {
              onLogin(phone, password)
            }
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isCreateMode) CashoutEmerald else ThrustCrimson,
            contentColor = if (isCreateMode) ObsidianBg else TextPrimary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("auth_submit_button"),
        ) {
          Text(
            text = if (isCreateMode) "CREATE PILOT ACCOUNT" else "SIGN IN TO COCKPIT",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
        }
      }

      // Quick Demo Login & Guest Access
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onDemoLogin,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("auth_demo_login_button"),
        ) {
          Text("Demo Pilot (0712345678)", color = RadarCyan, style = MaterialTheme.typography.labelMedium)
        }

        TextButton(
          onClick = onContinueGuest,
          modifier = Modifier
            .height(48.dp)
            .testTag("auth_guest_button"),
        ) {
          Text("Guest Mode", color = TextSecondary)
        }
      }
    }
  }
}

@Composable
private fun AuthModeTab(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  tag: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(9.dp))
      .background(if (selected) RadarCyan.copy(alpha = 0.2f) else CockpitSurface)
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp)
      .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) RadarCyan else TextSecondary,
    )
  }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = RadarCyan,
  unfocusedBorderColor = CockpitBorder,
  focusedTextColor = TextPrimary,
  unfocusedTextColor = TextPrimary,
  focusedLabelColor = RadarCyan,
  unfocusedLabelColor = TextSecondary,
)
