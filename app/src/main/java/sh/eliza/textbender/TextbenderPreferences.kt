package sh.eliza.textbender

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.preference.PreferenceManager
import java.util.concurrent.ConcurrentHashMap

private val onChangeListeners = ConcurrentHashMap<() -> Unit, OnSharedPreferenceChangeListener>()

data class TextbenderPreferences(
  val accessibilityShortcutEnabled: Boolean,
  val floatingButtonEnabled: Boolean,
  val tapDestination: Destination,
  val longPressDestination: Destination,
  val globalContextMenuDestination: Destination,
  val shareDestination: Destination,
  val urlDestination: Destination,
  val clipboardDestination: Destination,
  val urlFormat: String,
  // Hidden preferences
  val floatingButtonX: Int,
  val floatingButtonY: Int,
) {
  enum class Destination {
    DISABLED,
    CLIPBOARD,
    URL,
    SHARE,
    YOMICHAN,
  }

  companion object {
    fun createFromContext(context: Context): TextbenderPreferences {
      val preferences = PreferenceManager.getDefaultSharedPreferences(context)

      // UI Options
      val accessibilityShortcutEnabled = preferences.getBoolean("accessibility_shortcut", false)
      val floatingButtonEnabled = preferences.getBoolean("floating_button", false)

      // Mappings
      val tapDestination = preferences.getDestination("tap_destination")
      val longPressDestination = preferences.getDestination("long_press_destination")
      val globalContextMenuDestination =
        preferences.getDestination("global_context_menu_destination")
      val shareDestination = preferences.getDestination("share_destination")
      val urlDestination = preferences.getDestination("url_destination")
      val clipboardDestination = preferences.getDestination("clipboard_destination")
      if (clipboardDestination === Destination.CLIPBOARD) {
        throw IllegalArgumentException()
      }

      // Destionation Options
      val urlFormat =
        preferences.getString("url_format", null) ?: context.getString(R.string.url_format_default)

      // Hidden Options
      val floatingButtonX = preferences.getInt("floating_button_x", 0)
      val floatingButtonY = preferences.getInt("floating_button_y", 0)

      return TextbenderPreferences(
        accessibilityShortcutEnabled,
        floatingButtonEnabled,
        tapDestination,
        longPressDestination,
        globalContextMenuDestination,
        shareDestination,
        urlDestination,
        clipboardDestination,
        urlFormat,
        floatingButtonX,
        floatingButtonY
      )
    }

    fun registerOnChangeListener(context: Context, listener: () -> Unit) {
      val preferences = PreferenceManager.getDefaultSharedPreferences(context)
      val innerListener = OnSharedPreferenceChangeListener { _, _ -> listener() }
      onChangeListeners.put(listener, innerListener)
      preferences.registerOnSharedPreferenceChangeListener(innerListener)
    }

    fun unregisterOnChangeListener(context: Context, listener: () -> Unit) {
      val preferences = PreferenceManager.getDefaultSharedPreferences(context)
      onChangeListeners.remove(listener)?.let {
        preferences.unregisterOnSharedPreferenceChangeListener(it)
      }
    }

    fun putFloatingButtonEnabled(context: Context, enabled: Boolean) {
      PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putBoolean("floating_button", enabled)
        .apply()
    }

    fun putFloatingButtonPosition(context: Context, x: Int, y: Int) {
      PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putInt("floating_button_x", x)
        .putInt("floating_button_y", y)
        .apply()
    }

    private fun SharedPreferences.getDestination(key: String) =
      when (getString(key, "disabled")) {
        "disabled" -> Destination.DISABLED
        "clipboard" -> Destination.CLIPBOARD
        "url" -> Destination.URL
        "share" -> Destination.SHARE
        "yomichan" -> Destination.YOMICHAN
        else -> throw IllegalArgumentException()
      }
  }
}
