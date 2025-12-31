package com.op.back.search.init;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShortsIndexInitializer {

    private static final String INDEX = "shorts-index";

    private final ElasticsearchClient client;

    @PostConstruct
    public void init() {
        try {
            boolean exists = client.indices()
                    .exists(e -> e.index(INDEX))
                    .value();

            if (!exists) {
                createShortsIndex();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize shorts-index", e);
        }
    }

    private void createShortsIndex() throws Exception {
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
                        .properties("shortsId", p -> p.keyword(k -> k))
                        .properties("uid", p -> p.keyword(k -> k))
                        .properties("description", p -> p.text(t -> t.analyzer("korean_analyzer")))
                        .properties("hashtags", p -> p.keyword(k -> k))
                        .properties("createdAt", p -> p.date(d -> d))
                )
        );
    }
}
