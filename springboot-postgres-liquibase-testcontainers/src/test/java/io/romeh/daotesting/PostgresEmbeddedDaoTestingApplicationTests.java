package io.romeh.daotesting;

import java.time.Duration;
import java.util.Random;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import io.romeh.daotesting.dao.CustomerRepository;
import io.romeh.daotesting.domain.Customer;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DbConfig.class})
@ActiveProfiles("DaoTest")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:dao/TestData.sql")
public class PostgresEmbeddedDaoTestingApplicationTests {

	@ClassRule
	public static PostgreSQLContainer postgreSQLContainer = (PostgreSQLContainer) new PostgreSQLContainer(
			"postgres:10.3")
			.withDatabaseName("test")
			.withUsername("user")
			.withPassword("pass").withStartupTimeout(Duration.ofSeconds(600));

	@Autowired
	private CustomerRepository customerRepository;

	@Test
	@Transactional
	public void contextLoads() {

		customerRepository.save(Customer.builder()
				.id(new Random().nextInt())
				.address("brussels")
				.name("TestName")
				.build());

		Assert.assertTrue(customerRepository.findCustomerByName("TestName") != null);
	}
}
