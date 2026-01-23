package com.op.back.search.init;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LectureIndexInitializer {

    private static final String INDEX = "lecture-index";
    private final ElasticsearchClient client;
    private volatile boolean initialized = false;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        tryInit();
    }

    @Scheduled(fixedDelay = 10000)
    public void retry() {
        if (!initialized) {
            tryInit();
        }
    }

    public void tryInit() {
        try {
            boolean exists = client.indices()
                    .exists(e -> e.index(INDEX))
                    .value();

            if (!exists) {
                createLectureIndex();
                log.info("[ES] lecture-index created");
            } else {
                log.info("[ES] lecture-index already exists");
            }
            initialized = true;
        } catch (Exception e) {
            log.error("[ES] lecture-index initialization failed. Search disabled.", e);
        }
    }

    private void createLectureIndex() throws Exception {
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
                        .properties("lectureId", p -> p.keyword(k -> k))
                        .properties("title", p -> p.text(t -> t.analyzer("korean_analyzer")))
                        .properties("description", p -> p.text(t -> t.analyzer("korean_analyzer")))
                        .properties("tags", p -> p.keyword(k -> k))
                        .properties("category", p -> p.keyword(k -> k))
                        .properties("lecturerUid", p -> p.keyword(k -> k))
                        .properties("lecturerName", p -> p.text(t -> t.analyzer("korean_analyzer")))
                        .properties("rating", p -> p.float_(f -> f))
                        .properties("price", p -> p.integer(i -> i))
                        .properties("adminApproved", p -> p.boolean_(b -> b))
                        .properties("published", p -> p.boolean_(b -> b))
                )
        );
    }
}
