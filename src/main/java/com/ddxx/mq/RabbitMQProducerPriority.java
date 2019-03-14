package com.ddxx.mq;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 *优先级队列， 
 *顾名思义，具有高优先级的队列具有高的优先权，优先级高的消息具备优先被消费的特权。
 */
public class RabbitMQProducerPriority {
    private final static String PRIORITY_EXCHANGE_NAME = "priority_exchange";

    private final static String ROUTING_KEY = "routing_key";
    private final static String PRIORITY_QUEUE_NAME = "priority_queue";


    private final static int PORT = 5672;
    private final static String IP_ADDRESS = "127.0.0.1";

    public static void main(String[] args) throws IOException, Exception {
        // connection是socket连接的抽象，并且为我们管理协议版本协商（protocol version negotiation），
        // 认证（authentication ）等等事情。这里我们要连接的消息代理在本地，因此我们将host设为“localhost”。
        // 如果我们想连接其他机器上的代理，只需要将这里改为特定的主机名或IP地址。
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(IP_ADDRESS);
        factory.setPort(PORT); //默认端口号
        factory.setUsername("guest");//默认用户名
        factory.setPassword("guest");//默认密码
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        //创建一个type="direct" 、持久化的、非自动删除的交换器
        channel.exchangeDeclare(PRIORITY_EXCHANGE_NAME, BuiltinExchangeType.DIRECT.getType() ,true ,false ,null);
      
        //创建一个持久化、非排他的、非自动删除的队列
        Map<String, Object> argss = new HashMap<String , Object>();
        argss.put("x-max-priority" , 10);
        
        channel.queueDeclare(PRIORITY_QUEUE_NAME ,true, false, false, argss);

        //将交换器与队列通过路由键绑定
        channel.queueBind(PRIORITY_QUEUE_NAME ,PRIORITY_EXCHANGE_NAME ,ROUTING_KEY);

        String message = "Dead Message ";
        int i = 1;
    	AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
    	builder.priority(i);
    	AMQP.BasicProperties properties = builder.build();
        channel.basicPublish(PRIORITY_EXCHANGE_NAME ,ROUTING_KEY ,true ,properties ,message.concat(String.valueOf(i)).getBytes());
        System.out.println("[x] Sent '" + message + "'"+i);
        channel.close();
        connection.close();
    }
}