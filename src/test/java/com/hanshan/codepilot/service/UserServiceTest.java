package com.hanshan.codepilot.service;

import com.hanshan.codepilot.constant.RedisConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * 用户服务测试
 */
@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    @Test
    void userRegister() {
        String userAccount = "hanshan";
        String userPassword = "";
        String checkPassword = "123456";
        try {
            long result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "yu";
            result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
        } catch (Exception e) {

        }
    }

    @Test
    void RedissonTest() {
        long userId = 5;
        LocalDate date = LocalDate.now();
        System.out.println("date = " + date);
        String key = RedisConstant.getUserSignInRedisPrefix(date.getYear(), userId);
        RBitSet signInBitSet = redissonClient.getBitSet(key);
        System.out.println("signInBitSet = " + signInBitSet);
        int offset = date.getDayOfYear();
        System.out.println("offset = " + offset);
        signInBitSet.set(offset, true);
        System.out.println("signInBitSet = " + signInBitSet);
    }
}
