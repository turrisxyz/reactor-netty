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
package reactor.netty.tcp;

import java.util.Collections;
import java.util.Map;

import io.netty5.channel.ChannelOption;
import io.netty5.util.NetUtil;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.AddressUtils;

/**
 * Provides the actual {@link TcpClient} instance.
 *
 * @author Stephane Maldini
 * @author Violeta Georgieva
 */
final class TcpClientConnect extends TcpClient {

	final TcpClientConfig config;

	TcpClientConnect(ConnectionProvider provider) {
		this.config = new TcpClientConfig(
				provider,
				Map.of(ChannelOption.AUTO_READ, false, ChannelOption.RCVBUF_ALLOCATOR_USE_BUFFER, true),
				() -> AddressUtils.createUnresolved(NetUtil.LOCALHOST.getHostAddress(), DEFAULT_PORT));
	}

	TcpClientConnect(TcpClientConfig config) {
		this.config = config;
	}

	@Override
	public TcpClientConfig configuration() {
		return config;
	}

	@Override
	protected TcpClient duplicate() {
		return new TcpClientConnect(new TcpClientConfig(config));
	}

	/**
	 * The default port for reactor-netty servers. Defaults to 12012 but can be tuned via
	 * the {@code PORT} <b>environment variable</b>.
	 */
	static final int DEFAULT_PORT = System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) : 12012;
}
