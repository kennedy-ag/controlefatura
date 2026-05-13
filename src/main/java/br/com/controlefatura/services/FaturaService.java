package br.com.controlefatura.services;

import java.awt.HeadlessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import br.com.controlefatura.exception.FaturaException;
import br.com.controlefatura.model.Lancamento;
import br.com.controlefatura.persistence.FaturaDao;

/**
 * Serviço para controle de faturas.
 * Centraliza a lógica de cálculos e operações com lançamentos.
 */
public class FaturaService {
    private static final Logger logger = Logger.getLogger(FaturaService.class.getName());

    private static final LocalDate FECHAMENTO_FATURA = LocalDate.now().withDayOfMonth(11);

    private static final Locale LOCALE_PADRAO = Locale.GERMANY;
    private static final List<String> MESES = List.of(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    );

    private static final HashMap<String, String> comandosSQL = new HashMap<>();

    private final FaturaDao faturaDao;
    private final FormService formService;

    public FaturaService(FaturaDao faturaDao) {
        this.faturaDao = faturaDao;
        this.formService = new FormService(this);
        comandosSQL.put("historico", "SELECT * FROM historico_lancamento");
        comandosSQL.put("parcelas", "SELECT * FROM parcelas");
        comandosSQL.put("total-parcelado", "SELECT SUM(valor_parcela) FROM lancamento WHERE quantidade_parcelas > 1");
        comandosSQL.put("total-a-vista", "SELECT SUM(valor_parcela) FROM lancamento WHERE quantidade_parcelas = 1");
    }

    public List<String> getColunas() {
        return List.of("ID", "Data", "Nome", "A pagar", "Vezes", "Parcela", "É meu?", "Cartão", "Meses");
    }

    public List<Lancamento> getLancamentos() {
        try {
            return faturaDao.getLancamentos();
        } catch (Exception e) {
            logger.severe(String.format("Erro ao obter dados das faturas: %s", e.getMessage()));
            throw new FaturaException("Falha ao carregar faturas do banco de dados.", e);
        }
    }

    /**
     * Retorna o total a pagar de todas as faturas.
     */
    public BigDecimal getTotalFatura() {
        try {
            BigDecimal total = faturaDao.getTotalFatura();
            return total != null ? total.setScale(2) : BigDecimal.ZERO.setScale(2);
        } catch (Exception e) {
            logger.severe(String.format("Erro ao calcular total de faturas: %s", e.getMessage()));
            throw new FaturaException("Falha ao calcular total.", e);
        }
    }

    public String getTotalFaturaFormatado() {
        try {
            NumberFormat formato = NumberFormat.getNumberInstance(LOCALE_PADRAO);
            formato.setMinimumFractionDigits(2);
            formato.setMaximumFractionDigits(2);
            BigDecimal totalFatura = getTotalFatura();
            return formato.format(totalFatura);
        } catch (Exception e) {
            logger.severe(String.format("Erro ao formatar total de faturas: %s", e.getMessage()));
            return "0,00";
        }
    }

    public String getResumoFaturas() {
        try {
            NumberFormat formato = NumberFormat.getNumberInstance(LOCALE_PADRAO);
            formato.setMinimumFractionDigits(2);
            formato.setMaximumFractionDigits(2);

            HashMap<String, BigDecimal> proximasFaturas = faturaDao.getProximasFaturas();
            StringBuilder resumo = new StringBuilder();

            for (String mes : MESES) {
                if (proximasFaturas.containsKey(mes)) {
                    BigDecimal valor = proximasFaturas.get(mes).setScale(2, RoundingMode.HALF_UP);
                    resumo.append(String.format("%s: R$ %s\n", mes, formato.format(valor)));
                }
            }

            return resumo.toString();
        } catch (Exception e) {
            logger.severe(String.format("Erro ao obter valor do mês: %s", e.getMessage()));
            throw new FaturaException("Falha ao obter valor do mês.", e);
        }
    }

    public void inserirLancamento(Lancamento lancamento) {
        if (lancamento == null) {
            throw new FaturaException("Lançamento não pode ser nulo.");
        }

        try {
            BigDecimal valorPagar = lancamento.getTotalAPagar();
            int parcelas = lancamento.getQuantidadeParcelas();

            if (parcelas <= 0) {
                throw new FaturaException("Número de parcelas deve ser maior que zero.");
            }

            if (valorPagar.compareTo(BigDecimal.ZERO) <= 0) {
                throw new FaturaException("Valor a pagar deve ser maior que zero.");
            }

            // Calcula o valor da parcela
            BigDecimal valorParcela = valorPagar.divide(BigDecimal.valueOf(parcelas), RoundingMode.HALF_UP);
            lancamento.setValorParcela(valorParcela);

            faturaDao.inserirLancamento(lancamento);
            faturaDao.inserirHistoricoLancamento(lancamento);
            inserirParcelasLancamento(lancamento);

            logger.info(String.format("Lançamento inserido com sucesso: %s", lancamento.getNome()));
        } catch (ClassCastException e) {
            logger.severe(String.format("Erro ao converter tipo de dados do lançamento: %s", e.getMessage()));
            throw new FaturaException("Tipo de dados inválido no lançamento.", e);
        } catch (FaturaException e) {
            logger.severe(String.format("Erro ao inserir lançamento: %s", e.getMessage()));
            throw new FaturaException("Falha ao inserir lançamento.", e);
        }
    }

