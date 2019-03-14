package com.ddxx.mq;
import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

public class RabbitMQGetConsumer {
    private final static String QUEUE_NAME = "priority_queue";
    private final static int PORT = 5672;
    private final static String IP_ADDRESS = "127.0.0.1";
    
    public static void main(String[] args) throws IOException, Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(IP_ADDRESS);
        factory.setPort(PORT);
        factory.setUsername("guest");
        factory.setPassword("guest");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        GetResponse getResponse = channel.basicGet(QUEUE_NAME, false);
        channel.basicAck(getResponse.getEnvelope().getDeliveryTag(), false);
        System.out.println(new String(getResponse.getBody(), "UTF-8"));
    }

}