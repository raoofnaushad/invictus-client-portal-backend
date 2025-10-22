package com.asbitech.common.domain;

public interface FailEvent extends Event {
    CustomError getCustomError();
}
