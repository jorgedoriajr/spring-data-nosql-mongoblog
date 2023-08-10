package com.fiap.springblog.configuration;

import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class MongoTransactionConfig {

    @Autowired
    private MongoClient mongoClient;

    @Bean
    public MongoTransactionManager transactionManager(
                                      MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

}