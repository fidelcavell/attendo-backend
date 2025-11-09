package com.dev.attendo.utils.helper;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class AdditionalConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
