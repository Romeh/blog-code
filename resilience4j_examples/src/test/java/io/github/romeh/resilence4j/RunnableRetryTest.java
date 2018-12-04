package io.github.romeh.resilence4j;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import javax.xml.ws.WebServiceException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.CheckedRunnable;
import io.vavr.Predicates;
import io.vavr.control.Try;

/**
 * @author romeh
 */
@DisplayName("RunnableRetry Test ")
@ExtendWith(MockitoExtension.class)
public class RunnableRetryTest {
	@Mock
	private HelloWorldService helloWorldService;

	@Test
	public void shouldNotRetry() {
		// Create a Retry with default configuration
		Retry retryContext = Retry.ofDefaults("id");

		// Decorate the invocation of the HelloWorldService
		Runnable runnable = Retry.decorateRunnable(retryContext, helloWorldService::sayHelloWorld);

		// When
		runnable.run();
		// Then the helloWorldService should be invoked 1 time
		then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
	}

	@Test
	public void testDecorateRunnable() {
		// Given the HelloWorldService throws an exception
		willThrow(new WebServiceException("BAM!")).given(helloWorldService).sayHelloWorld();

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Runnable runnable = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);

		// When
		Try<Void> result = Try.run(runnable::run);

		// Then the helloWorldService should be invoked 3 times
		then(helloWorldService).should(Mockito.times(3)).sayHelloWorld();
		// and the result should be a failure
		Assertions.assertTrue(result.isFailure());
		// and the returned exception should be of type RuntimeException
		Assertions.assertTrue(result.failed().get() instanceof WebServiceException);
	}

	@Test
	public void testExecuteRunnable() {
		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		retry.executeRunnable(helloWorldService::sayHelloWorld);

		// Then the helloWorldService should be invoked 1 time
		then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
	}

	@Test
	public void shouldReturnAfterThreeAttempts() {
		// Given the HelloWorldService throws an exception
		willThrow(new WebServiceException("BAM!")).given(helloWorldService).sayHelloWorld();

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

		// When
		Try<Void> result = Try.run(retryableRunnable);

		// Then the helloWorldService should be invoked 3 times
		then(helloWorldService).should(Mockito.times(3)).sayHelloWorld();
		// and the result should be a failure
		Assertions.assertTrue(result.isFailure());
		// and the returned exception should be of type RuntimeException
		Assertions.assertTrue(result.failed().get() instanceof WebServiceException);

	}

	@Test
	public void shouldReturnAfterOneAttempt() {
		// Given the HelloWorldService throws an exception
		willThrow(new WebServiceException("BAM!")).given(helloWorldService).sayHelloWorld();

		// Create a Retry with default configuration
		RetryConfig config = RetryConfig.custom().maxAttempts(1).build();
		Retry retry = Retry.of("id", config);
		// Decorate the invocation of the HelloWorldService
		CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

		// When
		Try<Void> result = Try.run(retryableRunnable);

		// Then the helloWorldService should be invoked 1 time
		then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
		// and the result should be a failure
		Assertions.assertTrue(result.isFailure());
		// and the returned exception should be of type RuntimeException
		Assertions.assertTrue(result.failed().get() instanceof WebServiceException);
	}

	@Test
	public void shouldReturnAfterOneAttemptAndIgnoreException() {
		// Given the HelloWorldService throws an exception
		willThrow(new WebServiceException("BAM!")).given(helloWorldService).sayHelloWorld();

		// Create a Retry with default configuration
		RetryConfig config = RetryConfig.custom()
				.retryOnException(throwable -> Match(throwable).of(
						Case($(Predicates.instanceOf(WebServiceException.class)), false),
						Case($(), true)))
				.build();
		Retry retry = Retry.of("id", config);

		// Decorate the invocation of the HelloWorldService
		CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

		// When
		Try<Void> result = Try.run(retryableRunnable);

		// Then the helloWorldService should be invoked only once, because the exception should be rethrown immediately.
		then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
		// and the result should be a failure
		Assertions.assertTrue(result.isFailure());
		// and the returned exception should be of type RuntimeException
		Assertions.assertTrue(result.failed().get() instanceof WebServiceException);
	}


}
