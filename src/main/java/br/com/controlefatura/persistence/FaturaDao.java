package br.com.controlefatura.persistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import br.com.controlefatura.model.Lancamento;

public class FaturaDao {
    private static final String DB_DRIVER = "org.sqlite.JDBC";
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.FLOOR;
    private static final int SCALE = 2;
    private static String DB_URL;

    static {
        try {
            DB_URL = inicializarBancoDados();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao inicializar banco de dados: " + e.getMessage(), e);
        }
    }

    private static String inicializarBancoDados() throws IOException {
        // Tenta carregar o banco de dados a partir dos recursos
        InputStream recursoDb = FaturaDao.class.getResourceAsStream("/dados-fatura.db");
        
        if (recursoDb != null) {
            // Banco está nos recursos (dentro do jar)
            File dbTemp = new File(System.getProperty("user.home") + "/.controlefatura/dados-fatura.db");
            dbTemp.getParentFile().mkdirs();
            
            // Copia o banco apenas se ele não existe (primeira execução)
            if (!dbTemp.exists()) {
                Files.copy(recursoDb, dbTemp.toPath());
            }
            recursoDb.close();
            
            return "jdbc:sqlite:" + dbTemp.getAbsolutePath();
        } else {
            // Banco está no projeto (desenvolvimento)
            return "jdbc:sqlite:./src/main/resources/dados-fatura.db";
        }
    }

    private Connection obterConexao() {
        try {
            Class.forName(DB_DRIVER);
            return DriverManager.getConnection(DB_URL);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver JDBC não encontrado: " + DB_DRIVER, e);
        } catch (SQLException e) {
            throw new RuntimeException("Não foi possível se conectar ao banco de dados em: " + DB_URL, e);
        }
    }

