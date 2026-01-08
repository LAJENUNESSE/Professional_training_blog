#!/usr/bin/env bash
set -euo pipefail

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

BASE_URL="${1:-http://localhost:8080}"

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}    Redis 缓存演示脚本${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# 检查依赖
check_deps() {
    if ! command -v redis-cli &> /dev/null; then
        echo -e "${RED}错误: redis-cli 未安装${NC}"
        exit 1
    fi
    if ! command -v curl &> /dev/null; then
        echo -e "${RED}错误: curl 未安装${NC}"
        exit 1
    fi
    if ! redis-cli ping &> /dev/null; then
        echo -e "${RED}错误: Redis 未运行${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ 依赖检查通过${NC}"
}

# 暂停等待用户
pause() {
    echo ""
    echo -e "${YELLOW}按 Enter 继续...${NC}"
    read -r
}

# 步骤1: 清空缓存
step1_clear_cache() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}步骤 1: 清空现有缓存${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${YELLOW}执行: redis-cli FLUSHALL${NC}"
    redis-cli FLUSHALL
    echo ""
    echo -e "${GREEN}✓ 缓存已清空${NC}"
}

# 步骤2: 查看空缓存
step2_show_empty() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}步骤 2: 确认缓存为空${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${YELLOW}执行: redis-cli KEYS '*'${NC}"
    keys=$(redis-cli KEYS '*')
    if [ -z "$keys" ]; then
        echo -e "${GREEN}(空) - 没有任何缓存 key${NC}"
    else
        echo "$keys"
    fi
}

# 步骤3: 第一次请求（写入缓存）
step3_first_request() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}步骤 3: 第一次 API 请求（写入缓存）${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    echo -e "${YELLOW}请求: GET /api/categories${NC}"
    start=$(date +%s%3N)
    curl -s "${BASE_URL}/api/categories" > /dev/null
    end=$(date +%s%3N)
    time1=$((end - start))
    echo -e "${GREEN}✓ 响应时间: ${time1}ms（首次请求，查询数据库）${NC}"

    echo ""
    echo -e "${YELLOW}请求: GET /api/articles${NC}"
    start=$(date +%s%3N)
    curl -s "${BASE_URL}/api/articles" > /dev/null
    end=$(date +%s%3N)
    time2=$((end - start))
    echo -e "${GREEN}✓ 响应时间: ${time2}ms${NC}"

    echo ""
    echo -e "${YELLOW}请求: GET /api/tags${NC}"
    start=$(date +%s%3N)
    curl -s "${BASE_URL}/api/tags" > /dev/null
    end=$(date +%s%3N)
    time3=$((end - start))
    echo -e "${GREEN}✓ 响应时间: ${time3}ms${NC}"
}

# 步骤4: 查看缓存 key
step4_show_keys() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}步骤 4: 查看已缓存的 Key${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${YELLOW}执行: redis-cli KEYS '*'${NC}"
    echo ""
    redis-cli KEYS '*' | while read -r key; do
        ttl=$(redis-cli TTL "$key")
        echo -e "  ${CYAN}$key${NC}"
        echo -e "    └─ TTL: ${GREEN}${ttl}秒${NC}"
    done
}

# 步骤5: 查看缓存内容
step5_show_content() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}步骤 5: 查看缓存内容（categories 示例）${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    # 查找 categories 的 key
    cat_key=$(redis-cli KEYS '*categories*' | head -1)
    if [ -n "$cat_key" ]; then
        echo -e "${YELLOW}Key: $cat_key${NC}"
        echo ""
        echo -e "${YELLOW}内容 (前500字符):${NC}"
        redis-cli GET "$cat_key" | head -c 500
        echo ""
        echo -e "${CYAN}...${NC}"
        echo ""
        echo -e "${GREEN}✓ 注意 JSON 中的 @class 字段，这是类型信息${NC}"
    else
        echo -e "${RED}未找到 categories 缓存${NC}"
    fi
}

# 步骤6: 缓存命中对比
step6_cache_hit() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}步骤 6: 缓存命中性能对比${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    echo -e "${YELLOW}连续请求 5 次 /api/categories（命中缓存）:${NC}"
    total=0
    for i in 1 2 3 4 5; do
        start=$(date +%s%3N)
        curl -s "${BASE_URL}/api/categories" > /dev/null
        end=$(date +%s%3N)
        time=$((end - start))
        total=$((total + time))
        echo -e "  请求 $i: ${GREEN}${time}ms${NC}"
    done
    avg=$((total / 5))
    echo ""
    echo -e "${GREEN}✓ 平均响应时间: ${avg}ms（缓存命中，无需查询数据库）${NC}"
}

# 步骤7: 缓存失效演示
step7_cache_invalidation() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}步骤 7: 缓存失效演示${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    cat_key=$(redis-cli KEYS '*categories*' | head -1)
    if [ -n "$cat_key" ]; then
        echo -e "${YELLOW}删除缓存: $cat_key${NC}"
        redis-cli DEL "$cat_key"
        echo -e "${GREEN}✓ 缓存已删除${NC}"
        echo ""

        echo -e "${YELLOW}再次请求 /api/categories（重新写入缓存）:${NC}"
        start=$(date +%s%3N)
        curl -s "${BASE_URL}/api/categories" > /dev/null
        end=$(date +%s%3N)
        time=$((end - start))
        echo -e "${GREEN}✓ 响应时间: ${time}ms（重新查询数据库并缓存）${NC}"

        echo ""
        echo -e "${YELLOW}确认缓存已重建:${NC}"
        redis-cli KEYS '*categories*'
    fi
}

# 步骤8: 缓存架构说明
step8_summary() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}缓存架构说明${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${CYAN}┌─────────────────────────────────────────┐${NC}"
    echo -e "${CYAN}│           两级缓存架构                   │${NC}"
    echo -e "${CYAN}├─────────────────────────────────────────┤${NC}"
    echo -e "${CYAN}│  请求 → L1 Caffeine (本地内存, 30s TTL) │${NC}"
    echo -e "${CYAN}│           ↓ 未命中                      │${NC}"
    echo -e "${CYAN}│       L2 Redis (分布式, 5-30min TTL)    │${NC}"
    echo -e "${CYAN}│           ↓ 未命中                      │${NC}"
    echo -e "${CYAN}│       SQLite 数据库                     │${NC}"
    echo -e "${CYAN}└─────────────────────────────────────────┘${NC}"
    echo ""
    echo -e "${GREEN}缓存 Key 说明:${NC}"
    echo -e "  • articles:published  - 已发布文章列表 (TTL: 5min)"
    echo -e "  • articles:hot        - 热门文章 (TTL: 1min)"
    echo -e "  • categories:all      - 分类列表 (TTL: 30min)"
    echo -e "  • tags:all            - 标签列表 (TTL: 30min)"
    echo ""
    echo -e "${GREEN}Jitter 机制:${NC}"
    echo -e "  • TTL 添加随机抖动，防止缓存雪崩"
    echo -e "  • 例如: 5min + 0~30s 随机值"
}

# 主流程
main() {
    check_deps
    pause

    step1_clear_cache
    pause

    step2_show_empty
    pause

    step3_first_request
    pause

    step4_show_keys
    pause

    step5_show_content
    pause

    step6_cache_hit
    pause

    step7_cache_invalidation
    pause

    step8_summary

    echo ""
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}    演示完成！${NC}"
    echo -e "${CYAN}========================================${NC}"
}

main
