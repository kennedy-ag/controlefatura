package br.com.controlefatura.util;

import java.math.BigDecimal;
import java.util.logging.Logger;

import br.com.controlefatura.exception.FaturaException;

/**
 * Utilitário para validação de inputs da aplicação.
 */
public class ValidadorInput {
    private static final Logger logger = Logger.getLogger(ValidadorInput.class.getName());

    /**
     * Valida e converte uma string para BigDecimal.
     */
    public static BigDecimal validarBigDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new FaturaException("Valor não pode estar vazio.");
        }
        try {
            BigDecimal bd = new BigDecimal(valor.replace(",", "."));
            if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new FaturaException("Valor deve ser maior que zero.");
            }
            return bd.setScale(2);
        } catch (NumberFormatException e) {
            logger.warning(String.format("Falha ao converter valor para BigDecimal: %s", valor));
            throw new FaturaException("Valor inválido. Use formato numérico (ex: 100.50 ou 100,50).", e);
        }
    }

    /**
     * Valida e converte uma string para inteiro.
     */
    public static int validarInteiro(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new FaturaException("Valor não pode estar vazio.");
        }
        try {
            int num = Integer.parseInt(valor.trim());
            if (num <= 0) {
                throw new FaturaException("Valor deve ser maior que zero.");
            }
            return num;
        } catch (NumberFormatException e) {
            logger.warning(String.format("Falha ao converter valor para inteiro: %s", valor));
            throw new FaturaException("Valor inválido. Use apenas números.", e);
        }
    }

    /**
     * Valida se uma string não está vazia.
     */
    public static String validarString(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new FaturaException("Campo de texto não pode estar vazio.");
        }
        return valor.trim();
    }
}
