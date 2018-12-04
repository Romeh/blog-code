package io.github.romeh.resilence4j;

import java.util.concurrent.CompletionStage;

public interface AsyncHelloWorldService {
	CompletionStage<String> returnHelloWorld();
}
