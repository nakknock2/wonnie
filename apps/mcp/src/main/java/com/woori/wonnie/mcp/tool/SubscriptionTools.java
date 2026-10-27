package com.woori.wonnie.mcp.tool;

import java.util.Map;
import java.util.UUID;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.woori.wonnie.mcp.api.WonnieApiClient;
import com.woori.wonnie.mcp.policy.PaymentFailurePolicy;
import com.woori.wonnie.mcp.policy.SubscriptionPolicyCatalog;

@Component
public class SubscriptionTools {
	private final WonnieApiClient api;
	private final SubscriptionPolicyCatalog policies;

	public SubscriptionTools(WonnieApiClient api, SubscriptionPolicyCatalog policies) {
		this.api = api;
		this.policies = policies;
	}

	@McpTool(name = "get_subscriptions", description = "현재 고객의 구독 목록, 월 환산액, 카드 소비 대비 비중과 생활요금 합계를 조회합니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> getSubscriptions() {
		return api.report("subscriptions");
	}

	@McpTool(name = "get_payment_readiness", description = "결제 예정 구독의 카드 상태, 계좌 잔액, 한도 부족, 전월실적 미달과 포인트 사용 후보를 조회합니다. 결제수단을 변경하지 않습니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> getPaymentReadiness() {
		return api.report("payment-readiness");
	}

	@McpTool(name = "get_price_changes", description = "구독료 인상 이력과 최근 거래의 원래 통화 금액·환율 변화를 조회합니다. 환율 상승과 원가격 인상을 구분합니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> getPriceChanges() {
		return api.report("price-changes");
	}

	@McpTool(name = "find_subscription_overlaps", description = "같은 용도 구독과 통신 결합·가족 혜택 중복 후보를 조회합니다. 실제 이용 중복으로 단정하지 않습니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> findSubscriptionOverlaps() {
		return api.report("overlaps");
	}

	@McpTool(name = "get_renewal_alerts", description = "무료체험 종료와 연간 갱신 일정, 연결 카드 만료와 포인트 소멸 후보를 조회합니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> getRenewalAlerts() {
		return api.report("renewals");
	}

	@McpTool(name = "get_cancellation_summary", description = "계약 해지 확인 자료와 결제 거절을 구분하고 지출 감소 예상액·전월실적 영향·리볼빙 상환 가정의 이자 감소를 계산합니다. 이체나 해지를 실행하지 않습니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> getCancellationSummary() {
		return api.report("cancellations");
	}

	@McpTool(name = "get_usage_analysis", description = "기록된 이용 횟수, 1회당 비용, 거주 지역의 대안을 조회합니다. 이용 자료가 없는 구독은 미사용으로 단정하지 않습니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> getUsageAnalysis() {
		return api.report("usage");
	}

	@McpTool(name = "recommend_cards", description = "보유·후보 카드의 혜택을 전월실적, 할인한도, 연회비, 연령, 거주지, 직업 조건과 함께 계산합니다. 실제 상품 약관이나 발급 승인을 보장하지 않습니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> recommendCards() {
		return api.report("card-recommendations");
	}

	@McpTool(name = "get_customer_context", description = "현재 고객의 연령, 지역, 직업, 소득 대비 구독비 비중과 선호 알림 시간을 조회합니다. 성별과 원시 소득은 반환하지 않습니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> getCustomerContext() {
		return api.report("customer-context");
	}

	@McpTool(name = "get_card_schedule", description = "카드대금 결제일, 카드 유효기간, 포인트 소멸일, 연회비 예정일을 조회합니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> getCardSchedule() {
		return api.report("card-schedule");
	}

	@McpTool(name = "get_financial_alerts", description = "카드·구독 데이터에 근거한 사전 안내 후보를 조회합니다. 메시지 발송이나 금융 거래는 실행하지 않습니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> getFinancialAlerts() {
		return api.report("alerts");
	}

