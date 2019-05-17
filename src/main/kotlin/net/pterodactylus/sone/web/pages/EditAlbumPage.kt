package net.pterodactylus.sone.web.pages

import net.pterodactylus.sone.data.Album.Modifier.AlbumTitleMustNotBeEmpty
import net.pterodactylus.sone.data.Sone
import net.pterodactylus.sone.main.*
import net.pterodactylus.sone.utils.isPOST
import net.pterodactylus.sone.web.WebInterface
import net.pterodactylus.sone.web.page.*
import net.pterodactylus.util.template.Template
import net.pterodactylus.util.template.TemplateContext
import javax.inject.Inject

/**
 * Page that lets the user edit the name and description of an album.
 */
class EditAlbumPage @Inject constructor(template: Template, webInterface: WebInterface, loaders: Loaders):
		LoggedInPage("editAlbum.html", template, "Page.EditAlbum.Title", webInterface, loaders) {

	override fun handleRequest(soneRequest: SoneRequest, currentSone: Sone, templateContext: TemplateContext) {
		if (soneRequest.isPOST) {
			val album = soneRequest.core.getAlbum(soneRequest.httpRequest.getPartAsStringFailsafe("album", 36)) ?: throw RedirectException("invalid.html")
			album.takeUnless { it.sone.isLocal }?.run { throw RedirectException("noPermission.html") }
			if (soneRequest.httpRequest.getPartAsStringFailsafe("moveLeft", 4) == "true") {
				album.parent?.moveAlbumUp(album)
				soneRequest.core.touchConfiguration()
				throw RedirectException("imageBrowser.html?album=${album.parent?.id}")
			} else if (soneRequest.httpRequest.getPartAsStringFailsafe("moveRight", 4) == "true") {
				album.parent?.moveAlbumDown(album)
				soneRequest.core.touchConfiguration()
				throw RedirectException("imageBrowser.html?album=${album.parent?.id}")
			} else {
				try {
					album.modify()
							.setTitle(soneRequest.httpRequest.getPartAsStringFailsafe("title", 100))
							.setDescription(soneRequest.httpRequest.getPartAsStringFailsafe("description", 1000))
							.update()
				} catch (e: AlbumTitleMustNotBeEmpty) {
					throw RedirectException("emptyAlbumTitle.html")
				}
				soneRequest.core.touchConfiguration()
				throw RedirectException("imageBrowser.html?album=${album.id}")
			}
		}
	}

}
