/*
 * Sone - ImageBrowserPage.java - Copyright © 2011 David Roden
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.pterodactylus.sone.web;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.util.template.DataProvider;
import net.pterodactylus.util.template.Template;

/**
 * The image browser page is the entry page for the image management.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ImageBrowserPage extends SoneTemplatePage {

	/**
	 * Creates a new image browser page.
	 *
	 * @param template
	 *            The template to render
	 * @param webInterface
	 *            The Sone web interface
	 */
	public ImageBrowserPage(Template template, WebInterface webInterface) {
		super("imageBrowser.html", template, "Page.ImageBrowser.Title", webInterface, true);
	}

	//
	// SONETEMPLATEPAGE METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processTemplate(Request request, DataProvider dataProvider) throws RedirectException {
		super.processTemplate(request, dataProvider);
		String albumId = request.getHttpRequest().getParam("album", null);
		if (albumId != null) {
			Album album = webInterface.getCore().getAlbum(albumId, false);
			dataProvider.set("albumRequested", true);
			dataProvider.set("album", album);
			return;
		}
		String imageId = request.getHttpRequest().getParam("image", null);
		if (imageId != null) {
			Image image = webInterface.getCore().getImage(imageId, false);
			dataProvider.set("imageRequested", true);
			dataProvider.set("image", image);
		}
	}
}
