package br.com.controlefatura.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

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
     * Retorna null se o usuário cancelar a operação.
     */
    public Object[] formAdicionarLancamento() {
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

            return new Object[]{
                faturaService.getMaxId() + 1,
                nome,
                valor,
                vezes,
                ehMeu == 0 ? "N" : "S",
                meses,
                0
            };
        } catch (Exception e) {
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

