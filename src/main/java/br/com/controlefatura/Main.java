package br.com.controlefatura;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import br.com.controlefatura.services.FaturaService;
import br.com.controlefatura.services.FormService;
import br.com.controlefatura.services.TabelaService;

public class Main { 
    public static void main(String[] args) {
    	FaturaService faturaService = new FaturaService();
    	
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Controle de fatura");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(630, 420);
            frame.setLocationRelativeTo(null);

            DefaultTableModel tableModel = new DefaultTableModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            
            faturaService.getColunas().forEach((coluna) -> tableModel.addColumn(coluna));
            faturaService.getDadosFatura().forEach((lancamento) -> tableModel.addRow(lancamento));
            
            JTable tabela = TabelaService.criarTabela(tableModel);
            JScrollPane scrollPane = new JScrollPane(tabela);
            
            // Criação da label e do valor
            JLabel label = new JLabel("Total:");
            JLabel value = new JLabel("R$ "+faturaService.getTotalFaturaFormatado());
            value.setFont(new Font("Arial", Font.PLAIN, 20));

            JButton botaoAdicionar = new JButton("Adicionar"); 
            botaoAdicionar.addActionListener((e) -> {
                Object[] lancamento = FormService.formAdicionarLancamento();
                faturaService.inserirLancamento(lancamento);
                value.setText("R$ "+faturaService.getTotalFaturaFormatado());
                TabelaService.atualizarTabela(tableModel);
                JOptionPane.showMessageDialog(null, "Lançamento adicionado!");
            });
            
            JButton botaoPagar = new JButton("Pagar");
            botaoPagar.addActionListener((e) -> {
                faturaService.pagarFatura();
                value.setText("R$ "+faturaService.getTotalFaturaFormatado());
                TabelaService.atualizarTabela(tableModel);
            });
                
            JButton botaoRodarEventual = new JButton("SQL");
            botaoRodarEventual.addActionListener(
                (e) -> {
                    String sql = JOptionPane.showInputDialog("Digite a query eventual: ");
                    JOptionPane.showMessageDialog(null, faturaService.rodarQueryEventual(sql));
                    TabelaService.atualizarTabela(tableModel);
                }
            );

            JButton botaoVerValorMes = new JButton("Valor");
            botaoVerValorMes.addActionListener(
                (e) -> {
                    JOptionPane.showMessageDialog(null, faturaService.getValorMes());
                }
            );

            // Criação do painel para os botões
            JPanel painelBotoes = new JPanel();
            painelBotoes.add(botaoAdicionar);
            painelBotoes.add(botaoPagar);
            painelBotoes.add(botaoRodarEventual);
            painelBotoes.add(botaoVerValorMes);

            // Painel para a label e o valor
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            labelPanel.add(label);
            labelPanel.add(value);
            
            // Painel para agrupar label e valor
            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BorderLayout());
            bottomPanel.add(labelPanel, BorderLayout.NORTH);
            bottomPanel.add(painelBotoes, BorderLayout.SOUTH);

            // Criação do painel principal
            JPanel painelPrincipal = new JPanel(new BorderLayout());
            painelPrincipal.add(scrollPane, BorderLayout.CENTER);
            painelPrincipal.add(bottomPanel, BorderLayout.SOUTH);

            frame.add(painelPrincipal);
            frame.setVisible(true);
        });
    }
}