package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DataStoreManager
import com.example.util.PinHasher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppLockSecurityTest {

    @Test
    fun testPinLengthConstantValue() {
        assertEquals(4, PinHasher.PIN_LENGTH)
    }

    @Test
    fun testPinPersistenceAndProcessRestart() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. First process instance: save a 4-digit PIN to DataStore and wait for disk write to complete
        val dataStore1 = DataStoreManager(context)
        val testPin = "1234"
        dataStore1.setCustomPin(testPin)

        // Read stored hash to confirm write completed
        val storedHash1 = dataStore1.customPin.first()
        assertNotNull("Stored hash should not be null", storedHash1)

        // 2. Simulate process restart: create brand-new DataStoreManager instance reading from storage
        val dataStore2 = DataStoreManager(context)
        val loadedHash2 = dataStore2.customPin.first()
        assertNotNull("Loaded hash after process restart should not be null", loadedHash2)

        // 3. Verify correct 4-digit PIN succeeds and wrong PIN fails
        val isCorrectValid = PinHasher.verifyPin(testPin, loadedHash2)
        assertTrue("Correct PIN '1234' must succeed after process restart", isCorrectValid)

        val isWrongValid = PinHasher.verifyPin("9999", loadedHash2)
        assertFalse("Wrong PIN '9999' must fail after process restart", isWrongValid)
    }

    @Test
    fun testLegacyPinReplacement() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = DataStoreManager(context)

        // Save legacy 6-digit PIN
        dataStore.setCustomPin("123456")
        val legacyHash = dataStore.customPin.first()
        assertTrue(PinHasher.verifyPin("123456", legacyHash))

        // Replace with new 4-digit PIN
        dataStore.setCustomPin("4321")
        val newHash = dataStore.customPin.first()

        // Old 6-digit PIN should no longer verify
        assertFalse("Old 6-digit PIN must be completely replaced and fail", PinHasher.verifyPin("123456", newHash))
        // New 4-digit PIN must succeed
        assertTrue("New 4-digit PIN must succeed", PinHasher.verifyPin("4321", newHash))
    }
}
