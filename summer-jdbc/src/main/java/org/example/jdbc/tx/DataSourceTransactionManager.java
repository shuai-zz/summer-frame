package org.example.jdbc.tx;

import org.example.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author zhaoshuai
 */
public class DataSourceTransactionManager implements PlatformTransactionManager, InvocationHandler {
    static final ThreadLocal<TransactionStatus> TRANSACTION_STATUS = new ThreadLocal<>();

    final Logger logger= LoggerFactory.getLogger(getClass());

    final DataSource dataSource;

    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        TransactionStatus ts=TRANSACTION_STATUS.get();
        if(ts==null){
            // start new transaction
            try(Connection connection=dataSource.getConnection()){
                final boolean autoCommit=connection.getAutoCommit();
                if(autoCommit){
                    connection.setAutoCommit(false);
                }
                try{
                    TRANSACTION_STATUS.set(new TransactionStatus(connection));
                    Object r=method.invoke(proxy,args);
                    connection.commit();
                    return r;
                }catch (InvocationTargetException e){
                    logger.warn("will rollback transaction for caused exception: {}", e.getCause()==null?"null":e.getCause().getClass().getName());
                    TransactionException te = new TransactionException(e.getCause());
                    try{
                        connection.rollback();
                    }catch (SQLException sqlE){
                        te.addSuppressed(sqlE);
                    }
                    throw te;
                }finally {
                    TRANSACTION_STATUS.remove();
                    if(autoCommit){
                        connection.setAutoCommit(true);
                    }
                }
            }
        }else {
            // join current transaction
            return method.invoke(proxy, args);
        }
    }
}
