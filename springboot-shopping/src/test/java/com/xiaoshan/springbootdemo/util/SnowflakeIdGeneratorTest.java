package com.xiaoshan.springbootdemo.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SnowflakeIdGenerator 测试类
 */
class SnowflakeIdGeneratorTest {

    @Test
    void testNextId() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        
        // JavaScript 安全整数最大值
        long MAX_SAFE_INTEGER = 9007199254740991L;
        
        // 生成 1000 个 ID，验证都在安全范围内
        for (int i = 0; i < 1000; i++) {
            long id = generator.nextId();
            
            // 验证 ID 为正数
            assertTrue(id > 0, "ID 应该是正数");
            
            // 验证 ID 在 JavaScript 安全范围内
            assertTrue(id <= MAX_SAFE_INTEGER, 
                "ID 应该在 JavaScript 安全整数范围内: " + id);
            
            // 验证 ID 位数（最多 16 位）
            int digits = String.valueOf(id).length();
            assertTrue(digits <= 16, 
                "ID 位数应该 <= 16 位，实际: " + digits + " 位，ID: " + id);
            
            // 打印前 10 个 ID 用于验证
            if (i < 10) {
                System.out.println("ID " + (i + 1) + ": " + id + 
                    " (" + digits + " 位)");
            }
        }
    }

    @Test
    void testUniqueness() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        
        // 生成 10000 个 ID，验证唯一性
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (int i = 0; i < 10000; i++) {
            long id = generator.nextId();
            assertFalse(ids.contains(id), "ID 应该唯一: " + id);
            ids.add(id);
        }
        
        System.out.println("成功生成 10000 个唯一 ID");
    }

    @Test
    void testParseTimestamp() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        
        long beforeGenerate = System.currentTimeMillis();
        long id = generator.nextId();
        long afterGenerate = System.currentTimeMillis();
        
        long parsedTimestamp = generator.parseTimestamp(id);
        
        // 验证解析的时间戳在生成时间附近（允许 1 秒误差）
        assertTrue(parsedTimestamp >= beforeGenerate - 1000, 
            "解析的时间戳应该接近生成时间");
        assertTrue(parsedTimestamp <= afterGenerate + 1000, 
            "解析的时间戳应该接近生成时间");
        
        System.out.println("生成时间: " + beforeGenerate);
        System.out.println("解析时间: " + parsedTimestamp);
    }
}
