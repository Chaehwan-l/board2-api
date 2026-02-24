CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    nickname VARCHAR(50) NOT NULL,
    provider VARCHAR(20) NOT NULL, 
    provider_id VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    
    UNIQUE KEY uk_provider_id (provider, provider_id)
);