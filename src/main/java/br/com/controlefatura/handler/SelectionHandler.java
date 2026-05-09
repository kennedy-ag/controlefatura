package br.com.controlefatura.handler;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import br.com.controlefatura.exception.FaturaException;

/**
 * Responsável por gerenciar a seleção de IDs na tabela.
 */
public class SelectionHandler {
    private final JTable tabela;
    private final DefaultTableModel tableModel;

    public SelectionHandler(JTable tabela, DefaultTableModel tableModel) {
        this.tabela = tabela;
        this.tableModel = tableModel;
    }

    /**
     * Obtém os IDs selecionados da tabela ou solicita ao usuário.
     */
    public List<Integer> obterIdsSelecionadosOuSolicitados() {
        int[] selectedRows = tabela.getSelectedRows();
        if (selectedRows.length > 0) {
            return extrairIdsDasLinhas(selectedRows);
        }

        return obterIdsViaDialogo();
    }

    /**
     * Extrai os IDs das linhas selecionadas.
     */
    private List<Integer> extrairIdsDasLinhas(int[] selectedRows) {
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

    /**
     * Obtém IDs através de um diálogo do usuário.
     */
    private List<Integer> obterIdsViaDialogo() {
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
}
