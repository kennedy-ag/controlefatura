package br.com.controlefatura.ui;

import java.awt.HeadlessException;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import br.com.controlefatura.handler.SelectionHandler;
import br.com.controlefatura.model.Lancamento;
import br.com.controlefatura.services.FaturaService;
import br.com.controlefatura.services.FormService;
import br.com.controlefatura.services.TabelaService;

/**
 * Factory para criar botões da interface com seus respectivos listeners.
 */
public class ButtonFactory {
    private static final Logger logger = Logger.getLogger(ButtonFactory.class.getName());

    private final FaturaService faturaService;
    private final FormService formService;
    private final SelectionHandler selectionHandler;
    private final DefaultTableModel tableModel;
    private final Runnable atualizarInterface;

    public ButtonFactory(FaturaService faturaService, FormService formService, 
                         SelectionHandler selectionHandler, DefaultTableModel tableModel,
                         Runnable atualizarInterface) {
        this.faturaService = faturaService;
        this.formService = formService;
        this.selectionHandler = selectionHandler;
        this.tableModel = tableModel;
        this.atualizarInterface = atualizarInterface;
    }

    /**
     * Cria o botão Adicionar.
     */
    public JButton criarBotaoAdicionar() {
        JButton botao = new JButton("Adicionar");
        botao.addActionListener(e -> {
            try {
                Lancamento lancamento = formService.formAdicionarLancamento();
                if (lancamento != null) {
                    faturaService.inserirLancamento(lancamento);
                    atualizarInterface.run();
                    JOptionPane.showMessageDialog(null, "Lançamento adicionado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (HeadlessException ex) {
                logger.warning(String.format("Erro ao adicionar lançamento: %s", ex.getMessage()));
                JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return botao;
    }

    /**
     * Cria o botão Excluir.
     */
    public JButton criarBotaoExcluir() {
        JButton botao = new JButton("Excluir");
        botao.addActionListener(e -> {
            try {
                List<Integer> ids = selectionHandler.obterIdsSelecionadosOuSolicitados();
                if (ids == null || ids.isEmpty()) {
                    return;
                }

                int confirmado = JOptionPane.showConfirmDialog(
                    null,
                    "Deseja realmente excluir " + ids.size() + " lançamento(s)?\nIDs: " + ids,
                    "Confirmar Exclusão",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );

                if (confirmado == JOptionPane.YES_OPTION) {
                    faturaService.deletarLancamentos(ids);
                    atualizarInterface.run();
                    JOptionPane.showMessageDialog(null, "Lançamento(s) excluído(s) com sucesso!", "Excluído", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (HeadlessException ex) {
                logger.warning(String.format("Erro ao excluir lançamento: %s", ex.getMessage()));
                JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return botao;
    }

    /**
     * Cria o botão Pagar.
     */
    public JButton criarBotaoPagar() {
        JButton botao = new JButton("Pagar");
        botao.addActionListener(e -> {
            try {
                faturaService.pagarFatura();
                atualizarInterface.run();
            } catch (Exception ex) {
                logger.warning(String.format("Erro ao pagar fatura: %s", ex.getMessage()));
                JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return botao;
    }

    /**
     * Cria o botão Valor.
     */
    public JButton criarBotaoVerFaturas() {
        JButton botao = new JButton("Faturas");
        botao.addActionListener(e -> {
            try {
                String resumo = faturaService.getResumoFaturas();
                JOptionPane.showMessageDialog(null, "Próximas faturas: \n\n" + resumo + "\n", "Resumo", JOptionPane.INFORMATION_MESSAGE);
            } catch (HeadlessException ex) {
                logger.warning(String.format("Erro ao obter resumo das faturas: %s", ex.getMessage()));
                JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return botao;
    }

    /**
     * Cria o botão SQL.
     */
    public JButton criarBotaoRodarSQL() {
        JButton botao = new JButton("SQL");
        botao.addActionListener(e -> {
            try {
                String sql = JOptionPane.showInputDialog("Digite a query SQL eventual:");
                if (sql != null && !sql.isBlank()) {
                    String resultado = faturaService.rodarQueryEventual(sql);
                    JOptionPane.showMessageDialog(null, resultado, "Resultado", JOptionPane.INFORMATION_MESSAGE);
                    TabelaService.atualizarTabela(tableModel, faturaService);
                    atualizarInterface.run();
                }
            } catch (HeadlessException ex) {
                logger.warning(String.format("Erro ao executar SQL: %s", ex.getMessage()));
                JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return botao;
    }
}
