package br.com.controlefatura.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Responsável por criar painéis da interface.
 */
public class PainelBuilder {

    /**
     * Cria o painel com os botões.
     */
    public static JPanel criarPainelBotoes(JButton... botoes) {
        JPanel painel = new JPanel();
        for (JButton botao : botoes) {
            painel.add(botao);
        }
        return painel;
    }

    /**
     * Cria o painel inferior com as informações e botões.
     */
    public static JPanel criarPainelInferior(JLabel labelTotal, JPanel painelBotoes) {
        JLabel labelTexto = new JLabel("Total:");
        labelTexto.setFont(new Font("Arial", Font.PLAIN, 14));
        labelTotal.setFont(new Font("Arial", Font.PLAIN, 20));

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        labelPanel.add(labelTexto);
        labelPanel.add(labelTotal);
        labelPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(labelPanel, BorderLayout.NORTH);
        bottomPanel.add(painelBotoes, BorderLayout.SOUTH);

        return bottomPanel;
    }
}
