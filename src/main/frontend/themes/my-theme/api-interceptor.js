/**
 * API Interceptor - Automatyczne dodawanie tokena JWT do requestów API
 * 
 * Interceptuje wszystkie fetch() i XMLHttpRequest wywołania do /api/*
 * i automatycznie dodaje token JWT z localStorage do nagłówka Authorization.
 */

(function() {
    'use strict';
    
    // Przechowaj oryginalne funkcje
    const originalFetch = window.fetch;
    const originalXHROpen = XMLHttpRequest.prototype.open;
    const originalXHRSend = XMLHttpRequest.prototype.send;
    
    /**
     * Pobiera token z localStorage
     */
    function getToken() {
        return localStorage.getItem('eatgo-token');
    }
    
    /**
     * Sprawdza czy URL jest do API endpointu
     */
    function isApiUrl(url) {
        if (typeof url === 'string') {
            return url.includes('/api/');
        }
        if (url instanceof Request) {
            return url.url.includes('/api/');
        }
        return false;
    }
    
    /**
     * Sprawdza czy token jest ważny (podstawowa walidacja - sprawdza czy nie jest null/empty)
     */
    function isTokenValid(token) {
        return token && token !== 'null' && token !== '' && token.trim().length > 0;
    }
    
    /**
     * Interceptuj fetch()
     */
    window.fetch = function(...args) {
        const [resource, init = {}] = args;
        
        // Sprawdź czy to request do API
        if (isApiUrl(resource)) {
            const token = getToken();
            
            if (isTokenValid(token)) {
                // Dodaj token do nagłówków
                const headers = new Headers(init.headers || {});
                headers.set('Authorization', `Bearer ${token}`);
                init.headers = headers;
                
                console.log('[API Interceptor] Added token to fetch request:', resource);
            } else {
                console.warn('[API Interceptor] No valid token found for API request:', resource);
            }
        }
        
        // Wywołaj oryginalny fetch
        return originalFetch.apply(this, [resource, init]);
    };
    
    /**
     * Interceptuj XMLHttpRequest
     */
    let xhrRequestHeaders = new WeakMap();
    
    XMLHttpRequest.prototype.open = function(method, url, ...rest) {
        // Zapisz URL dla późniejszego użycia
        this._requestUrl = url;
        
        // Wywołaj oryginalny open
        return originalXHROpen.apply(this, [method, url, ...rest]);
    };
    
    XMLHttpRequest.prototype.send = function(data) {
        // Sprawdź czy to request do API
        if (this._requestUrl && isApiUrl(this._requestUrl)) {
            const token = getToken();
            
            if (isTokenValid(token)) {
                // Dodaj token do nagłówków
                this.setRequestHeader('Authorization', `Bearer ${token}`);
                console.log('[API Interceptor] Added token to XHR request:', this._requestUrl);
            } else {
                console.warn('[API Interceptor] No valid token found for XHR request:', this._requestUrl);
            }
        }
        
        // Wywołaj oryginalny send
        return originalXHRSend.apply(this, [data]);
    };
    
    console.log('[API Interceptor] Initialized - token will be automatically added to API requests');
})();

