package com.example.javaprep.tms;

import com.example.javaprep.tms.exception.InsufficientBalanceException;
import com.example.javaprep.tms.exception.ValidationException;
import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;
import com.example.javaprep.tms.model.TransactionStatus;
import com.example.javaprep.tms.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = TmsFactory.createDefaultService(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("Should successfully transfer funds between two accounts")
    void testSuccessfulTransfer() {
        Account a = service.createAccount("ACC-1", new BigDecimal("1000.00"));
        Account b = service.createAccount("ACC-2", new BigDecimal("500.00"));

        Transaction tx = service.transfer("TX-1", a.getId(), b.getId(), new BigDecimal("250.00"));

        assertEquals(TransactionStatus.COMPLETED, tx.getStatus());
        assertEquals(new BigDecimal("750.00"), a.getBalance());
        assertEquals(new BigDecimal("750.00"), b.getBalance());
        assertNull(tx.getFailureReason());
    }

    @Test
    @DisplayName("Should return FAILED status when balance is insufficient without throwing exceptions")
    void testInsufficientBalanceFails() {
        Account a = service.createAccount("ACC-1", new BigDecimal("100.00"));
        Account b = service.createAccount("ACC-2", new BigDecimal("500.00"));

        Transaction tx = service.transfer("TX-2", a.getId(), b.getId(), new BigDecimal("250.00"));

        assertEquals(TransactionStatus.FAILED, tx.getStatus());
        assertTrue(tx.getFailureReason().contains("Insufficient balance"));
        assertEquals(new BigDecimal("100.00"), a.getBalance());
        assertEquals(new BigDecimal("500.00"), b.getBalance());
    }

    @Test
    @DisplayName("Should return FAILED status when transfer amount exceeds limit without throwing exceptions")
    void testExceedsTransactionLimitFails() {
        Account a = service.createAccount("ACC-1", new BigDecimal("200000.00"));
        Account b = service.createAccount("ACC-2", new BigDecimal("500.00"));

        Transaction tx = service.transfer("TX-3", a.getId(), b.getId(), new BigDecimal("150000.00"));

        assertEquals(TransactionStatus.FAILED, tx.getStatus());
        assertTrue(tx.getFailureReason().contains("exceeds maximum allowed limit"));
        assertEquals(new BigDecimal("200000.00"), a.getBalance());
    }

    @Test
    @DisplayName("Should return FAILED status on self-transfers without throwing exceptions")
    void testSelfTransferFails() {
        Account a = service.createAccount("ACC-1", new BigDecimal("1000.00"));

        Transaction tx = service.transfer("TX-4", a.getId(), a.getId(), new BigDecimal("100.00"));

        assertEquals(TransactionStatus.FAILED, tx.getStatus());
        assertTrue(tx.getFailureReason().contains("cannot be identical"));
    }

    @Test
    @DisplayName("Should support Deposit and Withdrawal seamlessly (OCP)")
    void testDepositAndWithdrawal() {
        Account a = service.createAccount("ACC-1", new BigDecimal("500.00"));

        Transaction depositTx = service.deposit("TX-DEP", a.getId(), new BigDecimal("200.00"));
        assertEquals(TransactionStatus.COMPLETED, depositTx.getStatus());
        assertEquals(new BigDecimal("700.00"), a.getBalance());

        Transaction withdrawTx = service.withdraw("TX-WITH", a.getId(), new BigDecimal("150.00"));
        assertEquals(TransactionStatus.COMPLETED, withdrawTx.getStatus());
        assertEquals(new BigDecimal("550.00"), a.getBalance());
    }

    @Test
    @DisplayName("High-concurrency bidirectional transfers: Deadlock-free and Money Conservation guarantee")
    void testConcurrentBidirectionalTransfersDeadlockFree() throws InterruptedException {
        Account a = service.createAccount("ACC-A", new BigDecimal("100000.00"));
        Account b = service.createAccount("ACC-B", new BigDecimal("100000.00"));

        BigDecimal initialTotal = a.getBalance().add(b.getBalance());

        int threadCount = 20;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger txCounter = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final boolean forward = (i % 2 == 0);
            executor.submit(() -> {
                try {
                    startLatch.await(); // Thundering herd
                    for (int j = 0; j < operationsPerThread; j++) {
                        String txId = "TX-CONC-" + txCounter.incrementAndGet();
                        BigDecimal transferAmount = new BigDecimal("10.00");
                        if (forward) {
                            service.transfer(txId, a.getId(), b.getId(), transferAmount);
                        } else {
                            service.transfer(txId, b.getId(), a.getId(), transferAmount);
                        }
                    }
                } catch (Exception e) {
                    fail("Concurrent transfer threw unexpected exception: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads at once
        finishLatch.await();
        executor.shutdown();

        // 1. Check no deadlocks occurred and all 1,000 transactions finished
        assertEquals(1000, txCounter.get());

        // 2. Fundamental Invariant: Conservation of Money
        BigDecimal finalTotal = a.getBalance().add(b.getBalance());
        assertEquals(0, initialTotal.compareTo(finalTotal),
                "Total money in the system must remain invariant after concurrent transfers!");
    }

    @Test
    @DisplayName("Should maintain complete and isolated transaction history per account (O(1) Secondary Index)")
    void testTransactionHistoryRetrieval() {
        Account a = service.createAccount("ACC-A", new BigDecimal("1000.00"));
        Account b = service.createAccount("ACC-B", new BigDecimal("500.00"));

        // 1. Transfer A -> B
        service.transfer("TX-1", a.getId(), b.getId(), new BigDecimal("200.00"));

        // 2. Deposit to A
        service.deposit("TX-2", a.getId(), new BigDecimal("300.00"));

        // 3. Withdrawal from B
        service.withdraw("TX-3", b.getId(), new BigDecimal("100.00"));

        // 4. Failed transfer A -> B (exceeds balance) - returns FAILED transaction cleanly
        Transaction tx4 = service.transfer("TX-4", a.getId(), b.getId(), new BigDecimal("50000.00"));
        assertEquals(TransactionStatus.FAILED, tx4.getStatus());

        var aHistory = service.getTransactionHistory(a.getId());
        var bHistory = service.getTransactionHistory(b.getId());

        // Account A should see: TX-1 (Transfer), TX-2 (Deposit), TX-4 (Failed Transfer) = 3 transactions
        assertEquals(3, aHistory.size());
        assertEquals("TX-1", aHistory.get(0).getId());
        assertEquals(TransactionStatus.COMPLETED, aHistory.get(0).getStatus());
        assertEquals("TX-2", aHistory.get(1).getId());
        assertEquals("TX-4", aHistory.get(2).getId());
        assertEquals(TransactionStatus.FAILED, aHistory.get(2).getStatus());

        // Account B should see: TX-1 (Transfer), TX-3 (Withdrawal), TX-4 (Failed Transfer targeting B) = 3 transactions
        assertEquals(3, bHistory.size());
        assertEquals("TX-1", bHistory.get(0).getId());
        assertEquals("TX-3", bHistory.get(1).getId());
        assertEquals("TX-4", bHistory.get(2).getId());
    }
}
