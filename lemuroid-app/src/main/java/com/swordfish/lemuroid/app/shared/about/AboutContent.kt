package com.swordfish.lemuroid.app.shared.about

import android.content.Context
import android.text.TextUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.library.SystemID
import de.charlex.compose.material3.HtmlText

/**
 * App information: what PlayRetro is, help and license. The same content is shown in the mobile "About" tab
 * and in the TV "About" tab. With [focusableSections] each card can be focused, so a remote scrolls through them.
 */
@Composable
fun AboutContent(
    modifier: Modifier = Modifier,
    focusableSections: Boolean = false,
) {
    val context = LocalContext.current
    val appIcon = remember { context.packageManager.getApplicationIcon(context.packageName) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    val helpHtml = remember { buildHelpHtml(context) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING.dp),
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.size(ICON_SIZE.dp),
                painter = rememberDrawablePainter(drawable = appIcon),
                contentDescription = null,
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                // Explicit color: on TV the content is not inside a Surface, so the default text color is black.
                Text(
                    text = stringResource(R.string.lemuroid_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.about_version, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AboutSection(Icons.Outlined.Info, stringResource(R.string.about_what_is_title), focusableSections) {
            Text(text = stringResource(R.string.about_what_is_text), style = MaterialTheme.typography.bodyMedium)
        }
        AboutSection(Icons.AutoMirrored.Outlined.HelpOutline, stringResource(R.string.about_help_title), focusableSections) {
            HtmlText(text = helpHtml)
        }
        AboutSection(Icons.Outlined.Description, stringResource(R.string.about_license_title), focusableSections) {
            Text(text = stringResource(R.string.about_license_text), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AboutSection(
    icon: ImageVector,
    title: String,
    focusable: Boolean,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(CARD_CORNER_RADIUS.dp)
    // Only focused on TV: white border, like the other focused elements there.
    val borderColor = if (focused) Color.White else Color.Transparent

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .then(if (focusable) Modifier.focusable() else Modifier)
                .border(FOCUS_BORDER_WIDTH.dp, borderColor, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                content()
            }
        }
    }
}

/** Help questions of the old mobile dialog, with the list of folder names for each system. */
private fun buildHelpHtml(context: Context): String {
    val systemFolders = SystemID.values().joinToString(", ") { "<i>${it.dbname}</i>" }
    val coversQuestion = TextUtils.htmlEncode(context.getString(R.string.help_covers_question))
    val coversAnswer = TextUtils.htmlEncode(context.getString(R.string.help_covers_answer))
    return context.getString(R.string.lemuroid_help_content).trim().replace("\$SYSTEMS", systemFolders) +
        "<br/><br/><b>&#8226;$coversQuestion</b><br/>$coversAnswer"
}

private const val ICON_SIZE = 64
private const val SECTION_SPACING = 14
private const val CARD_CORNER_RADIUS = 14
private const val FOCUS_BORDER_WIDTH = 3
