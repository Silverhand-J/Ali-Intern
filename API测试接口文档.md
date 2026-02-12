# API 测试接口文档

本文档包含所有测试接口的详细说明，包括请求格式和响应示例。

> ⚠️ **注意：** 这些接口仅用于开发测试，生产环境应移除或限制访问。

---

## 目录

1. [StatTestController - 访问统计测试](#1-stattestcontroller---访问统计测试)
2. [HotspotTestController - 热点识别测试](#2-hotspottestcontroller---热点识别测试)
3. [StrategyTestController - 策略决策引擎测试](#3-strategytestcontroller---策略决策引擎测试)
4. [CacheProxyTestController - 缓存访问代理测试](#4-cacheproxytestcontroller---缓存访问代理测试)
5. [EndToEndTestController - 端到端测试](#5-endtoendtestcontroller---端到端测试)

---

## 1. StatTestController - 访问统计测试

**基础路径：** `/test/stat`

### 1.1 记录访问

**接口地址：** `GET /test/stat/record`

**描述：** 记录一次访问并返回统计结果

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizType | String | 是 | - | 业务类型 |
| bizKey | String | 是 | - | 业务键 |

**请求示例：**

```bash
GET http://localhost:8080/test/stat/record?bizType=product&bizKey=12345
```

**响应示例：**

```json
{
  "bizType": "product",
  "bizKey": "12345",
  "count1s": 1,
  "count60s": 1,
  "redisKey1s": "stat:product:12345:1s",
  "redisKey60s": "stat:product:12345:60s"
}
```

---

### 1.2 批量记录访问

**接口地址：** `GET /test/stat/batch`

**描述：** 批量记录访问（模拟并发场景）

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizType | String | 是 | - | 业务类型 |
| bizKey | String | 是 | - | 业务键 |
| count | Integer | 否 | 10 | 记录次数 |

**请求示例：**

```bash
GET http://localhost:8080/test/stat/batch?bizType=product&bizKey=12345&count=100
```

**响应示例：**

```json
{
  "bizType": "product",
  "bizKey": "12345",
  "recordCount": 100,
  "count1s": 100,
  "count60s": 100,
  "durationMs": 450,
  "avgLatencyMs": 4.5
}
```

---

## 2. HotspotTestController - 热点识别测试

**基础路径：** `/test/hotspot`

### 2.1 热点检测

**接口地址：** `GET /test/hotspot/detect`

**描述：** 访问统计 + 热点识别综合测试

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizType | String | 是 | - | 业务类型 |
| bizKey | String | 是 | - | 业务键 |

**请求示例：**

```bash
GET http://localhost:8080/test/hotspot/detect?bizType=product&bizKey=12345
```

**响应示例：**

```json
{
  "bizType": "product",
  "bizKey": "12345",
  "countShort": 1,
  "countLong": 1,
  "hotspotLevel": "COLD",
  "hotspotDescription": "冷数据，偶发访问，无需缓存",
  "threshold": 5
}
```

---

### 2.2 模拟热点场景

**接口地址：** `GET /test/hotspot/simulate`

**描述：** 模拟不同热度场景，验证热点识别准确性

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizType | String | 是 | - | 业务类型 |
| bizKey | String | 是 | - | 业务键 |
| scenario | String | 否 | WARM | 场景（COLD/WARM/HOT/EXTREMELY_HOT） |

**请求示例：**

```bash
GET http://localhost:8080/test/hotspot/simulate?bizType=product&bizKey=12345&scenario=HOT
```

**响应示例：**

```json
{
  "scenario": "HOT",
  "simulateCount": 25,
  "bizType": "product",
  "bizKey": "12345",
  "countShort": 25,
  "countLong": 25,
  "detectedLevel": "HOT",
  "expectedLevel": "HOT",
  "matched": true,
  "durationMs": 125
}
```

---

### 2.3 查看热点阈值

**接口地址：** `GET /test/hotspot/thresholds`

**描述：** 查看当前热点阈值配置

**请求参数：** 无

**请求示例：**

```bash
GET http://localhost:8080/test/hotspot/thresholds
```

**响应示例：**

```json
{
  "COLD": {
    "threshold": 5,
    "description": "冷数据，偶发访问"
  },
  "WARM": {
    "threshold": 20,
    "description": "中等热度，稳定访问"
  },
  "HOT": {
    "threshold": 100,
    "description": "高频热点"
  },
  "EXTREMELY_HOT": {
    "threshold": 2147483647,
    "description": "极热数据，突发流量"
  }
}
```

---

## 3. StrategyTestController - 策略决策引擎测试

**基础路径：** `/test/strategy`

### 3.1 完整流程测试

**接口地址：** `GET /test/strategy/full`

**描述：** 完整流程测试（访问统计 → 热点识别 → 策略决策）

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizType | String | 是 | - | 业务类型 |
| bizKey | String | 是 | - | 业务键 |

**请求示例：**

```bash
GET http://localhost:8080/test/strategy/full?bizType=product&bizKey=12345
```

**响应示例：**

```json
{
  "bizType": "product",
  "bizKey": "12345",
  "statResult": {
    "countShort": 1,
    "countLong": 1
  },
  "hotspotLevel": "COLD",
  "decision": {
    "cacheMode": "NONE",
    "ttlLevel": "SHORT"
  }
}
```

---

### 3.2 直接测试策略决策

**接口地址：** `GET /test/strategy/decide`

**描述：** 直接测试策略决策（指定热点等级）

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| level | String | 是 | - | 热点等级（COLD/WARM/HOT/EXTREMELY_HOT） |

**请求示例：**

```bash
GET http://localhost:8080/test/strategy/decide?level=HOT
```

**响应示例：**

```json
{
  "inputLevel": "HOT",
  "cacheMode": "LOCAL_AND_REMOTE",
  "ttlLevel": "LONG",
  "description": "本地缓存 + Redis 双层缓存，长 TTL"
}
```

---

### 3.3 查看策略映射

**接口地址：** `GET /test/strategy/mappings`

**描述：** 查看所有热点等级对应的策略配置

**请求参数：** 无

**请求示例：**

```bash
GET http://localhost:8080/test/strategy/mappings
```

**响应示例：**

```json
{
  "COLD": {
    "cacheMode": "NONE",
    "ttlLevel": "SHORT",
    "description": "不使用缓存，直接回源"
  },
  "WARM": {
    "cacheMode": "REMOTE_ONLY",
    "ttlLevel": "NORMAL",
    "description": "仅使用 Redis（L2），短期缓存"
  },
  "HOT": {
    "cacheMode": "LOCAL_AND_REMOTE",
    "ttlLevel": "LONG",
    "description": "本地缓存 + Redis 双层缓存，长 TTL"
  },
  "EXTREMELY_HOT": {
    "cacheMode": "LOCAL_ONLY",
    "ttlLevel": "LONG",
    "description": "仅使用本地缓存（L1）"
  }
}
```

---

### 3.4 模拟场景测试

**接口地址：** `GET /test/strategy/simulate`

**描述：** 模拟不同热度场景，验证策略决策

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizType | String | 是 | - | 业务类型 |
| bizKey | String | 是 | - | 业务键 |
| scenario | String | 否 | WARM | 场景（COLD/WARM/HOT/EXTREMELY_HOT） |

**请求示例：**

```bash
GET http://localhost:8080/test/strategy/simulate?bizType=product&bizKey=12345&scenario=HOT
```

**响应示例：**

```json
{
  "scenario": "HOT",
  "simulateCount": 25,
  "bizType": "product",
  "bizKey": "12345",
  "statResult": {
    "countShort": 25,
    "countLong": 25
  },
  "detectedLevel": "HOT",
  "decision": {
    "cacheMode": "LOCAL_AND_REMOTE",
    "ttlLevel": "LONG",
    "description": "本地缓存 + Redis 双层缓存，长 TTL"
  },
  "matched": true,
  "durationMs": 130
}
```

---

## 4. CacheProxyTestController - 缓存访问代理测试

**基础路径：** `/test/cache`

### 4.1 测试缓存访问

**接口地址：** `GET /test/cache/access`

**描述：** 测试不同缓存模式的访问行为

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| key | String | 是 | - | 缓存键 |
| mode | String | 否 | LOCAL_AND_REMOTE | 缓存模式（NONE/LOCAL_ONLY/REMOTE_ONLY/LOCAL_AND_REMOTE） |
| ttl | String | 否 | NORMAL | TTL等级（SHORT/NORMAL/LONG） |

**请求示例：**

```bash
GET http://localhost:8080/test/cache/access?key=product:12345&mode=LOCAL_AND_REMOTE&ttl=NORMAL
```

**响应示例：**

```json
{
  "key": "product:12345",
  "value": "db-value-a3f2d8e1",
  "cacheMode": "LOCAL_AND_REMOTE",
  "ttlLevel": "NORMAL",
  "dbCalled": true,
  "totalDbCalls": 1,
  "durationMs": 52
}
```

---

### 4.2 测试缓存命中率

**接口地址：** `GET /test/cache/hit-test`

**描述：** 连续访问同一个 key，观察缓存效果

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| key | String | 是 | - | 缓存键 |
| mode | String | 否 | LOCAL_AND_REMOTE | 缓存模式 |
| count | Integer | 否 | 10 | 访问次数 |

**请求示例：**

```bash
GET http://localhost:8080/test/cache/hit-test?key=product:99999&mode=LOCAL_AND_REMOTE&count=10
```

**响应示例：**

```json
{
  "key": "product:99999",
  "cacheMode": "LOCAL_AND_REMOTE",
  "totalAccess": 10,
  "dbCalls": 1,
  "cacheHits": 9,
  "hitRate": "90.00%",
  "durationMs": 65,
  "avgLatencyMs": 6.5
}
```

---

### 4.3 测试缓存失效

**接口地址：** `DELETE /test/cache/invalidate`

**描述：** 主动使缓存失效

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| key | String | 是 | - | 缓存键 |

**请求示例：**

```bash
DELETE http://localhost:8080/test/cache/invalidate?key=product:12345
```

**响应示例：**

```json
{
  "key": "product:12345",
  "action": "invalidated",
  "durationMs": 3
}
```

---

### 4.4 对比缓存模式性能

**接口地址：** `GET /test/cache/compare`

**描述：** 对比不同缓存模式的性能差异

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| key | String | 是 | - | 缓存键（会自动加后缀区分不同模式） |

**请求示例：**

```bash
GET http://localhost:8080/test/cache/compare?key=test:performance
```

**响应示例：**

```json
{
  "NONE": {
    "firstAccessNs": 1250000,
    "secondAccessNs": 1230000,
    "speedup": "1.02x"
  },
  "LOCAL_ONLY": {
    "firstAccessNs": 1180000,
    "secondAccessNs": 45000,
    "speedup": "26.22x"
  },
  "REMOTE_ONLY": {
    "firstAccessNs": 1350000,
    "secondAccessNs": 280000,
    "speedup": "4.82x"
  },
  "LOCAL_AND_REMOTE": {
    "firstAccessNs": 1420000,
    "secondAccessNs": 42000,
    "speedup": "33.81x"
  }
}
```

---

### 4.5 并发访问测试

**接口地址：** `GET /test/cache/concurrent`

**描述：** 测试多线程并发访问场景

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| key | String | 是 | - | 缓存键 |
| threads | Integer | 否 | 5 | 并发线程数 |
| iterations | Integer | 否 | 20 | 每个线程的迭代次数 |

**请求示例：**

```bash
GET http://localhost:8080/test/cache/concurrent?key=product:hot&threads=5&iterations=20
```

**响应示例：**

```json
{
  "key": "product:hot",
  "threads": 5,
  "iterationsPerThread": 20,
  "totalAccess": 100,
  "dbCalls": 1,
  "cacheHits": 99,
  "hitRate": "99.00%",
  "durationMs": 245,
  "qps": 408
}
```

---

### 4.6 重置计数器

**接口地址：** `POST /test/cache/reset`

**描述：** 重置 DB 访问计数器

**请求参数：** 无

**请求示例：**

```bash
POST http://localhost:8080/test/cache/reset
```

**响应示例：**

```json
{
  "previousCount": 156,
  "currentCount": 0
}
```

---

## 5. EndToEndTestController - 端到端测试

**基础路径：** `/test/e2e`

### 5.1 完整流程测试

**接口地址：** `GET /test/e2e/full`

**描述：** 完整的调度流程测试（访问统计 → 热点识别 → 策略决策 → 缓存访问）

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizType | String | 是 | - | 业务类型（PRODUCT/ORDER/USER等） |
| bizKey | String | 是 | - | 业务键 |

**请求示例：**

```bash
GET http://localhost:8080/test/e2e/full?bizType=product&bizKey=12345
```

**响应示例：**

```json
{
  "bizType": "product",
  "bizKey": "12345",
  "value": "db-value-12345-1707734521234",
  "hotspotLevel": "WARM",
  "dbCalled": true,
  "totalDbCalls": 1,
  "durationMs": 58
}
```

---

### 5.2 热点演化测试

**接口地址：** `GET /test/e2e/hotspot-evolution`

**描述：** 模拟从 COLD → WARM → HOT → EXTREMELY_HOT 的热点升级过程

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizType | String | 是 | - | 业务类型 |
| bizKey | String | 是 | - | 业务键 |

**请求示例：**

```bash
GET http://localhost:8080/test/e2e/hotspot-evolution?bizType=product&bizKey=99999
```

**响应示例：**

```json
{
  "bizType": "product",
  "bizKey": "99999",
  "evolution": [
    {
      "accessCount": 5,
      "hotspotLevel": "COLD"
    },
    {
      "accessCount": 10,
      "hotspotLevel": "WARM"
    },
    {
      "accessCount": 25,
      "hotspotLevel": "HOT"
    },
    {
      "accessCount": 50,
      "hotspotLevel": "HOT"
    },
    {
      "accessCount": 150,
      "hotspotLevel": "EXTREMELY_HOT"
    }
  ],
  "durationMs": 3245
}
```

---

### 5.3 缓存命中率测试

**接口地址：** `GET /test/e2e/cache-hit-rate`

**描述：** 连续访问同一个 key，观察缓存效果

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizType | String | 是 | - | 业务类型 |
| bizKey | String | 是 | - | 业务键 |
| count | Integer | 否 | 100 | 访问次数 |

**请求示例：**

```bash
GET http://localhost:8080/test/e2e/cache-hit-rate?bizType=product&bizKey=88888&count=100
```

**响应示例：**

```json
{
  "bizType": "product",
  "bizKey": "88888",
  "totalAccess": 100,
  "dbCalls": 1,
  "cacheHits": 99,
  "hitRate": "99.00%",
  "hotspotLevel": "EXTREMELY_HOT",
  "durationMs": 520,
  "avgLatencyMs": "5.20"
}
```

---

### 5.4 并发访问测试

**接口地址：** `GET /test/e2e/concurrent`

**描述：** 多线程并发访问同一个 key

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizType | String | 是 | - | 业务类型 |
| bizKey | String | 是 | - | 业务键 |
| threads | Integer | 否 | 10 | 并发线程数 |
| iterations | Integer | 否 | 50 | 每个线程的迭代次数 |

**请求示例：**

```bash
GET http://localhost:8080/test/e2e/concurrent?bizType=product&bizKey=77777&threads=10&iterations=50
```

**响应示例：**

```json
{
  "bizType": "product",
  "bizKey": "77777",
  "threads": 10,
  "iterationsPerThread": 50,
  "totalAccess": 500,
  "dbCalls": 1,
  "cacheHits": 499,
  "hitRate": "99.80%",
  "durationMs": 1250,
  "qps": 400
}
```

---

### 5.5 缓存失效测试

**接口地址：** `DELETE /test/e2e/invalidate`

**描述：** 使缓存和模拟数据库失效

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| bizKey | String | 是 | - | 业务键 |

**请求示例：**

```bash
DELETE http://localhost:8080/test/e2e/invalidate?bizKey=12345
```

**响应示例：**

```json
{
  "bizKey": "12345",
  "action": "invalidated",
  "durationMs": 5
}
```

---

### 5.6 重置所有状态

**接口地址：** `POST /test/e2e/reset`

**描述：** 重置所有测试状态（DB计数器、模拟数据库）

**请求参数：** 无

**请求示例：**

```bash
POST http://localhost:8080/test/e2e/reset
```

**响应示例：**

```json
{
  "previousDbCalls": 256,
  "previousDbSize": 15,
  "status": "reset"
}
```

---

## 附录

### A. 枚举类型说明

#### CacheMode（缓存模式）

| 枚举值 | 说明 |
|--------|------|
| NONE | 不使用缓存，直接回源 |
| LOCAL_ONLY | 仅使用本地缓存（L1） |
| REMOTE_ONLY | 仅使用 Redis 缓存（L2） |
| LOCAL_AND_REMOTE | 使用本地 + Redis 双层缓存 |

#### CacheTtlLevel（TTL等级）

| 枚举值 | 说明 |
|--------|------|
| SHORT | 短期 TTL |
| NORMAL | 正常 TTL |
| LONG | 长期 TTL |

#### HotspotLevel（热点等级）

| 枚举值 | 说明 | 典型阈值 |
|--------|------|----------|
| COLD | 冷数据，偶发访问 | < 5 次/秒 |
| WARM | 中等热度，稳定访问 | 5-20 次/秒 |
| HOT | 高频热点 | 20-100 次/秒 |
| EXTREMELY_HOT | 极热数据，突发流量 | > 100 次/秒 |

#### RequestType（请求类型）

| 枚举值 | 说明 |
|--------|------|
| PRODUCT | 商品相关请求 |
| ORDER | 订单相关请求 |
| USER | 用户相关请求 |

### B. 测试建议

1. **顺序测试：** 建议按照以下顺序测试各模块
   - StatTestController（访问统计基础功能）
   - HotspotTestController（热点识别功能）
   - StrategyTestController（策略决策功能）
   - CacheProxyTestController（缓存访问功能）
   - EndToEndTestController（端到端集成测试）

2. **环境要求：** 
   - 需要本地 Redis 服务运行
   - 建议使用默认端口 6379

3. **数据清理：** 
   - 测试前使用各 Controller 的 reset 接口清理数据
   - 避免历史数据影响测试结果

---

**文档版本：** v1.0  
**最后更新：** 2026-02-12
