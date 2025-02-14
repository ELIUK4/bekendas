SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS search_history;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL
);

CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    page_url TEXT,
    type VARCHAR(255) NOT NULL DEFAULT 'photo',
    tags TEXT,
    preview_url TEXT,
    preview_width INT DEFAULT 0,
    preview_height INT DEFAULT 0,
    webformat_url TEXT NOT NULL,
    webformat_width INT DEFAULT 0,
    webformat_height INT DEFAULT 0,
    large_image_url TEXT,
    fullhd_url TEXT,
    image_url TEXT,
    image_width INT DEFAULT 0,
    image_height INT DEFAULT 0,
    image_size BIGINT DEFAULT 0,
    views INT DEFAULT 0,
    downloads INT DEFAULT 0,
    likes INT DEFAULT 0,
    comments INT DEFAULT 0,
    user_id BIGINT,
    file_name VARCHAR(255),
    original_file_name VARCHAR(255),
    upload_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_image (user_id, image_id)
);

CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE
);

CREATE TABLE search_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    search_query VARCHAR(255) NOT NULL,
    filters TEXT,
    search_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    results_count INT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
