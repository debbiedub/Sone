/*
 * Sone - WebInterface.java - Copyright © 2010–2019 David Roden
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

import static com.google.common.collect.FluentIterable.from;
import static java.util.logging.Logger.getLogger;
import static net.pterodactylus.util.template.TemplateParser.parse;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.core.ElementLoader;
import net.pterodactylus.sone.core.event.ImageInsertAbortedEvent;
import net.pterodactylus.sone.core.event.ImageInsertFailedEvent;
import net.pterodactylus.sone.core.event.ImageInsertFinishedEvent;
import net.pterodactylus.sone.core.event.ImageInsertStartedEvent;
import net.pterodactylus.sone.core.event.MarkPostKnownEvent;
import net.pterodactylus.sone.core.event.MarkPostReplyKnownEvent;
import net.pterodactylus.sone.core.event.MarkSoneKnownEvent;
import net.pterodactylus.sone.core.event.NewPostFoundEvent;
import net.pterodactylus.sone.core.event.NewPostReplyFoundEvent;
import net.pterodactylus.sone.core.event.NewSoneFoundEvent;
import net.pterodactylus.sone.core.event.PostRemovedEvent;
import net.pterodactylus.sone.core.event.PostReplyRemovedEvent;
import net.pterodactylus.sone.core.event.SoneInsertAbortedEvent;
import net.pterodactylus.sone.core.event.SoneInsertedEvent;
import net.pterodactylus.sone.core.event.SoneInsertingEvent;
import net.pterodactylus.sone.core.event.SoneLockedEvent;
import net.pterodactylus.sone.core.event.SoneRemovedEvent;
import net.pterodactylus.sone.core.event.SoneUnlockedEvent;
import net.pterodactylus.sone.core.event.UpdateFoundEvent;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.L10nFilter;
import net.pterodactylus.sone.main.Loaders;
import net.pterodactylus.sone.main.PluginHomepage;
import net.pterodactylus.sone.main.PluginVersion;
import net.pterodactylus.sone.main.PluginYear;
import net.pterodactylus.sone.main.SonePlugin;
import net.pterodactylus.sone.notify.ListNotification;
import net.pterodactylus.sone.notify.ListNotificationFilter;
import net.pterodactylus.sone.notify.PostVisibilityFilter;
import net.pterodactylus.sone.notify.ReplyVisibilityFilter;
import net.pterodactylus.sone.template.LinkedElementRenderFilter;
import net.pterodactylus.sone.template.ParserFilter;
import net.pterodactylus.sone.template.RenderFilter;
import net.pterodactylus.sone.template.ShortenFilter;
import net.pterodactylus.sone.text.Part;
import net.pterodactylus.sone.text.SonePart;
import net.pterodactylus.sone.text.SoneTextParser;
import net.pterodactylus.sone.text.TimeTextConverter;
import net.pterodactylus.sone.web.ajax.BookmarkAjaxPage;
import net.pterodactylus.sone.web.ajax.CreatePostAjaxPage;
import net.pterodactylus.sone.web.ajax.CreateReplyAjaxPage;
import net.pterodactylus.sone.web.ajax.DeletePostAjaxPage;
import net.pterodactylus.sone.web.ajax.DeleteProfileFieldAjaxPage;
import net.pterodactylus.sone.web.ajax.DeleteReplyAjaxPage;
import net.pterodactylus.sone.web.ajax.DismissNotificationAjaxPage;
import net.pterodactylus.sone.web.ajax.DistrustAjaxPage;
import net.pterodactylus.sone.web.ajax.EditAlbumAjaxPage;
import net.pterodactylus.sone.web.ajax.EditImageAjaxPage;
import net.pterodactylus.sone.web.ajax.EditProfileFieldAjaxPage;
import net.pterodactylus.sone.web.ajax.FollowSoneAjaxPage;
import net.pterodactylus.sone.web.ajax.GetLikesAjaxPage;
import net.pterodactylus.sone.web.ajax.GetLinkedElementAjaxPage;
import net.pterodactylus.sone.web.ajax.GetNotificationsAjaxPage;
import net.pterodactylus.sone.web.ajax.GetPostAjaxPage;
import net.pterodactylus.sone.web.ajax.GetReplyAjaxPage;
import net.pterodactylus.sone.web.ajax.GetStatusAjaxPage;
import net.pterodactylus.sone.web.ajax.GetTimesAjaxPage;
import net.pterodactylus.sone.web.ajax.GetTranslationAjaxPage;
import net.pterodactylus.sone.web.ajax.LikeAjaxPage;
import net.pterodactylus.sone.web.ajax.LockSoneAjaxPage;
import net.pterodactylus.sone.web.ajax.MarkAsKnownAjaxPage;
import net.pterodactylus.sone.web.ajax.MoveProfileFieldAjaxPage;
import net.pterodactylus.sone.web.ajax.TrustAjaxPage;
import net.pterodactylus.sone.web.ajax.UnbookmarkAjaxPage;
import net.pterodactylus.sone.web.ajax.UnfollowSoneAjaxPage;
import net.pterodactylus.sone.web.ajax.UnlikeAjaxPage;
import net.pterodactylus.sone.web.ajax.UnlockSoneAjaxPage;
import net.pterodactylus.sone.web.ajax.UntrustAjaxPage;
import net.pterodactylus.sone.web.page.FreenetRequest;
import net.pterodactylus.sone.web.pages.AboutPage;
import net.pterodactylus.sone.web.pages.BookmarkPage;
import net.pterodactylus.sone.web.pages.BookmarksPage;
import net.pterodactylus.sone.web.pages.CreateAlbumPage;
import net.pterodactylus.sone.web.pages.CreatePostPage;
import net.pterodactylus.sone.web.pages.CreateReplyPage;
import net.pterodactylus.sone.web.pages.CreateSonePage;
import net.pterodactylus.sone.web.pages.DeleteAlbumPage;
import net.pterodactylus.sone.web.pages.DeleteImagePage;
import net.pterodactylus.sone.web.pages.DeletePostPage;
import net.pterodactylus.sone.web.pages.DeleteProfileFieldPage;
import net.pterodactylus.sone.web.pages.DeleteReplyPage;
import net.pterodactylus.sone.web.pages.DeleteSonePage;
import net.pterodactylus.sone.web.pages.DismissNotificationPage;
import net.pterodactylus.sone.web.pages.DistrustPage;
import net.pterodactylus.sone.web.pages.EditAlbumPage;
import net.pterodactylus.sone.web.pages.EditImagePage;
import net.pterodactylus.sone.web.pages.EditProfileFieldPage;
import net.pterodactylus.sone.web.pages.EditProfilePage;
import net.pterodactylus.sone.web.pages.FollowSonePage;
import net.pterodactylus.sone.web.pages.GetImagePage;
import net.pterodactylus.sone.web.pages.ImageBrowserPage;
import net.pterodactylus.sone.web.pages.IndexPage;
import net.pterodactylus.sone.web.pages.KnownSonesPage;
import net.pterodactylus.sone.web.pages.LikePage;
import net.pterodactylus.sone.web.pages.LockSonePage;
import net.pterodactylus.sone.web.pages.LoginPage;
import net.pterodactylus.sone.web.pages.LogoutPage;
import net.pterodactylus.sone.web.pages.MarkAsKnownPage;
import net.pterodactylus.sone.web.pages.NewPage;
import net.pterodactylus.sone.web.pages.OptionsPage;
import net.pterodactylus.sone.web.pages.RescuePage;
import net.pterodactylus.sone.web.pages.SearchPage;
import net.pterodactylus.sone.web.pages.SoneTemplatePage;
import net.pterodactylus.sone.web.pages.TrustPage;
import net.pterodactylus.sone.web.pages.UnbookmarkPage;
import net.pterodactylus.sone.web.pages.UnfollowSonePage;
import net.pterodactylus.sone.web.pages.UnlikePage;
import net.pterodactylus.sone.web.pages.UnlockSonePage;
import net.pterodactylus.sone.web.pages.UntrustPage;
import net.pterodactylus.sone.web.pages.UploadImagePage;
import net.pterodactylus.sone.web.pages.ViewPostPage;
import net.pterodactylus.sone.web.pages.ViewSonePage;
import net.pterodactylus.util.notify.Notification;
import net.pterodactylus.util.notify.NotificationManager;
import net.pterodactylus.util.notify.TemplateNotification;
import net.pterodactylus.util.template.Template;
import net.pterodactylus.util.template.TemplateContextFactory;
import net.pterodactylus.util.web.RedirectPage;
import net.pterodactylus.util.web.TemplatePage;

import freenet.clients.http.SessionManager;
import freenet.clients.http.SessionManager.Session;
import freenet.clients.http.ToadletContext;
import freenet.l10n.BaseL10n;

import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

/**
 * Bundles functionality that a web interface of a Freenet plugin needs, e.g.
 * references to l10n helpers.
 */
