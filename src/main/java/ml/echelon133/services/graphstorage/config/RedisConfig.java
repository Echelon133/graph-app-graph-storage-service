package ml.echelon133.services.graphstorage.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.math.BigDecimal;

@Configuration
public class RedisConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${redis.host}")
    private String host;

    @Value("${redis.port}")
    private Integer port;

    @Value("${redis.password}")
    private String redisPassword;

    private ObjectMapper oMapper;

    @Autowired
    public RedisConfig(ObjectMapper oMapper) {
        this.oMapper = oMapper;
        LOGGER.info("Instantiated RedisConfig with ObjectMapper: " + oMapper);
    }

    // We need this custom serializer because GenericJackson2JsonRedisSerializer cannot pass our JavaType to readValue
    private class RedisGraphSerializer implements RedisSerializer<Graph<BigDecimal>> {

        @Override
        public byte[] serialize(Graph<BigDecimal> bigDecimalGraph) throws SerializationException {
            try {
                return oMapper.writeValueAsBytes(bigDecimalGraph);
            } catch (JsonProcessingException ex) {
                throw new SerializationException(ex.getMessage(), ex);
            }
        }

        @Override
        public Graph<BigDecimal> deserialize(byte[] bytes) throws SerializationException {
            JavaType graphBigDecimalType = oMapper.getTypeFactory().constructParametricType(Graph.class, BigDecimal.class);
            try {
                return oMapper.readValue(bytes, graphBigDecimalType);
            } catch (IOException ex) {
                throw new SerializationException(ex.getMessage(), ex);
            }
        }
    }

    @Bean
    public RedisTemplate<String, Graph<BigDecimal>> redisTemplate() {
        RedisTemplate<String, Graph<BigDecimal>> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new RedisGraphSerializer());

        LOGGER.info("Instantiating RedisTemplate<String, Graph<BigDecimal>> bean");

        return redisTemplate;
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        LOGGER.info(String.format("Redis hostname: %s, port: %d, password: <hidden>", host, port));

        config.setHostName(host);
        config.setPort(port);
        config.setPassword(RedisPassword.of(redisPassword));

        LOGGER.info("Instantiating JedisConnectionFactory bean");

        return new JedisConnectionFactory(config);
    }
}
