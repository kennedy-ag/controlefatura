package br.com.controlefatura;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import br.com.controlefatura.exception.FaturaException;
import br.com.controlefatura.handler.SelectionHandler;
import br.com.controlefatura.persistence.FaturaDao;
import br.com.controlefatura.services.FaturaService;
import br.com.controlefatura.services.FormService;
import br.com.controlefatura.services.TabelaService;
import br.com.controlefatura.ui.ButtonFactory;
import br.com.controlefatura.ui.PainelBuilder;

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

    private final int FRAME_WIDTH = 860;

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
        frame.setSize(FRAME_WIDTH, getAlturaDinamicaFrame());
        frame.setLocationRelativeTo(null);

        // Criar modelo da tabela
        this.tableModel = criarModeloTabela();
        this.tabela = TabelaService.criarTabela(tableModel);
        JScrollPane scrollPane = new JScrollPane(tabela);

        // Criar labels e painéis
        criarLabelTotal();
        SelectionHandler selectionHandler = new SelectionHandler(tabela, tableModel);
        ButtonFactory buttonFactory = new ButtonFactory(faturaService, formService, selectionHandler, tableModel, this::atualizarInterface);
        
        JPanel painelBotoes = PainelBuilder.criarPainelBotoes(
            buttonFactory.criarBotaoAdicionar(),
            buttonFactory.criarBotaoExcluir(),
            buttonFactory.criarBotaoPagar(),
            buttonFactory.criarBotaoVerValor(),
            buttonFactory.criarBotaoRodarSQL()
        );
        
        JPanel bottomPanel = PainelBuilder.criarPainelInferior(labelTotal, painelBotoes);

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
            faturaService.getLancamentos().forEach(lancamento -> model.addRow(lancamento.toArray()));

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

    private int getAlturaDinamicaFrame() {
        int quantidadeLinhas = faturaService.getLancamentos().size();
        if(quantidadeLinhas <= 6) {
            return 350;
        } else if(quantidadeLinhas <= 17) {
            return 180 + (quantidadeLinhas * TabelaService.ROW_HEIGHT);
        } else {
            return 650;
        }
    }
}
