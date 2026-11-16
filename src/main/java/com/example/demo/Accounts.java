package com.example.demo;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

interface Accounts extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> lock(@Param("id") UUID id);
}

interface Confirmations extends JpaRepository<Confirmation, String> {
    @Modifying
    @Query("delete from Confirmation c where c.account.id = :id")
    void revoke(@Param("id") UUID id);

    @Query("select c.account.id from Confirmation c where c.hash = :hash")
    Optional<UUID> owner(@Param("hash") String hash);
}
