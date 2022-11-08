package sh.eliza.textbender

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object Textbender {
  fun handleText(
    context: Context,
    preferences: TextbenderPreferences,
    destination: TextbenderPreferences.Destination,
    text: CharSequence
  ) {
    when (destination) {
      TextbenderPreferences.Destination.CLIPBOARD -> {
        val clipboardManager =
          context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(context.getString(R.string.app_name), text)
        clipboardManager.setPrimaryClip(clipData)
      }
      TextbenderPreferences.Destination.URL -> {
        val uri =
          Uri.parse(
            preferences.urlFormat.replace(
              "{text}",
              URLEncoder.encode(text.toString(), Charsets.UTF_8.name())
            )
          )
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        if (intent.resolveActivity(context.packageManager) !== null) {
          context.startActivity(intent)
        }
      }
    }
  }
}