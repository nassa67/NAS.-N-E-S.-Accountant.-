package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveUserAccountBusinessClientInteractionAndTransaction_succeedsForOwner() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val accountResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureOrSaveUserAccount(
                displayName = "Alice Photographer",
                email = ALICE_EMAIL,
                studioRole = "Lead Photographer & Owner"
            )
        }
        assertTrue(accountResult.isSuccess)
        val savedAccount = accountResult.getOrThrow()
        assertEquals("Alice Photographer", savedAccount.displayName)

        val observedAccount = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserAccount().first { it != null }
        }
        assertNotNull(observedAccount)
        assertEquals("Lead Photographer & Owner", observedAccount?.studioRole)

        val bizResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBusiness(
                name = "GLOD PHOTO",
                category = "Wedding & Event",
                defaultPricePerPicture = 3000.0,
                monthlyTarget = 60000.0,
                accentColorHex = "#6366F1"
            )
        }
        assertTrue(bizResult.isSuccess)
        val businessId = bizResult.getOrThrow()

        val clientResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createClient(
                businessId = businessId,
                name = "Elena Rostova",
                phoneNumber = "+1 555-889-1200",
                email = "elena@example.com",
                notes = "Full wedding album package"
            )
        }
        assertTrue(clientResult.isSuccess)
        val clientId = clientResult.getOrThrow()

        val interactionResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInteraction(
                businessId = businessId,
                clientId = clientId,
                clientName = "Elena Rostova",
                type = "CALL",
                summary = "Discussed shot list and timeline",
                interactionDateMillis = System.currentTimeMillis()
            )
        }
        assertTrue(interactionResult.isSuccess)

        val txResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTransaction(
                businessId = businessId,
                clientId = clientId,
                clientName = "Elena Rostova",
                customerPhone = "+1 555-889-1200",
                transactionDateMillis = System.currentTimeMillis(),
                pictureCount = 5L,
                pricePerPicture = 3000.0,
                paymentMethod = "BANK_TRANSFER",
                notes = "Deposit + gallery delivery"
            )
        }
        assertTrue(txResult.isSuccess)
        val txId = txResult.getOrThrow()

        val transactions = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions().first { list -> list.any { it.id == txId } }
        }
        val savedTx = transactions.first { it.id == txId }
        assertEquals(15000.0, savedTx.totalAmount, 0.01)
        assertEquals("BANK_TRANSFER", savedTx.paymentMethod)

        // Verify adjusting price & adding extra pictures on an existing transaction
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateTransaction(
                savedTx.copy(
                    pictureCount = 10L,
                    pricePerPicture = 3000.0,
                    totalAmount = 30000.0,
                    notes = "Deposit + gallery delivery + 5 additional pictures"
                )
            )
        }
        assertTrue(updateResult.isSuccess)

        val updatedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions().first { list ->
                list.any { it.id == txId && it.totalAmount == 30000.0 }
            }
        }
        val updatedTx = updatedList.first { it.id == txId }
        assertEquals(10L, updatedTx.pictureCount)
        assertEquals(30000.0, updatedTx.totalAmount, 0.01)
    }

    @Test
    fun getBusinessById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudioRepository(firestore)
        val bizId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createBusiness(
                name = "Alice Private Studio",
                category = "Portraits",
                defaultPricePerPicture = 20.0,
                monthlyTarget = 4000.0,
                accentColorHex = "#10B981"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = StudioRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getBusinessById(aliceUid, bizId)
        }
        assertTrue(crossReadResult.isFailure)
        val firestoreException = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeBusinesses_unauthenticatedCaller_fails() = runBlocking {
        auth.signOut()
        val repository = StudioRepository(firestore)
        val result = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeBusinesses().first()
            }
        }
        assertTrue(result.isFailure)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_nas@test.com"
        const val BOB_EMAIL = "bob_nas@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
