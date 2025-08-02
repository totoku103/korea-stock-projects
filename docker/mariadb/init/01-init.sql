-- Korea Stock Projects Database Initialization Script
-- MariaDB 11.7.2

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS stock_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Use the database
USE stock_db;

-- Create indexes for better performance
-- Time-based queries optimization
SET GLOBAL innodb_large_prefix = 1;
SET GLOBAL innodb_file_format = Barracuda;

-- Enable JSON support and optimization
SET sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO';

-- Create sample tables for stock data (will be managed by JPA/Hibernate)
-- These are examples - actual tables will be created by Spring Boot

-- Stock information table
CREATE TABLE IF NOT EXISTS stock_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL UNIQUE,
    stock_name VARCHAR(100) NOT NULL,
    market_type VARCHAR(10) NOT NULL, -- KOSPI, KOSDAQ
    sector VARCHAR(50),
    listing_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stock_code (stock_code),
    INDEX idx_market_type (market_type),
    INDEX idx_listing_date (listing_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Daily stock price table (OHLCV data)
CREATE TABLE IF NOT EXISTS daily_stock_price (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    open_price DECIMAL(15,2),
    high_price DECIMAL(15,2),
    low_price DECIMAL(15,2),
    close_price DECIMAL(15,2),
    volume BIGINT,
    trading_value BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_date (stock_code, trade_date),
    INDEX idx_trade_date (trade_date),
    INDEX idx_stock_code_date (stock_code, trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Real-time stock price table
CREATE TABLE IF NOT EXISTS realtime_stock_price (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    current_price DECIMAL(15,2),
    change_price DECIMAL(15,2),
    change_rate DECIMAL(8,4),
    volume BIGINT,
    trading_value BIGINT,
    bid_price DECIMAL(15,2),
    ask_price DECIMAL(15,2),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_code (stock_code),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Market index table
CREATE TABLE IF NOT EXISTS market_index (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    index_code VARCHAR(20) NOT NULL,
    index_name VARCHAR(50) NOT NULL,
    index_value DECIMAL(12,2),
    change_value DECIMAL(12,2),
    change_rate DECIMAL(8,4),
    trade_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_index_date (index_code, trade_date),
    INDEX idx_trade_date (trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- API call log table for monitoring
CREATE TABLE IF NOT EXISTS api_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    endpoint VARCHAR(200) NOT NULL,
    method VARCHAR(10) NOT NULL,
    status_code INT,
    response_time_ms INT,
    request_size INT,
    response_size INT,
    error_message TEXT,
    called_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_endpoint (endpoint),
    INDEX idx_called_at (called_at),
    INDEX idx_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create user for application (if not exists)
CREATE USER IF NOT EXISTS 'stock_user'@'%' IDENTIFIED BY 'password';
GRANT SELECT, INSERT, UPDATE, DELETE ON stock_db.* TO 'stock_user'@'%';
FLUSH PRIVILEGES;

-- Insert some sample data for testing
INSERT IGNORE INTO stock_info (stock_code, stock_name, market_type, sector) VALUES
('005930', '삼성전자', 'KOSPI', '전자'),
('000660', 'SK하이닉스', 'KOSPI', '전자'),
('035420', 'NAVER', 'KOSPI', 'IT서비스'),
('051910', 'LG화학', 'KOSPI', '화학'),
('006400', '삼성SDI', 'KOSPI', '전자부품'),
('035720', '카카오', 'KOSPI', 'IT서비스'),
('207940', '삼성바이오로직스', 'KOSPI', '바이오'),
('068270', '셀트리온', 'KOSPI', '바이오'),
('323410', '카카오뱅크', 'KOSPI', '은행'),
('373220', 'LG에너지솔루션', 'KOSPI', '전자부품');

-- Insert sample market index data
INSERT IGNORE INTO market_index (index_code, index_name, index_value, change_value, change_rate, trade_date) VALUES
('KOSPI', 'KOSPI 지수', 2500.00, 10.50, 0.42, CURDATE()),
('KOSDAQ', 'KOSDAQ 지수', 800.00, -5.20, -0.65, CURDATE());

COMMIT;