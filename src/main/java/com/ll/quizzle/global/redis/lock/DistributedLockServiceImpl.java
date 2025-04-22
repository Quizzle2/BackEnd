package com.ll.quizzle.global.redis.lock;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    private final RedissonClient redissonClient;

    @Override
    public boolean acquireLock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
            if (acquired) {
                log.debug("분산 락 획득 성공: {}", lockKey);
            } else {
                log.debug("분산 락 획득 실패: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("분산 락 획득 중 인터럽트 발생: {}", lockKey, e);
            return false;
        } catch (Exception e) {
            log.error("분산 락 획득 중 예외 발생: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void releaseLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.isLocked()) {
                lock.unlock();
                log.debug("분산 락 해제 성공: {}", lockKey);
            } else {
                log.debug("분산 락 해제 불필요 (이미 해제됨): {}", lockKey);
            }
        } catch (IllegalMonitorStateException e) {
            log.debug("분산 락 해제 실패 (소유권 없음): {}, 오류: {}", lockKey, e.getMessage());
        } catch (Exception e) {
            log.error("분산 락 해제 중 예외 발생: {}", lockKey, e);
        }
    }
} 