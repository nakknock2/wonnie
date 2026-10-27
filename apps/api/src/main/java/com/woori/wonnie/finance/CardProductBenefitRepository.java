package com.woori.wonnie.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CardProductBenefitRepository extends JpaRepository<CardProductBenefit,String> {
    List<CardProductBenefit> findAllByCardProductId(String cardProductId);
}
