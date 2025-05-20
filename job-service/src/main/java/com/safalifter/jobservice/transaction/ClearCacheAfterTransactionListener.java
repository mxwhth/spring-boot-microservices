package com.safalifter.jobservice.transaction;

import com.safalifter.jobservice.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ClearCacheAfterTransactionListener {
    private final RedisUtil redisUtil;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleClearCacheEvent(ClearCacheAfterTransactionEvent event) {
        String cacheKey = event.getCacheKey();
        redisUtil.delete(cacheKey);
    }
}
