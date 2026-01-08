package com.example.blog.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.search")
public class SearchProperties {

    private boolean enabled = false;
    private Engine engine = Engine.MEILISEARCH;
    private int reindexBatchSize = 200;
    private Meilisearch meilisearch = new Meilisearch();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public int getReindexBatchSize() {
        return reindexBatchSize;
    }

    public void setReindexBatchSize(int reindexBatchSize) {
        this.reindexBatchSize = reindexBatchSize;
    }

    public Meilisearch getMeilisearch() {
        return meilisearch;
    }

    public void setMeilisearch(Meilisearch meilisearch) {
        this.meilisearch = meilisearch;
    }

    public enum Engine {
        MEILISEARCH,
        DATABASE
    }

    public static class Meilisearch {
        private String host = "http://localhost:7700";
        private String apiKey;
        private String index = "articles";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }
    }
}
