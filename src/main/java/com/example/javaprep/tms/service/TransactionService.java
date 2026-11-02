package com.example.javaprep.tms.service;

import com.example.javaprep.tms.exception.AccountNotFoundException;
import com.example.javaprep.tms.exception.TmsException;
import com.example.javaprep.tms.exception.ValidationException;
import com.example.javaprep.tms.executor.TransactionExecutor;
import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;
import com.example.javaprep.tms.model.TransactionStatus;
import com.example.javaprep.tms.model.TransactionType;
import com.example.javaprep.tms.repository.AccountRepository;
import com.example.javaprep.tms.repository.TransactionRepository;
import com.example.javaprep.tms.validator.ValidationPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ValidationPipeline validationPipeline;
    private final List<TransactionExecutor> executors;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              ValidationPipeline validationPipeline,
                              List<TransactionExecutor> executors) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.validationPipeline = Objects.requireNonNull(validationPipeline);
        this.executors = Objects.requireNonNull(executors);
    }

    public Account createAccount(String id, BigDecimal initialBalance) {
        Account account = new Account(id, initialBalance);
        accountRepository.save(account);
        log.info("Created account: id={}, initialBalance={}", id, initialBalance);
        return account;
    }

    public Transaction transfer(String transactionId, String fromAccountId, String toAccountId, BigDecimal amount) {
        Transaction transaction = new Transaction(transactionId, TransactionType.TRANSFER, fromAccountId, toAccountId, amount);
        return process(transaction);
    }

    public Transaction deposit(String transactionId, String toAccountId, BigDecimal amount) {
        Transaction transaction = new Transaction(transactionId, TransactionType.DEPOSIT, null, toAccountId, amount);
        return process(transaction);
    }

    public Transaction withdraw(String transactionId, String fromAccountId, BigDecimal amount) {
        Transaction transaction = new Transaction(transactionId, TransactionType.WITHDRAWAL, fromAccountId, null, amount);
        return process(transaction);
    }

    public List<Transaction> getTransactionHistory(String accountId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        return transactionRepository.findByAccountId(accountId);
    }

    public Transaction process(Transaction transaction) {
        transactionRepository.save(transaction);
        log.info("Processing transaction: id={}, type={}, from={}, to={}, amount={}",
                transaction.getId(), transaction.getType(), transaction.getFromAccountId(),
                transaction.getToAccountId(), transaction.getAmount());

        Account fromAccount = null;
        Account toAccount = null;

        try {
            if (transaction.getFromAccountId() != null) {
                fromAccount = accountRepository.findById(transaction.getFromAccountId())
                        .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + transaction.getFromAccountId()));
            }

            if (transaction.getToAccountId() != null) {
                toAccount = accountRepository.findById(transaction.getToAccountId())
                        .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + transaction.getToAccountId()));
            }

            // 1. Run Validation Pipeline
            validationPipeline.validate(transaction, fromAccount, toAccount);

            // 2. Resolve and Execute Strategy
            Account finalFromAccount = fromAccount;
            Account finalToAccount = toAccount;
            TransactionExecutor executor = executors.stream()
                    .filter(e -> e.supports(transaction.getType()))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Unsupported transaction type: " + transaction.getType()));

            executor.execute(transaction, finalFromAccount, finalToAccount);

            transactionRepository.save(transaction);
            log.info("Transaction completed successfully: id={}", transaction.getId());
            return transaction;

        } catch (TmsException ex) {
            transaction.markFailed(ex.getMessage());
            transactionRepository.save(transaction);
            log.warn("Transaction failed: id={}, type={}, from={}, to={}, amount={}, failureReason={}",
                    transaction.getId(), transaction.getType(), transaction.getFromAccountId(),
                    transaction.getToAccountId(), transaction.getAmount(), ex.getMessage());
            return transaction;
        } catch (Exception ex) {
            String errorMsg = "Unexpected system error: " + ex.getMessage();
            transaction.markFailed(errorMsg);
            transactionRepository.save(transaction);
            log.error("Transaction failed with unexpected error: id={}", transaction.getId(), ex);
            return transaction;
        }
    }
}
