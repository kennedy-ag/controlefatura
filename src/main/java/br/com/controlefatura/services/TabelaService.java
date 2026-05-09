package br.com.controlefatura.services;

import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.logging.Logger;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import br.com.controlefatura.exception.FaturaException;

/**
 * Renderer customizado para aplicar negrito e centralização.
 */
class BoldCenterRenderer extends DefaultTableCellRenderer {
    @Override
    public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setHorizontalAlignment(SwingConstants.CENTER);
        
        Font fontAtual = getFont();
        if (fontAtual != null) {
            setFont(new Font(fontAtual.getName(), Font.BOLD, fontAtual.getSize()));
        }
        return this;
    }
}

/**
 * Renderer customizado para aplicar cores baseado no valor (S/N).
 */
class ColoredCellRenderer extends DefaultTableCellRenderer {
    @Override
    public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setHorizontalAlignment(SwingConstants.CENTER);
        
        if ("S".equals(value)) {
            setForeground(Color.BLUE);
        } else if ("N".equals(value)) {
            setForeground(new Color(255, 140, 0)); // Laranja
        } else {
            setForeground(Color.BLACK);
        }
        return this;
    }
}

/**
 * Renderer customizado para formatar números decimais com padrão brasileiro.
 */
class FormattedDecimalRenderer extends DefaultTableCellRenderer {
    private static final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    private static final DecimalFormat decimalFormat;
    
    static {
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        decimalFormat = new DecimalFormat("#,##0.00", symbols);
    }
    
    @Override
    public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setHorizontalAlignment(SwingConstants.CENTER);
        
        if (value instanceof BigDecimal bigDecimal) {
            setText(decimalFormat.format(bigDecimal));
        }
        return this;
    }
}

/**
 * Renderer customizado com espaçamento lateral.
 */
class PaddedLeftRenderer extends DefaultTableCellRenderer {
    private static final int PADDING = 10;
    
    @Override
    public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(new EmptyBorder(0, PADDING, 0, 0));
        return this;
    }
}

/**
 * Serviço para gerenciar a tabela de faturas na interface.
 */
public class TabelaService {
    private static final Logger logger = Logger.getLogger(TabelaService.class.getName());
    private static final int ROW_HEIGHT = 27;
    private static final int HEADER_HEIGHT = 27;
    private static final int[] COLUMN_WIDTHS = {60, 170, 80, 50, 80, 50, 90};
    private static final int[] COLUNAS_CENTRALIZAR = {0, 2, 3, 4, 5};

    /**
     * Cria uma JTable com formatação e sorter configurados.
     */
    public static JTable criarTabela(DefaultTableModel tableModel) {
        if (tableModel == null) {
            throw new FaturaException("Modelo da tabela não pode ser nulo.");
        }

        try {
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
            configurarComparadores(sorter);

            JTable tabela = new JTable(tableModel);
            tabela.setRowHeight(ROW_HEIGHT);
            tabela.getTableHeader().setPreferredSize(new java.awt.Dimension(0, HEADER_HEIGHT));
            tabela.setRowSorter(sorter);
            tabela.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            tabela.setRowSelectionAllowed(true);
            tabela.setColumnSelectionAllowed(false);

            redimensionarColunas(tabela);
            centralizarColunas(tabela, COLUNAS_CENTRALIZAR);
            aplicarNegroNaColuna(tabela, 0);
            aplicarCoresNaColuna(tabela, 5);
            aplicarFormatacaoDecimal(tabela, 2, 4);
            aplicarEspacamentoNaColuna(tabela, 1);

            return tabela;
        } catch (Exception e) {
            logger.severe(String.format("Erro ao criar tabela: %s", e.getMessage()));
            throw new FaturaException("Falha ao criar tabela.", e);
        }
    }

    /**
     * Atualiza a tabela com os dados mais recentes do banco.
     */
    public static void atualizarTabela(DefaultTableModel tableModel, FaturaService faturaService) {
        if (tableModel == null) {
            throw new FaturaException("Modelo da tabela não pode ser nulo.");
        }
        if (faturaService == null) {
            throw new FaturaException("Serviço de fatura não pode ser nulo.");
        }

        try {
            // Remove todas as linhas
            while (tableModel.getRowCount() > 0) {
                tableModel.removeRow(0);
            }

            // Adiciona as linhas atualizadas
            faturaService.getDadosFatura().forEach(tableModel::addRow);
            logger.info("Tabela atualizada com sucesso.");
        } catch (Exception e) {
            logger.severe(String.format("Erro ao atualizar tabela: %s", e.getMessage()));
            throw new FaturaException("Falha ao atualizar tabela.", e);
        }
    }

    /**
     * Configura os comparadores para as colunas da tabela.
     */
    private static void configurarComparadores(TableRowSorter<DefaultTableModel> sorter) {
        sorter.setComparator(0, (Integer id1, Integer id2) -> Integer.compare(id1, id2));
        sorter.setComparator(2, (BigDecimal b1, BigDecimal b2) -> {
            if (b1 == null && b2 == null) return 0;
            if (b1 == null) return -1;
            if (b2 == null) return 1;
            return b1.compareTo(b2);
        });
        sorter.setComparator(3, (Integer i1, Integer i2) -> Integer.compare(i1, i2));
        sorter.setComparator(4, (BigDecimal b1, BigDecimal b2) -> {
            if (b1 == null && b2 == null) return 0;
            if (b1 == null) return -1;
            if (b2 == null) return 1;
            return b1.compareTo(b2);
        });
    }

    /**
     * Redimensiona as colunas da tabela.
     */
    private static void redimensionarColunas(JTable tabela) {
        for (int i = 0; i < COLUMN_WIDTHS.length && i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(COLUMN_WIDTHS[i]);
        }
    }

    /**
     * Centraliza as colunas especificadas.
     */
    private static void centralizarColunas(JTable tabela, int[] colunas) {
        DefaultTableCellRenderer centralizado = new DefaultTableCellRenderer();
        centralizado.setHorizontalAlignment(SwingConstants.CENTER);

        for (int coluna : colunas) {
            if (coluna < tabela.getColumnCount()) {
                tabela.getColumnModel().getColumn(coluna).setCellRenderer(centralizado);
            }
        }
    }

    /**
     * Aplica negrito e centralização em uma coluna específica.
     */
    private static void aplicarNegroNaColuna(JTable tabela, int coluna) {
        if (coluna < tabela.getColumnCount()) {
            tabela.getColumnModel().getColumn(coluna).setCellRenderer(new BoldCenterRenderer());
        }
    }

    /**
     * Aplica cores (azul para S, laranja para N) em uma coluna específica.
     */
    private static void aplicarCoresNaColuna(JTable tabela, int coluna) {
        if (coluna < tabela.getColumnCount()) {
            tabela.getColumnModel().getColumn(coluna).setCellRenderer(new ColoredCellRenderer());
        }
    }

    /**
     * Aplica formatação de números decimais em colunas específicas.
     */
    private static void aplicarFormatacaoDecimal(JTable tabela, int... colunas) {
        FormattedDecimalRenderer renderer = new FormattedDecimalRenderer();
        for (int coluna : colunas) {
            if (coluna < tabela.getColumnCount()) {
                tabela.getColumnModel().getColumn(coluna).setCellRenderer(renderer);
            }
        }
    }

    /**
     * Aplica espaçamento lateral em uma coluna específica.
     */
    private static void aplicarEspacamentoNaColuna(JTable tabela, int coluna) {
        if (coluna < tabela.getColumnCount()) {
            tabela.getColumnModel().getColumn(coluna).setCellRenderer(new PaddedLeftRenderer());
        }
    }
}

