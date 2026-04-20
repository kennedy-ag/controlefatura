package br.com.controlefatura.exception;

/**
 * Exceção customizada para erros relacionados ao controle de fatura.
 */
public class FaturaException extends RuntimeException {
    public FaturaException(String mensagem) {
        super(mensagem);
    }

    public FaturaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
