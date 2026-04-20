package br.com.controlefatura.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import javax.swing.JOptionPane;

public class FormService {
    public static Object[] formAdicionarLancamento() {
        FaturaService faturaService = new FaturaService();
        Object[] opcoes = {"Não", "Sim"};

        String nome = JOptionPane.showInputDialog("O que deseja adicionar?");

        BigDecimal valor = BigDecimal.valueOf(
            Double.valueOf(JOptionPane.showInputDialog("Qual o valor?"))
        ).setScale(2);

        int vezes = Integer.parseInt(JOptionPane.showInputDialog("Em quantas vezes?"));

        int ehMeu = JOptionPane.showOptionDialog(
            null,
            "Este lançamento é seu?",
            "Lançamento",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            opcoes,
            opcoes[0]
        );
        
        int incluirMesAtual = JOptionPane.showOptionDialog(
            null,
            "Já vem na fatura desse mês?",
            "Incluir mês atual",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            opcoes,
            opcoes[0]
        );

        String meses = faturaService.obterStringMeses(vezes, incluirMesAtual==0? false:true);

        Object[] lancamento = new Object[]
            {faturaService.getMaxId()+1, nome, valor, vezes, ehMeu==0?"N":"S", meses, 0};

        return lancamento;
    }

    public static int confirmarPagamentoFatura() {
        Object[] opcoes = {"Não", "Sim"};
        return JOptionPane.showOptionDialog(
            null,
            "Confirma o pagamento da fatura de "+
                    LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())+"?",
            "Pagar fatura",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            opcoes,
            opcoes[0]
        );
    }
}
