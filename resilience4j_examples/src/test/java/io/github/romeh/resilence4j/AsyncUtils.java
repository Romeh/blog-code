package io.github.romeh.resilence4j;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class AsyncUtils {


	private static final long DEFAULT_TIMEOUT_SECONDS = 5;

	public static <T> T awaitResult(CompletionStage<T> completionStage, long timeoutSeconds) {
		try {
			return completionStage.toCompletableFuture().get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException | TimeoutException e) {
			throw new AssertionError(e);
		} catch (ExecutionException e) {
			throw new RuntimeExecutionException(e.getCause());
		}
	}

	public static <T> T awaitResult(CompletionStage<T> completionStage) {
		return awaitResult(completionStage, DEFAULT_TIMEOUT_SECONDS);
	}

	public static <T> T awaitResult(Supplier<CompletionStage<T>> completionStageSupplier, long timeoutSeconds) {
		return awaitResult(completionStageSupplier.get(), timeoutSeconds);
	}

	public static <T> T awaitResult(Supplier<CompletionStage<T>> completionStageSupplier) {
		return awaitResult(completionStageSupplier, DEFAULT_TIMEOUT_SECONDS);
	}

	private static class RuntimeExecutionException extends RuntimeException {
		RuntimeExecutionException(Throwable cause) {
			super(cause);
		}
	}
}
