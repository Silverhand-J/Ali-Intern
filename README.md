# Ali-Intern
```mermaid
flowchart TD
    A[Client / Web<br/>搜索 / 详情 / 交易] --> B[API / BFF / Gateway]

    B --> C[请求分类器<br/>生成 Cache Key]
    C --> D[访问统计模块<br/>统计周期内访问次数]

    D --> E{策略决策引擎<br/>是否热点?}

    E -- 是 --> F[Redis 缓存]
    E -- 否 --> G[MySQL 数据库]

    G --> H{缓存准入判断<br/>是否写入 Redis?}
    H -- 是 --> F
    H -- 否 --> I[直接返回结果]

    F --> J[返回响应]
    I --> J

