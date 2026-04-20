package br.com.controlefatura.services;

import java.math.BigDecimal;
import java.util.logging.Logger;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import br.com.controlefatura.exception.FaturaException;

/**
 * Serviço para gerenciar a tabela de faturas na interface.
 */
public class TabelaService {
    private static final Logger logger = Logger.getLogger(TabelaService.class.getName());
    private static final int ROW_HEIGHT = 25;
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
            tabela.setRowSorter(sorter);
            tabela.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            tabela.setRowSelectionAllowed(true);
            tabela.setColumnSelectionAllowed(false);

            redimensionarColunas(tabela);
            centralizarColunas(tabela, COLUNAS_CENTRALIZAR);

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
}

