/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.messaging;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.binding.MessageChannelConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.InterceptableChannel;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} that registers a Sleuth version of the
 * {@link MessageChannelConfigurer}.
 *
 * @author Marcin Grzejszczak
 * @since 1.3.3
 *
 * @see TraceChannelInterceptor
 * @see MessageChannelConfigurer
 */
@Configuration
@ConditionalOnClass(MessageChannelConfigurer.class)
@ConditionalOnBean(TraceChannelInterceptor.class)
@AutoConfigureAfter(TraceSpringIntegrationAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.sleuth.stream.enabled", matchIfMissing = true)
public class TraceSpringStreamAutoConfiguration {

	@Bean
	TracingMessageChannelConfigurer tracingMessageChannelConfigurer(TraceChannelInterceptor interceptor) {
		return new TracingMessageChannelConfigurer(interceptor);
	}

}

class TracingMessageChannelConfigurer implements MessageChannelConfigurer {

	private final TraceChannelInterceptor interceptor;

	TracingMessageChannelConfigurer(TraceChannelInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override public void configureInputChannel(MessageChannel messageChannel,
			String channelName) {
		addInterceptor(messageChannel);
	}

	@Override public void configureOutputChannel(MessageChannel messageChannel,
			String channelName) {
		addInterceptor(messageChannel);
	}

	private void addInterceptor(MessageChannel messageChannel) {
		if (messageChannel instanceof ChannelInterceptorAware) {
			ChannelInterceptorAware interceptorAware = (ChannelInterceptorAware) messageChannel;
			interceptorAware.addInterceptor(this.interceptor);
		} else if (messageChannel instanceof InterceptableChannel) {
			InterceptableChannel interceptorAware = (InterceptableChannel) messageChannel;
			interceptorAware.addInterceptor(this.interceptor);
		}
	}
}