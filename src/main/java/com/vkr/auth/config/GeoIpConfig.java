package com.vkr.auth.config;

import com.maxmind.geoip2.DatabaseReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class GeoIpConfig {

    @Value("${auth.geoip.database:classpath:GeoLite2-City.mmdb}")
    private Resource database;

    @Bean
    public DatabaseReader geoIpReader() throws IOException {
        return new DatabaseReader.Builder(database.getInputStream()).build();
    }
}