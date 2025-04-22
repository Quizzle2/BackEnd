package com.ll.quizzle.global.redis.lock;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.ll.quizzle.global.exceptions.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Order(1)
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {
    
    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    @Around("@annotation(com.ll.quizzle.global.redis.lock.DistributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        
        String rawKey = distributedLock.key();
        long leaseTime = distributedLock.leaseTime();
        long waitTime = distributedLock.waitTime();
        
        String lockKey = parseLockKey(rawKey, joinPoint);
        
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        
        try {
            log.debug("분산 락 획득 시도: {}, 대기시간: {}ms, 유효기간: {}ms", lockKey, waitTime, leaseTime);
            locked = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
            
            if (!locked) {
                log.debug("분산 락 획득 실패: {}", lockKey);
                throw ErrorCode.DISTRIBUTED_LOCK_ACQUISITION_FAILED.throwServiceException();
            }
            
            log.debug("분산 락 획득 성공: {}", lockKey);
            return joinPoint.proceed();
        } finally {
            if (locked) {
                try {
                    lock.unlock();
                    log.debug("분산 락 해제: {}", lockKey);
                } catch (IllegalMonitorStateException e) {
                    log.debug("분산 락 해제 실패 (이미 해제됨): {}, {}", lockKey, e.getMessage());
                }
            }
        }
    }
    

    private String parseLockKey(String rawKey, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        
        return parser.parseExpression(rawKey).getValue(context, String.class);
    }
} 