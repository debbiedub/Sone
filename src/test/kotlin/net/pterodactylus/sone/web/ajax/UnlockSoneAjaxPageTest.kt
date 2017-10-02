package net.pterodactylus.sone.web.ajax

import net.pterodactylus.sone.data.Sone
import net.pterodactylus.sone.test.mock
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.mockito.Mockito.verify

/**
 * Unit test for [UnlockSoneAjaxPage].
 */
class UnlockSoneAjaxPageTest : JsonPageTest("unlockSone.ajax", requiresLogin = false, pageSupplier = ::UnlockSoneAjaxPage) {

	@Test
	fun `request without sone results in invalid-sone-id`() {
		assertThat(json.isSuccess, equalTo(false))
		assertThat(json.error, equalTo("invalid-sone-id"))
	}

	@Test
	fun `request with invalid sone results in invalid-sone-id`() {
		addRequestParameter("sone", "invalid")
		assertThat(json.isSuccess, equalTo(false))
		assertThat(json.error, equalTo("invalid-sone-id"))
	}

	@Test
	fun `request with valid sone results in locked sone`() {
		val sone = mock<Sone>()
		addLocalSone(sone, "sone-id")
		addRequestParameter("sone", "sone-id")
		assertThat(json.isSuccess, equalTo(true))
		verify(core).unlockSone(sone)
	}

}
