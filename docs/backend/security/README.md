# 后端安全认证系统 (Security Layer)

## 📋 概述

安全系统基于Spring Security 7 + JWT，提供无状态的认证和授权机制。采用Filter链架构，支持角色权限控制。

## 🏗️ 架构设计

### 认证流程
```
HTTP请求
    ↓
JwtAuthenticationFilter (提取Token)
    ↓
JwtTokenProvider (验证Token)
    ↓
UserDetailsService (加载用户)
    ↓
SecurityContextHolder (设置认证)
    ↓
SecurityFilterChain (权限检查)
    ↓
Controller (业务处理)
```

### 权限架构
```
HTTP请求 → Filter链 → 路由匹配 → 权限验证 → 访问控制
```

## 📚 核心组件详解

### 1. JwtAuthenticationFilter (JWT认证过滤器)

**文件**: `src/main/java/com/example/blog/security/JwtAuthenticationFilter.java`

**功能描述**: 在每个请求前拦截，提取并验证JWT Token，设置用户认证信息。

**核心代码**:
```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 从请求中提取Token
            String token = getTokenFromRequest(request);

            // 2. 验证Token有效性
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                // 3. 从Token中提取用户名
                String username = jwtTokenProvider.getUsernameFromToken(token);

                // 4. 加载用户详情
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // 6. 设置认证详情
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. 存入Security上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        // 8. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // 去掉"Bearer "前缀
        }
        return null;
    }
}
```

**执行时机**: 在Spring Security过滤器链中，位于`UsernamePasswordAuthenticationFilter`之前。

**工作流程**:
```
1. 请求到达 → doFilterInternal()
2. 提取Token → getTokenFromRequest()
3. 验证Token → validateToken()
4. 解析用户名 → getUsernameFromToken()
5. 加载用户 → loadUserByUsername()
6. 创建认证 → new UsernamePasswordAuthenticationToken()
7. 设置上下文 → SecurityContextHolder.setAuthentication()
8. 继续处理 → filterChain.doFilter()
```

**异常处理**:
- Token无效 → 静默失败，继续执行（可能被后续Filter拒绝）
- 用户不存在 → 静默失败
- 其他异常 → 记录日志，继续执行

---

### 2. JwtTokenProvider (JWT令牌提供者)

**文件**: `src/main/java/com/example/blog/security/JwtTokenProvider.java`

**功能描述**: 负责JWT Token的生成、解析、验证。

**核心代码**:
```java
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    // 获取签名密钥
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 从Authentication生成Token
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername());
    }

    // 生成访问Token（24小时）
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .subject(username)           // 主题（用户名）
                .issuedAt(now)               // 签发时间
                .expiration(expiryDate)      // 过期时间
                .signWith(getSigningKey())   // 签名
                .compact();                  // 生成Token
    }

    // 生成刷新Token（7天）
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getRefreshExpiration());

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    // 从Token提取用户名
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    // 验证Token
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
```

**JWT结构**:
```
Header.Payload.Signature
├── Header: {"alg": "HS256", "typ": "JWT"}
├── Payload: {
│     "sub": "username",
│     "iat": 1234567890,
│     "exp": 1234567890 + 86400000
│   }
└── Signature: HMAC-SHA256(Header + "." + Payload, Secret)
```

**Token生命周期**:
| Token类型 | 有效期 | 用途 |
|-----------|--------|------|
| Access Token | 24小时 | API访问 |
| Refresh Token | 7天 | 刷新Access Token |

**验证规则**:
- ✅ 签名有效
- ✅ 未过期
- ✅ 格式正确
- ✅ 非空

---

### 3. SecurityConfig (安全配置)

**文件**: `src/main/java/com/example/blog/config/SecurityConfig.java`

**功能描述**: 配置Spring Security规则，定义访问控制策略。

