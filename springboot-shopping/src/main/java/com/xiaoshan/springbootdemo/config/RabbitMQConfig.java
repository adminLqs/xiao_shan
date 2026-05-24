package com.xiaoshan.springbootdemo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /**
     * 配置 RabbitTemplate 使用 JSON 序列化
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }

    /**
     * 配置 RabbitListener 容器使用 JSON 序列化
     */
    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory factory = 
            new org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        return factory;
    }

    // ==================== 订单延迟队列 ====================

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable("order.delay.queue").build();
    }

    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange("order.delay.exchange");
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange())
                .with("order.delay");
    }

    // ==================== 套餐到期队列 ====================

    @Bean
    public Queue packageExpireQueue() {
        return QueueBuilder.durable("package.expire.queue").build();
    }

    @Bean
    public DirectExchange packageExpireExchange() {
        return new DirectExchange("package.expire.exchange");
    }

    @Bean
    public Binding packageExpireRemindBinding() {
        return BindingBuilder.bind(packageExpireQueue())
                .to(packageExpireExchange())
                .with("package.expire.remind");
    }

    @Bean
    public Binding packageExpireProcessBinding() {
        return BindingBuilder.bind(packageExpireQueue())
                .to(packageExpireExchange())
                .with("package.expire.process");
    }
}
