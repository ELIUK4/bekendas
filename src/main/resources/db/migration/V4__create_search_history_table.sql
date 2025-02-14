CREATE TABLE search_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    search_query VARCHAR(255) NOT NULL,
    filters TEXT,
    search_date TIMESTAMP,
    results_count INT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
