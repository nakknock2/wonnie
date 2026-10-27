package com.woori.wonnie.finance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceCardRepository extends JpaRepository<FinanceCard, String> {
    java.util.List<FinanceCard> findAllByCustomerId(String customerId);
}
