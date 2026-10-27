package com.woori.wonnie.subscription;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import com.woori.wonnie.subscription.SubscriptionCheckoutService.PlanOption;

/** Paid personal plans used by the simulated checkout. Prices checked on 2026-10-28. */
final class SubscriptionCheckoutCatalog {
    private SubscriptionCheckoutCatalog() {}

    // https://www.netflix.com/kr/
    // https://www.spotify.com/kr-ko/premium/
    // https://apps.apple.com/kr/app/chatgpt/id6448311069
    // https://blog.youtube/intl/ko-kr/news-and-events/yt-premium-lite-kr/
    // https://help.naver.com/service/30016/contents/22496
    private static final List<ServiceOption> SERVICES = List.of(
            new ServiceOption("svc-netflix", "넷플릭스", "OTT", "#e50914", "N",
                    "mch-netflix", "NETFLIX.COM", List.of("넷플릭스", "netflix"),
                    "프로모션을 제외한 월 정기결제 기준 금액이에요.", List.of(
                    monthly("AD_STANDARD", "광고형 스탠다드", "7000", "광고 포함 · Full HD · 동시접속 2명", false),
                    monthly("STANDARD", "스탠다드", "13500", "광고 없이 · Full HD · 동시접속 2명", true),
                    monthly("PREMIUM", "프리미엄", "17000", "광고 없이 · 4K · 동시접속 4명", false))),
            new ServiceOption("svc-spotify", "스포티파이", "MUSIC", "#1db954", "S",
                    "mch-spotify", "SPOTIFY", List.of("스포티파이", "스포티", "spotify", "spotifypremium", "스포티파이프리미엄"),
                    "한국 공식 요금의 부가세 포함 정가 기준이며 무료 체험은 적용하지 않아요.", List.of(
                    monthly("BASIC", "프리미엄 베이직", "8690", "개인 계정 1개 · 광고 없이 감상 · 다운로드 제외", false),
                    monthly("INDIVIDUAL", "프리미엄 개인", "11990", "개인 계정 1개 · 광고 없이 감상 · 오프라인 다운로드", true),
                    monthly("DUO", "프리미엄 듀오", "17985", "같은 주소지에 거주하는 2명 · 별도 계정 2개", false),
                    monthly("STUDENT", "프리미엄 학생", "6600", "이용 자격이 있는 대학생 대상 · 학생 인증 필요", false))),
            new ServiceOption("svc-chatgpt", "ChatGPT", "AI", "#10a37f", "G",
                    "mch-chatgpt", "OPENAI*CHATGPT", List.of("chatgpt", "챗지피티", "챗gpt", "지피티", "gpt", "chatgptplus", "chatgptgo", "chatgptpro"),
                    "한국 App Store에 공개된 원화 금액을 모의 결제 기준으로 사용해요. 웹 결제 금액과 다를 수 있어요.", List.of(
                    monthly("GO", "Go", "13000", "일상적인 AI 활용을 위한 개인 유료 요금제", false),
                    monthly("PLUS", "Plus", "29000", "고급 AI 기능과 확장된 사용 한도를 위한 개인 요금제", true),
                    monthly("PRO_100", "Pro 100", "159000", "연구·코딩 작업을 위한 Pro 사용량 등급", false),
                    monthly("PRO_200", "Pro 200", "299000", "더 많은 사용량이 필요한 Pro 사용량 등급", false),
                    monthly("PRO_500", "Pro 500", "790000", "집중적인 작업을 위한 Pro 상위 사용량 등급", false))),
            new ServiceOption("svc-youtube", "유튜브 프리미엄", "OTT", "#ff0033", "▶",
                    "mch-youtube", "GOOGLE*YOUTUBE PREMIUM", List.of("유튜브프리미엄", "유튜브", "youtubepremium", "youtube"),
                    "한국 웹·Android 정가 기준이에요. iOS 앱 결제 금액과 다를 수 있어요.", List.of(
                    monthly("LITE", "프리미엄 라이트", "8500", "대부분의 음악 외 영상 광고 제거 · YouTube Music 제외", false),
                    monthly("PREMIUM", "프리미엄", "14900", "광고 없이 시청 · 오프라인 저장 · YouTube Music 포함", true))),
            new ServiceOption("svc-naverplus", "네이버플러스 멤버십", "MEMBERSHIP", "#03c75a", "N+",
                    "mch-naverplus", "네이버플러스멤버십", List.of("네이버플러스멤버십", "네이버멤버십", "네이버플러스멤버쉽", "네이버멤버쉽", "네이버플러스", "naverplus", "navermembership"),
                    "부가세 포함 정가 기준이에요. 연간 이용권은 12개월 금액을 한 번에 결제해요.", List.of(
                    monthly("MONTHLY", "월간 이용권", "4900", "매월 결제 · 멤버십 쇼핑 적립과 콘텐츠 혜택", true),
                    new PlanOption("YEARLY", "연간 이용권", new BigDecimal("46800"),
                            "12개월 선결제 · 월 환산 3,900원", false, "YEARLY")))
    );

    static ServiceOption service(String name) {
        var normalized = name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
        return SERVICES.stream().filter(service -> service.id().equalsIgnoreCase(name)
                        || service.aliases().contains(normalized)
                        || service.name().replaceAll("\\s+", "").toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "현재 가입은 넷플릭스, 스포티파이, ChatGPT, 유튜브 프리미엄, 네이버플러스 멤버십을 지원해요."));
    }

    static PlanOption plan(ServiceOption service, String code) {
        return service.plans().stream().filter(plan -> plan.code().equalsIgnoreCase(code))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        service.name() + "의 요금제를 선택해 주세요."));
    }

    private static PlanOption monthly(String code, String name, String price, String description, boolean recommended) {
        return new PlanOption(code, name, new BigDecimal(price), description, recommended, "MONTHLY");
    }

    record ServiceOption(String id, String name, String category, String brandColor, String initial,
                         String merchantId, String merchantName, List<String> aliases, String pricingNote,
                         List<PlanOption> plans) {}
}
