/*
 * Copyright (c) 2017-2022 VMware, Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.netty.http.client;

import io.netty5.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import io.netty5.handler.codec.http.websocketx.WebSocketHandshakeException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableChannel;
import reactor.netty.http.server.HttpServer;
import reactor.netty.BaseHttpTest;
import reactor.netty.http.server.WebsocketServerSpec;
import reactor.test.StepVerifier;

import java.time.Duration;

/**
 * @author Violeta Georgieva
 */
class WebsocketClientOperationsTest extends BaseHttpTest {

	@Test
	void requestError() {
		failOnClientServerError(401, "", "");
	}

	@Test
	void serverError() {
		failOnClientServerError(500, "", "");
	}

	@Test
	void failedNegotiation() {
		failOnClientServerError(200, "Server-Protocol", "Client-Protocol");
	}

	private void failOnClientServerError(
			int serverStatus, String serverSubprotocol, String clientSubprotocol) {
		disposableServer =
				createServer()
				          .route(routes ->
				              routes.post("/login", (req, res) -> res.status(serverStatus).sendHeaders())
				                    .get("/ws", (req, res) -> {
				                        int token = Integer.parseInt(req.requestHeaders().get("Authorization"));
				                        if (token >= 400) {
				                            return res.status(token).send();
				                        }
				                        return res.sendWebsocket((i, o) -> o.sendString(Mono.just("test")),
				                                WebsocketServerSpec.builder().protocols(serverSubprotocol).build());
				                    }))
				          .bindNow();

		Flux<String> response =
				createClient(disposableServer.port())
				          .headersWhen(h -> login(disposableServer.port()).map(token -> h.set("Authorization", token)))
				          .websocket(WebsocketClientSpec.builder().protocols(clientSubprotocol).build())
				          .uri("/ws")
				          .handle((i, o) -> i.receive().asString())
				          .log()
				          .switchIfEmpty(Mono.error(new Exception()));

		StepVerifier.create(response)
		            .expectError(WebSocketHandshakeException.class)
		            .verify();
	}

	private Mono<String> login(int port) {
		return createClient(port)
		                 .post()
		                 .uri("/login")
		                 .responseSingle((res, buf) -> Mono.just(res.status().code() + ""));
	}

	@Test
	void testExceptionThrownWhenConnectionClosedBeforeHandshakeCompleted() {
		disposableServer =
				HttpServer.create()
				          .port(0)
				          .wiretap(true)
				          .handle((req, res) -> res.withConnection(DisposableChannel::dispose))
				          .bindNow();

		HttpClient.create()
		          .port(disposableServer.port())
		          .wiretap(true)
		          .websocket()
		          .uri("/")
		          .receive()
		          .as(StepVerifier::create)
		          .expectErrorMatches(t -> t instanceof WebSocketClientHandshakeException &&
		                  "Connection prematurely closed BEFORE opening handshake is complete.".equals(t.getMessage()))
		          .verify(Duration.ofSeconds(30));
	}
}