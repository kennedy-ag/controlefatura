package br.com.controlefatura.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import br.com.controlefatura.handler.SelectionHandler;
import br.com.controlefatura.model.Lancamento;
import br.com.controlefatura.services.FaturaService;
import br.com.controlefatura.services.FormService;

/**
 * Factory para criar botões da interface com seus respectivos listeners.
 */
public class ButtonFactory {
    private static final Logger logger = Logger.getLogger(ButtonFactory.class.getName());

    private final FaturaService faturaService;
    private final FormService formService;
    private final SelectionHandler selectionHandler;
    private final Runnable atualizarInterface;

    public ButtonFactory(FaturaService faturaService, FormService formService,
                         SelectionHandler selectionHandler, Runnable atualizarInterface) {
        this.faturaService = faturaService;
        this.formService = formService;
        this.selectionHandler = selectionHandler;
        this.atualizarInterface = atualizarInterface;
    }

    private void registrarAtalhoTeclado(JButton botao, int keyCode) {
        String actionKey = "shortcut-" + botao.getText();
        botao.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                botao.doClick();
            }
        });
        botao.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(keyCode, 0), actionKey);
    }

    private void mostrarResultadoSql(String resultado) {
        JTextArea areaResultado = new JTextArea(resultado);
        areaResultado.setEditable(false);
        areaResultado.setWrapStyleWord(true);
        areaResultado.setLineWrap(false);
        areaResultado.setCaretPosition(0);
        areaResultado.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        areaResultado.setMargin(new java.awt.Insets(8, 8, 8, 8));

        JScrollPane painelRolavel = new JScrollPane(areaResultado);
        painelRolavel.setPreferredSize(new Dimension(700, 350));

        JOptionPane.showMessageDialog(null, painelRolavel, "Resultado", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Cria o botão Adicionar.
     */
    public JButton criarBotaoAdicionar() {
        JButton botao = new JButton("Adicionar (A)");
        registrarAtalhoTeclado(botao, KeyEvent.VK_A);
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
        JButton botao = new JButton("Excluir (E)");
        registrarAtalhoTeclado(botao, KeyEvent.VK_E);
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
        JButton botao = new JButton("Pagar (P)");
        registrarAtalhoTeclado(botao, KeyEvent.VK_P);
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
        JButton botao = new JButton("Faturas (F)");
        registrarAtalhoTeclado(botao, KeyEvent.VK_F);
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
        JButton botao = new JButton("SQL (S)");
        registrarAtalhoTeclado(botao, KeyEvent.VK_S);
        botao.addActionListener(e -> {
            try {
                String sql = JOptionPane.showInputDialog(
                """
                    Escolha um comando ou digite uma query SQL:
                    
                    - 'historico' para ver o histórico de lançamentos
                    - 'parcelas' para ver as parcelas ativas
                    - 'total-parcelado' para ver lançamentos parcelados
                    - 'total-a-vista' para ver lançamentos à vista

                """);
                if (sql != null && !sql.isBlank()) {
                    String resultado = faturaService.rodarQueryEventual(sql);
                    mostrarResultadoSql(resultado);
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