public class WebInterface implements SessionProvider {

	/** The logger. */
	private static final Logger logger = getLogger(WebInterface.class.getName());

	/** The loaders for templates, pages, and classpath providers. */
	private final Loaders loaders;

	/** The notification manager. */
	private final NotificationManager notificationManager = new NotificationManager();

	/** The Sone plugin. */
	private final SonePlugin sonePlugin;

	/** The form password. */
	private final String formPassword;

	/** The template context factory. */
	private final TemplateContextFactory templateContextFactory;

	/** The Sone text parser. */
	private final SoneTextParser soneTextParser;

	/** The parser filter. */
	private final ParserFilter parserFilter;
	private final ShortenFilter shortenFilter;
	private final RenderFilter renderFilter;

	private final ListNotificationFilter listNotificationFilter;
	private final PostVisibilityFilter postVisibilityFilter;
	private final ReplyVisibilityFilter replyVisibilityFilter;

	private final ElementLoader elementLoader;
	private final LinkedElementRenderFilter linkedElementRenderFilter;
	private final TimeTextConverter timeTextConverter = new TimeTextConverter();
	private final L10nFilter l10nFilter;

	private final PageToadletRegistry pageToadletRegistry;

	/** The “new Sone” notification. */
	private final ListNotification<Sone> newSoneNotification;

	/** The “new post” notification. */
	private final ListNotification<Post> newPostNotification;

	/** The “new reply” notification. */
	private final ListNotification<PostReply> newReplyNotification;

	/** The invisible “local post” notification. */
	private final ListNotification<Post> localPostNotification;

	/** The invisible “local reply” notification. */
	private final ListNotification<PostReply> localReplyNotification;

	/** The “you have been mentioned” notification. */
	private final ListNotification<Post> mentionNotification;

	/** Notifications for sone inserts. */
	private final Map<Sone, TemplateNotification> soneInsertNotifications = new HashMap<>();

	/** Sone locked notification ticker objects. */
	private final Map<Sone, ScheduledFuture<?>> lockedSonesTickerObjects = Collections.synchronizedMap(new HashMap<Sone, ScheduledFuture<?>>());

	/** The “Sone locked” notification. */
	private final ListNotification<Sone> lockedSonesNotification;

	/** The “new version” notification. */
	private final TemplateNotification newVersionNotification;

	/** The “inserting images” notification. */
	private final ListNotification<Image> insertingImagesNotification;

	/** The “inserted images” notification. */
	private final ListNotification<Image> insertedImagesNotification;

