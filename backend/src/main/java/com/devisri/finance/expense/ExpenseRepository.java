package com.devisri.finance.expense;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExpenseRepository extends MongoRepository<Expense, String> {

    List<Expense> findByUserOrderByDateDescCreatedAtDesc(String user);

    Optional<Expense> findByIdAndUser(String id, String user);

    void deleteByUser(String user);
}
