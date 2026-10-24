package com.aman.hospital_management.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ProjectConfig {

    @Bean
    public Cloudinary getcloudinary(@Value("${cloudinary.cloud-name}") String cloudName,
                                    @Value("${cloudinary.api-key}") String apiKey,
                                    @Value("${cloudinary.api-secret}") String apiSecret){
        Map config=new HashMap<>();
        config.put("cloud_name",cloudName);
        config.put("api_key",apiKey);
        config.put("api_secret",apiSecret);
        config.put("secure",true);
        return new Cloudinary(config);
    }
}
