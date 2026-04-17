# /sb-cache — Configure Spring Boot Caching

Add Caffeine or Redis caching to a CDB microservice service class with proper eviction, TTL, and metrics.

## Usage
```
/sb-cache <ServiceClass> [--module <moduleName>] [--type caffeine|redis] [--ttl <minutes>]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `ServiceClass` — the service class to add caching to (e.g., `ProviderConfigurationService`)
- `--module` — CDB module (e.g., `provider-registry`)
- `--type` — `caffeine` (local, single-instance) or `redis` (distributed, multi-instance)
- `--ttl` — cache TTL in minutes (default: 10 for operational data, 60 for reference data)

### Step 1: Read the service class
Read the target service class to identify:
- Which `findAll`/`findById` methods should be cached (read-heavy)
- Which create/update/delete methods should evict the cache
- What the cache key structure should be

### Step 2: Add caching annotations to service

```java
// Add to class or import @EnableCaching in config

@Cacheable(value = "<resourceName>s", unless = "#result == null")
public <ResourceName>Dto findById(UUID id) { ... }

@Cacheable(value = "<resourceName>-list",
           key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
public Page<<ResourceName>Dto> findAll(Pageable pageable) { ... }

@CachePut(value = "<resourceName>s", key = "#result.id")
@CacheEvict(value = "<resourceName>-list", allEntries = true)
@Transactional
public <ResourceName>Dto create(Create<ResourceName>Request request) { ... }

@CachePut(value = "<resourceName>s", key = "#id")
@CacheEvict(value = "<resourceName>-list", allEntries = true)
@Transactional
public <ResourceName>Dto update(UUID id, Update<ResourceName>Request request) { ... }

@Caching(evict = {
    @CacheEvict(value = "<resourceName>s", key = "#id"),
    @CacheEvict(value = "<resourceName>-list", allEntries = true)
})
@Transactional
public void delete(UUID id) { ... }
```

### Step 3: Generate cache configuration

#### Caffeine (local, no external dependency)
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("<resourceName>s",
            buildCache(Duration.ofMinutes(<ttl>), 1_000));
        manager.registerCustomCache("<resourceName>-list",
            buildCache(Duration.ofMinutes(<ttl>), 100));
        return manager;
    }

    private Cache<Object, Object> buildCache(Duration ttl, long maxSize) {
        return Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(maxSize)
            .recordStats()         // enables Micrometer metrics
            .build();
    }
}
```

Add to `pom.xml`:
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

#### Redis (distributed — for multi-instance deployments)
```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(<ttl>))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("<resourceName>s",
                defaultConfig.entryTtl(Duration.ofMinutes(<ttl>)))
            .withCacheConfiguration("<resourceName>-list",
                defaultConfig.entryTtl(Duration.ofMinutes(<ttl> / 2)))
            .transactionAware()
            .build();
    }
}
```

Add to `application.yml`:
```yaml
spring:
  data:
    redis:
      host: ${CDB_REDIS_HOST:localhost}
      port: ${CDB_REDIS_PORT:6379}
      password: ${CDB_REDIS_PASSWORD:}
      lettuce:
        pool:
          max-active: ${CDB_REDIS_POOL_MAX_ACTIVE:8}
          max-idle: ${CDB_REDIS_POOL_MAX_IDLE:8}
          min-idle: ${CDB_REDIS_POOL_MIN_IDLE:2}
          max-wait: -1ms
        shutdown-timeout: 200ms
```

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### Step 4: Add cache management endpoint
```java
@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
@Tag(name = "Cache Management", description = "Cache administration endpoints")
public class CacheAdminController {

    private final CacheManager cacheManager;

    @DeleteMapping("/{cacheName}")
    @Operation(summary = "Evict all entries from a named cache")
    public ResponseEntity<Void> evict(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return ResponseEntity.notFound().build();
        cache.clear();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Evict all caches")
    public ResponseEntity<Void> evictAll() {
        cacheManager.getCacheNames().forEach(name ->
            Optional.ofNullable(cacheManager.getCache(name)).ifPresent(Cache::clear));
        return ResponseEntity.noContent().build();
    }
}
```

### Step 5: Report
- Which methods were annotated and with what cache names
- TTL and max size per cache
- Whether Redis or Caffeine was chosen and why
- Which eviction events clear which caches
- Warn if `@Cacheable` was added to a `@Transactional(readOnly=false)` method (incorrect — caching write operations is a bug)
