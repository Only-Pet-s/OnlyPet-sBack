package com.op.back.common.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(
        basePackages = "com.op.back.lecture.search"
)
@EntityScan(basePackages = {
        "com.op.back.lecture.search" //ES Document만 있는 패키지
})
public class ElasticsearchConfig {
}
