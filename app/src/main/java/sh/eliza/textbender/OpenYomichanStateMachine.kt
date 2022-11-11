package sh.eliza.textbender

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock.sleep
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.net.URLEncoder

private const val TAG = "OpenYomichanStateMachine"

private const val MAX_RETRIES = 10
private const val RETRY_INTERVAL_MS = 100L

private const val YOMICHAN_URL_PREFIX =
  "chrome-extension://ogmnaimimemjmbakcfefmnahgdfhfami/search.html?query="

/** Replace this with a coroutine or something eventually. */
class OpenYomichanStateMachine(
  private val service: TextbenderService,
  private val text: CharSequence
) : AutoCloseable {
  interface State {
    fun advance(): State?
  }

  private inner class LocateKiwiBrowserWindow : State {
    override fun advance(): State? {
      Log.i(TAG, "LocateKiwiBrowserWindow")
      service.windows.forEach { Log.i(TAG, "title: ${it?.title}") }
      val root = service.windows.firstOrNull { it?.title == "Kiwi Browser" }?.root
      if (root === null) {
        return this
      }
      return LocateAddressBar(root)
    }
  }

  private inner class LocateAddressBar(private val root: AccessibilityNodeInfo) : State {
    override fun advance(): State? {
      Log.i(TAG, "LocateAddressBar")
      val addressBar =
        root.findAccessibilityNodeInfosByViewId("com.kiwibrowser.browser:id/url_bar")?.firstOrNull()
      if (addressBar === null) {
        return this
      }

      val url = YOMICHAN_URL_PREFIX + URLEncoder.encode(text.toString(), Charsets.UTF_8.name())

      addressBar.performAction(AccessibilityNodeInfo.ACTION_FOCUS, Bundle())
      sleep(RETRY_INTERVAL_MS)
      addressBar.performAction(
        AccessibilityNodeInfo.ACTION_SET_TEXT,
        Bundle().apply {
          putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, url)
        }
      )

      return LocateResult(root, url)
    }
  }

  private inner class LocateResult(
    private val root: AccessibilityNodeInfo,
    private val url: String
  ) : State {
    override fun advance(): State? {
      Log.i(TAG, "LocateGoButton")
      val result =
        root
          .findAccessibilityNodeInfosByViewId(
            "com.kiwibrowser.browser:id/omnibox_results_container"
          )
          ?.firstOrNull()
          ?.getChild(0)
          ?.getChild(0)
          ?.children
          ?.firstOrNull { it.find { it.text?.startsWith(YOMICHAN_URL_PREFIX) ?: false } !== null }
      if (result === null) {
        return this
      }
      result.performAction(AccessibilityNodeInfo.ACTION_CLICK, Bundle())

      // Delay a bit before exiting the state machine.
      sleep(RETRY_INTERVAL_MS)

      return null
    }
  }

  val isAlive: Boolean
    get() = state !== null

  private val handlerThread = HandlerThread(TAG).apply { start() }
  private val handler = Handler(handlerThread.looper)

  private var state: State? = LocateKiwiBrowserWindow()
  private var tries = 0

  init {
    service.softKeyboardController.showMode = AccessibilityService.SHOW_MODE_HIDDEN
    advance()
  }

  private fun advance() {
    while (true) {
      val newState =
        try {
          state?.advance()
        } catch (t: Throwable) {
          Log.e(TAG, "Exception on handler thread", t)
          state
        }
      if (state === newState) {
        break
      }
      state = newState
    }
    tries++

    if (state !== null) {
      if (tries < MAX_RETRIES) {
        handler.postDelayed(this::advance, RETRY_INTERVAL_MS)
        return
      }
      Log.i(TAG, "Giving up")
      service.makeToast(service.applicationContext.getString(R.string.could_not_open_yomichan))
      state = null
    }
    handlerThread.quit()
    service.softKeyboardController.showMode = AccessibilityService.SHOW_MODE_AUTO
  }

  override fun close() {
    if (isAlive) {
      handlerThread.run {
        quit()
        join()
      }
      state = null
      service.softKeyboardController.showMode = AccessibilityService.SHOW_MODE_AUTO
    }
  }
}