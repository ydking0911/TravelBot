package com.yd.travelbot.global.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JsonUtil 테스트")
class JsonUtilTest {

    @Test
    @DisplayName("JSON 파싱 - 정상 케이스")
    void JSON_파싱_성공() {
        // given
        String json = "{\"name\":\"테스트\",\"age\":30}";

        // when
        JsonNode result = JsonUtil.fromJson(json, JsonNode.class);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("name").asText()).isEqualTo("테스트");
        assertThat(result.get("age").asInt()).isEqualTo(30);
    }

    @Test
    @DisplayName("JSON 파싱 - 배열 파싱")
    void JSON_파싱_배열() {
        // given
        String json = "[{\"id\":1,\"name\":\"항목1\"},{\"id\":2,\"name\":\"항목2\"}]";

        // when
        JsonNode result = JsonUtil.fromJson(json, JsonNode.class);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isArray()).isTrue();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).get("name").asText()).isEqualTo("항목1");
    }

    @Test
    @DisplayName("JSON 파싱 - 잘못된 JSON 형식 시 예외 발생")
    void JSON_파싱_잘못된_형식_예외() {
        // given
        String invalidJson = "{name:테스트}"; // 따옴표 없음

        // when & then
        assertThatThrownBy(() -> JsonUtil.fromJson(invalidJson, JsonNode.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("JSON 파싱 실패");
    }

    @Test
    @DisplayName("JSON 파싱 - null 처리")
    void JSON_파싱_null() {
        // given
        String json = "null";

        // when
        JsonNode result = JsonUtil.fromJson(json, JsonNode.class);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isNull()).isTrue();
    }

    @Test
    @DisplayName("JSON 변환 - 객체를 JSON 문자열로 변환")
    void JSON_변환_성공() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put("name", "테스트");
        map.put("age", 30);

        // when
        String result = JsonUtil.toJson(map);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("테스트");
        assertThat(result).contains("30");
    }

    @Test
    @DisplayName("JSON 변환 - 복잡한 객체 변환")
    void JSON_변환_복잡한_객체() {
        // given
        Map<String, Object> nestedMap = new HashMap<>();
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("city", "서울");
        innerMap.put("country", "한국");
        nestedMap.put("location", innerMap);
        nestedMap.put("name", "호텔");

        // when
        String result = JsonUtil.toJson(nestedMap);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("호텔");
        assertThat(result).contains("서울");
        assertThat(result).contains("한국");
    }

    @Test
    @DisplayName("JSON 변환 - null 처리")
    void JSON_변환_null() {
        // given
        Object obj = null;

        // when
        String result = JsonUtil.toJson(obj);

        // then
        assertThat(result).isEqualTo("null");
    }

    @Test
    @DisplayName("JSON 파싱 및 변환 - 왕복 변환")
    void JSON_왕복_변환() {
        // given
        Map<String, Object> original = new HashMap<>();
        original.put("name", "테스트");
        original.put("value", 100);

        // when
        String json = JsonUtil.toJson(original);
        JsonNode parsed = JsonUtil.fromJson(json, JsonNode.class);

        // then
        assertThat(parsed.get("name").asText()).isEqualTo("테스트");
        assertThat(parsed.get("value").asInt()).isEqualTo(100);
    }

    @Test
    @DisplayName("JSON 파싱 - 빈 객체")
    void JSON_파싱_빈_객체() {
        // given
        String json = "{}";

        // when
        JsonNode result = JsonUtil.fromJson(json, JsonNode.class);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("JSON 변환 - 빈 객체")
    void JSON_변환_빈_객체() {
        // given
        Map<String, Object> emptyMap = new HashMap<>();

        // when
        String result = JsonUtil.toJson(emptyMap);

        // then
        assertThat(result).isEqualTo("{}");
    }
}

