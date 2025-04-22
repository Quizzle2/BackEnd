package com.ll.quizzle.global.redis.lock;

public interface DistributedLockService {

    /**
     * 분산 락 획득
     */
    boolean acquireLock(String lockKey, long waitTime, long leaseTime);

    /**
     * 분산 락 해제
     */
    void releaseLock(String lockKey);
} 