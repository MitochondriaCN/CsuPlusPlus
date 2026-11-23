package ink.qingshuige.csuplusplus.core.network

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenProviderTest {

    @Test
    fun inMemoryTokenProviderManagesLifecycle() = runBlocking {
        val provider = InMemoryTokenProvider()
        assertTrue(provider.isReady.value)
        provider.awaitReady()
        assertNull(provider.getToken())

        provider.updateToken("abc.def.ghi")
        assertEquals("abc.def.ghi", provider.getToken())
        assertEquals("abc.def.ghi", provider.tokenFlow.value)

        provider.updateToken("   ")
        assertNull(provider.getToken())

        provider.updateToken("token-2")
        val eventDeferred = async {
            withTimeout(2_000) { provider.unauthorizedEvents.first() }
        }
        kotlinx.coroutines.yield()
        provider.onUnauthorized()
        assertNull(provider.getToken())
        assertEquals(Unit, eventDeferred.await())
    }

    @Test
    fun persistentTokenProviderPersistsAndClearsAcrossInstances() = runBlocking {
        val tempDir = Files.createTempDirectory("ksafe-token-test").toFile()
        try {
            val fileName = "test_auth_vault"
            val config = KSafeConfig(appNamespace = "ink.qingshuige.csuplusplus.test")

            val firstStore = KSafe(fileName = fileName, config = config, baseDir = tempDir)
            val firstProvider = PersistentTokenProvider(firstStore)
            assertTrue(firstProvider.isReady.value)
            assertNull(firstProvider.getToken())

            firstProvider.updateToken("jwt-secret-token")
            assertEquals("jwt-secret-token", firstProvider.getToken())
            // Give DataStore background writer a moment to flush before closing.
            delay(200)
            firstStore.close()

            val secondStore = KSafe(fileName = fileName, config = config, baseDir = tempDir)
            val secondProvider = PersistentTokenProvider(secondStore)
            assertEquals("jwt-secret-token", secondProvider.getToken())

            val eventDeferred = async {
                withTimeout(2_000) { secondProvider.unauthorizedEvents.first() }
            }
            kotlinx.coroutines.yield()
            secondProvider.onUnauthorized()
            assertNull(secondProvider.getToken())
            assertEquals(Unit, eventDeferred.await())
            delay(200)
            secondStore.close()

            val thirdStore = KSafe(fileName = fileName, config = config, baseDir = tempDir)
            val thirdProvider = PersistentTokenProvider(thirdStore)
            assertNull(thirdProvider.getToken())
            thirdStore.close()
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
