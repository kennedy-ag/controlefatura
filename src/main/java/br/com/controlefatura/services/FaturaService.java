package br.com.controlefatura.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import javax.swing.JOptionPane;

import br.com.controlefatura.persistence.FaturaDao;

public class FaturaService {
    private final LocalDate DIA_20 = LocalDate.now().withDayOfMonth(20);
    private final FaturaDao faturaDao = new FaturaDao();

	public List<String> getColunas(){
		List<String> colunas = List.of(
            "ID", "Nome", "A pagar", "Vezes", "Parcela", "É meu?", "Meses"
        );
		return colunas;
	}
	
	public List<Object[]> getDadosFatura(){
		return faturaDao.getDadosFatura();
	}

    public BigDecimal getTotalFatura() {
        return faturaDao.getTotalFatura().setScale(2);
    }

    public BigDecimal getValorMes() {
        LocalDate hoje = LocalDate.now();
        String inicialMesAtual = hoje.getMonth()
                            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .substring(0, 1)
                            .toUpperCase();

        String inicialMesSeguinte = hoje.plusMonths(1).getMonth()
                            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .substring(0, 1)
                            .toUpperCase();

        if(hoje.isAfter(DIA_20) || hoje.isEqual(DIA_20)) {
            return faturaDao.getValorMes(inicialMesSeguinte+"%").setScale(2, RoundingMode.CEILING);
        } else {
            return faturaDao.getValorMes(inicialMesAtual+"%").setScale(2, RoundingMode.CEILING);
        }
    }

    public String getTotalFaturaFormatado() {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.GERMANY);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        BigDecimal totalFatura = getTotalFatura();
        return formato.format(totalFatura);
    }

    public void inserirLancamento(Object[] lancamento) {
        BigDecimal valorPagar = (BigDecimal) lancamento[2];
        lancamento[6] = valorPagar.divide(BigDecimal.valueOf((int) lancamento[3]), RoundingMode.HALF_UP);
        faturaDao.inserirLancamento(lancamento);
    }

    public int getMaxId() {
        return faturaDao.getMaxId();
    }

    public String obterStringMeses(int x, boolean incluirMesAtual) {
        StringBuilder resultado = new StringBuilder();
        LocalDate dataAtual = LocalDate.now();

        for (int i = incluirMesAtual ? 0 : 1; i < (incluirMesAtual ? x : x + 1); i++) {
            Month mes = dataAtual.plusMonths(i).getMonth();
            String primeiraLetra = mes.getDisplayName(TextStyle.SHORT, Locale.getDefault()).substring(0, 1);
            resultado.append(primeiraLetra);
        }

        return resultado.toString().toUpperCase();
    }

    public void pagarFatura() {
        int confirmado = FormService.confirmarPagamentoFatura();

        if(confirmado==1) {
            String inicialMes = LocalDate.now().getMonth()
                            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .substring(0, 1)
                            .toUpperCase();
            faturaDao.pagarFatura(inicialMes+"%");
            JOptionPane.showMessageDialog(null, "Pagamento realizado!");
        }
    }

    public String rodarQueryEventual(String sql) {
        return faturaDao.rodarQueryEventual(sql);
    }
}
