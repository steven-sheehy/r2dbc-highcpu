package com.example.r2dbc.example;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.query.Criteria;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@ContextConfiguration(initializers = R2DbcExampleApplicationTests.TestDatabaseConfiguration.class)
@SpringBootTest
@Slf4j
class R2DbcExampleApplicationTests {

	@Resource
	private DatabaseClient databaseClient;

	@Test
	void test() throws Exception {
		Flux<TopicMessage> generator = Flux.range(1, 1_000)
				.map(i -> TopicMessage.builder().consensusTimestamp((long)i).build());

		databaseClient.insert()
				.into(TopicMessage.class)
				.using(generator)
				.fetch()
				.all()
				.blockLast();
		log.info("Row insertion complete");

		Criteria whereClause = Criteria.where("realm_num")
				.is(0)
				.and("topic_num")
				.is(0)
				.and("consensus_timestamp")
				.greaterThanOrEquals(0);

		databaseClient.select()
				.from(TopicMessage.class)
				.matching(whereClause)
				.orderBy(Sort.by("consensus_timestamp"))
				.fetch()
				.all()
				.as(t -> t.limitRequest(2))
				.doOnSubscribe(s -> log.warn("Executing query"))
				.doOnCancel(() -> log.warn("Cancelled query"))
				.doOnComplete(() -> log.warn("Completed query"))
				.doOnNext(t -> log.warn("onNext: {}", t))
		 		.map(TopicMessage::getConsensusTimestamp)
				.as(StepVerifier::create)
		 		.expectNext(1L, 2L)
		 		.verifyComplete();

		log.info("Retrieved first two rows of a long query");
		Thread.sleep(300_000);

		// Debug or use jstack and see busy spin in ReactorNettyClient$BackendMessageSubscriber.drainLoop()
	}

	@TestConfiguration
	static class TestDatabaseConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		private static PostgreSQLContainer postgresql;

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			try {
				postgresql = new PostgreSQLContainer<>("postgres:9.6-alpine");
				postgresql.start();

				TestPropertyValues
						.of("spring.r2dbc.name=" + postgresql.getDatabaseName())
						.and("spring.r2dbc.password=" + postgresql.getPassword())
						.and("spring.r2dbc.username=" + postgresql.getUsername())
						.and("spring.r2dbc.url=" + postgresql.getJdbcUrl()
								.replace("jdbc:", "r2dbc:"))
						.applyTo(applicationContext);
			} catch (Throwable ex) {
			}
		}

		@PreDestroy
		public void stop() {
			if (postgresql != null && postgresql.isRunning()) {
				postgresql.stop();
			}
		}
	}
}
