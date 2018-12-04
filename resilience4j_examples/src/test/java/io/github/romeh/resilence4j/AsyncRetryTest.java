package io.github.romeh.resilence4j;


import static io.github.romeh.resilence4j.AsyncUtils.awaitResult;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.BDDMockito.given;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.xml.ws.WebServiceException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;


@DisplayName("AsyncRetry Testing")
@ExtendWith(MockitoExtension.class)
public class AsyncRetryTest {
	@Mock
	private AsyncHelloWorldService helloWorldService;
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	@Test
	public void testShowRetryConfig() {
		// Given the HelloWorldService returns Hello world
		given(helloWorldService.returnHelloWorld())
				.willReturn(completedFuture("Hello world"));

		final AsyncRetry retryContext = AsyncRetry.of("retryConfig",
				// we set the response type to String
				RetryConfig.<String>custom()
						// max retry attempts
						.maxAttempts(3)
						// what are the ignore exception to no retry on,it is var args
						.ignoreExceptions(IllegalStateException.class)
						// what are the exceptions to try on , it is var args
						.retryExceptions(TimeoutException.class)
						// retry if the response contains world
						.retryOnResult(s -> s.contains("world"))
						// retry backoff strategy, IntervalFunction has many built in interface functions you can check it out
						.intervalFunction(IntervalFunction.ofExponentialBackoff())
						.build());

		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		String result = awaitResult(supplier);
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();
		Assertions.assertEquals(result, "Hello world");

	}

	@Test
	public void shouldNotRetry() throws InterruptedException, ExecutionException, TimeoutException {
		// Given the HelloWorldService returns Hello world
		given(helloWorldService.returnHelloWorld())
				.willReturn(completedFuture("Hello world"));
		// Create a Retry with default configuration
		AsyncRetry retryContext = AsyncRetry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		String result = awaitResult(supplier);
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		Assertions.assertEquals(result, "Hello world");
	}


	@Test
	public void shouldNotRetryWithThatResult() throws InterruptedException, ExecutionException, TimeoutException {
		// Given the HelloWorldService returns Hello world
		given(helloWorldService.returnHelloWorld())
				.willReturn(completedFuture("Hello world"));
		// Create a Retry with default configuration
		final RetryConfig retryConfig = RetryConfig.<String>custom().retryOnResult(s -> s.contains("NoRetry"))
				.maxAttempts(1)
				.build();
		AsyncRetry retryContext = AsyncRetry.of("id", retryConfig);
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		String result = awaitResult(supplier);
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		Assertions.assertEquals(result, "Hello world");
	}

	@Test
	public void shouldRetryInCaseOResultRetryMatchAtSyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStage(1, "Hello world");
	}

	@Test
	public void shouldRetryTowAttemptsInCaseOResultRetryMatchAtSyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStage(2, "Hello world");
	}

	@Test
	public void shouldRetryInCaseOfExceptionAtSyncStage() {
		// Given the HelloWorldService throws an exception
		given(helloWorldService.returnHelloWorld())
				.willThrow(new WebServiceException("BAM!"))
				.willReturn(completedFuture("Hello world"));

		// Create a Retry with default configuration
		AsyncRetry retryContext = AsyncRetry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		String result = awaitResult(supplier.get());

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
		Assertions.assertEquals(result, "Hello world");
	}

	@Test
	public void shouldRetryInCaseOfAnExceptionAtAsyncStage() {
		CompletableFuture<String> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new WebServiceException("BAM!"));

		// Given the HelloWorldService throws an exception
		given(helloWorldService.returnHelloWorld())
				.willReturn(failedFuture)
				.willReturn(completedFuture("Hello world"));

		// Create a Retry with default configuration
		AsyncRetry retryContext = AsyncRetry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		String result = awaitResult(supplier.get());

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
		Assertions.assertEquals(result, "Hello world");
	}

	@Test
	public void shouldCompleteFutureAfterOneAttemptInCaseOfExceptionAtSyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtSyncStage(1);
	}

	@Test
	public void shouldCompleteFutureAfterTwoAttemptsInCaseOfExceptionAtSyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtSyncStage(2);
	}

	@Test
	public void shouldCompleteFutureAfterThreeAttemptsInCaseOfExceptionAtSyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtSyncStage(3);
	}

	private void shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtSyncStage(int noOfAttempts) {
		// Given the HelloWorldService throws an exception
		given(helloWorldService.returnHelloWorld())
				.willThrow(new WebServiceException("BAM!"));

		// Create a Retry with default configuration
		AsyncRetry retryContext = AsyncRetry.of(
				"id",
				RetryConfig
						.custom()
						.maxAttempts(noOfAttempts)
						.build());
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

		// Then the helloWorldService should be invoked n + 1  times
		BDDMockito.then(helloWorldService).should(Mockito.times(noOfAttempts)).returnHelloWorld();
		Assertions.assertTrue(resultTry.isFailure());
		Assertions.assertTrue(resultTry.getCause().getCause() instanceof WebServiceException);

	}

	@Test
	public void shouldCompleteFutureAfterOneAttemptInCaseOfExceptionAtAsyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStage(1);
	}

	@Test
	public void shouldCompleteFutureAfterTwoAttemptsInCaseOfExceptionAtAsyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStage(2);
	}

	@Test
	public void shouldCompleteFutureAfterThreeAttemptsInCaseOfExceptionAtAsyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStage(3);
	}

	private void shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStage(int noOfAttempts) {
		CompletableFuture<String> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new WebServiceException("BAM!"));

		// Given the HelloWorldService throws an exception
		given(helloWorldService.returnHelloWorld())
				.willReturn(failedFuture);

		// Create a Retry with default configuration
		AsyncRetry retryContext = AsyncRetry.of(
				"id",
				RetryConfig
						.custom()
						.maxAttempts(noOfAttempts)
						.build());
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

		// Then the helloWorldService should be invoked n + 1 times
		BDDMockito.then(helloWorldService).should(Mockito.times(noOfAttempts)).returnHelloWorld();
		Assertions.assertTrue(resultTry.isFailure());

	}

	private void shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStage(int noOfAttempts,
	                                                                               String retryResponse) {

		// Given the HelloWorldService throws an exception
		given(helloWorldService.returnHelloWorld())
				.willReturn(completedFuture("Hello world"));

		// Create a Retry with default configuration
		AsyncRetry retryContext = AsyncRetry.of(
				"id",
				RetryConfig
						.<String>custom()
						.maxAttempts(noOfAttempts)
						.retryOnResult(s -> s.contains(retryResponse))
						.build());
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

		// Then the helloWorldService should be invoked n + 1 times
		BDDMockito.then(helloWorldService).should(Mockito.times(noOfAttempts)).returnHelloWorld();
		Assertions.assertTrue(resultTry.isSuccess());

	}

}
