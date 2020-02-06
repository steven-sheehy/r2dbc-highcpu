package com.example.r2dbc.queryactive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.transaction.reactive.ReactiveTransactionAutoConfiguration;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.query.Criteria;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;

@SpringBootApplication(exclude = ReactiveTransactionAutoConfiguration.class)
public class R2dbcExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(R2dbcExampleApplication.class, args);
	}
}
