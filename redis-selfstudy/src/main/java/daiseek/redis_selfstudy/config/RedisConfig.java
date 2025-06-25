package daiseek.redis_selfstudy.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.resource.ClientResources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration // Spring 설정 클래스임을 명시
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @org.springframework.beans.factory.annotation.Value("${spring.redis.host}") String redisHost,
            @org.springframework.beans.factory.annotation.Value("${spring.redis.port}") int redisPort) {

        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        standaloneConfiguration.setHostName(redisHost);
        standaloneConfiguration.setPort(redisPort);

        // 소켓 연결 타임아웃만 명시
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build()) // 연결 타임아웃
                .build();

        // 클라이언트 이름, 명령 타임아웃 등은 여기에 두겠습니다.
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofSeconds(5)) // 명령 실행 타임아웃 5초
                // Spring Boot 3.x에서는 clientName을 RedisURI를 통해 설정하는 것이 일반적입니다.
                // RedisTemplate은 RedisURI를 직접 사용하지 않으므로, 이 부분은 제거하거나
                // RedisURI를 사용하는 다른 ConnectionFactory 구현체 (예: Cluster, Sentinel)에서 사용합니다.
                // 일단 이 부분은 오류를 피하기 위해 제거합니다.
                .build();

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(standaloneConfiguration, clientConfiguration);
        connectionFactory.afterPropertiesSet();
        return connectionFactory;
    }


    /**
     * RedisTemplate 빈을 설정합니다.
     * 키는 String, 값은 JSON 형태로 직렬화하여 Redis에 저장하도록 합니다.
     * (Product 객체는 Serializable 구현되어 있으므로 JSON으로 직렬화 가능)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        System.out.println("RedisConnectionFactory is: " + (connectionFactory != null ? "not null" : "null"));

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        // Key 직렬화 설정 (String 형태)
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // Value 직렬화 설정 (JSON 형태) - Product 객체를 JSON으로 변환(직렬화)하여 저장
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        redisTemplate.afterPropertiesSet(); // 설정 완료 후 초기화
        return redisTemplate;
    }


    /**
     * Spring Cache Abstraction을 위한 RedisCacheConfiguration 빈을 설정합니다.
     * 캐시 항목의 기본 만료 시간, null 값 캐싱 여부, 키/값 직렬화 방식을 정의합니다.
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig() // 기본 캐시 설정 가져오기
                .entryTtl(Duration.ofMinutes(10)) // 캐시 항목의 기본 만료 시간을 10분으로 설정
                .disableCachingNullValues() // null 값은 캐싱하지 않도록 설정
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())) // 캐시 키는 String으로 직렬화
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())); // 캐시 값은 JSON으로 직렬화
    }
}
