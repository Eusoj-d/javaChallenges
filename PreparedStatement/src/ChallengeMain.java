package dev.lpa.challenge;

import com.mysql.cj.jdbc.MysqlDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChallengeMain {

    private static final String INSERT_ORDER = "INSERT INTO storefront.order (order_date) VALUES (?)";
    private static final String INSERT_DETAIL = "INSERT INTO storefront.order_details (quantity, item_description, order_id) VALUES (?,?,?)";

    public static void main(String[] args) {

        var dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabaseName("storefront");
        dataSource.setUser(System.getenv("USER"));
        dataSource.setPassword(System.getenv("PASSWORD"));

        try (Connection connection = dataSource.getConnection()) {
            readOrders(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static void readOrders(Connection connection){
        try {
            String readingOrders = Files.readString(Path.of("Orders.csv"));
            Pattern pattern = Pattern.compile("(?m)^order,.*(?:\\R(?!order,).*)*", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(readingOrders);

            int lastOrderId = -1;
            try (
                    PreparedStatement insertOrder = connection.prepareStatement(INSERT_ORDER, PreparedStatement.RETURN_GENERATED_KEYS);
                    PreparedStatement insertDetail = connection.prepareStatement(INSERT_DETAIL, PreparedStatement.RETURN_GENERATED_KEYS)
            ) {
                connection.setAutoCommit(false);

                while(matcher.find()) {

                    String[] order = matcher.group().split("\n");
                    String date = order[0].split(",")[1].trim();

                    if(isValidDateTime(date.split(" ")[0])){
                        lastOrderId = insertOrder(insertOrder, date);
                        if (lastOrderId > 0) {
                            for(int i = 1; i < order.length; i++) {
                                String[] columnsDetails = order[i].split(",");
                                int quantity = Integer.parseInt(columnsDetails[1]);
                                String description = columnsDetails[2].trim();
                                insertDetailMethod(insertDetail, quantity, description, lastOrderId);

                            }
                            var insertedDetails = insertDetail.executeBatch();
                            System.out.println("Inserting " + Arrays.toString(insertedDetails) + " details order");
                            connection.commit();
                        } else {
                            System.out.println("Rolling back");
                            connection.rollback();
                        }
                    } else {
                        System.err.println("Invalid date...");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
                System.err.println(e.getErrorCode());
                System.err.println("Rolling back...");
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void insertDetailMethod(PreparedStatement ps, int quantity, String description, int orderId) throws SQLException{
        ps.setInt(1, quantity);
        ps.setString(2, description);
        ps.setInt(3, orderId);
        System.out.println(ps);
        ps.addBatch();
    }

    private static int insertOrder(PreparedStatement ps, String dateTime) throws SQLException {
        int orderId = -1;
        ps.setString(1, dateTime);
        int insertCount = ps.executeUpdate();
        if(insertCount > 0){
            ResultSet rs = ps.getGeneratedKeys();
            if(rs.next()) {
                orderId = rs.getInt(1);
                System.out.println("Inserting order with Id: " + orderId);
            } else {
                System.out.println("Error in generating orderId");
            }
        } else {
            System.out.println("Couldn't insert order");
        }
        return orderId;
    }

    private static boolean isValidDateTime(String date) {
        try {
            LocalDate.parse(date);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