	/** The “image insert failed” notification. */
	private final ListNotification<Image> imageInsertFailedNotification;

	/** Scheduled executor for time-based notifications. */
	private final ScheduledExecutorService ticker = Executors.newScheduledThreadPool(1);

	@Inject
	public WebInterface(SonePlugin sonePlugin, Loaders loaders, ListNotificationFilter listNotificationFilter,
			PostVisibilityFilter postVisibilityFilter, ReplyVisibilityFilter replyVisibilityFilter,
			ElementLoader elementLoader, TemplateContextFactory templateContextFactory,
			ParserFilter parserFilter, ShortenFilter shortenFilter,
			RenderFilter renderFilter,
			LinkedElementRenderFilter linkedElementRenderFilter,
			PageToadletRegistry pageToadletRegistry) {
		this.sonePlugin = sonePlugin;
		this.loaders = loaders;
		this.listNotificationFilter = listNotificationFilter;
		this.postVisibilityFilter = postVisibilityFilter;
		this.replyVisibilityFilter = replyVisibilityFilter;
		this.elementLoader = elementLoader;
		this.parserFilter = parserFilter;
		this.shortenFilter = shortenFilter;
		this.renderFilter = renderFilter;
		this.linkedElementRenderFilter = linkedElementRenderFilter;
		this.pageToadletRegistry = pageToadletRegistry;
		formPassword = sonePlugin.pluginRespirator().getToadletContainer().getFormPassword();
		soneTextParser = new SoneTextParser(getCore(), getCore());
		l10nFilter = new L10nFilter(getL10n());

		this.templateContextFactory = templateContextFactory;
		templateContextFactory.addTemplateObject("webInterface", this);
		templateContextFactory.addTemplateObject("formPassword", formPassword);

		/* create notifications. */
		Template newSoneNotificationTemplate = loaders.loadTemplate("/templates/notify/newSoneNotification.html");
		newSoneNotification = new ListNotification<>("new-sone-notification", "sones", newSoneNotificationTemplate, false);

		Template newPostNotificationTemplate = loaders.loadTemplate("/templates/notify/newPostNotification.html");
		newPostNotification = new ListNotification<>("new-post-notification", "posts", newPostNotificationTemplate, false);

		Template localPostNotificationTemplate = loaders.loadTemplate("/templates/notify/newPostNotification.html");
		localPostNotification = new ListNotification<>("local-post-notification", "posts", localPostNotificationTemplate, false);

		Template newReplyNotificationTemplate = loaders.loadTemplate("/templates/notify/newReplyNotification.html");
		newReplyNotification = new ListNotification<>("new-reply-notification", "replies", newReplyNotificationTemplate, false);

		Template localReplyNotificationTemplate = loaders.loadTemplate("/templates/notify/newReplyNotification.html");
		localReplyNotification = new ListNotification<>("local-reply-notification", "replies", localReplyNotificationTemplate, false);

		Template mentionNotificationTemplate = loaders.loadTemplate("/templates/notify/mentionNotification.html");
		mentionNotification = new ListNotification<>("mention-notification", "posts", mentionNotificationTemplate, false);

		Template lockedSonesTemplate = loaders.loadTemplate("/templates/notify/lockedSonesNotification.html");
		lockedSonesNotification = new ListNotification<>("sones-locked-notification", "sones", lockedSonesTemplate);

		Template newVersionTemplate = loaders.loadTemplate("/templates/notify/newVersionNotification.html");
		newVersionNotification = new TemplateNotification("new-version-notification", newVersionTemplate);

		Template insertingImagesTemplate = loaders.loadTemplate("/templates/notify/inserting-images-notification.html");
		insertingImagesNotification = new ListNotification<>("inserting-images-notification", "images", insertingImagesTemplate);

		Template insertedImagesTemplate = loaders.loadTemplate("/templates/notify/inserted-images-notification.html");
		insertedImagesNotification = new ListNotification<>("inserted-images-notification", "images", insertedImagesTemplate);

		Template imageInsertFailedTemplate = loaders.loadTemplate("/templates/notify/image-insert-failed-notification.html");
		imageInsertFailedNotification = new ListNotification<>("image-insert-failed-notification", "images", imageInsertFailedTemplate);
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the Sone core used by the Sone plugin.
	 *
	 * @return The Sone core
	 */
	@Nonnull
	public Core getCore() {
		return sonePlugin.core();
	}

	/**
	 * Returns the template context factory of the web interface.
	 *
	 * @return The template context factory
	 */
	public TemplateContextFactory getTemplateContextFactory() {
		return templateContextFactory;
	}

	private Session getCurrentSessionWithoutCreation(ToadletContext toadletContenxt) {
		return getSessionManager().useSession(toadletContenxt);
	}

	private Session getOrCreateCurrentSession(ToadletContext toadletContenxt) {
		Session session = getCurrentSessionWithoutCreation(toadletContenxt);
		if (session == null) {
			session = getSessionManager().createSession(UUID.randomUUID().toString(), toadletContenxt);
		}
		return session;
	}

	public Sone getCurrentSoneCreatingSession(ToadletContext toadletContext) {
		Collection<Sone> localSones = getCore().getLocalSones();
		if (localSones.size() == 1) {
			return localSones.iterator().next();
		}
		return getCurrentSone(getOrCreateCurrentSession(toadletContext));
	}

	public Sone getCurrentSoneWithoutCreatingSession(ToadletContext toadletContext) {
		Collection<Sone> localSones = getCore().getLocalSones();
		if (localSones.size() == 1) {
			return localSones.iterator().next();
		}
		return getCurrentSone(getCurrentSessionWithoutCreation(toadletContext));
	}

	/**
	 * Returns the currently logged in Sone.
	 *
	 * @param session
	 *            The session
	 * @return The currently logged in Sone, or {@code null} if no Sone is
	 *         currently logged in
	 */
	private Sone getCurrentSone(Session session) {
		if (session == null) {
			return null;
		}
		String soneId = (String) session.getAttribute("Sone.CurrentSone");
		if (soneId == null) {
			return null;
		}
		return getCore().getLocalSone(soneId);
	}

	@Override
	@Nullable
	public Sone getCurrentSone(@Nonnull ToadletContext toadletContext, boolean createSession) {
		return createSession ? getCurrentSoneCreatingSession(toadletContext) : getCurrentSoneWithoutCreatingSession(toadletContext);
	}

	/**
	 * Sets the currently logged in Sone.
	 *
	 * @param toadletContext
	 *            The toadlet context
	 * @param sone
	 *            The Sone to set as currently logged in
	 */
	@Override
	public void setCurrentSone(@Nonnull ToadletContext toadletContext, @Nullable Sone sone) {
		Session session = getOrCreateCurrentSession(toadletContext);
		if (sone == null) {
			session.removeAttribute("Sone.CurrentSone");
		} else {
			session.setAttribute("Sone.CurrentSone", sone.getId());
		}
	}

	/**
	 * Returns the notification manager.
	 *
	 * @return The notification manager
	 */
	public NotificationManager getNotifications() {
		return notificationManager;
	}

	@Nonnull
	public Optional<Notification> getNotification(@Nonnull String notificationId) {
		return Optional.fromNullable(notificationManager.getNotification(notificationId));
	}

	@Nonnull
	public Collection<Notification> getNotifications(@Nullable Sone currentSone) {
		return listNotificationFilter.filterNotifications(notificationManager.getNotifications(), currentSone);
	}

	/**
	 * Returns the l10n helper of the node.
	 *
	 * @return The node’s l10n helper
	 */
	public BaseL10n getL10n() {
		return sonePlugin.l10n().getBase();
	}

	/**
	 * Returns the session manager of the node.
	 *
	 * @return The node’s session manager
	 */
	public SessionManager getSessionManager() {
		return sonePlugin.pluginRespirator().getSessionManager("Sone");
	}

	/**
	 * Returns the node’s form password.
	 *
	 * @return The form password
	 */
	public String getFormPassword() {
		return formPassword;
	}

	/**
	 * Returns the posts that have been announced as new in the
	 * {@link #newPostNotification}.
	 *
	 * @return The new posts
	 */
	public Set<Post> getNewPosts() {
		return ImmutableSet.<Post> builder().addAll(newPostNotification.getElements()).addAll(localPostNotification.getElements()).build();
	}

	@Nonnull
	public Collection<Post> getNewPosts(@Nullable Sone currentSone) {
		Set<Post> allNewPosts = ImmutableSet.<Post> builder()
				.addAll(newPostNotification.getElements())
				.addAll(localPostNotification.getElements())
				.build();
		return from(allNewPosts).filter(postVisibilityFilter.isVisible(currentSone)).toSet();
	}

	/**
	 * Returns the replies that have been announced as new in the
	 * {@link #newReplyNotification}.
	 *
	 * @return The new replies
	 */
	public Set<PostReply> getNewReplies() {
		return ImmutableSet.<PostReply> builder().addAll(newReplyNotification.getElements()).addAll(localReplyNotification.getElements()).build();
	}

	@Nonnull
	public Collection<PostReply> getNewReplies(@Nullable Sone currentSone) {
		Set<PostReply> allNewReplies = ImmutableSet.<PostReply>builder()
				.addAll(newReplyNotification.getElements())
				.addAll(localReplyNotification.getElements())
				.build();
		return from(allNewReplies).filter(replyVisibilityFilter.isVisible(currentSone)).toSet();
	}

	/**
	 * Sets whether the current start of the plugin is the first start. It is
	 * considered a first start if the configuration file does not exist.
	 *
	 * @param firstStart
	 *            {@code true} if no configuration file existed when Sone was
	 *            loaded, {@code false} otherwise
	 */
	public void setFirstStart(boolean firstStart) {
		if (firstStart) {
			Template firstStartNotificationTemplate = loaders.loadTemplate("/templates/notify/firstStartNotification.html");
			Notification firstStartNotification = new TemplateNotification("first-start-notification", firstStartNotificationTemplate);
			notificationManager.addNotification(firstStartNotification);
		}
	}

	/**
	 * Sets whether Sone was started with a fresh configuration file.
	 *
	 * @param newConfig
	 *            {@code true} if Sone was started with a fresh configuration,
	 *            {@code false} if the existing configuration could be read
	 */
	public void setNewConfig(boolean newConfig) {
		if (newConfig && !hasFirstStartNotification()) {
			Template configNotReadNotificationTemplate = loaders.loadTemplate("/templates/notify/configNotReadNotification.html");
			Notification configNotReadNotification = new TemplateNotification("config-not-read-notification", configNotReadNotificationTemplate);
			notificationManager.addNotification(configNotReadNotification);
		}
	}

	//
	// PRIVATE ACCESSORS
	//

	/**
	 * Returns whether the first start notification is currently displayed.
	 *
	 * @return {@code true} if the first-start notification is currently
	 *         displayed, {@code false} otherwise
	 */
	private boolean hasFirstStartNotification() {
		return notificationManager.getNotification("first-start-notification") != null;
	}

	//
	// ACTIONS
	//

	/**
	 * Starts the web interface and registers all toadlets.
	 */
	public void start() {
		registerToadlets();

		/* notification templates. */
		Template startupNotificationTemplate = loaders.loadTemplate("/templates/notify/startupNotification.html");

		final TemplateNotification startupNotification = new TemplateNotification("startup-notification", startupNotificationTemplate);
		notificationManager.addNotification(startupNotification);

		ticker.schedule(new Runnable() {

			@Override
			public void run() {
				startupNotification.dismiss();
			}
		}, 2, TimeUnit.MINUTES);

		Template wotMissingNotificationTemplate = loaders.loadTemplate("/templates/notify/wotMissingNotification.html");
		final TemplateNotification wotMissingNotification = new TemplateNotification("wot-missing-notification", wotMissingNotificationTemplate);
		ticker.scheduleAtFixedRate(new Runnable() {

			@Override
			@SuppressWarnings("synthetic-access")
			public void run() {
				if (getCore().getIdentityManager().isConnected()) {
					wotMissingNotification.dismiss();
				} else {
					notificationManager.addNotification(wotMissingNotification);
				}
			}

		}, 15, 15, TimeUnit.SECONDS);
	}

	/**
	 * Stops the web interface and unregisters all toadlets.
	 */
	public void stop() {
		pageToadletRegistry.unregisterToadlets();
		ticker.shutdownNow();
	}

	//
	// PRIVATE METHODS
	//

	/**
	 * Register all toadlets.
	 */
	private void registerToadlets() {
		Template emptyTemplate = parse(new StringReader(""));
		Template loginTemplate = loaders.loadTemplate("/templates/login.html");
		Template indexTemplate = loaders.loadTemplate("/templates/index.html");
		Template newTemplate = loaders.loadTemplate("/templates/new.html");
		Template knownSonesTemplate = loaders.loadTemplate("/templates/knownSones.html");
		Template createSoneTemplate = loaders.loadTemplate("/templates/createSone.html");
		Template createPostTemplate = loaders.loadTemplate("/templates/createPost.html");
		Template createReplyTemplate = loaders.loadTemplate("/templates/createReply.html");
		Template bookmarksTemplate = loaders.loadTemplate("/templates/bookmarks.html");
		Template searchTemplate = loaders.loadTemplate("/templates/search.html");
		Template editProfileTemplate = loaders.loadTemplate("/templates/editProfile.html");
		Template editProfileFieldTemplate = loaders.loadTemplate("/templates/editProfileField.html");
		Template deleteProfileFieldTemplate = loaders.loadTemplate("/templates/deleteProfileField.html");
		Template viewSoneTemplate = loaders.loadTemplate("/templates/viewSone.html");
		Template viewPostTemplate = loaders.loadTemplate("/templates/viewPost.html");
		Template deletePostTemplate = loaders.loadTemplate("/templates/deletePost.html");
		Template deleteReplyTemplate = loaders.loadTemplate("/templates/deleteReply.html");
		Template deleteSoneTemplate = loaders.loadTemplate("/templates/deleteSone.html");
		Template imageBrowserTemplate = loaders.loadTemplate("/templates/imageBrowser.html");
		Template createAlbumTemplate = loaders.loadTemplate("/templates/createAlbum.html");
		Template deleteAlbumTemplate = loaders.loadTemplate("/templates/deleteAlbum.html");
		Template deleteImageTemplate = loaders.loadTemplate("/templates/deleteImage.html");
		Template noPermissionTemplate = loaders.loadTemplate("/templates/noPermission.html");
		Template emptyImageTitleTemplate = loaders.loadTemplate("/templates/emptyImageTitle.html");
		Template emptyAlbumTitleTemplate = loaders.loadTemplate("/templates/emptyAlbumTitle.html");
		Template optionsTemplate = loaders.loadTemplate("/templates/options.html");
		Template rescueTemplate = loaders.loadTemplate("/templates/rescue.html");
		Template aboutTemplate = loaders.loadTemplate("/templates/about.html");
		Template invalidTemplate = loaders.loadTemplate("/templates/invalid.html");
		Template postTemplate = loaders.loadTemplate("/templates/include/viewPost.html");
		Template replyTemplate = loaders.loadTemplate("/templates/include/viewReply.html");
		Template openSearchTemplate = loaders.loadTemplate("/templates/xml/OpenSearch.xml");

		pageToadletRegistry.addPage(new RedirectPage<FreenetRequest>("", "index.html"));
		pageToadletRegistry.addPage(new IndexPage(indexTemplate, this, loaders, postVisibilityFilter));
		pageToadletRegistry.addPage(new NewPage(newTemplate, this, loaders));
		pageToadletRegistry.addPage(new CreateSonePage(createSoneTemplate, this, loaders));
		pageToadletRegistry.addPage(new KnownSonesPage(knownSonesTemplate, this, loaders));
		pageToadletRegistry.addPage(new EditProfilePage(editProfileTemplate, this, loaders));
		pageToadletRegistry.addPage(new EditProfileFieldPage(editProfileFieldTemplate, this, loaders));
		pageToadletRegistry.addPage(new DeleteProfileFieldPage(deleteProfileFieldTemplate, this, loaders));
		pageToadletRegistry.addPage(new CreatePostPage(createPostTemplate, this, loaders));
		pageToadletRegistry.addPage(new CreateReplyPage(createReplyTemplate, this, loaders));
		pageToadletRegistry.addPage(new ViewSonePage(viewSoneTemplate, this, loaders));
		pageToadletRegistry.addPage(new ViewPostPage(viewPostTemplate, this, loaders));
		pageToadletRegistry.addPage(new LikePage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new UnlikePage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new DeletePostPage(deletePostTemplate, this, loaders));
		pageToadletRegistry.addPage(new DeleteReplyPage(deleteReplyTemplate, this, loaders));
		pageToadletRegistry.addPage(new LockSonePage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new UnlockSonePage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new FollowSonePage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new UnfollowSonePage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new ImageBrowserPage(imageBrowserTemplate, this, loaders));
		pageToadletRegistry.addPage(new CreateAlbumPage(createAlbumTemplate, this, loaders));
		pageToadletRegistry.addPage(new EditAlbumPage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new DeleteAlbumPage(deleteAlbumTemplate, this, loaders));
		pageToadletRegistry.addPage(new UploadImagePage(invalidTemplate, this, loaders));
		pageToadletRegistry.addPage(new EditImagePage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new DeleteImagePage(deleteImageTemplate, this, loaders));
		pageToadletRegistry.addPage(new TrustPage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new DistrustPage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new UntrustPage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new MarkAsKnownPage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new BookmarkPage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new UnbookmarkPage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new BookmarksPage(bookmarksTemplate, this, loaders));
		pageToadletRegistry.addPage(new SearchPage(searchTemplate, this, loaders));
		pageToadletRegistry.addPage(new DeleteSonePage(deleteSoneTemplate, this, loaders));
		pageToadletRegistry.addPage(new LoginPage(loginTemplate, this, loaders));
		pageToadletRegistry.addPage(new LogoutPage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new OptionsPage(optionsTemplate, this, loaders));
		pageToadletRegistry.addPage(new RescuePage(rescueTemplate, this, loaders));
		pageToadletRegistry.addPage(new AboutPage(aboutTemplate, this, loaders, new PluginVersion(SonePlugin.getPluginVersion()), new PluginYear(sonePlugin.getYear()), new PluginHomepage(sonePlugin.getHomepage())));
		pageToadletRegistry.addPage(new SoneTemplatePage("noPermission.html", this, loaders, noPermissionTemplate, "Page.NoPermission.Title"));
		pageToadletRegistry.addPage(new SoneTemplatePage("emptyImageTitle.html", this, loaders, emptyImageTitleTemplate, "Page.EmptyImageTitle.Title"));
		pageToadletRegistry.addPage(new SoneTemplatePage("emptyAlbumTitle.html", this, loaders, emptyAlbumTitleTemplate, "Page.EmptyAlbumTitle.Title"));
		pageToadletRegistry.addPage(new DismissNotificationPage(emptyTemplate, this, loaders));
		pageToadletRegistry.addPage(new SoneTemplatePage("invalid.html", this, loaders, invalidTemplate, "Page.Invalid.Title"));
		pageToadletRegistry.addPage(loaders.<FreenetRequest>loadStaticPage("css/", "/static/css/", "text/css"));
		pageToadletRegistry.addPage(loaders.<FreenetRequest>loadStaticPage("javascript/", "/static/javascript/", "text/javascript"));
		pageToadletRegistry.addPage(loaders.<FreenetRequest>loadStaticPage("images/", "/static/images/", "image/png"));
		pageToadletRegistry.addPage(new TemplatePage<FreenetRequest>("OpenSearch.xml", "application/opensearchdescription+xml", templateContextFactory, openSearchTemplate));
		pageToadletRegistry.addPage(new GetImagePage(this));
		pageToadletRegistry.addPage(new GetTranslationAjaxPage(this));
		pageToadletRegistry.addPage(new GetStatusAjaxPage(this, elementLoader, timeTextConverter, l10nFilter, TimeZone.getDefault()));
		pageToadletRegistry.addPage(new GetNotificationsAjaxPage(this));
		pageToadletRegistry.addPage(new DismissNotificationAjaxPage(this));
		pageToadletRegistry.addPage(new CreatePostAjaxPage(this));
		pageToadletRegistry.addPage(new CreateReplyAjaxPage(this));
		pageToadletRegistry.addPage(new GetReplyAjaxPage(this, replyTemplate));
		pageToadletRegistry.addPage(new GetPostAjaxPage(this, postTemplate));
		pageToadletRegistry.addPage(new GetLinkedElementAjaxPage(this, elementLoader, linkedElementRenderFilter));
		pageToadletRegistry.addPage(new GetTimesAjaxPage(this, timeTextConverter, l10nFilter, TimeZone.getDefault()));
		pageToadletRegistry.addPage(new MarkAsKnownAjaxPage(this));
		pageToadletRegistry.addPage(new DeletePostAjaxPage(this));
		pageToadletRegistry.addPage(new DeleteReplyAjaxPage(this));
		pageToadletRegistry.addPage(new LockSoneAjaxPage(this));
		pageToadletRegistry.addPage(new UnlockSoneAjaxPage(this));
		pageToadletRegistry.addPage(new FollowSoneAjaxPage(this));
		pageToadletRegistry.addPage(new UnfollowSoneAjaxPage(this));
		pageToadletRegistry.addPage(new EditAlbumAjaxPage(this));
		pageToadletRegistry.addPage(new EditImageAjaxPage(this, parserFilter, shortenFilter, renderFilter));
		pageToadletRegistry.addPage(new TrustAjaxPage(this));
		pageToadletRegistry.addPage(new DistrustAjaxPage(this));
		pageToadletRegistry.addPage(new UntrustAjaxPage(this));
		pageToadletRegistry.addPage(new LikeAjaxPage(this));
		pageToadletRegistry.addPage(new UnlikeAjaxPage(this));
		pageToadletRegistry.addPage(new GetLikesAjaxPage(this));
		pageToadletRegistry.addPage(new BookmarkAjaxPage(this));
		pageToadletRegistry.addPage(new UnbookmarkAjaxPage(this));
		pageToadletRegistry.addPage(new EditProfileFieldAjaxPage(this));
		pageToadletRegistry.addPage(new DeleteProfileFieldAjaxPage(this));
		pageToadletRegistry.addPage(new MoveProfileFieldAjaxPage(this));

		pageToadletRegistry.registerToadlets();
	}

	/**
	 * Returns all {@link Sone#isLocal() local Sone}s that are referenced by
	 * {@link SonePart}s in the given text (after parsing it using
	 * {@link SoneTextParser}).
	 *
	 * @param text
	 *            The text to parse
	 * @return All mentioned local Sones
	 */
	private Collection<Sone> getMentionedSones(String text) {
		/* we need no context to find mentioned Sones. */
		Set<Sone> mentionedSones = new HashSet<>();
		for (Part part : soneTextParser.parse(text, null)) {
			if (part instanceof SonePart) {
				mentionedSones.add(((SonePart) part).getSone());
			}
		}
		return Collections2.filter(mentionedSones, Sone.LOCAL_SONE_FILTER);
	}

	/**
	 * Returns the Sone insert notification for the given Sone. If no
	 * notification for the given Sone exists, a new notification is created and
	 * cached.
	 *
	 * @param sone
	 *            The Sone to get the insert notification for
	 * @return The Sone insert notification
	 */
	private TemplateNotification getSoneInsertNotification(Sone sone) {
		synchronized (soneInsertNotifications) {
			TemplateNotification templateNotification = soneInsertNotifications.get(sone);
			if (templateNotification == null) {
				templateNotification = new TemplateNotification(loaders.loadTemplate("/templates/notify/soneInsertNotification.html"));
				templateNotification.set("insertSone", sone);
				soneInsertNotifications.put(sone, templateNotification);
			}
			return templateNotification;
		}
	}

	private boolean localSoneMentionedInNewPostOrReply(Post post) {
		if (!post.getSone().isLocal()) {
			if (!getMentionedSones(post.getText()).isEmpty() && !post.isKnown()) {
				return true;
			}
		}
		for (PostReply postReply : getCore().getReplies(post.getId())) {
			if (postReply.getSone().isLocal()) {
				continue;
			}
			if (!getMentionedSones(postReply.getText()).isEmpty() && !postReply.isKnown()) {
				return true;
			}
		}
		return false;
	}

	//
	// EVENT HANDLERS
	//

	/**
	 * Notifies the web interface that a new {@link Sone} was found.
	 *
	 * @param newSoneFoundEvent
	 *            The event
	 */
	@Subscribe
	public void newSoneFound(NewSoneFoundEvent newSoneFoundEvent) {
		newSoneNotification.add(newSoneFoundEvent.sone());
		if (!hasFirstStartNotification()) {
			notificationManager.addNotification(newSoneNotification);
		}
	}

	/**
	 * Notifies the web interface that a new {@link Post} was found.
	 *
	 * @param newPostFoundEvent
	 *            The event
	 */
	@Subscribe
	public void newPostFound(NewPostFoundEvent newPostFoundEvent) {
		Post post = newPostFoundEvent.post();
		boolean isLocal = post.getSone().isLocal();
		if (isLocal) {
			localPostNotification.add(post);
		} else {
			newPostNotification.add(post);
		}
		if (!hasFirstStartNotification()) {
			notificationManager.addNotification(isLocal ? localPostNotification : newPostNotification);
			if (!getMentionedSones(post.getText()).isEmpty() && !isLocal) {
				mentionNotification.add(post);
				notificationManager.addNotification(mentionNotification);
			}
		} else {
			getCore().markPostKnown(post);
		}
	}

	/**
	 * Notifies the web interface that a new {@link PostReply} was found.
	 *
	 * @param newPostReplyFoundEvent
	 *            The event
	 */
	@Subscribe
	public void newReplyFound(NewPostReplyFoundEvent newPostReplyFoundEvent) {
		PostReply reply = newPostReplyFoundEvent.postReply();
		boolean isLocal = reply.getSone().isLocal();
		if (isLocal) {
			localReplyNotification.add(reply);
		} else {
			newReplyNotification.add(reply);
		}
		if (!hasFirstStartNotification()) {
			notificationManager.addNotification(isLocal ? localReplyNotification : newReplyNotification);
			if (reply.getPost().isPresent() && localSoneMentionedInNewPostOrReply(reply.getPost().get())) {
				mentionNotification.add(reply.getPost().get());
				notificationManager.addNotification(mentionNotification);
			}
		} else {
			getCore().markReplyKnown(reply);
		}
	}

	/**
	 * Notifies the web interface that a {@link Sone} was marked as known.
	 *
	 * @param markSoneKnownEvent
	 *            The event
	 */
	@Subscribe
	public void markSoneKnown(MarkSoneKnownEvent markSoneKnownEvent) {
		newSoneNotification.remove(markSoneKnownEvent.sone());
	}

	@Subscribe
	public void markPostKnown(MarkPostKnownEvent markPostKnownEvent) {
		removePost(markPostKnownEvent.post());
	}

	@Subscribe
	public void markReplyKnown(MarkPostReplyKnownEvent markPostReplyKnownEvent) {
		removeReply(markPostReplyKnownEvent.postReply());
	}

	@Subscribe
	public void soneRemoved(SoneRemovedEvent soneRemovedEvent) {
		newSoneNotification.remove(soneRemovedEvent.sone());
	}

	@Subscribe
	public void postRemoved(PostRemovedEvent postRemovedEvent) {
		removePost(postRemovedEvent.post());
	}

	private void removePost(Post post) {
		newPostNotification.remove(post);
		localPostNotification.remove(post);
		if (!localSoneMentionedInNewPostOrReply(post)) {
			mentionNotification.remove(post);
		}
	}

	@Subscribe
	public void replyRemoved(PostReplyRemovedEvent postReplyRemovedEvent) {
		removeReply(postReplyRemovedEvent.postReply());
	}

	private void removeReply(PostReply reply) {
		newReplyNotification.remove(reply);
		localReplyNotification.remove(reply);
		if (reply.getPost().isPresent() && !localSoneMentionedInNewPostOrReply(reply.getPost().get())) {
			mentionNotification.remove(reply.getPost().get());
		}
	}

	/**
	 * Notifies the web interface that a Sone was locked.
	 *
	 * @param soneLockedEvent
	 *            The event
	 */
	@Subscribe
	public void soneLocked(SoneLockedEvent soneLockedEvent) {
		final Sone sone = soneLockedEvent.sone();
		ScheduledFuture<?> tickerObject = ticker.schedule(new Runnable() {

			@Override
			@SuppressWarnings("synthetic-access")
			public void run() {
				lockedSonesNotification.add(sone);
				notificationManager.addNotification(lockedSonesNotification);
			}
		}, 5, TimeUnit.MINUTES);
		lockedSonesTickerObjects.put(sone, tickerObject);
	}

	/**
	 * Notifies the web interface that a Sone was unlocked.
	 *
	 * @param soneUnlockedEvent
	 *            The event
	 */
	@Subscribe
	public void soneUnlocked(SoneUnlockedEvent soneUnlockedEvent) {
		lockedSonesNotification.remove(soneUnlockedEvent.sone());
		lockedSonesTickerObjects.remove(soneUnlockedEvent.sone()).cancel(false);
	}

	/**
	 * Notifies the web interface that a {@link Sone} is being inserted.
	 *
	 * @param soneInsertingEvent
	 *            The event
	 */
	@Subscribe
	public void soneInserting(SoneInsertingEvent soneInsertingEvent) {
		TemplateNotification soneInsertNotification = getSoneInsertNotification(soneInsertingEvent.sone());
		soneInsertNotification.set("soneStatus", "inserting");
		if (soneInsertingEvent.sone().getOptions().isSoneInsertNotificationEnabled()) {
			notificationManager.addNotification(soneInsertNotification);
		}
	}

	/**
	 * Notifies the web interface that a {@link Sone} was inserted.
	 *
	 * @param soneInsertedEvent
	 *            The event
	 */
	@Subscribe
	public void soneInserted(SoneInsertedEvent soneInsertedEvent) {
		TemplateNotification soneInsertNotification = getSoneInsertNotification(soneInsertedEvent.sone());
		soneInsertNotification.set("soneStatus", "inserted");
		soneInsertNotification.set("insertDuration", soneInsertedEvent.insertDuration() / 1000);
		if (soneInsertedEvent.sone().getOptions().isSoneInsertNotificationEnabled()) {
			notificationManager.addNotification(soneInsertNotification);
		}
	}

	/**
	 * Notifies the web interface that a {@link Sone} insert was aborted.
	 *
	 * @param soneInsertAbortedEvent
	 *            The event
	 */
	@Subscribe
	public void soneInsertAborted(SoneInsertAbortedEvent soneInsertAbortedEvent) {
		TemplateNotification soneInsertNotification = getSoneInsertNotification(soneInsertAbortedEvent.sone());
		soneInsertNotification.set("soneStatus", "insert-aborted");
		soneInsertNotification.set("insert-error", soneInsertAbortedEvent.cause());
		if (soneInsertAbortedEvent.sone().getOptions().isSoneInsertNotificationEnabled()) {
			notificationManager.addNotification(soneInsertNotification);
		}
	}

	/**
	 * Notifies the web interface that a new Sone version was found.
	 *
	 * @param updateFoundEvent
	 *            The event
	 */
	@Subscribe
	public void updateFound(UpdateFoundEvent updateFoundEvent) {
		newVersionNotification.set("latestVersion", updateFoundEvent.version());
		newVersionNotification.set("latestEdition", updateFoundEvent.latestEdition());
		newVersionNotification.set("releaseTime", updateFoundEvent.releaseTime());
		newVersionNotification.set("disruptive", updateFoundEvent.disruptive());
		notificationManager.addNotification(newVersionNotification);
	}

	/**
	 * Notifies the web interface that an image insert was started
	 *
	 * @param imageInsertStartedEvent
	 *            The event
	 */
	@Subscribe
	public void imageInsertStarted(ImageInsertStartedEvent imageInsertStartedEvent) {
		insertingImagesNotification.add(imageInsertStartedEvent.image());
		notificationManager.addNotification(insertingImagesNotification);
	}

	/**
	 * Notifies the web interface that an {@link Image} insert was aborted.
	 *
	 * @param imageInsertAbortedEvent
	 *            The event
	 */
	@Subscribe
	public void imageInsertAborted(ImageInsertAbortedEvent imageInsertAbortedEvent) {
		insertingImagesNotification.remove(imageInsertAbortedEvent.image());
	}

	/**
	 * Notifies the web interface that an {@link Image} insert is finished.
	 *
	 * @param imageInsertFinishedEvent
	 *            The event
	 */
	@Subscribe
	public void imageInsertFinished(ImageInsertFinishedEvent imageInsertFinishedEvent) {
		insertingImagesNotification.remove(imageInsertFinishedEvent.image());
		insertedImagesNotification.add(imageInsertFinishedEvent.image());
		notificationManager.addNotification(insertedImagesNotification);
	}

	/**
	 * Notifies the web interface that an {@link Image} insert has failed.
	 *
	 * @param imageInsertFailedEvent
	 *            The event
	 */
	@Subscribe
	public void imageInsertFailed(ImageInsertFailedEvent imageInsertFailedEvent) {
		insertingImagesNotification.remove(imageInsertFailedEvent.image());
		imageInsertFailedNotification.add(imageInsertFailedEvent.image());
		notificationManager.addNotification(imageInsertFailedNotification);
	}

}
