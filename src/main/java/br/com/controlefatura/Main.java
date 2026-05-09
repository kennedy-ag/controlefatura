package br.com.controlefatura;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import br.com.controlefatura.exception.FaturaException;
import br.com.controlefatura.persistence.FaturaDao;
import br.com.controlefatura.services.FaturaService;
import br.com.controlefatura.services.FormService;
import br.com.controlefatura.services.TabelaService;

/**
 * Classe principal da aplicação de controle de fatura.
 * Inicializa a interface gráfica e coordena os serviços.
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    
    private FaturaService faturaService;
    private FormService formService;
    private JLabel labelTotal;
    private DefaultTableModel tableModel;
    private JTable tabela;

    public static void main(String[] args) {
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    new Main().inicializarInterface();
                } catch (Exception e) {
                    logger.severe(String.format("Erro fatal ao inicializar interface: %s", e.getMessage()));
                    JOptionPane.showMessageDialog(
                        null,
                        "Erro ao iniciar a aplicação: " + e.getMessage(),
                        "Erro Fatal",
                        JOptionPane.ERROR_MESSAGE
                    );
                    System.exit(1);
                }
            });
        } catch (Exception e) {
            logger.severe(String.format("Erro ao executar aplicação: %s", e.getMessage()));
            System.exit(1);
        }
    }

    /**
     * Inicializa os serviços necessários.
     */
    private void inicializarServicos() {
        try {
            FaturaDao faturaDao = new FaturaDao();
            this.faturaService = new FaturaService(faturaDao);
            this.formService = new FormService(faturaService);
            logger.info("Serviços inicializados com sucesso.");
        } catch (Exception e) {
            logger.severe(String.format("Erro ao inicializar serviços: %s", e.getMessage()));
            throw new FaturaException("Falha ao inicializar serviços.", e);
        }
    }

    /**
     * Inicializa a interface gráfica da aplicação.
     */
    private void inicializarInterface() {
        inicializarServicos();

        JFrame frame = new JFrame("Controle de Faturas");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(750, 420);
        frame.setLocationRelativeTo(null);

        // Criar modelo da tabela
        this.tableModel = criarModeloTabela();
        this.tabela = TabelaService.criarTabela(tableModel);
        JScrollPane scrollPane = new JScrollPane(tabela);

        // Criar labels e botões
        criarLabelTotal();
        JPanel painelBotoes = criarPainelBotoes();
        JPanel bottomPanel = criarPainelInferior(painelBotoes);

        // Criar painel principal
        JPanel painelPrincipal = new JPanel(new BorderLayout());
        painelPrincipal.add(scrollPane, BorderLayout.CENTER);
        painelPrincipal.add(bottomPanel, BorderLayout.SOUTH);

        frame.add(painelPrincipal);
        frame.setVisible(true);
        logger.info("Interface inicializada com sucesso.");
    }

    /**
     * Cria o modelo da tabela com dados iniciais.
     */
    private DefaultTableModel criarModeloTabela() {
        try {
            DefaultTableModel model = new DefaultTableModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            faturaService.getColunas().forEach(model::addColumn);
            faturaService.getDadosFatura().forEach(model::addRow);

            return model;
        } catch (Exception e) {
            logger.severe(String.format("Erro ao criar modelo da tabela: %s", e.getMessage()));
            throw new FaturaException("Falha ao carregar dados da tabela.", e);
        }
    }

    /**
     * Cria a label com o total da fatura.
     */
    private void criarLabelTotal() {
        JLabel value = new JLabel("R$ " + faturaService.getTotalFaturaFormatado());
        value.setFont(new Font("Arial", Font.PLAIN, 20));
        this.labelTotal = value;
    }

    /**
     * Cria o painel com os botões da interface.
     */
    private JPanel criarPainelBotoes() {
        JPanel painel = new JPanel();

        JButton botaoAdicionar = criarBotaoAdicionar();
        JButton botaoExcluir = criarBotaoExcluir();
        JButton botaoPagar = criarBotaoPagar();
        JButton botaoVerValor = criarBotaoVerValor();
        JButton botaoRodarSQL = criarBotaoRodarSQL();

        painel.add(botaoAdicionar);
        painel.add(botaoExcluir);
        painel.add(botaoPagar);
        painel.add(botaoVerValor);
        painel.add(botaoRodarSQL);

        return painel;
    }

    /**
     * Cria o botão para adicionar novo lançamento.
     */
    private JButton criarBotaoAdicionar() {
        JButton botao = new JButton("Adicionar");
        botao.addActionListener(e -> {
            try {
                Object[] lancamento = formService.formAdicionarLancamento();
                if (lancamento != null) {
                    faturaService.inserirLancamento(lancamento);
                    atualizarInterface();
                    JOptionPane.showMessageDialog(null, "Lançamento adicionado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                logger.warning(String.format("Erro ao adicionar lançamento: %s", ex.getMessage()));
                JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return botao;
    }

    /**
     * Cria o botão para pagar fatura.
     */
    private JButton criarBotaoPagar() {
        JButton botao = new JButton("Pagar");
        botao.addActionListener(e -> {
            try {
                faturaService.pagarFatura();
                atualizarInterface();
            } catch (Exception ex) {
                logger.warning(String.format("Erro ao pagar fatura: %s", ex.getMessage()));
                JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return botao;
    }

    /**
     * Cria o botão para excluir um lançamento.
     */
    private JButton criarBotaoExcluir() {
        JButton botao = new JButton("Excluir");
        botao.addActionListener(e -> {
            try {
                List<Integer> ids = obterIdsSelecionadosOuSolicitados();
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
                    atualizarInterface();
                    JOptionPane.showMessageDialog(null, "Lançamento(s) excluído(s) com sucesso!", "Excluído", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                logger.warning(String.format("Erro ao excluir lançamento: %s", ex.getMessage()));
                JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return botao;
    }

    private List<Integer> obterIdsSelecionadosOuSolicitados() {
        int[] selectedRows = tabela.getSelectedRows();
        if (selectedRows.length > 0) {
            List<Integer> ids = new ArrayList<>();
            for (int viewRow : selectedRows) {
                int modelRow = tabela.convertRowIndexToModel(viewRow);
                Object idValue = tableModel.getValueAt(modelRow, 0);
                if (idValue instanceof Integer id) {
                    ids.add(id);
                } else {
                    throw new FaturaException("ID selecionado inválido.");
                }
            }
            return ids;
        }

        String idInput = JOptionPane.showInputDialog("Digite o(s) ID(s) do lançamento a excluir, separados por vírgula:");
        if (idInput == null || idInput.isBlank()) {
            return null;
        }

        String[] parts = idInput.split(",");
        List<Integer> ids = new ArrayList<>();
        for (String part : parts) {
            try {
                ids.add(Integer.valueOf(part.trim()));
            } catch (NumberFormatException e) {
                throw new FaturaException("ID inválido. Use apenas números inteiros separando por vírgula.", e);
            }
        }
        return ids;
    }

    /**
     * Cria o botão para executar SQL customizado.
     */
    private JButton criarBotaoRodarSQL() {
        JButton botao = new JButton("SQL");
        botao.addActionListener(e -> {
            try {
                String sql = JOptionPane.showInputDialog("Digite a query SQL eventual:");
                if (sql != null && !sql.isBlank()) {
                    String resultado = faturaService.rodarQueryEventual(sql);
                    JOptionPane.showMessageDialog(null, resultado, "Resultado", JOptionPane.INFORMATION_MESSAGE);
                    TabelaService.atualizarTabela(tableModel, faturaService);
                    atualizarTotal();
                }
            } catch (Exception ex) {
                logger.warning(String.format("Erro ao executar SQL: %s", ex.getMessage()));
                JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return botao;
    }

    /**
     * Cria o botão para visualizar valor do mês.
     */
    private JButton criarBotaoVerValor() {
        JButton botao = new JButton("Valor");
        botao.addActionListener(e -> {
            try {
                BigDecimal valor = faturaService.getValorMes();
                JOptionPane.showMessageDialog(null, "Valor do mês: R$ " + valor, "Valor", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                logger.warning(String.format("Erro ao obter valor do mês: %s", ex.getMessage()));
                JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return botao;
    }

    /**
     * Cria o painel inferior com as informações e botões.
     */
    private JPanel criarPainelInferior(JPanel painelBotoes) {
        JLabel labelTexto = new JLabel("Total:");
        labelTexto.setFont(new Font("Arial", Font.PLAIN, 14));
        labelTotal.setFont(new Font("Arial", Font.PLAIN, 20));

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        labelPanel.add(labelTexto);
        labelPanel.add(labelTotal);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(labelPanel, BorderLayout.NORTH);
        bottomPanel.add(painelBotoes, BorderLayout.SOUTH);

        return bottomPanel;
    }

    /**
     * Atualiza os dados exibidos na interface.
     */
    private void atualizarInterface() {
        try {
            TabelaService.atualizarTabela(tableModel, faturaService);
            atualizarTotal();
            logger.info("Interface atualizada com sucesso.");
        } catch (Exception e) {
            logger.severe(String.format("Erro ao atualizar interface: %s", e.getMessage()));
            JOptionPane.showMessageDialog(null, "Erro ao atualizar interface: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Atualiza apenas o valor total exibido.
     */
    private void atualizarTotal() {
        try {
            labelTotal.setText("R$ " + faturaService.getTotalFaturaFormatado());
        } catch (Exception e) {
            logger.warning(String.format("Erro ao atualizar total: %s", e.getMessage()));
        }
    }
}
