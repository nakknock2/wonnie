package com.woori.wonnie.subscription;

import com.woori.wonnie.finance.BusinessClock;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SubscriptionCheckoutService {
    private final JdbcOperations jdbc;
    private final BusinessClock clock;

    public SubscriptionCheckoutService(JdbcOperations jdbc, BusinessClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CheckoutOptions options(String customerId, String serviceName, String planCode) {
        requireCustomer(customerId);
        var service = SubscriptionCheckoutCatalog.service(serviceName);
        var selectedPlan = planCode == null || planCode.isBlank() ? null : SubscriptionCheckoutCatalog.plan(service, planCode);
        var defaultPlan = service.plans().stream().filter(PlanOption::recommended).findFirst().orElse(service.plans().get(0));
        var price = selectedPlan == null ? defaultPlan.price() : selectedPlan.price();
        var owned = cards(customerId, service.id(), price, true);
        var recommended = cards(customerId, service.id(), price, false).stream()
                .filter(card -> "우리카드".equals(card.issuer()))
                .toList();
        return new CheckoutOptions(service.id(), service.name(), service.category(), service.brandColor(),
                service.initial(), service.plans(), selectedPlan, owned, recommended, true, service.pricingNote());
    }

    @Transactional
    public IssuedCard issueCard(String customerId, String productId, boolean allTermsAgreed) {
        requireCustomer(customerId);
        if (!allTermsAgreed) throw badRequest("카드 발급 필수 약관에 모두 동의해 주세요.");
        var product = jdbc.query("""
                SELECT id,name,card_type,annual_fee,summary,COALESCE(issuer_name,'') issuer_name
                FROM card_product WHERE id=?
                """, (rs, rowNum) -> new Product(rs.getString("id"), rs.getString("name"),
                rs.getString("card_type"), rs.getBigDecimal("annual_fee"), rs.getString("summary"),
                rs.getString("issuer_name")), productId).stream().findFirst()
                .orElseThrow(() -> badRequest("선택한 카드 상품을 찾을 수 없어요."));
        if (!"우리카드".equals(product.issuer())) throw badRequest("우리카드 상품만 바로 발급할 수 있어요.");

        var existing = jdbc.query("""
                SELECT id,masked_number FROM card WHERE customer_id=? AND card_product_id=? AND status='ACTIVE'
                """, (rs, rowNum) -> new IssuedCard(rs.getString("id"), product.id(), product.name(),
                rs.getString("masked_number"), product.annualFee(), false), customerId, productId);
        if (!existing.isEmpty()) return existing.get(0);

        var id = "card-" + UUID.randomUUID();
        var lastFour = String.format(Locale.ROOT, "%04d", Math.floorMod(id.hashCode(), 10_000));
        var issuedOn = clock.today();
        var expiresOn = YearMonth.from(issuedOn.plusYears(5)).atEndOfMonth();
        BigDecimal limit = "CREDIT".equals(product.cardType()) ? new BigDecimal("3000000") : null;
        jdbc.update("""
                INSERT INTO card (id,customer_id,card_product_id,masked_number,expires_on,status,credit_limit,
                  available_limit,billing_day,settlement_account_id,revolving,issued_on,annual_fee_month,
                  outstanding_amount,revolving_balance,revolving_apr)
                VALUES (?,?,?,?,?,'ACTIVE',?,?,14,NULL,FALSE,?,?,0,NULL,NULL)
                """, id, customerId, product.id(), "****" + lastFour, expiresOn, limit, limit,
                issuedOn, issuedOn.getMonthValue());
        saveConsent(customerId, "CARD_ISSUANCE", true);
        return new IssuedCard(id, product.id(), product.name(), "****" + lastFour, product.annualFee(), true);
    }

    @Transactional
    public CheckoutResult subscribe(String customerId, UUID requestId, String serviceName, String planCode,
            String cardId, boolean allTermsAgreed) {
        requireCustomer(customerId);
        if (!allTermsAgreed) throw badRequest("정기결제 필수 약관에 모두 동의해 주세요.");
        var service = SubscriptionCheckoutCatalog.service(serviceName);
        var selectedPlan = SubscriptionCheckoutCatalog.plan(service, planCode);
        var subscriptionId = "sub-checkout-" + requestId;
        var previous = jdbc.query("""
                SELECT customer_id,service_id,card_id,user_label,amount,billing_cycle FROM subscription WHERE id=?
                """, (rs, rowNum) -> new ExistingCheckout(rs.getString("customer_id"), rs.getString("service_id"),
                rs.getString("card_id"), rs.getString("user_label"), rs.getBigDecimal("amount"),
                rs.getString("billing_cycle")), subscriptionId);
        if (!previous.isEmpty()) {
            var existing = previous.get(0);
            if (!customerId.equals(existing.customerId()) || !service.id().equals(existing.serviceId())
                    || !cardId.equals(existing.cardId()) || !selectedPlan.name().equals(existing.planName())
                    || selectedPlan.price().compareTo(existing.amount()) != 0
                    || !selectedPlan.billingCycle().equals(existing.billingCycle())) {
                throw conflict("이미 다른 결제에 사용된 요청 ID예요. 결제 내용을 다시 확인해 주세요.");
            }
            return result(customerId, subscriptionId, cardId, service.name(), selectedPlan, false);
        }

        var card = jdbc.query("""
                SELECT c.id,c.status,c.available_limit,p.card_type,p.name,p.id product_id
                FROM card c JOIN card_product p ON p.id=c.card_product_id
                WHERE c.id=? AND c.customer_id=?
                """, (rs, rowNum) -> new PaymentCard(rs.getString("id"), rs.getString("status"),
                rs.getBigDecimal("available_limit"), rs.getString("card_type"), rs.getString("name"),
                rs.getString("product_id")), cardId, customerId).stream().findFirst()
                .orElseThrow(() -> badRequest("보유 중인 카드만 결제에 사용할 수 있어요."));
        if (!"ACTIVE".equals(card.status())) throw conflict("사용할 수 없는 카드예요.");
        if (card.availableLimit() != null && card.availableLimit().compareTo(selectedPlan.price()) < 0) {
            throw conflict("카드 이용 가능 한도가 부족해요.");
        }
        var duplicate = jdbc.queryForObject("""
                SELECT COUNT(*) FROM subscription
                WHERE customer_id=? AND service_id=? AND status IN ('ACTIVE','TRIAL')
                """, Integer.class, customerId, service.id());
        if (duplicate != null && duplicate > 0) throw conflict(service.name() + " 구독을 이미 이용 중이에요.");

        var today = clock.today();
        var now = LocalDateTime.now(ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO subscription (id,customer_id,card_id,merchant_id,service_id,display_name,user_label,
                  category,billing_cycle,amount,currency,original_amount,previous_amount,charge_day,first_charged_on,
                  last_charged_on,next_charge_on,status,status_changed_at,created_at,updated_at,detection_confidence,
                  user_confirmed,detected_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,'KRW',NULL,NULL,?,?,?,?, 'ACTIVE',?,?,?,?,TRUE,?)
                """, subscriptionId, customerId, card.id(), service.merchantId(), service.id(), service.name(),
                selectedPlan.name(), service.category(), selectedPlan.billingCycle(), selectedPlan.price(),
                today.getDayOfMonth(), today, today, today.plusMonths(selectedPlan.billingMonths()),
                now, now, now, new BigDecimal("1.0000"), now);
        jdbc.update("""
                INSERT INTO card_transaction (id,card_id,customer_id,approved_at,merchant_id,merchant_name_raw,
                  category,amount,currency,original_amount,exchange_rate,status,canceled_at,decline_reason,subscription_id)
                VALUES (?,?,?,?,?,?,'SUBSCRIPTION',?,'KRW',?,NULL,'APPROVED',NULL,NULL,?)
                """, "tx-checkout-" + requestId, card.id(), customerId, now, service.merchantId(), service.merchantName(),
                selectedPlan.price(), selectedPlan.price(), subscriptionId);
        jdbc.update("""
                UPDATE card SET outstanding_amount=COALESCE(outstanding_amount,0)+?,
                  available_limit=CASE WHEN available_limit IS NULL THEN NULL ELSE available_limit-? END
                WHERE id=? AND customer_id=?
                """, selectedPlan.price(), selectedPlan.price(), card.id(), customerId);
        saveConsent(customerId, "SUBSCRIPTION_PURCHASE", true);
        return result(customerId, subscriptionId, card.id(), service.name(), selectedPlan, true);
    }

    private CheckoutResult result(String customerId, String subscriptionId, String cardId, String serviceName, PlanOption selectedPlan,
            boolean created) {
        var cardName = jdbc.queryForObject("""
                SELECT p.name FROM card c JOIN card_product p ON p.id=c.card_product_id WHERE c.id=?
                """, String.class, cardId);
        return new CheckoutResult(subscriptionId, serviceName, selectedPlan, cardId, cardName,
                selectedPlan.price(), clock.today(), "APPROVED", created, subscriptions(customerId));
    }

    private List<SubscriptionItem> subscriptions(String customerId) {
        return jdbc.query("""
                SELECT s.id,s.display_name,s.user_label,s.amount,s.billing_cycle,s.next_charge_on,s.status,
                  COALESCE(v.brand_color,'#2789ec') brand_color,COALESCE(v.initial_text,SUBSTRING(s.display_name,1,1)) initial_text,
                  p.name card_name,c.masked_number
                FROM subscription s LEFT JOIN service v ON v.id=s.service_id
                JOIN card c ON c.id=s.card_id JOIN card_product p ON p.id=c.card_product_id
                WHERE s.customer_id=? AND s.status IN ('ACTIVE','TRIAL') ORDER BY s.next_charge_on,s.display_name
                """, (rs, rowNum) -> new SubscriptionItem(rs.getString("id"), rs.getString("display_name"),
                rs.getString("user_label"), rs.getBigDecimal("amount"), rs.getString("billing_cycle"),
                rs.getObject("next_charge_on", LocalDate.class), rs.getString("status"),
                rs.getString("brand_color"), rs.getString("initial_text"), rs.getString("card_name"),
                rs.getString("masked_number")), customerId);
    }

    private List<CardOption> cards(String customerId, String serviceId, BigDecimal price, boolean owned) {
        var sql = owned ? """
                SELECT c.id,p.id product_id,p.name,COALESCE(p.issuer_name,'') issuer_name,c.masked_number,
                  p.annual_fee,p.summary,c.status
                FROM card c JOIN card_product p ON p.id=c.card_product_id
                WHERE c.customer_id=? AND c.status='ACTIVE'
                """ : """
                SELECT p.id,p.id product_id,p.name,COALESCE(p.issuer_name,'') issuer_name,NULL masked_number,
                  p.annual_fee,p.summary,'CANDIDATE' status
                FROM card_product p WHERE NOT EXISTS (
                  SELECT 1 FROM card c WHERE c.customer_id=? AND c.card_product_id=p.id AND c.status='ACTIVE')
                """;
        return jdbc.query(sql, (rs, rowNum) -> cardOption(rs, customerId, serviceId, price, owned), customerId)
                .stream().sorted(Comparator.comparing(CardOption::estimatedDiscount).reversed()
                        .thenComparing(Comparator.comparing(CardOption::discountRate).reversed())
                        .thenComparing(CardOption::annualFee))
                .toList();
    }

    private CardOption cardOption(ResultSet rs, String customerId, String serviceId, BigDecimal price, boolean owned)
            throws SQLException {
        var productId = rs.getString("product_id");
        var benefit = jdbc.query("""
                SELECT discount_rate,monthly_cap,min_prev_month_spend,note FROM card_product_benefit
                WHERE card_product_id=? AND (service_id=? OR service_id IS NULL)
                ORDER BY CASE WHEN service_id=? THEN 0 ELSE 1 END,discount_rate DESC LIMIT 1
                """, (benefitRs, rowNum) -> new Benefit(benefitRs.getBigDecimal("discount_rate"),
                benefitRs.getBigDecimal("monthly_cap"), benefitRs.getBigDecimal("min_prev_month_spend"),
                benefitRs.getString("note")), productId, serviceId, serviceId).stream().findFirst().orElse(Benefit.none());
        var previousSpend = BigDecimal.ZERO;
        if (owned) {
            previousSpend = jdbc.queryForObject("""
                    SELECT COALESCE(MAX(spend_amount),0) FROM card_monthly_stat WHERE card_id=?
                    """, BigDecimal.class, rs.getString("id"));
            if (previousSpend == null) previousSpend = BigDecimal.ZERO;
        }
        var potential = price.multiply(benefit.rate()).divide(new BigDecimal("100"), 0, RoundingMode.DOWN)
                .min(benefit.cap());
        var ready = !owned || previousSpend.compareTo(benefit.minimumSpend()) >= 0;
        var estimated = ready ? potential : BigDecimal.ZERO;
        var colors = cardColors(productId);
        return new CardOption(rs.getString("id"), productId, rs.getString("name"), rs.getString("issuer_name"),
                owned, rs.getString("masked_number"), rs.getBigDecimal("annual_fee"), benefit.rate(), potential,
                estimated, benefit.minimumSpend(), previousSpend, ready, benefit.note(), rs.getString("summary"),
                colors[0], colors[1], cardImage(productId));
    }

    private void saveConsent(String customerId, String type, boolean required) {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM consent WHERE customer_id=? AND consent_type=?",
                Integer.class, customerId, type);
        var now = LocalDateTime.now(ZoneOffset.UTC);
        if (count != null && count > 0) {
            jdbc.update("UPDATE consent SET required=?,agreed=TRUE,agreed_at=?,withdrawn_at=NULL,decided_at=? WHERE customer_id=? AND consent_type=?",
                    required, now, now, customerId, type);
        } else {
            jdbc.update("INSERT INTO consent (id,customer_id,consent_type,required,agreed,agreed_at,decided_at) VALUES (?,?,?,?,TRUE,?,?)",
                    "cons-" + UUID.randomUUID(), customerId, type, required, now, now);
        }
    }

    private void requireCustomer(String customerId) {
        if (customerId == null || customerId.isBlank() || customerId.length() > 100) throw badRequest("고객 정보가 올바르지 않아요.");
        var exists = jdbc.queryForObject("SELECT COUNT(*) FROM customer WHERE id=?", Integer.class, customerId);
        if (exists == null || exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "고객 정보를 찾을 수 없어요.");
    }

    private static String[] cardColors(String productId) {
        return switch (productId) {
            case "prd-daily" -> new String[]{"#171b2e", "#596784"};
            case "prd-routine" -> new String[]{"#283b64", "#8ba6d9"};
            default -> new String[]{"#176b87", "#64ccc5"};
        };
    }

    private static String cardImage(String productId) {
        return switch (productId) {
            case "prd-daily" -> "woori-daily";
            case "prd-routine" -> "woori-routine";
            case "prd-jeongseok2" -> "woori-jeongseok2";
            default -> null;
        };
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    public record PlanOption(String code, String name, BigDecimal price, String description, boolean recommended,
                             String billingCycle) {
        int billingMonths() { return "YEARLY".equals(billingCycle) ? 12 : 1; }
    }
    public record CardOption(String id, String productId, String name, String issuer, boolean owned,
                             String maskedNumber, BigDecimal annualFee, BigDecimal discountRate,
                             BigDecimal potentialDiscount, BigDecimal estimatedDiscount, BigDecimal minimumSpend,
                             BigDecimal previousMonthSpend, boolean benefitReady, String benefitNote, String summary,
                             String gradientStart, String gradientEnd, String imageKey) {}
    public record CheckoutOptions(String serviceId, String serviceName, String category, String brandColor,
                                  String initial, List<PlanOption> plans, PlanOption selectedPlan,
                                  List<CardOption> ownedCards, List<CardOption> recommendedCards,
                                  boolean mockTransaction, String pricingNote) {}
    public record IssuedCard(String cardId, String productId, String name, String maskedNumber,
                             BigDecimal annualFee, boolean newlyIssued) {}
    public record SubscriptionItem(String id, String name, String plan, BigDecimal price, String billingCycle,
                                   LocalDate nextPaymentDate, String status, String color, String letter,
                                   String cardName, String maskedNumber) {}
    public record CheckoutResult(String subscriptionId, String serviceName, PlanOption plan, String cardId,
                                 String cardName, BigDecimal approvedAmount, LocalDate approvedOn, String status,
                                 boolean created, List<SubscriptionItem> subscriptions) {}
    private record ExistingCheckout(String customerId, String serviceId, String cardId, String planName,
                                    BigDecimal amount, String billingCycle) {}
    private record Product(String id, String name, String cardType, BigDecimal annualFee, String summary, String issuer) {}
    private record PaymentCard(String id, String status, BigDecimal availableLimit, String cardType,
                               String name, String productId) {}
    private record Benefit(BigDecimal rate, BigDecimal cap, BigDecimal minimumSpend, String note) {
        static Benefit none() { return new Benefit(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "구독 할인 혜택 없음"); }
    }
}
