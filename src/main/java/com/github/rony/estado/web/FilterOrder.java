package com.github.rony.estado.web;

// Fonte unica de verdade da ordem da servlet filter chain. Antes, cada
// filtro declarava seu @Order isoladamente (numeros -2, -1, 1, 2 espalhados
// em 4 classes), sem nada documentando a ordem completa nem prevenindo
// colisao/intercalacao incorreta ao adicionar um filtro novo.
public final class FilterOrder {

    public static final int CORRELATION_ID = -2;
    public static final int SECURITY_HEADERS = -1;
    public static final int RATE_LIMIT = 1;
    public static final int API_KEY_AUTH = 2;

    private FilterOrder() {
    }
}
