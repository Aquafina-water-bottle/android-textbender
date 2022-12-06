package sh.eliza.textbender

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class ActivateOverlayTileService : TileService() {
  private var serviceInstance: TextbenderService? = null
  private val handler = Handler(Looper.getMainLooper())

  override fun onStartListening() {
    super.onStartListening()
    qsTile.subtitle = getString(R.string.app_name)

    TextbenderService.addOnInstanceChangedListener(this::onServiceInstanceChanged, handler)

    serviceInstance = TextbenderService.instance
    updateState()
  }

  override fun onStopListening() {
    super.onStopListening()

    TextbenderService.removeOnInstanceChangedListener(this::onServiceInstanceChanged)
  }

  override fun onClick() {
    super.onClick()
    val serviceInstance = serviceInstance
    if (serviceInstance !== null) {
      serviceInstance.openOverlay(500L)
      startActivityAndCollapse(
        Intent(this, DummyActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
      )
    } else {
      Toast.makeText(
          this,
          getString(R.string.could_not_access_accessibility_service),
          Toast.LENGTH_LONG
        )
        .show()
    }
  }

  private fun onServiceInstanceChanged(serviceInstance: TextbenderService?) {
    this.serviceInstance = serviceInstance
    updateState()
  }

  private fun updateState() {
    qsTile.run {
      state =
        if (serviceInstance === null) {
          Tile.STATE_UNAVAILABLE
        } else {
          Tile.STATE_INACTIVE
        }
      updateTile()
    }
  }
}
