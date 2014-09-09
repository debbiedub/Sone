package net.pterodactylus.sone.core;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;
import static net.pterodactylus.sone.Matchers.isAlbum;
import static net.pterodactylus.sone.Matchers.isImage;
import static net.pterodactylus.sone.Matchers.isPost;
import static net.pterodactylus.sone.Matchers.isPostReply;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import net.pterodactylus.sone.core.ConfigurationSoneParser.InvalidAlbumFound;
import net.pterodactylus.sone.core.ConfigurationSoneParser.InvalidImageFound;
import net.pterodactylus.sone.core.ConfigurationSoneParser.InvalidParentAlbumFound;
import net.pterodactylus.sone.core.ConfigurationSoneParser.InvalidPostFound;
import net.pterodactylus.sone.core.ConfigurationSoneParser.InvalidPostReplyFound;
import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Album.Modifier;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Profile;
import net.pterodactylus.sone.data.Profile.Field;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.AlbumBuilder;
import net.pterodactylus.sone.database.AlbumBuilderFactory;
import net.pterodactylus.sone.database.ImageBuilder;
import net.pterodactylus.sone.database.ImageBuilderFactory;
import net.pterodactylus.sone.database.PostBuilder;
import net.pterodactylus.sone.database.PostBuilderFactory;
import net.pterodactylus.sone.database.PostReplyBuilder;
import net.pterodactylus.sone.database.PostReplyBuilderFactory;
import net.pterodactylus.util.config.Configuration;
import net.pterodactylus.util.config.ConfigurationException;
import net.pterodactylus.util.config.Value;

