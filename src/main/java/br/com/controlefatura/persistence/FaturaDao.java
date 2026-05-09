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
import java.util.List;

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

    public List<Object[]> getDadosFatura() {
        List<Object[]> lista = new ArrayList<>();

        try (Connection conn = obterConexao();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM lancamento")) {
            
            while (resultSet.next()) {
                Object[] linha = new Object[]{
                    resultSet.getInt("id"),
                    resultSet.getString("nome"),
                    normalizarBigDecimal(resultSet.getDouble("valor_a_pagar")),
                    resultSet.getInt("parcelas_restantes"),
                    normalizarBigDecimal(resultSet.getBigDecimal("valor_da_parcela")),
                    resultSet.getString("in_eh_meu"),
                    resultSet.getString("cartao_utilizado"),
                    resultSet.getString("meses_restantes"),
                };
                lista.add(linha);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Não foi possível carregar os dados!", e);
        }

        return lista;
    }

    public BigDecimal getTotalFatura() {
        try (Connection conn = obterConexao();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT SUM(valor_a_pagar) FROM lancamento")) {
            
            if (resultSet.next()) {
                BigDecimal total = resultSet.getBigDecimal(1);
                return total == null ? BigDecimal.ZERO.setScale(SCALE) : total.setScale(SCALE, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO.setScale(SCALE);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter o valor total da fatura!", e);
        }
    }

    public void inserirLancamento(Object[] lancamento) {
        String sql = "INSERT INTO lancamento (id, nome, valor_a_pagar, parcelas_restantes, in_eh_meu, meses_restantes, valor_da_parcela, cartao_utilizado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, (int) lancamento[0]);
            ps.setString(2, (String) lancamento[1]);
            ps.setBigDecimal(3, (BigDecimal) lancamento[2]);
            ps.setInt(4, (int) lancamento[3]);
            ps.setString(5, (String) lancamento[4]);
            ps.setString(6, (String) lancamento[5]);
            ps.setBigDecimal(7, (BigDecimal) lancamento[6]);
            ps.setString(8, (String) lancamento[7]);
            int rowsAffected = ps.executeUpdate();
            System.out.println(rowsAffected + " linha(s) inserida(s).");
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir lançamento!", e);
        }
    }

    public int getMaxId() {
        try (Connection conn = obterConexao();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT MAX(id) FROM lancamento")) {
            
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            throw new RuntimeException("Nenhum ID foi encontrado!");
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter max id!", e);
        }
    }

    public BigDecimal getValorMes(String like) {
        String sql = "SELECT SUM(valor_da_parcela) FROM lancamento WHERE meses_restantes LIKE ?";
        
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, like);
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal(1);
                }
                throw new RuntimeException("Sem lançamentos a serem pagos.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter valor do mês!", e);
        }
    }

    public void pagarFatura(String filtroMesAtual) {
        String updateSql = "UPDATE lancamento SET valor_a_pagar = (valor_a_pagar - (valor_a_pagar / parcelas_restantes)), "
                         + "parcelas_restantes = parcelas_restantes - 1, "
                         + "meses_restantes = substr(meses_restantes, 2) "
                         + "WHERE meses_restantes LIKE ?";
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

    public String rodarQueryEventual(String sql) {
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            int rowsAffected = ps.executeUpdate();
            return "Query eventual executada com sucesso!\n" + rowsAffected + " linha(s) afetada(s).";
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
