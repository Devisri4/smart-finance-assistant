package com.devisri.finance.budget;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BudgetRepository extends MongoRepository<Budget, String> {

    List<Budget> findByUserOrderByCategoryAsc(String user);

    Optional<Budget> findByUserAndCategory(String user, String category);

    Optional<Budget> findByIdAndUser(String id, String user);

    void deleteByUser(String user);
}
