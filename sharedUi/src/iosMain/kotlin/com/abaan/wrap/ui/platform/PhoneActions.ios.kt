package com.abaan.wrap.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

@Composable
actual fun rememberPhoneActions(): PhoneActions = remember { IosPhoneActions }

private object IosPhoneActions : PhoneActions {
    override fun openUrl(url: String, noAppMessage: String?) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
    }

    override fun openMaps(place: String) {
        val trimmed = place.trim()
        openUrl(if (isLink(trimmed)) trimmed else "https://maps.apple.com/?q=${urlEncode(trimmed)}")
    }

    override fun dial(phone: String) = openUrl("tel:${urlEncode(phone.trim())}")

    override fun shareFile(path: String, mimeType: String, subject: String, title: String) {
        // The share sheet takes the subject from the file name, so [subject] isn't needed here.
        val sheet = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
        topController()?.presentViewController(sheet, animated = true, completion = null)
    }

    // iOS has no toast, so a small alert shows the message and closes itself.
    override fun showMessage(text: String) {
        val alert = UIAlertController.alertControllerWithTitle(null, message = text, preferredStyle = UIAlertControllerStyleAlert)
        topController()?.presentViewController(alert, animated = true, completion = null)
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, MESSAGE_SECONDS * 1_000_000_000L), dispatch_get_main_queue()) {
            alert.dismissViewControllerAnimated(true, completion = null)
        }
    }

    private fun topController(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (controller?.presentedViewController != null) controller = controller.presentedViewController
        return controller
    }

    private const val MESSAGE_SECONDS = 2L
}
