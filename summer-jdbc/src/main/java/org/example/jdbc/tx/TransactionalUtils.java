package org.example.jdbc.tx;

import jakarta.annotation.Nullable;

import java.sql.Connection;

/**
 * @author zhaoshuai
 */
public class TransactionalUtils {
    @Nullable
    public static Connection getCurrentConnection(){
        TransactionStatus ts=DataSourceTransactionManager.TRANSACTION_STATUS.get();
        return ts==null?null: ts.connection();
    }
}
