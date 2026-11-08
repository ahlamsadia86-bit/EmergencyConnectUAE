package com.emergencyconnectuae.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisLockService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean acquireLock(String lockKey, String lockValue, long seconds) {
    	System.out.println("Attempting Redis lock: " + lockKey);
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(seconds));

        System.out.println("Redis lock acquired: " + acquired);

        return Boolean.TRUE.equals(acquired);
        
    }

    public void releaseLock(String lockKey, String lockValue) {
    	System.out.println("Releasing Redis lock: " + lockKey);
        String currentValue = stringRedisTemplate.opsForValue().get(lockKey);

        if (lockValue.equals(currentValue)) {
            stringRedisTemplate.delete(lockKey);
        }
    }
}