    /**
     * Retorna todos os lançamentos como objetos Lancamento.
     * Este é o método preferido para uso interno.
     */
    public List<Lancamento> getLancamentos() {
        List<Lancamento> lista = new ArrayList<>();

        try (Connection conn = obterConexao();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM lancamento")) {
            
            while (resultSet.next()) {
                Lancamento lancamento = new Lancamento(
                    resultSet.getInt("id"),
                    resultSet.getString("data"),
                    resultSet.getString("nome"),
                    normalizarBigDecimal(resultSet.getDouble("total_a_pagar")),
                    resultSet.getInt("quantidade_parcelas"),
                    normalizarBigDecimal(resultSet.getBigDecimal("valor_parcela")),
                    resultSet.getBoolean("eh_meu")? "S" : "N",
                    resultSet.getString("cartao_utilizado"),
                    resultSet.getString("parcelas_restantes")
                );
                lista.add(lancamento);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Não foi possível carregar os dados!", e);
        }

        return lista;
    }

    public BigDecimal getTotalFatura() {
        try (Connection conn = obterConexao();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT SUM(total_a_pagar) FROM lancamento")) {
            
            if (resultSet.next()) {
                BigDecimal total = resultSet.getBigDecimal(1);
                return total == null ? BigDecimal.ZERO.setScale(SCALE) : total.setScale(SCALE, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO.setScale(SCALE);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter o valor total da fatura!", e);
        }
    }

    /**
     * Insere um novo lançamento no banco de dados usando um objeto Lancamento.
     */
    public void inserirLancamento(Lancamento lancamento) {
        String sql = "INSERT INTO lancamento (data, nome, total_a_pagar, quantidade_parcelas, valor_parcela, eh_meu, cartao_utilizado, parcelas_restantes) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, lancamento.getData());
            ps.setString(2, lancamento.getNome());
            ps.setBigDecimal(3, lancamento.getTotalAPagar());
            ps.setInt(4, lancamento.getQuantidadeParcelas());
            ps.setBigDecimal(5, lancamento.getValorParcela());
            ps.setBoolean(6, "S".equals(lancamento.getEhMeu()));
            ps.setString(7, lancamento.getCartaoUtilizado());
            ps.setString(8, lancamento.getParcelasRestantes());
            int rowsAffected = ps.executeUpdate();
            System.out.println(rowsAffected + " linha(s) inserida(s).");
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir lançamento!", e);
        }
    }

    /**
     * Insere um novo lançamento no histórico de lançamentos
     */
    public void inserirHistoricoLancamento(Lancamento lancamento) {
        String sql = "INSERT INTO historico_lancamento (data, nome, valor, quantidade_parcelas, eh_meu, cartao_utilizado) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, lancamento.getData());
            ps.setString(2, lancamento.getNome());
            ps.setBigDecimal(3, lancamento.getTotalAPagar());
            ps.setInt(4, lancamento.getQuantidadeParcelas());
            ps.setBoolean(5, "S".equals(lancamento.getEhMeu()));
            ps.setString(6, lancamento.getCartaoUtilizado());
            int rowsAffected = ps.executeUpdate();
            System.out.println(rowsAffected + " linha(s) inserida(s).");
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir lançamento no histórico!", e);
        }
    }

    public void inserirParcelaLancamento(int lancamentoId, BigDecimal valorParcela, String mes) {
        String sql = "INSERT INTO parcelas (lancamento_id, valor, mes) VALUES (?, ?, ?)";
        
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, lancamentoId);
            ps.setBigDecimal(2, valorParcela);
            ps.setString(3, mes);
            int rowsAffected = ps.executeUpdate();
            System.out.println(rowsAffected + " linha(s) inserida(s).");
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir parcela do lançamento!", e);
        }
    }

    public HashMap<String, BigDecimal> getProximasFaturas() {
        String sql = "SELECT mes, SUM(valor) FROM parcelas GROUP BY mes";
        
        try (Connection conn = obterConexao();
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)) {

            HashMap<String, BigDecimal> faturas = new HashMap<>();

            while (resultSet.next()) {
                String mes = resultSet.getString(1);
                BigDecimal valor = resultSet.getBigDecimal(2).setScale(2, RoundingMode.HALF_UP);
                faturas.put(mes, valor);
            }
            return faturas;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter próximas faturas!", e);
        }
    }

    public int getUltimoIdLancamento() {
        String sql = "SELECT MAX(id) FROM lancamento";
        
        try (Connection conn = obterConexao();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter o ID máximo do lançamento!", e);
        }
    }

    public void pagarFatura(String filtroMesAtual) {
        String updateSql = "UPDATE lancamento SET total_a_pagar = (total_a_pagar - (total_a_pagar / quantidade_parcelas)), "
                         + "quantidade_parcelas = quantidade_parcelas - 1, "
                         + "parcelas_restantes = substr(parcelas_restantes, 2) "
                         + "WHERE parcelas_restantes LIKE ?";
        String deleteSql = "DELETE FROM lancamento WHERE parcelas_restantes = 0";
        
        try (Connection conn = obterConexao()) {
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setString(1, filtroMesAtual);
                int rowsAffected = psUpdate.executeUpdate();
                System.out.println(rowsAffected + " linha(s) atualizadas(s).");
            }
            
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql)) {
                int rowsAffected = psDelete.executeUpdate();
                System.out.println(rowsAffected + " linha(s) excluída(s).");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao pagar fatura!", e);
        }
    }

    public int deletarLancamentoPorId(int id) {
        String sql = "DELETE FROM lancamento WHERE id = ?";
        
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir lançamento!", e);
        }
    }

    public int deletarLancamentosPorIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Lista de IDs não pode ser vazia.");
        }

        String sql = "DELETE FROM lancamento WHERE id = ?";
        Connection conn = null;
        try {
            conn = obterConexao();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int totalDeleted = 0;
                for (Integer id : ids) {
                    ps.setInt(1, id);
                    totalDeleted += ps.executeUpdate();
                }
                conn.commit();
                return totalDeleted;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    // ignore rollback failure
                }
            }
            throw new RuntimeException("Erro ao excluir lançamentos!", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    // ignore close failure
                }
            }
        }
    }

    public String rodarQueryEventual(String sql, boolean isSelect) {
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            if (isSelect) {
                try (ResultSet rs = ps.executeQuery()) {
                    StringBuilder resultado = new StringBuilder();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        resultado.append(rs.getMetaData().getColumnName(i)).append(" - ");
                        if (i == rs.getMetaData().getColumnCount()) {
                            resultado.setLength(resultado.length() - 3);
                        }
                    }
                    resultado.append("\n").append("-".repeat(50)).append("\n");

                    while (rs.next()) {
                        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                            resultado.append(rs.getString(i)).append(" - ");
                            if (i == rs.getMetaData().getColumnCount()) {
                                resultado.setLength(resultado.length() - 3);
                            }
                        }
                        resultado.append("\n");
                    }
                    return resultado.toString();
                }
            } else {
                int rowsAffected = ps.executeUpdate();
                return "Query eventual executada com sucesso!\n" + rowsAffected + " linha(s) afetada(s).";
            }
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    private BigDecimal normalizarBigDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        if (valor instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(SCALE, DEFAULT_ROUNDING);
        }
        return BigDecimal.valueOf(((Number) valor).doubleValue()).setScale(SCALE, DEFAULT_ROUNDING);
    }
}
