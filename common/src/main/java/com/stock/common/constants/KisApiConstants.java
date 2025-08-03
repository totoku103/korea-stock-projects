package com.stock.common.constants;

public final class KisApiConstants {
    
    private KisApiConstants() {}
    
    // API Base URLs
    public static final String REAL_BASE_URL = "https://openapi.koreainvestment.com:9443";
    public static final String MOCK_BASE_URL = "https://openapivts.koreainvestment.com:29443";
    
    // OAuth2 Endpoints
    public static final String TOKEN_ENDPOINT = "/oauth2/tokenP";
    public static final String TOKEN_REVOKE_ENDPOINT = "/oauth2/revokeP";
    
    // WebSocket Endpoints
    public static final String WEBSOCKET_APPROVAL_KEY_ENDPOINT = "/oauth2/Approval";
    
    // Stock API Endpoints
    public static final String STOCK_PRICE_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-price";
    public static final String STOCK_ORDERBOOK_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-asking-price-exp-ccn";
    public static final String STOCK_DAILY_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-daily-price";
    public static final String STOCK_SEARCH_ENDPOINT = "/uapi/domestic-stock/v1/quotations/search-stock-info";
    
    // Market Index Endpoints
    public static final String KOSPI_INDEX_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-index-price";
    public static final String KOSDAQ_INDEX_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-index-price";
    
    // Ranking Endpoints
    public static final String VOLUME_RANKING_ENDPOINT = "/uapi/domestic-stock/v1/quotations/volume-rank";
    public static final String PRICE_RANKING_ENDPOINT = "/uapi/domestic-stock/v1/quotations/price-rank";
    
    // WebSocket Endpoints
    public static final String WS_REAL_URL = "ws://ops.koreainvestment.com:21000";
    public static final String WS_MOCK_URL = "ws://ops.koreainvestment.com:31000";
    
    // Headers
    public static final String HEADER_AUTHORIZATION = "authorization";
    public static final String HEADER_APP_KEY = "appkey";
    public static final String HEADER_APP_SECRET = "appsecret";
    public static final String HEADER_TR_ID = "tr_id";
    public static final String HEADER_CUSTTYPE = "custtype";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    
    // TR IDs
    public static final String TR_ID_STOCK_PRICE = "FHKST01010100";
    public static final String TR_ID_STOCK_ORDERBOOK = "FHKST01010200";
    public static final String TR_ID_STOCK_DAILY = "FHKST01010400";
    public static final String TR_ID_VOLUME_RANKING = "FHPST01710000";
    public static final String TR_ID_PRICE_RANKING = "FHPST01700000";
    
    // Customer Types
    public static final String CUST_TYPE_PERSONAL = "P";
    public static final String CUST_TYPE_CORPORATE = "B";
    
    // Grant Types
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    
    // Market Codes
    public static final String MARKET_CODE_KOSPI = "J";
    public static final String MARKET_CODE_KOSDAQ = "Q";
    
    // API Rate Limits
    public static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 20;
    public static final int TOKEN_VALIDITY_HOURS = 24;
}