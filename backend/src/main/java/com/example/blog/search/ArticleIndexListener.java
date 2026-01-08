package com.example.blog.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ArticleIndexListener {

    private final ArticleSearchService articleSearchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIndexEvent(ArticleIndexEvent event) {
        if (event == null) {
            return;
        }
        if (event.action() == ArticleIndexEvent.Action.DELETE) {
            articleSearchService.deleteArticle(event.articleId());
            return;
        }
        articleSearchService.indexArticle(event.articleId());
    }
}
