package com.example.inoconnect.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inoconnect.ui.auth.BrandBlue

// ... (ProfileStatsGrid and StatItem remain the same) ...

@Composable
fun ProfileStatsGrid(connections: Int, following: Int, projects: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Connections", connections.toString())
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))
            StatItem("Following", following.toString())
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))
            StatItem("Projects", projects.toString())
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BrandBlue)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

// --- UPDATED: Accepted onEditClick to show pencil icon ---
@Composable
fun ProfileSectionCard(
    title: String,
    onEditClick: (() -> Unit)? = null, // <--- NEW PARAMETER
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)

                // Show Edit Pencil if callback is provided
                if (onEditClick != null) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit $title",
                        tint = BrandBlue,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onEditClick() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// ... (Rest of the file: InfoRow, ContactRow, EditTextField, SocialIconBtn remains the same) ...
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String?) {
    if (value.isNullOrEmpty()) return
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 14.sp, color = Color.Black)
        }
    }
}

@Composable
fun ContactRow(icon: ImageVector, value: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = BrandBlue)
        Spacer(Modifier.width(12.dp))
        Text(value, fontSize = 14.sp)
    }
}

@Composable
fun EditTextField(
    label: String,
    value: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        keyboardOptions = keyboardOptions,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandBlue,
            unfocusedBorderColor = Color.LightGray
        )
    )
}

@Composable
fun SocialIconBtn(iconRes: Int, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFF5F5F5),
        modifier = Modifier.size(40.dp).clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Share, null, tint = Color.Black, modifier = Modifier.padding(8.dp))
        }
    }
}