	@McpTool(name = "explain_merchant", description = "결제명·가맹점명에 대한 매핑 후보와 추가 확인 방법을 조회합니다. APPLE.COM/BILL만으로 정확한 앱을 특정하지 않습니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> explainMerchant(
		@McpToolParam(description = "카드 내역의 원래 결제명. 예: APPLE.COM/BILL") String merchantName) {
		if (merchantName == null || merchantName.isBlank() || merchantName.length() > 200)
			throw new IllegalArgumentException("결제명은 1~200자로 입력하세요.");
		return api.report("merchant", merchantName);
	}

	@McpTool(name = "get_payment_failure_policy", description = "서비스별 결제 실패 처리 참고 자료와 조사일을 조회합니다. 경로를 모르면 UNKNOWN. 최신 실시간 약관이 아닙니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public PaymentFailurePolicy getPaymentFailurePolicy(@McpToolParam(description = "서비스명") String serviceName,
		@McpToolParam(description = "DIRECT, APP_STORE, GOOGLE_PLAY, PARTNER, UNKNOWN") String billingChannel) {
		return policies.find(serviceName, billingChannel);
	}

	@McpTool(name = "get_subscription_checkout_options", description = "구독 서비스의 요금제와 현재 고객의 보유 카드 및 미보유 우리카드 추천을 예상 할인액 순서로 조회합니다. 요금제가 없으면 가입을 실행하지 말고 고객에게 선택을 요청합니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
	public Map<String, Object> getSubscriptionCheckoutOptions(
		@McpToolParam(description = "가입할 서비스명. 예: 넷플릭스") String serviceName,
		@McpToolParam(description = "선택한 요금제 코드. 아직 선택하지 않았다면 빈 문자열") String planCode) {
		if (serviceName == null || serviceName.isBlank() || serviceName.length() > 60)
			throw new IllegalArgumentException("서비스명은 1~60자로 입력하세요.");
		if (planCode != null && planCode.length() > 40) throw new IllegalArgumentException("요금제 코드가 너무 깁니다.");
		return api.checkoutOptions(serviceName, planCode);
	}

	@McpTool(name = "issue_woori_card", description = "고객이 카드 발급 ConsentSheet에서 필수 약관 전체 동의를 명시적으로 완료한 경우에만 우리카드를 모의 발급합니다. 동의를 추측하거나 대신하지 마세요.", annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, openWorldHint = false))
	public Map<String, Object> issueWooriCard(
		@McpToolParam(description = "발급할 우리카드 상품 ID") String productId,
		@McpToolParam(description = "ConsentSheet에서 고객이 필수 약관 전체 동의를 완료했는지 여부") boolean allTermsAgreed) {
		if (productId == null || productId.isBlank() || productId.length() > 64)
			throw new IllegalArgumentException("카드 상품 ID가 올바르지 않습니다.");
		if (!allTermsAgreed) throw new IllegalArgumentException("카드 발급 동의가 필요합니다.");
		return api.issueCard(productId, true);
	}

	@McpTool(name = "create_subscription", description = "고객이 요금제와 보유 카드를 선택하고 정기결제 ConsentSheet에서 최종 결제 승인을 명시적으로 완료한 경우에만 모의 구독과 승인 거래를 생성합니다. 미보유 카드는 사용할 수 없습니다.", annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, openWorldHint = false))
	public Map<String, Object> createSubscription(
		@McpToolParam(description = "멱등 처리를 위한 UUID 요청 ID") String requestId,
		@McpToolParam(description = "서비스명") String serviceName,
		@McpToolParam(description = "고객이 선택한 요금제 코드") String planCode,
		@McpToolParam(description = "고객이 보유한 결제 카드 ID") String cardId,
		@McpToolParam(description = "ConsentSheet에서 고객이 최종 결제 승인을 완료했는지 여부") boolean allTermsAgreed) {
		try { UUID.fromString(requestId); } catch (RuntimeException error) { throw new IllegalArgumentException("요청 ID는 UUID여야 합니다."); }
		if (serviceName == null || serviceName.isBlank() || planCode == null || planCode.isBlank()
				|| cardId == null || cardId.isBlank()) throw new IllegalArgumentException("서비스, 요금제, 보유 카드를 모두 선택하세요.");
		if (!allTermsAgreed) throw new IllegalArgumentException("최종 결제 승인이 필요합니다.");
		return api.createSubscription(requestId, serviceName, planCode, cardId, true);
	}
}
