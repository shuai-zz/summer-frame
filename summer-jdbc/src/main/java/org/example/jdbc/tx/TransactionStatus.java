package org.example.jdbc.tx;

import java.sql.Connection;

/**
 * @author zhaoshuai
 */
public record TransactionStatus(Connection connection) {
}
