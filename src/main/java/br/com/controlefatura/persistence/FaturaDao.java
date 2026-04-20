package br.com.controlefatura.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FaturaDao {
    private Connection obterConexao() {
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:./dados-fatura.db";
            Connection connection = DriverManager.getConnection(url);
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível se conectar ao banco de dados!");
        }
    }

    public List<Object[]> getDadosFatura() {
        List<Object[]> lista = new ArrayList<>();

        try {
            Connection conn = obterConexao();
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM lancamento");

            while (resultSet.next()) {
                Object[] linha = new Object[]{
                    resultSet.getInt("id"),
                    resultSet.getString("nome"),
                    BigDecimal.valueOf(resultSet.getDouble("valor_a_pagar")).setScale(2, RoundingMode.FLOOR),
                    resultSet.getInt("parcelas_restantes"),
                    resultSet.getBigDecimal("valor_da_parcela").setScale(2, RoundingMode.FLOOR),
                    resultSet.getString("in_eh_meu"),
                    resultSet.getString("meses_restantes"),
                };
                lista.add(linha);
            }

            resultSet.close();
            statement.close();
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("Não foi possível carregar os dados!");
        }

        return lista;
    }

    public BigDecimal getTotalFatura() {
        BigDecimal totalFatura = new BigDecimal(0);

        try {
            Connection conn = obterConexao();
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT SUM(valor_a_pagar) FROM lancamento");

            if (resultSet.next()) {
                totalFatura = resultSet.getBigDecimal(1) == null? 
                                new BigDecimal(0).setScale(2) : 
                                resultSet.getBigDecimal(1).setScale(2, RoundingMode.HALF_UP);
            } else {
                totalFatura = new BigDecimal(0).setScale(2, RoundingMode.UNNECESSARY);
            }

            resultSet.close();
            statement.close();
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter o valor total da fatura!");
        }

        return totalFatura;
    }

    public void inserirLancamento(Object[] lancamento) {
        try {
            Connection conn = obterConexao();
            String sql = "INSERT INTO lancamento "
                        +"(id, nome, valor_a_pagar, parcelas_restantes, in_eh_meu, meses_restantes, valor_da_parcela) "
                        +"VALUES (?, ?, ?, ?, ?, ?, ?);";

            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setInt(1, (int) lancamento[0]); // id
                preparedStatement.setString(2, (String) lancamento[1]); // nome
                preparedStatement.setBigDecimal(3, (BigDecimal) lancamento[2]); // valor_a_pagar
                preparedStatement.setInt(4, (int) lancamento[3]); // parcelas_restantes
                preparedStatement.setString(5, (String) lancamento[4]); // in_eh_meu
                preparedStatement.setString(6, (String) lancamento[5]); // meses_restantes
                preparedStatement.setBigDecimal(7, (BigDecimal) lancamento[6]); // valor_da_parcela

                int rowsAffected = preparedStatement.executeUpdate();
                System.out.println(rowsAffected + " linha(s) inserida(s).");

                preparedStatement.close();
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir lançamento!");
        }
    }

    public int getMaxId() {
        int maxId;
        try {
            Connection conn = obterConexao();
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT MAX(id) FROM lancamento");

            if (resultSet.next()) {
                maxId = resultSet.getInt(1);
            } else {
                throw new RuntimeException("Nenhum ID foi encontrado!");
            }

            resultSet.close();
            statement.close();
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter max id!");
        }

        return maxId;
    }

    public BigDecimal getValorMes(String like) {
        BigDecimal valorFaturaMes;
        try {
            Connection conn = obterConexao();
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT SUM(valor_da_parcela) FROM lancamento WHERE meses_restantes LIKE '"+like+"'"
            );

            if (resultSet.next()) {
                valorFaturaMes = resultSet.getBigDecimal(1);
            } else {
                throw new RuntimeException("Sem lançamentos a serem pagos.");
            }

            resultSet.close();
            statement.close();
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter max id!");
        }

        return valorFaturaMes;
    }

    public void pagarFatura(String filtroMesAtual) {
        try {
            Connection conn = obterConexao();
            String update = "UPDATE lancamento "
                        +"SET valor_a_pagar=(valor_a_pagar-(valor_a_pagar/parcelas_restantes)), "
                        +"parcelas_restantes=parcelas_restantes-1, "
                        +"meses_restantes=substr(meses_restantes, 2) "
                        +"WHERE meses_restantes LIKE '"+filtroMesAtual+"'";
            
            String delete = "DELETE FROM lancamento WHERE parcelas_restantes=0";
            try (PreparedStatement preparedStatement = conn.prepareStatement(update)) {
                int rowsAffected = preparedStatement.executeUpdate();
                System.out.println(rowsAffected + " linha(s) atualizadas(s).");
                preparedStatement.close();
            }
            try (PreparedStatement preparedStatement = conn.prepareStatement(delete)) {
                int rowsAffected = preparedStatement.executeUpdate();
                System.out.println(rowsAffected + " linha(s) excluída(s).");
                preparedStatement.close();
            }
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao pagar fatura!");
        }
    }

    public String rodarQueryEventual(String sql) {
        Connection conn = obterConexao();

        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            int rowsAffected = preparedStatement.executeUpdate();
            preparedStatement.close();
            conn.close();
            return "Query eventual executada com sucesso!\n" + rowsAffected + " linha(s) afetada(s).";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }
}