**核心代码**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用CSRF（无状态API不需要）
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 配置CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 无状态Session
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. 请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 静态资源（SPA前端）
                        .requestMatchers("/", "/index.html").permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        .requestMatchers("/*.js", "/*.css", "/*.ico").permitAll()

                        // SPA路由（前端路由）
                        .requestMatchers("/login", "/register", "/about", "/search").permitAll()
                        .requestMatchers("/article/**", "/category/**", "/tag/**").permitAll()
                        .requestMatchers("/admin/**").permitAll()

                        // 公开API
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/settings/**").permitAll()

                        // 文件上传
                        .requestMatchers("/uploads/**").permitAll()

                        // 管理API（需要ADMIN角色）
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )

                // 5. 认证提供者
                .authenticationProvider(authenticationProvider(userDetailsService, passwordEncoder()))

                // 6. 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS配置
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 认证提供者
    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    // 认证管理器
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 密码编码器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**访问控制规则**:

#### 公开访问 (无需认证)
```
/
/index.html
/assets/**
/*.js, *.css, *.ico
/login, /register, /about, /search
/article/**, /category/**, /tag/**
/admin/**
/api/auth/**
GET /api/articles/**
GET /api/categories/**
GET /api/tags/**
GET/POST /api/comments/**
GET /api/settings/**
/uploads/**
```

#### 需要登录
```
POST /api/articles/{id}/like
其他未明确公开的API
```

#### 需要管理员
```
/api/admin/**
```

---

### 4. UserDetailsServiceImpl (用户详情服务)

**文件**: `src/main/java/com/example/blog/security/UserDetailsServiceImpl.java`

**功能描述**: Spring Security的UserDetailsService实现，从数据库加载用户。

**核心代码**:
```java
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return CustomUserDetails.fromUser(user);
    }
}
```

### 5. CustomUserDetails (自定义用户详情)

**文件**: `src/main/java/com/example/blog/security/CustomUserDetails.java`

**功能描述**: 包装User实体，实现Spring Security的UserDetails接口。

**核心代码**:
```java
@Data
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled();
    }

    // 其他方法返回true（不使用账户锁定等功能）
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    // 工厂方法
    public static CustomUserDetails fromUser(User user) {
        return new CustomUserDetails(user);
    }
}
```

**权限映射**:
```
User.Role.ADMIN → ROLE_ADMIN
User.Role.USER  → ROLE_USER
```

---

## 🔐 认证流程详解

### 1. 用户登录
```
1. 用户提交用户名密码
   ↓
2. AuthController.login()
   ↓
3. AuthService.login()
   ↓
4. AuthenticationManager.authenticate()
   ↓
5. UserDetailsServiceImpl.loadUserByUsername()
   ↓
6. PasswordEncoder.matches() 验证密码
   ↓
7. JwtTokenProvider.generateToken() 生成Token
   ↓
8. 返回Token给客户端
```

### 2. API访问
```
1. 客户端请求API
   Authorization: Bearer <token>
   ↓
2. JwtAuthenticationFilter.doFilterInternal()
   ↓
3. 提取Token
   ↓
4. JwtTokenProvider.validateToken()
   ↓
5. 提取用户名
   ↓
6. UserDetailsServiceImpl.loadUserByUsername()
   ↓
7. 创建Authentication
   ↓
8. SecurityContextHolder.setAuthentication()
   ↓
9. SecurityFilterChain检查权限
   ↓
10. 访问Controller
```

### 3. Token刷新
```
1. Access Token过期
   ↓
2. 客户端使用Refresh Token请求
   ↓
3. AuthService.refreshToken()
   ↓
4. JwtTokenProvider.validateToken(refreshToken)
   ↓
5. 提取用户名
   ↓
6. 生成新Access Token + Refresh Token
   ↓
7. 返回新Token
```

## 🔑 密码安全

### BCrypt加密
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**特点**:
- 单向哈希，不可逆
- 自动加盐（Salt）
- 可配置强度（默认10）

**使用示例**:
```java
// 加密
String encodedPassword = passwordEncoder.encode(rawPassword);

// 验证
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

## 🛡️ 安全最佳实践

### 1. Token安全
```java
// ✅ 推荐
- 使用HTTPS传输
- 设置合理的过期时间
- Refresh Token独立存储
- 定期轮换密钥

// ❌ 避免
- Token存储在Cookie中
- 永不过期的Token
- 在URL中传递Token
```

### 2. 密码安全
```java
// ✅ 推荐
- 使用BCrypt加密
- 最小长度6位
- 强制复杂度要求
- 定期更换密码

// ❌ 避免
- 明文存储密码
- MD5/SHA1等弱哈希
- 简单密码（123456）
```

### 3. 权限控制
```java
// ✅ 推荐
- 最小权限原则
- 角色分离（USER/ADMIN）
- 接口级别权限控制
- 方法级别权限控制

// ❌ 避免
- 所有用户都是ADMIN
- 无权限检查
- 前端绕过权限
```

### 4. CORS配置
```java
// ✅ 推荐
- 限制允许的Origin
- 指定允许的Method
- 限制允许的Header
- 生产环境关闭Wildcard

// ❌ 避免
- allowedOrigins("*") 生产环境
- 无CORS配置
```

## 📊 配置说明

### JWT配置 (application.yml)
```yaml
jwt:
  secret: Y2xhdWRlLWJsb2ctand0LXNlY3JldC1rZXktMjU2LWJpdHMtbG9uZw==
  expiration: 86400000        # 24小时（毫秒）
  refresh-expiration: 604800000  # 7天（毫秒）
```

**密钥生成**:
```bash
# 生成256位密钥并Base64编码
openssl rand -base64 32
```

### 安全配置
```yaml
spring:
  security:
    filter:
      order: 10  # 过滤器顺序
```

## 🔍 调试技巧

### 1. 查看当前认证
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (auth != null) {
    String username = auth.getName();
    Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
}
```

### 2. 检查Token
```java
// 解码Token（不验证）
String[] parts = token.split("\\.");
String payload = new String(Base64.getDecoder().decode(parts[1]));
// 查看内容
```

### 3. 日志配置
```yaml
logging:
  level:
    org.springframework.security: DEBUG
    com.example.blog.security: DEBUG
```

## 🧪 测试

### 1. 测试Token生成
```java
@Test
void shouldGenerateValidToken() {
    String username = "testuser";
    String token = jwtTokenProvider.generateToken(username);

    assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(username);
}
```

### 2. 测试过滤器
```java
@WebMvcTest
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowRequestWithValidToken() throws Exception {
        String token = generateValidToken();

        mockMvc.perform(get("/api/articles/1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectRequestWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/articles/1")
                .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }
}
```

### 3. 测试权限
```java
@Test
@WithMockUser(roles = "USER")
void shouldAllowUserAccess() {
    // 模拟普通用户
}

@Test
@WithMockUser(roles = "ADMIN")
void shouldAllowAdminAccess() {
    // 模拟管理员
}
```

## 📝 总结

### 架构优势
1. **无状态**: 无需Session，适合分布式
2. **高性能**: Token验证在内存中完成
3. **可扩展**: 易于添加新角色和权限
4. **标准化**: 遵循JWT规范

### 关键配置
| 组件 | 作用 |
|------|------|
| JwtAuthenticationFilter | Token提取和验证 |
| JwtTokenProvider | Token生成和解析 |
| SecurityConfig | 访问规则配置 |
| UserDetailsServiceImpl | 用户加载 |
| BCryptPasswordEncoder | 密码加密 |

### 安全要点
- ✅ HTTPS传输
- ✅ 密码BCrypt加密
- ✅ Token有过期时间
- ✅ 权限最小化
- ✅ CORS限制
- ✅ 管理员权限隔离

---

**文档版本**: v1.0
**最后更新**: 2026-01-04
**维护者**: Blog 开发团队