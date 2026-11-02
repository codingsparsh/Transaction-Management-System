package com.example.javaprep;

import com.example.javaprep.tms.TmsFactory;
import com.example.javaprep.tms.exception.TmsException;
import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;
import com.example.javaprep.tms.service.TransactionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class JavaprepApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(JavaprepApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("\n" + "=".repeat(75));
		System.out.println("     FINTECH TRANSACTION MANAGEMENT SYSTEM (TMS) — LIVE DEMO");
		System.out.println("=".repeat(75));

		// 1. Initialize Service with ₹100,000 single transaction limit
		BigDecimal maxLimit = new BigDecimal("100000.00");
		TransactionService tms = TmsFactory.createDefaultService(maxLimit);

		// 2. Setup Accounts
		System.out.println("\n[1] SETUP ACCOUNTS");
		Account alice = tms.createAccount("ACC-ALICE", new BigDecimal("10000.00"));
		Account bob = tms.createAccount("ACC-BOB", new BigDecimal("5000.00"));
		System.out.printf("  Created: %s | Balance: ₹%s%n", alice.getId(), alice.getBalance());
		System.out.printf("  Created: %s | Balance: ₹%s%n", bob.getId(), bob.getBalance());

		// 3. Happy Path Transfer
		System.out.println("\n[2] HAPPY PATH: ACCOUNT-TO-ACCOUNT TRANSFER");
		Transaction tx1 = tms.transfer("TX-001", alice.getId(), bob.getId(), new BigDecimal("2500.00"));
		System.out.printf("  Transfer ₹2500.00 from Alice -> Bob%n");
		System.out.printf("  Status: %s | FailureReason: %s%n", tx1.getStatus(), tx1.getFailureReason());
		System.out.printf("  Alice Balance: ₹%s | Bob Balance: ₹%s%n", alice.getBalance(), bob.getBalance());

		// 4. Extensibility: Cash Operations (Deposit & Withdrawal)
		System.out.println("\n[3] EXTENSIBILITY (OCP): CASH DEPOSIT & WITHDRAWAL");
		Transaction depTx = tms.deposit("TX-DEP-001", alice.getId(), new BigDecimal("1000.00"));
		System.out.printf("  Deposited ₹1000.00 to Alice. Status: %s | New Balance: ₹%s%n", depTx.getStatus(),
				alice.getBalance());

		Transaction withTx = tms.withdraw("TX-WITH-001", bob.getId(), new BigDecimal("500.00"));
		System.out.printf("  Withdrew ₹500.00 from Bob. Status: %s | New Balance: ₹%s%n", withTx.getStatus(),
				bob.getBalance());

		// 4. Validation Rules & State Lifecycle Failures (Non-Crashing Result Pattern)
		System.out.println("\n[4] VALIDATION RULES & FAILURE AUDITING (NON-CRASHING RESULT PATTERN)");

		// 4.1 Transaction Limit Check
		System.out.println("  Testing Limit Exceeded (Attempting transfer of ₹150,000.00 with ₹100,000.00 limit):");
		Transaction txLimitFail = tms.transfer("TX-FAIL-LIMIT", alice.getId(), bob.getId(),
				new BigDecimal("150000.00"));
		System.out.printf("  -> [Status: %s] User Message: %s%n", txLimitFail.getStatus(),
				txLimitFail.getFailureReason());

		// 4.2 Insufficient Balance Check
		System.out.println("  Testing Insufficient Balance (Attempting transfer of ₹50,000.00 from Alice):");
		Transaction txBalanceFail = tms.transfer("TX-FAIL-BALANCE", alice.getId(), bob.getId(),
				new BigDecimal("50000.00"));
		System.out.printf("  -> [Status: %s] User Message: %s%n", txBalanceFail.getStatus(),
				txBalanceFail.getFailureReason());

		// 4.3 Self-Transfer Check
		System.out.println("  Testing Self-Transfer (Alice -> Alice):");
		Transaction txSelfFail = tms.transfer("TX-FAIL-SELF", alice.getId(), alice.getId(), new BigDecimal("100.00"));
		System.out.printf("  -> [Status: %s] User Message: %s%n", txSelfFail.getStatus(),
				txSelfFail.getFailureReason());

		// 5. Account Passbook / Transaction History (O(1) Secondary Index)
		System.out.println("\n[5] ACCOUNT PASSBOOK / TRANSACTION HISTORY (O(1) SECONDARY INDEX)");
		System.out.printf("  Fetching ledger passbook for Alice (%s):%n", alice.getId());
		var aliceHistory = tms.getTransactionHistory(alice.getId());
		for (Transaction tx : aliceHistory) {
			System.out.printf(
					"   - [%-9s] ID: %-15s | Type: %-10s | Amount: ₹%-8s | From: %-9s | To: %-9s | Note: %s%n",
					tx.getStatus(), tx.getId(), tx.getType(), tx.getAmount(),
					tx.getFromAccountId() != null ? tx.getFromAccountId() : "N/A",
					tx.getToAccountId() != null ? tx.getToAccountId() : "N/A",
					tx.getFailureReason() != null ? tx.getFailureReason() : "Success");
		}

		// // 6. Concurrency & Deadlock Prevention Demo
		// System.out.println("\n[6] CONCURRENCY & DEADLOCK-FREE BIDIRECTIONAL STRESS
		// TEST");
		// System.out.println(" Launching 200 concurrent transfers (Alice->Bob AND
		// Bob->Alice) across 20 threads...");

		// BigDecimal totalBefore = alice.getBalance().add(bob.getBalance());
		// int threadCount = 20;
		// int transfersPerThread = 10;
		// ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		// CountDownLatch startGate = new CountDownLatch(1);
		// CountDownLatch doneGate = new CountDownLatch(threadCount);
		// AtomicInteger completedCount = new AtomicInteger(0);

		// for (int i = 0; i < threadCount; i++) {
		// final boolean forward = (i % 2 == 0);
		// pool.submit(() -> {
		// try {
		// startGate.await();
		// for (int j = 0; j < transfersPerThread; j++) {
		// String txId = "TX-STRESS-" + completedCount.incrementAndGet();
		// if (forward) {
		// tms.transfer(txId, alice.getId(), bob.getId(), new BigDecimal("10.00"));
		// } else {
		// tms.transfer(txId, bob.getId(), alice.getId(), new BigDecimal("10.00"));
		// }
		// }
		// } catch (Exception ignored) {
		// } finally {
		// doneGate.countDown();
		// }
		// });
		// }

		// startGate.countDown();
		// doneGate.await();
		// pool.shutdown();

		// BigDecimal totalAfter = alice.getBalance().add(bob.getBalance());
		// System.out.printf(" Completed Transfers: %d%n", completedCount.get());
		// System.out.printf(" Total Money Invariant: Total Before: ₹%s | Total After:
		// ₹%s | Conserved: %b%n",
		// totalBefore, totalAfter, totalBefore.compareTo(totalAfter) == 0);
		// System.out.printf(" Final Balances: Alice: ₹%s | Bob: ₹%s%n",
		// alice.getBalance(), bob.getBalance());

		// System.out.println("=".repeat(75));
		// System.out.println(" ALL VERIFICATIONS PASSED: ZERO DEADLOCKS, THREAD-SAFE
		// CONCURRENCY");
		// System.out.println("=".repeat(75) + "\n");
	}
}
