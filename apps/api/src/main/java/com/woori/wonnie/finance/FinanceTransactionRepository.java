package com.woori.wonnie.finance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, String> {
    java.util.List<FinanceTransaction> findAllByCustomerId(String customerId);
}
