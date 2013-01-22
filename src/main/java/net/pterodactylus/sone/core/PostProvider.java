/*
 * Sone - PostProvider.java - Copyright © 2011–2013 David Roden
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

package net.pterodactylus.sone.core;

import java.util.Collection;

import net.pterodactylus.sone.data.Post;

/**
 * Interface for objects that can provide {@link Post}s by their ID.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public interface PostProvider {

	/**
	 * Returns the post with the given ID.
	 *
	 * @param postId
	 *            The ID of the post to return
	 * @return The post with the given ID, or {@code null}
	 */
	public Post getPost(String postId);

	/**
	 * Returns all posts that have the given Sone as recipient.
	 *
	 * @see Post#getRecipient()
	 * @param recipientId
	 *            The ID of the recipient of the posts
	 * @return All posts that have the given Sone as recipient
	 */
	public Collection<Post> getDirectedPosts(String recipientId);

}
