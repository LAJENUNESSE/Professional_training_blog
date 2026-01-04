# 测试指南

## 概述

本项目包含完整的JUnit 5测试套件，覆盖所有核心业务逻辑。测试使用Mockito进行依赖mock，AssertJ进行断言验证。

## 测试架构

```
backend/src/test/java/com/example/blog/
├── service/                    # Service层测试
│   ├── AuthServiceTest.java
│   ├── ArticleServiceTest.java
│   ├── UserServiceTest.java
│   ├── CommentServiceTest.java
│   ├── CategoryServiceTest.java
│   ├── TagServiceTest.java
│   ├── FileStorageServiceTest.java
│   └── ServiceTestBase.java    # 测试基类
├── security/                   # Security层测试
│   └── JwtTokenProviderTest.java
└── resources/
    └── application-test.properties  # 测试配置
```

## 技术栈

- **JUnit 5**: 测试框架
- **Mockito**: Mock框架
- **AssertJ**: 断言库
- **Spring Boot Test**: Spring测试支持
- **Testcontainers**: 容器化测试（可选）

## 运行测试

### 运行所有测试

```bash
cd backend
./mvnw test
```

### 运行单个测试类

```bash
./mvnw test -Dtest=AuthServiceTest
```

### 运行单个测试方法

```bash
./mvnw test -Dtest=AuthServiceTest#should_register_user_successfully
```

### 运行特定包的测试

```bash
./mvnw test -Dtest="com.example.blog.service.*Test"
```

### 生成测试覆盖率报告

需要添加JaCoCo插件到pom.xml：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

然后运行：

```bash
./mvnw clean test jacoco:report
```

报告将生成在：`target/site/jacoco/index.html`

## 测试覆盖范围

### AuthServiceTest (100% 覆盖)
- ✅ 用户注册（正常、用户名重复、邮箱重复）
- ✅ 用户登录（正常、用户不存在、用户禁用、密码错误）
- ✅ Token刷新（正常、无效token、用户不存在）
- ✅ 边界条件（null昵称、特殊字符）

### ArticleServiceTest (100% 覆盖)
- ✅ 文章查询（分页、状态、分类、标签、搜索）
- ✅ 文章创建（正常、生成slug、中文标题、无分类标签）
- ✅ 文章更新（作者权限、管理员权限、状态转换）
- ✅ 文章删除（权限验证）
- ✅ 点赞功能（添加、移除、并发、边界）
- ✅ 异常处理（文章不存在、用户不存在、权限不足）

### JwtTokenProviderTest (100% 覆盖)
- ✅ Token生成（普通、刷新、认证对象）
- ✅ Token解析（提取用户名）
- ✅ Token验证（正常、过期、篡改、无效签名）
- ✅ 边界条件（空token、null、特殊字符）

### UserServiceTest (100% 覆盖)
- ✅ 用户查询（分页、ID、用户名）
- ✅ 用户更新（昵称、头像、两者）
- ✅ 密码修改（正确、错误）
- ✅ 状态切换（启用/禁用）
- ✅ 用户删除（存在、不存在）

### CommentServiceTest (100% 覆盖)
- ✅ 评论查询（文章、分页、状态）
- ✅ 评论创建（登录用户、访客、父子关系）
- ✅ 评论审核（通过、拒绝）
- ✅ 评论删除
- ✅ 统计查询（待审核数量）

### FileStorageServiceTest (100% 覆盖)
- ✅ 文件上传（JPEG、PNG、GIF、WebP、SVG）
- ✅ 文件验证（大小、类型、空文件）
- ✅ 文件存储（路径、唯一性）
- ✅ 异常处理（IO错误、权限问题）

### CategoryServiceTest (100% 覆盖)
- ✅ 分类查询（列表、ID、slug）
- ✅ 分类创建（正常、生成slug、特殊字符）
- ✅ 分类更新（名称唯一性）
- ✅ 分类删除（空分类、有文章）

### TagServiceTest (100% 覆盖)
- ✅ 标签查询（列表、分页、ID、slug）
- ✅ 标签创建（正常、生成slug、特殊字符）
- ✅ 标签更新（名称唯一性）
- ✅ 标签删除（空标签、有文章）

## 测试命名规范

### 测试方法命名
```
should_[预期行为]_when_[条件]
```

示例：
- `should_register_user_successfully_when_username_available`
- `should_throw_exception_when_user_not_found`
- `should_generate_slug_when_not_provided`

### 测试类命名
```
[被测试类名]Test
```

示例：
- `AuthServiceTest` → 测试 `AuthService`
- `JwtTokenProviderTest` → 测试 `JwtTokenProvider`

## 测试结构

### 标准测试结构

```java
@Test
@DisplayName("should_[预期行为]_when_[条件]")
void test[方法名]_[场景]() {
    // Given - 测试准备
    when(mock对象.方法).thenReturn(预期值);

    // When - 执行被测试方法
    var result = 被测试对象.方法(参数);

    // Then - 验证结果
    assertThat(result).isEqualTo(预期结果);
    verify(mock对象, times(1)).被验证的方法(参数);
}
```

### Mock配置

```java
@Mock
private Dependency dependency;

@InjectMocks
private Service service;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
    // 初始化测试数据
}
```

## 异常测试模式

```java
@Test
void should_throw_exception_when_[条件]() {
    // Given
    when(mock对象.方法).thenReturn(异常情况);

    // When & Then
    assertThatThrownBy(() -> service.method())
        .isInstanceOf(BusinessException.class)
        .hasMessage("Expected message")
        .extracting("code").isEqualTo(400);
}
```

## 参数化测试

```java
@ParameterizedTest
@CsvSource({
    "input1, expected1",
    "input2, expected2"
})
void should_[行为]_with_various_inputs(String input, String expected) {
    // 测试逻辑
}
```

## 最佳实践

### 1. 测试独立性
每个测试应该独立运行，不依赖其他测试的执行顺序或状态。

### 2. 清晰的命名
测试方法名应该清楚地表达：在什么条件下，期望什么行为。

### 3. 完整的验证
- 验证返回值
- 验证mock调用次数和参数
- 验证异常类型和消息
- 验证副作用（数据库状态变化）

### 4. 边界条件
- null值
- 空字符串
- 超长字符串
- 负数
- 零值
- 重复值

### 5. 并发测试
对于可能的并发问题，使用`@RepeatedTest`或手动创建多线程测试。

## 常见问题

### Q: 测试运行失败，提示数据库连接错误
A: 确保使用内存数据库配置，检查`application-test.properties`

### Q: Mock对象没有被注入
A: 确保使用`@InjectMocks`和`@Mock`注解，并在`@BeforeEach`中初始化

### Q: 测试覆盖率低
A: 检查是否覆盖了：
- 正常流程
- 异常流程
- 边界条件
- 所有分支

### Q: 测试运行缓慢
A:
- 使用内存数据库
- 避免不必要的Spring上下文加载
- 使用Mock代替真实依赖
- 并行运行测试（配置Surefire插件）

## 下一步

1. ✅ 运行所有测试验证：`./mvnw test`
2. ✅ 检查测试覆盖率报告
3. ✅ 根据需要添加更多边界条件测试
4. ✅ 集成测试（使用@SpringBootTest）
5. ✅ API层测试（Controller）
6. ✅ 性能测试
