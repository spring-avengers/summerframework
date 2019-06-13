
package com.bkjk.platform.rabbit.delay;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.bkjk.platform.rabbit.extend.RabbitTemplateExtend;

public class DelayQueueInterceptor implements MethodInterceptor {
    private final RabbitTemplate rabbitTemplate;

    public DelayQueueInterceptor(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public Object invoke(MethodInvocation arg) throws Throwable {
        Delay delayAnnotation = arg.getMethod().getAnnotation(Delay.class);
        if (delayAnnotation != null) {
            this.setDelay(delayAnnotation.queue(), delayAnnotation.interval());
            return arg.proceed();
        } else {
            return arg.proceed();
        }
    }

    private void setDelay(String queueName, Long interval) {
        if (rabbitTemplate instanceof RabbitTemplateExtend) {
            RabbitTemplateExtend rabbitTemplateWrap = (RabbitTemplateExtend)rabbitTemplate;
            rabbitTemplateWrap.setDelayQueue(queueName, interval);
        }
    }

}