    private void inserirParcelasLancamento(Lancamento lancamento) {
        LocalDate dataLancamento = LocalDate.parse(lancamento.getData(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int aux = dataLancamento.isAfter(FECHAMENTO_FATURA) ? 1 : 0;

        for (int i = 0; i < lancamento.getQuantidadeParcelas(); i++) {
            faturaDao.inserirParcelaLancamento(
                faturaDao.getUltimoIdLancamento(), 
                lancamento.getValorParcela(), 
                dataLancamento.plusMonths(aux+i).getMonth().getDisplayName(TextStyle.FULL, Locale.of("pt", "BR"))
            );
        }
    }

    public void deletarLancamento(int id) {
        deletarLancamentos(List.of(id));
    }

    public void deletarLancamentos(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new FaturaException("Nenhum ID informado para exclusão.");
        }
        for (Integer id : ids) {
            if (id == null || id <= 0) {
                throw new FaturaException("IDs inválidos para exclusão.");
            }
        }

        try {
            int rowsDeleted = faturaDao.deletarLancamentosPorIds(ids);
            if (rowsDeleted != ids.size()) {
                throw new FaturaException("Nem todos os lançamentos foram encontrados para exclusão.");
            }
            logger.info(String.format("Lançamentos excluídos com sucesso: %s", ids));
        } catch (FaturaException e) {
            logger.severe(String.format("Erro ao excluir lançamentos: %s", e.getMessage()));
            throw new FaturaException("Falha ao excluir lançamentos.", e);
        }
    }

    /**
     * Gera uma string com as iniciais dos meses.
     * @param quantidade Quantidade de meses a gerar
     * @param incluirMesAtual Se deve incluir o mês atual
     */
    public String obterStringMeses(int quantidade, boolean incluirMesAtual) {
        try {
            if (quantidade <= 0) {
                throw new FaturaException("Quantidade de meses deve ser maior que zero.");
            }

            StringBuilder resultado = new StringBuilder();
            LocalDate dataAtual = LocalDate.now();

            for (int i = incluirMesAtual ? 0 : 1; i < (incluirMesAtual ? quantidade : quantidade + 1); i++) {
                Month mes = dataAtual.plusMonths(i).getMonth();
                String primeiraLetra = extrairInicialMes(mes);
                resultado.append(primeiraLetra);
            }

            return resultado.toString().toUpperCase();
        } catch (FaturaException e) {
            logger.severe(String.format("Erro ao gerar string de meses: %s", e.getMessage()));
            throw new FaturaException("Falha ao gerar string de meses.", e);
        }
    }

    /**
     * Realiza o pagamento da fatura do mês atual.
     */
    public void pagarFatura() {
        try {
            int confirmado = formService.confirmarPagamentoFatura();

            if (confirmado == 1) {
                String inicialMes = extrairInicialMes(LocalDate.now().getMonth());
                faturaDao.pagarFatura(inicialMes + "%");
                logger.info(String.format("Fatura paga com sucesso para o mês: %s", inicialMes));
                JOptionPane.showMessageDialog(null, "Pagamento realizado!");
            }
        } catch (HeadlessException e) {
            logger.severe(String.format("Erro ao pagar fatura: %s", e.getMessage()));
            throw new FaturaException("Falha ao realizar pagamento.", e);
        }
    }

    /**
     * Executa uma query SQL eventual.
     */
    public String rodarQueryEventual(String arg) {
        if (arg == null || arg.isBlank()) {
            throw new FaturaException("SQL não pode estar vazio.");
        }

        if (comandosSQL.containsKey(arg)) {
            arg = comandosSQL.get(arg);
        }

        boolean isSelect = arg.trim().toLowerCase().startsWith("select");
        arg = isSelect ? arg.replace(";", "") + " LIMIT 40" : arg;

        try {
            return faturaDao.rodarQueryEventual(arg, isSelect);
        } catch (Exception e) {
            logger.severe(String.format("Erro ao executar query eventual: %s", e.getMessage()));
            return "Erro: " + e.getMessage();
        }
    }

    /**
     * Extrai a primeira letra do nome de um mês em maiúscula.
     */
    private String extrairInicialMes(Month mes) {
        return mes.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .substring(0, 1)
            .toUpperCase();
    }
}