import com.google.common.base.Optional;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Unit test for {@link ConfigurationSoneParser}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ConfigurationSoneParserTest {

	private final Configuration configuration = mock(Configuration.class);
	private final Sone sone = mock(Sone.class);
	private final ConfigurationSoneParser configurationSoneParser;

	public ConfigurationSoneParserTest() {
		when(sone.getId()).thenReturn("1");
		configurationSoneParser =
				new ConfigurationSoneParser(configuration, sone);
	}

	@Test
	public void emptyProfileIsLoadedCorrectly() {
		setupEmptyProfile();
		Profile profile = configurationSoneParser.parseProfile();
		assertThat(profile, notNullValue());
		assertThat(profile.getFirstName(), nullValue());
		assertThat(profile.getMiddleName(), nullValue());
		assertThat(profile.getLastName(), nullValue());
		assertThat(profile.getBirthDay(), nullValue());
		assertThat(profile.getBirthMonth(), nullValue());
		assertThat(profile.getBirthYear(), nullValue());
		assertThat(profile.getFields(), emptyIterable());
	}

	private void setupEmptyProfile() {
		when(configuration.getStringValue(anyString())).thenReturn(
				new TestValue<String>(null));
		when(configuration.getIntValue(anyString())).thenReturn(
				new TestValue<Integer>(null));
	}

	@Test
	public void filledProfileWithFieldsIsParsedCorrectly() {
		setupFilledProfile();
		Profile profile = configurationSoneParser.parseProfile();
		assertThat(profile, notNullValue());
		assertThat(profile.getFirstName(), is("First"));
		assertThat(profile.getMiddleName(), is("M."));
		assertThat(profile.getLastName(), is("Last"));
		assertThat(profile.getBirthDay(), is(18));
		assertThat(profile.getBirthMonth(), is(12));
		assertThat(profile.getBirthYear(), is(1976));
		final List<Field> fields = profile.getFields();
		assertThat(fields, hasSize(2));
		assertThat(fields.get(0).getName(), is("Field1"));
		assertThat(fields.get(0).getValue(), is("Value1"));
		assertThat(fields.get(1).getName(), is("Field2"));
		assertThat(fields.get(1).getValue(), is("Value2"));
	}

	private void setupFilledProfile() {
		setupString("Sone/1/Profile/FirstName", "First");
		setupString("Sone/1/Profile/MiddleName", "M.");
		setupString("Sone/1/Profile/LastName", "Last");
		setupInteger("Sone/1/Profile/BirthDay", 18);
		setupInteger("Sone/1/Profile/BirthMonth", 12);
		setupInteger("Sone/1/Profile/BirthYear", 1976);
		setupString("Sone/1/Profile/Fields/0/Name", "Field1");
		setupString("Sone/1/Profile/Fields/0/Value", "Value1");
		setupString("Sone/1/Profile/Fields/1/Name", "Field2");
		setupString("Sone/1/Profile/Fields/1/Value", "Value2");
		setupString("Sone/1/Profile/Fields/2/Name", null);
	}

	private void setupString(String nodeName, String value) {
		when(configuration.getStringValue(eq(nodeName))).thenReturn(
				new TestValue<String>(value));
	}

	private void setupInteger(String nodeName, Integer value) {
		when(configuration.getIntValue(eq(nodeName))).thenReturn(
				new TestValue<Integer>(value));
	}

	@Test
	public void postsAreParsedCorrectly() {
		setupCompletePosts();
		PostBuilderFactory postBuilderFactory = createPostBuilderFactory();
		Collection<Post> posts =
				configurationSoneParser.parsePosts(postBuilderFactory);
		assertThat(posts,
				Matchers.<Post>containsInAnyOrder(
						isPost("P0", 1000L, "T0", Optional.<String>absent()),
						isPost("P1", 1001L, "T1",
								of("1234567890123456789012345678901234567890123"))));
	}

	private PostBuilderFactory createPostBuilderFactory() {
		PostBuilderFactory postBuilderFactory =
				mock(PostBuilderFactory.class);
		when(postBuilderFactory.newPostBuilder()).thenAnswer(
				new Answer<PostBuilder>() {
					@Override
					public PostBuilder answer(InvocationOnMock invocation)
					throws Throwable {
						return new TestPostBuilder();
					}
				});
		return postBuilderFactory;
	}

	private void setupCompletePosts() {
		setupPost("0", "P0", 1000L, "T0", null);
		setupPost("1", "P1", 1001L, "T1",
				"1234567890123456789012345678901234567890123");
		setupPost("2", null, 0L, null, null);
	}

	private void setupPost(String postNumber, String postId, long time,
			String text, String recipientId) {
		setupString("Sone/1/Posts/" + postNumber + "/ID", postId);
		setupLong("Sone/1/Posts/" + postNumber + "/Time", time);
		setupString("Sone/1/Posts/" + postNumber + "/Text", text);
		setupString("Sone/1/Posts/" + postNumber + "/Recipient", recipientId);
	}

	private void setupLong(String nodeName, Long value) {
		when(configuration.getLongValue(eq(nodeName))).thenReturn(
				new TestValue<Long>(value));
	}

	@Test(expected = InvalidPostFound.class)
	public void postWithoutTimeIsRecognized() {
		setupPostWithoutTime();
		configurationSoneParser.parsePosts(createPostBuilderFactory());
	}

	private void setupPostWithoutTime() {
		setupPost("0", "P0", 0L, "T0", null);
	}

	@Test(expected = InvalidPostFound.class)
	public void postWithoutTextIsRecognized() {
		setupPostWithoutText();
		configurationSoneParser.parsePosts(createPostBuilderFactory());
	}

	private void setupPostWithoutText() {
		setupPost("0", "P0", 1000L, null, null);
	}

	@Test
	public void postWithInvalidRecipientIdIsRecognized() {
		setupPostWithInvalidRecipientId();
		Collection<Post> posts = configurationSoneParser.parsePosts(
				createPostBuilderFactory());
		assertThat(posts, contains(
				isPost("P0", 1000L, "T0", Optional.<String>absent())));
	}

	private void setupPostWithInvalidRecipientId() {
		setupPost("0", "P0", 1000L, "T0", "123");
		setupPost("1", null, 0L, null, null);
	}

	@Test
	public void postRepliesAreParsedCorrectly() {
		setupPostReplies();
		PostReplyBuilderFactory postReplyBuilderFactory =
				new PostReplyBuilderFactory() {
					@Override
					public PostReplyBuilder newPostReplyBuilder() {
						return new TestPostReplyBuilder();
					}
				};
		Collection<PostReply> postReplies =
				configurationSoneParser.parsePostReplies(
						postReplyBuilderFactory);
		assertThat(postReplies, hasSize(2));
		assertThat(postReplies,
				containsInAnyOrder(isPostReply("R0", "P0", 1000L, "T0"),
						isPostReply("R1", "P1", 1001L, "T1")));
	}

	private void setupPostReplies() {
		setupPostReply("0", "R0", "P0", 1000L, "T0");
		setupPostReply("1", "R1", "P1", 1001L, "T1");
		setupPostReply("2", null, null, 0L, null);
	}

	private void setupPostReply(String postReplyNumber, String postReplyId,
			String postId, long time, String text) {
		setupString("Sone/1/Replies/" + postReplyNumber + "/ID", postReplyId);
		setupString("Sone/1/Replies/" + postReplyNumber + "/Post/ID", postId);
		setupLong("Sone/1/Replies/" + postReplyNumber + "/Time", time);
		setupString("Sone/1/Replies/" + postReplyNumber + "/Text", text);
	}

	@Test(expected = InvalidPostReplyFound.class)
	public void missingPostIdIsRecognized() {
		setupPostReplyWithMissingPostId();
		configurationSoneParser.parsePostReplies(null);
	}

	private void setupPostReplyWithMissingPostId() {
		setupPostReply("0", "R0", null, 1000L, "T0");
	}

	@Test(expected = InvalidPostReplyFound.class)
	public void missingPostReplyTimeIsRecognized() {
		setupPostReplyWithMissingPostReplyTime();
		configurationSoneParser.parsePostReplies(null);
	}

	private void setupPostReplyWithMissingPostReplyTime() {
		setupPostReply("0", "R0", "P0", 0L, "T0");
	}

	@Test(expected = InvalidPostReplyFound.class)
	public void missingPostReplyTextIsRecognized() {
		setupPostReplyWithMissingPostReplyText();
		configurationSoneParser.parsePostReplies(null);
	}

	private void setupPostReplyWithMissingPostReplyText() {
		setupPostReply("0", "R0", "P0", 1000L, null);
	}

	@Test
	public void likedPostIdsParsedCorrectly() {
		setupLikedPostIds();
		Set<String> likedPostIds =
				configurationSoneParser.parseLikedPostIds();
		assertThat(likedPostIds, containsInAnyOrder("P1", "P2", "P3"));
	}

	private void setupLikedPostIds() {
		setupString("Sone/1/Likes/Post/0/ID", "P1");
		setupString("Sone/1/Likes/Post/1/ID", "P2");
		setupString("Sone/1/Likes/Post/2/ID", "P3");
		setupString("Sone/1/Likes/Post/3/ID", null);
	}

	@Test
	public void likedPostReplyIdsAreParsedCorrectly() {
		setupLikedPostReplyIds();
		Set<String> likedPostReplyIds =
				configurationSoneParser.parseLikedPostReplyIds();
		assertThat(likedPostReplyIds, containsInAnyOrder("R1", "R2", "R3"));
	}

	private void setupLikedPostReplyIds() {
		setupString("Sone/1/Likes/Reply/0/ID", "R1");
		setupString("Sone/1/Likes/Reply/1/ID", "R2");
		setupString("Sone/1/Likes/Reply/2/ID", "R3");
		setupString("Sone/1/Likes/Reply/3/ID", null);
	}

	@Test
	public void friendsAreParsedCorrectly() {
		setupFriends();
		Set<String> friends = configurationSoneParser.parseFriends();
		assertThat(friends, containsInAnyOrder("F1", "F2", "F3"));
	}

	private void setupFriends() {
		setupString("Sone/1/Friends/0/ID", "F1");
		setupString("Sone/1/Friends/1/ID", "F2");
		setupString("Sone/1/Friends/2/ID", "F3");
		setupString("Sone/1/Friends/3/ID", null);
	}

	@Test
	public void topLevelAlbumsAreParsedCorrectly() {
		setupTopLevelAlbums();
		AlbumBuilderFactory albumBuilderFactory = createAlbumBuilderFactory();
		List<Album> topLevelAlbums =
				configurationSoneParser.parseTopLevelAlbums(
						albumBuilderFactory);
		assertThat(topLevelAlbums, hasSize(2));
		Album firstAlbum = topLevelAlbums.get(0);
		assertThat(firstAlbum, isAlbum("A1", null, "T1", "D1", "I1"));
		assertThat(firstAlbum.getAlbums(), emptyIterable());
		assertThat(firstAlbum.getImages(), emptyIterable());
		Album secondAlbum = topLevelAlbums.get(1);
		assertThat(secondAlbum, isAlbum("A2", null, "T2", "D2", null));
		assertThat(secondAlbum.getAlbums(), hasSize(1));
		assertThat(secondAlbum.getImages(), emptyIterable());
		Album thirdAlbum = secondAlbum.getAlbums().get(0);
		assertThat(thirdAlbum, isAlbum("A3", "A2", "T3", "D3", "I3"));
		assertThat(thirdAlbum.getAlbums(), emptyIterable());
		assertThat(thirdAlbum.getImages(), emptyIterable());
	}

	private void setupTopLevelAlbums() {
		setupAlbum(0, "A1", null, "T1", "D1", "I1");
		setupAlbum(1, "A2", null, "T2", "D2", null);
		setupAlbum(2, "A3", "A2", "T3", "D3", "I3");
		setupAlbum(3, null, null, null, null, null);
	}

	private void setupAlbum(int albumNumber, String albumId,
			String parentAlbumId,
			String title, String description, String imageId) {
		final String albumPrefix = "Sone/1/Albums/" + albumNumber;
		setupString(albumPrefix + "/ID", albumId);
		setupString(albumPrefix + "/Title", title);
		setupString(albumPrefix + "/Description", description);
		setupString(albumPrefix + "/Parent", parentAlbumId);
		setupString(albumPrefix + "/AlbumImage", imageId);
	}

	private AlbumBuilderFactory createAlbumBuilderFactory() {
		AlbumBuilderFactory albumBuilderFactory =
				mock(AlbumBuilderFactory.class);
		when(albumBuilderFactory.newAlbumBuilder()).thenAnswer(
				new Answer<AlbumBuilder>() {
					@Override
					public AlbumBuilder answer(InvocationOnMock invocation) {
						return new TestAlbumBuilder();
					}
				});
		return albumBuilderFactory;
	}

	@Test(expected = InvalidAlbumFound.class)
	public void albumWithInvalidTitleIsRecognized() {
		setupAlbum(0, "A1", null, null, "D1", "I1");
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
	}

	@Test(expected = InvalidAlbumFound.class)
	public void albumWithInvalidDescriptionIsRecognized() {
		setupAlbum(0, "A1", null, "T1", null, "I1");
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
	}

	@Test(expected = InvalidParentAlbumFound.class)
	public void albumWithInvalidParentIsRecognized() {
		setupAlbum(0, "A1", "A0", "T1", "D1", "I1");
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
	}

	@Test
	public void imagesAreParsedCorrectly() {
		setupTopLevelAlbums();
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
		setupImages();
		configurationSoneParser.parseImages(createImageBuilderFactory());
		Map<String, Album> albums = configurationSoneParser.getAlbums();
		assertThat(albums.get("A1").getImages(),
				contains(isImage("I1", 1000L, "K1", "T1", "D1", 16, 9)));
		assertThat(albums.get("A2").getImages(), contains(
				isImage("I2", 2000L, "K2", "T2", "D2", 16 * 2, 9 * 2)));
		assertThat(albums.get("A3").getImages(), contains(
				isImage("I3", 3000L, "K3", "T3", "D3", 16 * 3, 9 * 3)));
	}

	private void setupImages() {
		setupImage(0, "I1", "A1", 1000L, "K1", "T1", "D1", 16, 9);
		setupImage(1, "I2", "A2", 2000L, "K2", "T2", "D2", 16 * 2, 9 * 2);
		setupImage(2, "I3", "A3", 3000L, "K3", "T3", "D3", 16 * 3, 9 * 3);
		setupImage(3, null, null, 0L, null, null, null, 0, 0);
	}

	private void setupImage(int imageNumber, String id,
			String parentAlbumId, Long creationTime, String key, String title,
			String description, Integer width, Integer height) {
		final String imagePrefix = "Sone/1/Images/" + imageNumber;
		setupString(imagePrefix + "/ID", id);
		setupString(imagePrefix + "/Album", parentAlbumId);
		setupLong(imagePrefix + "/CreationTime", creationTime);
		setupString(imagePrefix + "/Key", key);
		setupString(imagePrefix + "/Title", title);
		setupString(imagePrefix + "/Description", description);
		setupInteger(imagePrefix + "/Width", width);
		setupInteger(imagePrefix + "/Height", height);
	}

	private ImageBuilderFactory createImageBuilderFactory() {
		ImageBuilderFactory imageBuilderFactory =
				mock(ImageBuilderFactory.class);
		when(imageBuilderFactory.newImageBuilder()).thenAnswer(
				new Answer<ImageBuilder>() {
					@Override
					public ImageBuilder answer(InvocationOnMock invocation)
					throws Throwable {
						return new TestImageBuilder();
					}
				});
		return imageBuilderFactory;
	}

	@Test(expected = InvalidImageFound.class)
	public void missingAlbumIdIsRecognized() {
		setupTopLevelAlbums();
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
		setupImage(0, "I1", null, 1000L, "K1", "T1", "D1", 16, 9);
		configurationSoneParser.parseImages(createImageBuilderFactory());
	}

	@Test(expected = InvalidParentAlbumFound.class)
	public void invalidAlbumIdIsRecognized() {
		setupTopLevelAlbums();
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
		setupImage(0, "I1", "A4", 1000L, "K1", "T1", "D1", 16, 9);
		configurationSoneParser.parseImages(createImageBuilderFactory());
	}

	@Test(expected = InvalidImageFound.class)
	public void missingCreationTimeIsRecognized() {
		setupTopLevelAlbums();
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
		setupImage(0, "I1", "A1", null, "K1", "T1", "D1", 16, 9);
		configurationSoneParser.parseImages(createImageBuilderFactory());
	}

	@Test(expected = InvalidImageFound.class)
	public void missingKeyIsRecognized() {
		setupTopLevelAlbums();
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
		setupImage(0, "I1", "A1", 1000L, null, "T1", "D1", 16, 9);
		configurationSoneParser.parseImages(createImageBuilderFactory());
	}

	@Test(expected = InvalidImageFound.class)
	public void missingTitleIsRecognized() {
		setupTopLevelAlbums();
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
		setupImage(0, "I1", "A1", 1000L, "K1", null, "D1", 16, 9);
		configurationSoneParser.parseImages(createImageBuilderFactory());
	}

	@Test(expected = InvalidImageFound.class)
	public void missingDescriptionIsRecognized() {
		setupTopLevelAlbums();
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
		setupImage(0, "I1", "A1", 1000L, "K1", "T1", null, 16, 9);
		configurationSoneParser.parseImages(createImageBuilderFactory());
	}

	@Test(expected = InvalidImageFound.class)
	public void missingWidthIsRecognized() {
		setupTopLevelAlbums();
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
		setupImage(0, "I1", "A1", 1000L, "K1", "T1", "D1", null, 9);
		configurationSoneParser.parseImages(createImageBuilderFactory());
	}

	@Test(expected = InvalidImageFound.class)
	public void missingHeightIsRecognized() {
		setupTopLevelAlbums();
		configurationSoneParser.parseTopLevelAlbums(
				createAlbumBuilderFactory());
		setupImage(0, "I1", "A1", 1000L, "K1", "T1", "D1", 16, null);
		configurationSoneParser.parseImages(createImageBuilderFactory());
	}

	private static class TestValue<T> implements Value<T> {

		private final AtomicReference<T> value = new AtomicReference<T>();

		public TestValue(T originalValue) {
			value.set(originalValue);
		}

		@Override
		public T getValue() throws ConfigurationException {
			return value.get();
		}

		@Override
		public T getValue(T defaultValue) {
			final T realValue = value.get();
			return (realValue != null) ? realValue : defaultValue;
		}

		@Override
		public void setValue(T newValue) throws ConfigurationException {
			value.set(newValue);
		}

	}

	private static class TestPostBuilder implements PostBuilder {

		private final Post post = mock(Post.class);
		private String recipientId = null;

		@Override
		public PostBuilder copyPost(Post post) throws NullPointerException {
			return this;
		}

		@Override
		public PostBuilder from(String senderId) {
			final Sone sone = mock(Sone.class);
			when(sone.getId()).thenReturn(senderId);
			when(post.getSone()).thenReturn(sone);
			return this;
		}

		@Override
		public PostBuilder randomId() {
			when(post.getId()).thenReturn(randomUUID().toString());
			return this;
		}

		@Override
		public PostBuilder withId(String id) {
			when(post.getId()).thenReturn(id);
			return this;
		}

		@Override
		public PostBuilder currentTime() {
			when(post.getTime()).thenReturn(currentTimeMillis());
			return this;
		}

		@Override
		public PostBuilder withTime(long time) {
			when(post.getTime()).thenReturn(time);
			return this;
		}

		@Override
		public PostBuilder withText(String text) {
			when(post.getText()).thenReturn(text);
			return this;
		}

		@Override
		public PostBuilder to(String recipientId) {
			this.recipientId = recipientId;
			return this;
		}

		@Override
		public Post build() throws IllegalStateException {
			when(post.getRecipientId()).thenReturn(fromNullable(recipientId));
			return post;
		}

	}

	private static class TestPostReplyBuilder implements PostReplyBuilder {

		private final PostReply postReply = mock(PostReply.class);

		@Override
		public PostReplyBuilder to(String postId) {
			when(postReply.getPostId()).thenReturn(postId);
			return this;
		}

		@Override
		public PostReply build() throws IllegalStateException {
			return postReply;
		}

		@Override
		public PostReplyBuilder randomId() {
			when(postReply.getId()).thenReturn(randomUUID().toString());
			return this;
		}

		@Override
		public PostReplyBuilder withId(String id) {
			when(postReply.getId()).thenReturn(id);
			return this;
		}

		@Override
		public PostReplyBuilder from(String senderId) {
			Sone sone = mock(Sone.class);
			when(sone.getId()).thenReturn(senderId);
			when(postReply.getSone()).thenReturn(sone);
			return this;
		}

		@Override
		public PostReplyBuilder currentTime() {
			when(postReply.getTime()).thenReturn(currentTimeMillis());
			return this;
		}

		@Override
		public PostReplyBuilder withTime(long time) {
			when(postReply.getTime()).thenReturn(time);
			return this;
		}

		@Override
		public PostReplyBuilder withText(String text) {
			when(postReply.getText()).thenReturn(text);
			return this;
		}

	}

	private static class TestAlbumBuilder implements AlbumBuilder {

		private final Album album = mock(Album.class);
		private final List<Album> albums = new ArrayList<Album>();
		private final List<Image> images = new ArrayList<Image>();
		private Album parentAlbum;
		private String title;
		private String description;
		private String imageId;

		public TestAlbumBuilder() {
			when(album.getTitle()).thenAnswer(new Answer<String>() {
				@Override
				public String answer(InvocationOnMock invocation) {
					return title;
				}
			});
			when(album.getDescription()).thenAnswer(new Answer<String>() {
				@Override
				public String answer(InvocationOnMock invocation) {
					return description;
				}
			});
			when(album.getAlbumImage()).thenAnswer(new Answer<Image>() {
				@Override
				public Image answer(InvocationOnMock invocation) {
					if (imageId == null) {
						return null;
					}
					Image image = mock(Image.class);
					when(image.getId()).thenReturn(imageId);
					return image;
				}
			});
			when(album.getAlbums()).thenReturn(albums);
			when(album.getImages()).thenReturn(images);
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation) {
					albums.add((Album) invocation.getArguments()[0]);
					((Album) invocation.getArguments()[0]).setParent(album);
					return null;
				}
			}).when(album).addAlbum(any(Album.class));
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation) {
					images.add((Image) invocation.getArguments()[0]);
					return null;
				}
			}).when(album).addImage(any(Image.class));
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation) {
					parentAlbum = (Album) invocation.getArguments()[0];
					return null;
				}
			}).when(album).setParent(any(Album.class));
			when(album.getParent()).thenAnswer(new Answer<Album>() {
				@Override
				public Album answer(InvocationOnMock invocation) {
					return parentAlbum;
				}
			});
			when(album.modify()).thenReturn(new Modifier() {
				@Override
				public Modifier setTitle(String title) {
					TestAlbumBuilder.this.title = title;
					return this;
				}

				@Override
				public Modifier setDescription(String description) {
					TestAlbumBuilder.this.description = description;
					return this;
				}

				@Override
				public Modifier setAlbumImage(String imageId) {
					TestAlbumBuilder.this.imageId = imageId;
					return this;
				}

				@Override
				public Album update() throws IllegalStateException {
					return album;
				}
			});
		}

		@Override
		public AlbumBuilder randomId() {
			when(album.getId()).thenReturn(randomUUID().toString());
			return this;
		}

		@Override
		public AlbumBuilder withId(String id) {
			when(album.getId()).thenReturn(id);
			return this;
		}

		@Override
		public AlbumBuilder by(Sone sone) {
			when(album.getSone()).thenReturn(sone);
			return this;
		}

		@Override
		public Album build() throws IllegalStateException {
			return album;
		}

	}

	private static class TestImageBuilder implements ImageBuilder {

		private final Image image;

		private TestImageBuilder() {
			image = mock(Image.class);
			Image.Modifier imageModifier = new Image.Modifier() {
				private Sone sone = image.getSone();
				private long creationTime = image.getCreationTime();
				private String key = image.getKey();
				private String title = image.getTitle();
				private String description = image.getDescription();
				private int width = image.getWidth();
				private int height = image.getHeight();

				@Override
				public Image.Modifier setSone(Sone sone) {
					this.sone = sone;
					return this;
				}

				@Override
				public Image.Modifier setCreationTime(long creationTime) {
					this.creationTime = creationTime;
					return this;
				}

				@Override
				public Image.Modifier setKey(String key) {
					this.key = key;
					return this;
				}

				@Override
				public Image.Modifier setTitle(String title) {
					this.title = title;
					return this;
				}

				@Override
				public Image.Modifier setDescription(String description) {
					this.description = description;
					return this;
				}

				@Override
				public Image.Modifier setWidth(int width) {
					this.width = width;
					return this;
				}

				@Override
				public Image.Modifier setHeight(int height) {
					this.height = height;
					return this;
				}

				@Override
				public Image update() throws IllegalStateException {
					when(image.getSone()).thenReturn(sone);
					when(image.getCreationTime()).thenReturn(creationTime);
					when(image.getKey()).thenReturn(key);
					when(image.getTitle()).thenReturn(title);
					when(image.getDescription()).thenReturn(description);
					when(image.getWidth()).thenReturn(width);
					when(image.getHeight()).thenReturn(height);
					return image;
				}
			};
			when(image.modify()).thenReturn(imageModifier);
		}

		@Override
		public ImageBuilder randomId() {
			when(image.getId()).thenReturn(randomUUID().toString());
			return this;
		}

		@Override
		public ImageBuilder withId(String id) {
			when(image.getId()).thenReturn(id);
			return this;
		}

		@Override
		public Image build() throws IllegalStateException {
			return image;
		}

	}

}
