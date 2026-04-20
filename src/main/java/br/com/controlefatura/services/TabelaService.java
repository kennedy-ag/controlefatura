package br.com.controlefatura.services;

import java.math.BigDecimal;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class TabelaService {
	private static final int ROW_HEIGHT = 25;
	
	public static JTable criarTabela(DefaultTableModel tableModel) {
		TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
		sorter.setComparator(0, (Integer id1, Integer id2) -> Integer.compare(id1, id2));
		sorter.setComparator(2, (BigDecimal b1, BigDecimal b2) -> b1.compareTo(b2));
        sorter.setComparator(3, (Integer id1, Integer id2) -> Integer.compare(id1, id2));
        sorter.setComparator(4, (BigDecimal b1, BigDecimal b2) -> b1.compareTo(b2));
		
		JTable tabela = new JTable(tableModel);
		tabela.setRowHeight(ROW_HEIGHT);
        tabela.setRowSorter(sorter);
        
        // Redimensionando colunas
        tabela.getColumnModel().getColumn(0).setPreferredWidth(60);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(170);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(50);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(50);
        tabela.getColumnModel().getColumn(6).setPreferredWidth(90);
        
        int[] colunasParaCentralizar = {0, 2, 3, 4, 5};
        centralizarColunas(tabela, colunasParaCentralizar);
        
        return tabela;
	}
	
	private static void centralizarColunas(JTable tabela, int[] colunas) {
		DefaultTableCellRenderer centralizado = new DefaultTableCellRenderer();
        centralizado.setHorizontalAlignment(SwingConstants.CENTER);
        
        for(int i: colunas) {
        	tabela.getColumnModel().getColumn(i).setCellRenderer(centralizado);
        }
	}

    public static void atualizarTabela(DefaultTableModel tableModel) {
        FaturaService faturaService = new FaturaService();
        for(int i=tableModel.getRowCount()-1; i>=0; i--) {
            tableModel.removeRow(i);
        }
        faturaService.getDadosFatura().forEach((lancamento) -> tableModel.addRow(lancamento));
    }
}
