package com.example.blog.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Service层测试基类
 * 提供通用的测试环境设置和工具方法
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class ServiceTestBase {

    @BeforeEach
    void setUpBase() {
        // 通用的测试前设置
        // 可以在这里初始化共享的测试数据或mock
    }

    @AfterEach
    void tearDownBase() {
        // 通用的测试后清理
        // 可以在这里清理测试数据
    }

    /**
     * 生成测试用的唯一字符串
     */
    protected String generateUniqueString(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    /**
     * 验证异常消息包含指定文本
     */
    protected void assertExceptionMessageContains(Throwable exception, String expectedText) {
        if (exception.getMessage() == null || !exception.getMessage().contains(expectedText)) {
            throw new AssertionError(
                    String.format("Expected exception message to contain '%s', but got: %s",
                            expectedText, exception.getMessage())
            );
        }
    }
}
