package br.com.controlefatura.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Representa um lançamento de fatura.
 * Encapsula todos os dados de um lançamento em uma estrutura tipada e segura.
 */
public class Lancamento {
    private final int id;
    private final String data;
    private final String nome;
    private BigDecimal totalAPagar;
    private int quantidadeParcelas;
    private BigDecimal valorParcela;
    private String ehMeu;
    private String cartaoUtilizado;
    private String parcelasRestantes;

    /**
     * Construtor que recebe todos os parâmetros obrigatórios.
     */
    public Lancamento(int id, String data, String nome, BigDecimal totalAPagar, int quantidadeParcelas,
                      BigDecimal valorParcela, String ehMeu, String cartaoUtilizado,
                      String parcelasRestantes) {
        this.id = id;
        this.data = Objects.requireNonNull(data, "Data não pode ser nula");
        this.nome = Objects.requireNonNull(nome, "Nome não pode ser nulo");
        this.totalAPagar = Objects.requireNonNull(totalAPagar, "Total a pagar não pode ser nulo");
        this.quantidadeParcelas = quantidadeParcelas;
        this.valorParcela = Objects.requireNonNull(valorParcela, "Valor da parcela não pode ser nulo");
        this.ehMeu = Objects.requireNonNull(ehMeu, "ehMeu não pode ser nulo");
        this.cartaoUtilizado = Objects.requireNonNull(cartaoUtilizado, "Cartão utilizado não pode ser nulo");
        this.parcelasRestantes = Objects.requireNonNull(parcelasRestantes, "Parcelas restantes não pode ser nulo");
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getData() {
        return data;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getTotalAPagar() {
        return totalAPagar;
    }

    public int getQuantidadeParcelas() {
        return quantidadeParcelas;
    }

    public BigDecimal getValorParcela() {
        return valorParcela;
    }

    public String getEhMeu() {
        return ehMeu;
    }

    public String getCartaoUtilizado() {
        return cartaoUtilizado;
    }

    public String getParcelasRestantes() {
        return parcelasRestantes;
    }

    // Setters
    public void setTotalAPagar(BigDecimal totalAPagar) {
        this.totalAPagar = Objects.requireNonNull(totalAPagar, "Total a pagar não pode ser nulo");
    }

    public void setQuantidadeParcelas(int quantidadeParcelas) {
        this.quantidadeParcelas = quantidadeParcelas;
    }

    public void setValorParcela(BigDecimal valorParcela) {
        this.valorParcela = Objects.requireNonNull(valorParcela, "Valor da parcela não pode ser nulo");
    }

    public void setEhMeu(String ehMeu) {
        this.ehMeu = Objects.requireNonNull(ehMeu, "ehMeu não pode ser nulo");
    }

    public void setCartaoUtilizado(String cartaoUtilizado) {
        this.cartaoUtilizado = Objects.requireNonNull(cartaoUtilizado, "Cartão utilizado não pode ser nulo");
    }

    public void setParcelasRestantes(String parcelasRestantes) {
        this.parcelasRestantes = Objects.requireNonNull(parcelasRestantes, "Parcelas restantes não pode ser nulo");
    }

    /**
     * Converte o lançamento em um array de objetos compatível com DefaultTableModel.
     * Útil para integração com a interface Swing.
     */
    public Object[] toArray() {
        return new Object[]{
            id,
            data,
            nome,
            totalAPagar,
            quantidadeParcelas,
            valorParcela,
            ehMeu,
            cartaoUtilizado,
            parcelasRestantes
        };
    }

    @Override
    public String toString() {
        return "Lancamento {" +
                "id=" + id +
                ", data='" + data + '\'' +
                ", nome='" + nome + '\'' +
                ", totalAPagar=" + totalAPagar +
                ", quantidadeParcelas=" + quantidadeParcelas +
                ", valorParcela=" + valorParcela +
                ", ehMeu='" + ehMeu + '\'' +
                ", cartaoUtilizado='" + cartaoUtilizado + '\'' +
                ", parcelasRestantes='" + parcelasRestantes + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lancamento that)) return false;
        return id == that.id &&
                data.equals(that.data) &&
                quantidadeParcelas == that.quantidadeParcelas &&
                nome.equals(that.nome) &&
                totalAPagar.equals(that.totalAPagar) &&
                valorParcela.equals(that.valorParcela) &&
                ehMeu.equals(that.ehMeu) &&
                cartaoUtilizado.equals(that.cartaoUtilizado) &&
                parcelasRestantes.equals(that.parcelasRestantes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, data, nome, totalAPagar, quantidadeParcelas, valorParcela,
                ehMeu, cartaoUtilizado, parcelasRestantes);
    }
}
