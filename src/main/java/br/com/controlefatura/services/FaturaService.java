package br.com.controlefatura.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import br.com.controlefatura.exception.FaturaException;
import br.com.controlefatura.persistence.FaturaDao;

/**
 * Serviço para controle de faturas.
 * Centraliza a lógica de cálculos e operações com lançamentos.
 */
public class FaturaService {
    private static final Logger logger = Logger.getLogger(FaturaService.class.getName());
    private static final LocalDate DIA_20 = LocalDate.now().withDayOfMonth(20);
    private static final Locale LOCALE_PADRAO = Locale.GERMANY;

    private final FaturaDao faturaDao;
    private final FormService formService;

    public FaturaService(FaturaDao faturaDao) {
        this.faturaDao = faturaDao;
        this.formService = new FormService(this);
    }

    /**
     * Retorna as colunas da tabela de faturas.
     */
    public List<String> getColunas() {
        return List.of("ID", "Nome", "A pagar", "Vezes", "Parcela", "É meu?", "Meses");
    }

    /**
     * Obtém todos os dados das faturas.
     */
    public List<Object[]> getDadosFatura() {
        try {
            return faturaDao.getDadosFatura();
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

    /**
     * Retorna o total formatado como string em formato de moeda (locales Alemanha).
     */
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

    /**
     * Obtém o valor da fatura do mês informado.
     */
    public BigDecimal getValorMes() {
        try {
            LocalDate hoje = LocalDate.now();
            String inicialMesAtual = extrairInicialMes(hoje.getMonth());
            String inicialMesSeguinte = extrairInicialMes(hoje.plusMonths(1).getMonth());

            if (hoje.isAfter(DIA_20) || hoje.isEqual(DIA_20)) {
                BigDecimal valor = faturaDao.getValorMes(inicialMesSeguinte + "%");
                return valor != null ? valor.setScale(2, RoundingMode.CEILING) : BigDecimal.ZERO;
            } else {
                BigDecimal valor = faturaDao.getValorMes(inicialMesAtual + "%");
                return valor != null ? valor.setScale(2, RoundingMode.CEILING) : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            logger.severe(String.format("Erro ao obter valor do mês: %s", e.getMessage()));
            throw new FaturaException("Falha ao obter valor do mês.", e);
        }
    }

    /**
     * Insere um novo lançamento de fatura.
     */
    public void inserirLancamento(Object[] lancamento) {
        if (lancamento == null || lancamento.length != 7) {
            throw new FaturaException("Dados do lançamento inválidos.");
        }

        try {
            BigDecimal valorPagar = (BigDecimal) lancamento[2];
            int parcelas = (int) lancamento[3];

            if (parcelas <= 0) {
                throw new FaturaException("Número de parcelas deve ser maior que zero.");
            }

            if (valorPagar.compareTo(BigDecimal.ZERO) <= 0) {
                throw new FaturaException("Valor da parcela deve ser maior que zero.");
            }

            // Calcula o valor da parcela
            BigDecimal valorParcela = valorPagar.divide(BigDecimal.valueOf(parcelas), RoundingMode.HALF_UP);
            lancamento[6] = valorParcela;

            faturaDao.inserirLancamento(lancamento);
            logger.info(String.format("Lançamento inserido com sucesso: %s", lancamento[1]));
        } catch (ClassCastException e) {
            logger.severe(String.format("Erro ao converter tipo de dados do lançamento: %s", e.getMessage()));
            throw new FaturaException("Tipo de dados inválido no lançamento.", e);
        } catch (Exception e) {
            logger.severe(String.format("Erro ao inserir lançamento: %s", e.getMessage()));
            throw new FaturaException("Falha ao inserir lançamento.", e);
        }
    }

    /**
     * Exclui um lançamento pelo ID.
     */
    public void deletarLancamento(int id) {
        deletarLancamentos(List.of(id));
    }

    /**
     * Exclui vários lançamentos pelos IDs.
     */
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
        } catch (Exception e) {
            logger.severe(String.format("Erro ao excluir lançamentos: %s", e.getMessage()));
            throw new FaturaException("Falha ao excluir lançamentos.", e);
        }
    }

    /**
     * Retorna o ID máximo atual no banco de dados.
     */
    public int getMaxId() {
        try {
            return faturaDao.getMaxId();
        } catch (Exception e) {
            logger.severe(String.format("Erro ao obter max ID: %s", e.getMessage()));
            throw new FaturaException("Falha ao obter ID máximo.", e);
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            logger.severe(String.format("Erro ao pagar fatura: %s", e.getMessage()));
            throw new FaturaException("Falha ao realizar pagamento.", e);
        }
    }

    /**
     * Executa uma query SQL eventual.
     */
    public String rodarQueryEventual(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new FaturaException("SQL não pode estar vazio.");
        }

        try {
            return faturaDao.rodarQueryEventual(sql);
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

