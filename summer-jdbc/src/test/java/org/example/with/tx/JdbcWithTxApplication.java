package org.example.with.tx;

import com.itranswarp.summer.annotation.ComponentScan;
import com.itranswarp.summer.annotation.Configuration;
import com.itranswarp.summer.annotation.Import;
import org.example.jdbc.JdbcConfiguration;

@ComponentScan
@Configuration
@Import(JdbcConfiguration.class)
public class JdbcWithTxApplication {
}
