src/main/java/com/example/aliintern/
├── AliInternApplication.java
└── scheduler/
    ├── SchedulerFacade.java                    # 调度层核心门面
    ├── common/
    │   ├── enums/
    │   │   ├── RequestType.java               # 请求类型枚举
    │   │   ├── HotspotLevel.java              # 热点等级枚举
    │   │   └── CacheStrategy.java             # 缓存策略枚举
    │   └── model/
    │       ├── RequestContext.java            # 请求上下文
    │       ├── AccessStatistics.java          # 访问统计数据
    │       └── PolicyDecision.java            # 策略决策结果
    ├── classifier/                            # 请求分类器
    │   ├── RequestClassifier.java
    │   └── impl/DefaultRequestClassifier.java
    ├── statistics/                            # 访问统计模块
    │   ├── AccessStatisticsService.java
    │   └── impl/RedisAccessStatisticsService.java
    ├── hotspot/                               # 热点识别模块
    │   ├── HotspotDetector.java
    │   └── impl/DefaultHotspotDetector.java
    ├── decision/                              # 策略决策引擎
    │   ├── PolicyDecisionEngine.java
    │   └── impl/DefaultPolicyDecisionEngine.java
    ├── admission/                             # 缓存准入控制
    │   ├── CacheAdmissionControl.java
    │   └── impl/DefaultCacheAdmissionControl.java
    ├── proxy/                                 # Redis访问代理
    │   ├── RedisAccessProxy.java
    │   └── impl/DefaultRedisAccessProxy.java
    └── limiter/                               # 限流降级
        ├── RateLimiterService.java
        ├── DegradeService.java
        └── impl/
            ├── RedisRateLimiterService.java
            └── DefaultDegradeService.java