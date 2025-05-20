package com.safalifter.jobservice.transaction;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClearCacheAfterTransactionEvent extends ApplicationEvent {
    private final String cacheKey;

    public ClearCacheAfterTransactionEvent(String cacheKey) {
        super(cacheKey);
        this.cacheKey = cacheKey;
    }

}
