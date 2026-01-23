package com.op.back.search.init;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostIndexInitializer {

    private static final String INDEX = "post-index";

    private final ElasticsearchClient client;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            boolean exists = client.indices()
                    .exists(e -> e.index(INDEX))
                    .value();

            if (!exists) {
                createPostIndex();
                log.info("[ES] post-index created");
            } else {
                log.info("[ES] post-index already exists");
            }
        } catch (Exception e) {
            log.error("[ES] post-index initialization failed. Search disabled.", e);
        }
    }

    private void createPostIndex() throws Exception {
        client.indices().create(c -> c
                .index(INDEX)
                .settings(s -> s
                        .analysis(a -> a
                                .analyzer("korean_analyzer", an -> an
                                        .custom(ca -> ca
                                                .tokenizer("nori_tokenizer")
                                        )
                                )
                        )
                )
                .mappings(m -> m
                        .properties("postId", p -> p.keyword(k -> k))
                        .properties("uid", p -> p.keyword(k -> k))
                        .properties("content", p -> p.text(t -> t.analyzer("korean_analyzer")))
                        .properties("hashtags", p -> p.keyword(k -> k))
                        .properties("mediaType", p -> p.keyword(k -> k))
                        .properties("createdAt", p -> p.date(d -> d))
                )
        );
    }
}
