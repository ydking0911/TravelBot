package com.yd.travelbot.global.exception.code;

import org.springframework.http.HttpStatus;

public interface BaseCodeInterface {
    String name();
    String message();
    HttpStatus httpStatus();
}


