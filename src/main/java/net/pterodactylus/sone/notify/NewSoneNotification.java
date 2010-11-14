/*
 * Sone - NewSoneNotification.java - Copyright © 2010 David Roden
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

package net.pterodactylus.sone.notify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.util.notify.TemplateNotification;
import net.pterodactylus.util.template.Template;

/**
 * Notification that signals that new Sones have been discovered.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class NewSoneNotification extends TemplateNotification {

	/** The new Sones. */
	private List<Sone> newSones = Collections.synchronizedList(new ArrayList<Sone>());

	/**
	 * Creates a new “new Sone discovered” notification.
	 *
	 * @param template
	 *            The template to render
	 */
	public NewSoneNotification(Template template) {
		super(template);
		template.set("sones", newSones);
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns whether there are any new Sones.
	 *
	 * @return {@code true} if there are no new Sones, {@code false} if there
	 *         are new Sones
	 */
	public boolean isEmpty() {
		return newSones.isEmpty();
	}

	/**
	 * Adds a discovered Sone.
	 *
	 * @param sone
	 *            The new Sone
	 */
	public void addSone(Sone sone) {
		newSones.add(sone);
		touch();
	}

	/**
	 * Removes the given Sone from the list of new Sones.
	 *
	 * @param sone
	 *            The Sone to remove
	 */
	public void removeSone(Sone sone) {
		newSones.remove(sone);
		touch();
	}

	//
	// ABSTRACTNOTIFICATION METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dismiss() {
		super.dismiss();
		newSones.clear();
	}

}
