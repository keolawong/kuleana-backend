package com.kahua.registration.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.time.Clock;

@Configuration
public class ApplicationConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }

    @Bean
    public HibernatePropertiesCustomizer jsonFormatMapperCustomizer(ObjectMapper objectMapper) {
        return properties -> properties.put(AvailableSettings.JSON_FORMAT_MAPPER, new AbstractJsonFormatMapper() {
            @Override
            protected <T> T fromString(CharSequence charSequence, Type type) {
                return objectMapper.readValue(charSequence.toString(), objectMapper.constructType(type));
            }

            @Override
            protected <T> String toString(T value, Type type) {
                return objectMapper.writeValueAsString(value);
            }
        });
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
