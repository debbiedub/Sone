package net.pterodactylus.sone.web.pages

import net.pterodactylus.sone.data.Sone
import net.pterodactylus.sone.main.*
import net.pterodactylus.sone.utils.isPOST
import net.pterodactylus.sone.utils.parameters
import net.pterodactylus.sone.web.WebInterface
import net.pterodactylus.sone.web.page.*
import net.pterodactylus.util.template.Template
import net.pterodactylus.util.template.TemplateContext
import javax.inject.Inject

/**
 * This page lets the user unfollow another Sone.
 */
class UnfollowSonePage @Inject constructor(template: Template, webInterface: WebInterface, loaders: Loaders) :
		LoggedInPage("unfollowSone.html", template, "Page.UnfollowSone.Title", webInterface, loaders) {

	override fun handleRequest(soneRequest: SoneRequest, currentSone: Sone, templateContext: TemplateContext) {
		if (soneRequest.isPOST) {
			soneRequest.parameters["sone"]!!.split(Regex("[ ,]+"))
					.forEach { soneRequest.core.unfollowSone(currentSone, it) }
			throw RedirectException(soneRequest.parameters["returnPage", 256])
		}
	}

}
