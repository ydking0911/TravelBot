package com.yd.travelbot.global.common;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseEntity {

    private Instant createdAt;
    private Instant updatedAt;

    protected BaseEntity() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

}


