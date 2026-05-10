package br.com.controlefatura.services;

import java.awt.HeadlessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import br.com.controlefatura.model.Lancamento;
import br.com.controlefatura.util.ValidadorInput;

/**
 * Serviço responsável por formulários e entrada de dados do usuário.
 */
public class FormService {
    private static final Logger logger = Logger.getLogger(FormService.class.getName());
    private static final Object[] OPCOES_SIM_NAO = {"Não", "Sim"};

    private final FaturaService faturaService;

    public FormService(FaturaService faturaService) {
        this.faturaService = faturaService;
    }

    /**
     * Abre formulário para adicionar novo lançamento.
     * Retorna um objeto Lancamento ou null se o usuário cancelar a operação.
     */
    public Lancamento formAdicionarLancamento() {
        try {
            String nome = JOptionPane.showInputDialog("O que deseja adicionar?");
            if (nome == null) {
                return null;
            }
            nome = ValidadorInput.validarString(nome);

            String valorInput = JOptionPane.showInputDialog("Qual o valor?");
            if (valorInput == null) {
                return null;
            }
            BigDecimal valor = ValidadorInput.validarBigDecimal(valorInput);

            String vezesInput = JOptionPane.showInputDialog("Em quantas vezes?");
            if (vezesInput == null) {
                return null;
            }
            int vezes = ValidadorInput.validarInteiro(vezesInput);

            int ehMeu = JOptionPane.showOptionDialog(
                null,
                "Este lançamento é seu?",
                "Lançamento",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                OPCOES_SIM_NAO,
                OPCOES_SIM_NAO[0]
            );

            if (ehMeu == JOptionPane.CLOSED_OPTION) {
                return null;
            }

            int incluirMesAtual = JOptionPane.showOptionDialog(
                null,
                "Já vem na fatura desse mês?",
                "Incluir mês atual",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                OPCOES_SIM_NAO,
                OPCOES_SIM_NAO[0]
            );

            if (incluirMesAtual == JOptionPane.CLOSED_OPTION) {
                return null;
            }

            String meses = faturaService.obterStringMeses(vezes, incluirMesAtual != 0);

            String cartao = JOptionPane.showInputDialog("Qual foi o cartão utilizado?");
            if (cartao == null) {
                return null;
            }
            cartao = ValidadorInput.validarString(cartao);

            String data = JOptionPane.showInputDialog("Qual foi a data da compra?");
            if (data == null) {
                return null;
            }
            data = ValidadorInput.validarString(data);

            // Retorna um novo Lancamento com os dados do formulário
            return new Lancamento(
                0, // ID será gerado pelo banco de dados
                data,
                nome,
                valor,
                vezes,
                BigDecimal.ZERO,  // valor_da_parcela será calculado no FaturaService
                ehMeu == 0 ? "N" : "S",
                cartao,
                meses
            );
        } catch (HeadlessException e) {
            logger.severe(String.format("Erro ao validar formulário: %s", e.getMessage()));
            JOptionPane.showMessageDialog(null, "Erro: " + e.getMessage(), "Erro na entrada", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /**
     * Pede confirmação para pagar a fatura do mês atual.
     * Retorna 1 para Sim, 0 para Não, -1 para cancelado.
     */
    public int confirmarPagamentoFatura() {
        String mesCheio = LocalDate.now().getMonth()
            .getDisplayName(TextStyle.FULL, Locale.getDefault());

        return JOptionPane.showOptionDialog(
            null,
            "Confirma o pagamento da fatura de " + mesCheio + "?",
            "Pagar fatura",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            OPCOES_SIM_NAO,
            OPCOES_SIM_NAO[0]
        );
    }
}

