package net.pterodactylus.sone.web.ajax

import net.pterodactylus.sone.data.Sone
import net.pterodactylus.sone.utils.parameters
import net.pterodactylus.sone.web.WebInterface
import net.pterodactylus.sone.web.page.FreenetRequest

/**
 * AJAX page that lets a Sone unfollow another Sone.
 */
class UnfollowSoneAjaxPage(webInterface: WebInterface) : LoggedInJsonPage("unfollowSone.ajax", webInterface) {

	override fun createJsonObject(currentSone: Sone, request: FreenetRequest) =
			request.parameters["sone"]
					?.takeIf { core.getSone(it).isPresent }
					?.also { core.unfollowSone(currentSone, it) }
					?.let { createSuccessJsonObject() }
					?: createErrorJsonObject("invalid-sone-id")

}
