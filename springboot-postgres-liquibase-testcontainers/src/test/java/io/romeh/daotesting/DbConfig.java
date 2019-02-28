package io.romeh.daotesting;

import static io.romeh.daotesting.PostgresEmbeddedDaoTestingApplicationTests.postgreSQLContainer;
import static java.lang.String.format;

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;

import io.romeh.daotesting.dao.CustomerRepository;
import liquibase.integration.spring.SpringLiquibase;

/**
 * @author romeh
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackageClasses = {CustomerRepository.class})
@Profile("DaoTest")
public class DbConfig {

	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName("org.postgresql.Driver");
		ds.setUrl(format("jdbc:postgresql://%s:%s/%s", postgreSQLContainer.getContainerIpAddress(),
				postgreSQLContainer.getMappedPort(
						PostgreSQLContainer.POSTGRESQL_PORT), postgreSQLContainer.getDatabaseName()));
		ds.setUsername(postgreSQLContainer.getUsername());
		ds.setPassword(postgreSQLContainer.getPassword());
		ds.setSchema(postgreSQLContainer.getDatabaseName());
		return ds;
	}


	/**
	 * @param dataSource the db data source
	 * @return the local entity manager factory bean
	 */
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

		LocalContainerEntityManagerFactoryBean lcemfb
				= new LocalContainerEntityManagerFactoryBean();
		lcemfb.setDataSource(dataSource);
		// set the packages to scan , it can be useful if you have big project and you just need to local partial entities for testing
		lcemfb.setPackagesToScan("io.romeh.daotesting.domain", "io.romeh.aotesting.dao");
		HibernateJpaVendorAdapter va = new HibernateJpaVendorAdapter();
		lcemfb.setJpaVendorAdapter(va);
		lcemfb.setJpaProperties(getHibernateProperties());
		lcemfb.afterPropertiesSet();
		return lcemfb;

	}

	/**
	 * @param localContainerEntityManagerFactoryBean
	 * @return the JPA transaction manager
	 */
	@Bean
	public JpaTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();

		transactionManager.setEntityManagerFactory(localContainerEntityManagerFactoryBean.getObject());

		return transactionManager;
	}

	@Bean
	public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
		return new PersistenceExceptionTranslationPostProcessor();
	}

	@Bean
	public SpringLiquibase springLiquibase(DataSource dataSource) throws SQLException {
		tryToCreateSchema(dataSource);
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setDropFirst(true);
		liquibase.setDataSource(dataSource);
		liquibase.setDefaultSchema("test");
		//liquibase.setIgnoreClasspathPrefix(false);
		liquibase.setChangeLog("classpath:/db/changelog/changelog-master.xml");
		return liquibase;
	}


	/**
	 * @return the hibernate properties
	 */
	private Properties getHibernateProperties() {
		Properties ps = new Properties();
		ps.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
		ps.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect");
		ps.put("hibernate.hbm2ddl.auto", "none");
		ps.put("hibernate.connection.characterEncoding", "UTF-8");
		ps.put("hibernate.connection.charSet", "UTF-8");

		ps.put(AvailableSettings.FORMAT_SQL, "true");
		ps.put(AvailableSettings.SHOW_SQL, "true");
		return ps;

	}

	private void tryToCreateSchema(DataSource dataSource) throws SQLException {
		String CREATE_SCHEMA_QUERY = "CREATE SCHEMA IF NOT EXISTS test";
		dataSource.getConnection().createStatement().execute(CREATE_SCHEMA_QUERY);
	}

}
