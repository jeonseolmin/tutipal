package com.tutipal.global.ai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * FastAPI(ai 컨테이너) 호출은 전부 여기 한 곳으로. 내부 키를 붙이고, 스트림을 줄 단위로 읽는다.
 * tutor · quiz · material · screensummary · review 가 같이 쓴다.
 */
@Component
public class AiClient {

    /** LLM 에 넘기는 대화 한 줄 (role = user | assistant). images = base64 이미지 한 장 (화면 요약용, 없으면 null). */
    public record Message(String role, String content, @JsonInclude(JsonInclude.Include.NON_NULL) List<String> images) {
        public Message(String role, String content) {
            this(role, content, null);
        }
    }

    private record Chunk(String delta, String error) {
    }

    private final RestClient rest;
    private final JsonMapper json;

    public AiClient(@Value("${app.ai.base-url}") String baseUrl,
                    @Value("${app.ai.internal-key}") String internalKey,
                    JsonMapper json) {
        // HTTP/1.1 고정: JDK 기본(HTTP/2 업그레이드 시도)을 uvicorn 이 거부해서 요청이 깨짐
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        // Spring 의 이 제한은 조각 사이가 아니라 응답 전체에 걸린다 → 답변 하나의 최대 길이로 넉넉히.
        // 조각 사이 멈춤(180초)은 FastAPI 쪽 httpx 가 끊고 error 줄을 보낸다.
        factory.setReadTimeout(Duration.ofMinutes(10));
        this.rest = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Key", internalKey)
                .build();
        this.json = json;
    }

    /**
     * 답변을 글자 조각 단위로 onDelta 에 넘긴다.
     * AI 서버 연결 실패 · 타임아웃 · 오류 응답 · 깨진 줄은 전부 AiUnavailableException.
     * onDelta 가 던진 예외(예: 화면이 연결을 끊음)는 그대로 올라간다.
     */
    public void chatStream(String system, List<Message> messages, Consumer<String> onDelta) {
        try {
            exchangeChat(system, messages, false, onDelta);
        } catch (RestClientException | JacksonException e) {
            throw new AiUnavailableException();
        }
    }

    /** 답변 전체를 한 번에 (채점 · 문제 만들기). jsonMode 면 모델이 JSON 하나만 낸다. */
    public String complete(String system, List<Message> messages, boolean jsonMode) {
        StringBuilder sb = new StringBuilder();
        try {
            exchangeChat(system, messages, jsonMode, sb::append);
        } catch (RestClientException | JacksonException e) {
            throw new AiUnavailableException();
        }
        return sb.toString();
    }

    /** pgvector 리터럴 "[0.1,0.2,…]" (JDBC 에서 ?::vector 로 씀). 자료 RAG · 복습이 같이 씀 */
    public static String pgVector(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            sb.append(i == 0 ? "" : ",").append(v[i]);
        }
        return sb.append(']').toString();
    }

    /** 자료 파일에서 글자 뽑기 (txt · md · pdf). page 는 pdf 쪽 번호, 아니면 null */
    public record Page(Integer page, String text) {
    }

    private record ExtractResponse(List<Page> pages) {
    }

    private record EmbedResponse(String model, List<float[]> vectors) {
    }

    public List<Page> extract(String filename, byte[] data) {
        try {
            ExtractResponse res = rest.post().uri("/v1/extract")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("filename", filename, "data", java.util.Base64.getEncoder().encodeToString(data)))
                    .retrieve()
                    .body(ExtractResponse.class);
            return res == null ? List.of() : res.pages();
        } catch (RestClientException e) {
            throw new AiUnavailableException();
        }
    }

    /** 임베딩 (Ollama bge-m3, 1024차원). 한 번에 64개까지 */
    public List<float[]> embed(List<String> texts) {
        try {
            EmbedResponse res = rest.post().uri("/v1/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("texts", texts))
                    .retrieve()
                    .body(EmbedResponse.class);
            if (res == null || res.vectors() == null || res.vectors().size() != texts.size()) {
                throw new AiUnavailableException();
            }
            return res.vectors();
        } catch (RestClientException e) {
            throw new AiUnavailableException();
        }
    }

    private void exchangeChat(String system, List<Message> messages, boolean jsonMode, Consumer<String> onDelta) {
        rest.post().uri("/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("system", system, "messages", messages, "json_mode", jsonMode))
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new AiUnavailableException();
                    }
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isBlank()) {
                                continue;
                            }
                            Chunk chunk = json.readValue(line, Chunk.class);
                            if (chunk.error() != null) {
                                throw new AiUnavailableException();
                            }
                            if (chunk.delta() != null) {
                                onDelta.accept(chunk.delta());
                            }
                        }
                    }
                    return null;
                });
    }
}
