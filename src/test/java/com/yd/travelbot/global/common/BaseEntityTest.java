package com.yd.travelbot.global.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseEntity 테스트")
class BaseEntityTest {

    @Test
    @DisplayName("생성 시간 초기화 - 생성 시 createdAt이 현재 시간으로 설정됨")
    void 생성_시간_초기화() {
        // given
        Instant beforeCreation = Instant.now();

        // when
        TestEntity entity = new TestEntity();
        Instant afterCreation = Instant.now();

        // then
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isAfterOrEqualTo(beforeCreation);
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(afterCreation);
    }

    @Test
    @DisplayName("수정 시간 초기화 - 생성 시 updatedAt이 현재 시간으로 설정됨")
    void 수정_시간_초기화() {
        // given
        Instant beforeCreation = Instant.now();

        // when
        TestEntity entity = new TestEntity();
        Instant afterCreation = Instant.now();

        // then
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(beforeCreation);
        assertThat(entity.getUpdatedAt()).isBeforeOrEqualTo(afterCreation);
    }

    @Test
    @DisplayName("생성 시간과 수정 시간 동일 - 초기 생성 시 createdAt과 updatedAt이 동일함")
    void 생성_시간과_수정_시간_동일() {
        // when
        TestEntity entity = new TestEntity();

        // then
        assertThat(entity.getCreatedAt()).isEqualTo(entity.getUpdatedAt());
    }

    @Test
    @DisplayName("수정 시간 변경 가능 - setter로 updatedAt 변경 가능")
    void 수정_시간_변경_가능() {
        // given
        TestEntity entity = new TestEntity();
        Instant originalUpdatedAt = entity.getUpdatedAt();
        Instant newUpdatedAt = Instant.now().plusSeconds(100);

        // when
        entity.setUpdatedAt(newUpdatedAt);

        // then
        assertThat(entity.getUpdatedAt()).isEqualTo(newUpdatedAt);
        assertThat(entity.getUpdatedAt()).isNotEqualTo(originalUpdatedAt);
        assertThat(entity.getCreatedAt()).isNotEqualTo(entity.getUpdatedAt());
    }

    /**
     * BaseEntity를 테스트하기 위한 구체 클래스
     */
    static class TestEntity extends BaseEntity {
        // BaseEntity의 기능만 테스트하기 위한 최소 구현
    }
}

