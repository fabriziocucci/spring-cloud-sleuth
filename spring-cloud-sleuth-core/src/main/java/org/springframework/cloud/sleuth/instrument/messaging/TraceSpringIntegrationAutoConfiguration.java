/*
 * Copyright 2013-2015 the original author or authors.
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

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} that registers a Sleuth version of the
 * {@link org.springframework.messaging.support.ChannelInterceptor}.
 *
 * @author Spencer Gibb
 * @since 1.0.0
 *
 * @see TraceChannelInterceptor
 */
@Configuration
@ConditionalOnClass(GlobalChannelInterceptor.class)
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter({ TraceAutoConfiguration.class,
		TraceSpanMessagingAutoConfiguration.class })
@ConditionalOnProperty(value = "spring.sleuth.integration.enabled", matchIfMissing = true)
@EnableConfigurationProperties(TraceKeys.class)
public class TraceSpringIntegrationAutoConfiguration {

	@Bean
	@GlobalChannelInterceptor(patterns = "${spring.sleuth.integration.patterns:*}")
	public TraceChannelInterceptor traceChannelInterceptor(BeanFactory beanFactory) {
		return new IntegrationTraceChannelInterceptor(beanFactory);
	}

	@Bean BeanPostProcessor tracingMessageChannelBPP(BeanFactory beanFactory) {
		return new TracingMessageChannelBPP(beanFactory);
	}
}

class TracingMessageChannelBPP implements BeanPostProcessor {

	private final BeanFactory beanFactory;
	private TraceChannelInterceptor interceptor;

	TracingMessageChannelBPP(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof ChannelInterceptorAware) {
			ChannelInterceptorAware interceptorAware = (ChannelInterceptorAware) bean;
			if (!hasTracedChannelInterceptor(interceptorAware.getChannelInterceptors())) {
				interceptorAware.addInterceptor(interceptor());
			}
		} else if (bean instanceof InterceptableChannel) {
			InterceptableChannel interceptorAware = (InterceptableChannel) bean;
			if (!hasTracedChannelInterceptor(interceptorAware.getInterceptors())) {
				interceptorAware.addInterceptor(interceptor());
			}
		}
		return bean;
	}

	private boolean hasTracedChannelInterceptor(List<ChannelInterceptor> interceptors) {
		for (ChannelInterceptor channelInterceptor : interceptors) {
			if (channelInterceptor instanceof TraceChannelInterceptor) {
				return true;
			}
		}
		return false;
	}

	private TraceChannelInterceptor interceptor() {
		if (this.interceptor == null) {
			this.interceptor = this.beanFactory.getBean(TraceChannelInterceptor.class);
		}
		return this.interceptor;
	}
}