package com.example.javaprep.tms.repository;

import com.example.javaprep.tms.model.Account;

import java.util.Optional;

public interface AccountRepository {
    void save(Account account);
    Optional<Account> findById(String id);
}
