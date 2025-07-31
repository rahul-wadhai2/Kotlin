package com.jejecomms.realtimechatfeature.ui.chatscreen.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jejecomms.realtimechatfeature.utils.DateUtils

/**
 *  Composable function for a date separator.
 *  This separator is used to group messages by date.
 */
@Composable
fun DateSeparator(timestamp: Long, eventText: String? = null) {
    val formattedDate = DateUtils.formatDate(timestamp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Divider
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.Gray.copy(alpha = 0.3f),
            thickness = 1.dp
        )

        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (eventText != null) {
                    "$formattedDate - $eventText"
                } else {
                    formattedDate // Display only date
                },
                color = Color.Gray,
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelSmall
            )
        }
        // Right Divider
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.Gray.copy(alpha = 0.3f),
            thickness = 1.dp
        )
    }
}