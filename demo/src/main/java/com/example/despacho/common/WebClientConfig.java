package com.example.despacho.common;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebClientConfig {

    /**
     * Cliente hacia los "servicios externos" (en realidad /external/** de esta
     * misma app, simulados). Se usa siempre en paralelo con Mono.zip (OE3).
     */
    @Bean
    public WebClient transportistaWebClient(AppProperties props) {
        return WebClient.builder()
                .baseUrl(props.external().baseUrl())
                .build();
    }

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager tm) {
        return TransactionalOperator.create(tm);
    }
}
