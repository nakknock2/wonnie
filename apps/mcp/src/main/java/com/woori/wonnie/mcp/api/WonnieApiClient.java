package com.woori.wonnie.mcp.api;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.Map;

@Component
public class WonnieApiClient {
    private final RestClient restClient;
    public WonnieApiClient(RestClient wonnieApiRestClient) { this.restClient=wonnieApiRestClient; }
    public Map<String,Object> report(String topic) { return report(topic, null); }
    public Map<String,Object> report(String topic, String merchant) {
        try {
            var result=restClient.get().uri(builder -> {
                builder.path("/api/finance/{topic}");
                if (merchant!=null) builder.queryParam("name", "{merchant}");
                return merchant==null?builder.build(topic):builder.build(topic,merchant);
            }).retrieve().body(new ParameterizedTypeReference<Map<String,Object>>() {});
            if(result==null)throw new IllegalStateException("Wonnie API가 빈 응답을 반환했습니다.");
            return result;
        } catch(RestClientResponseException exception) {
            throw new IllegalStateException("금융 조회 API 요청에 실패했습니다. HTTP "+exception.getStatusCode().value());
        } catch(RestClientException exception) {
            throw new IllegalStateException("금융 API에 연결할 수 없습니다. API 서버 실행 상태를 확인하세요.");
        }
    }
}
