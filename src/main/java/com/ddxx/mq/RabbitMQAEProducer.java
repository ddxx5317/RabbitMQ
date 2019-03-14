package com.ddxx.mq;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;

/**
 * 备用交换机-替换mandatory
 *
 */
public class RabbitMQAEProducer {
    private final static String EXCHANGE_NAME = "normal_exchange_name";
    private final static String ROUTING_KEY = "normal_routing_key";
    private final static String QUEUE_NAME = "normal_queue_name";
    
    private final static String AE_EXCHANGE_NAME = "ae_exchange_name";
    private final static String AE_QUEUE_NAME = "ae_queue_name";
    private final static String AE_ROUTING_KEY = "ae_routing_key";


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
        Map<String, Object> param = new HashMap<String , Object>();
        param.put("alternate-exchange", AE_EXCHANGE_NAME);
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT.getType() ,true ,false ,param);
        
        //创建一个持久化、非排他的、非自动删除的队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        
        //将交换器与队列通过路由键绑定
        channel.queueBind(QUEUE_NAME ,EXCHANGE_NAME ,ROUTING_KEY);

        String message = "Hello world";
        channel.basicPublish(EXCHANGE_NAME ,ROUTING_KEY ,true ,MessageProperties.PERSISTENT_TEXT_PLAIN ,message.getBytes());
        
        //如果备份交换器和mandatory参数一起使用,那么mandatory参数无效。
        channel.exchangeDeclare(AE_EXCHANGE_NAME ,BuiltinExchangeType.FANOUT.getType() ,true ,false ,null);
        channel.queueDeclare(AE_QUEUE_NAME,true,false,false,null);
        channel.queueBind(AE_QUEUE_NAME ,AE_EXCHANGE_NAME,"");
        channel.basicPublish(EXCHANGE_NAME ,AE_ROUTING_KEY ,MessageProperties.PERSISTENT_TEXT_PLAIN ,message.getBytes());

        /**
         * 当mandatory参数设为true时，交换器无法根据自身的类型和路由键找到一个符合条件的队列，
         * 那么RabbitMQ会调用Basic.Return命令将消息返回给生产者
         */
        
        /**
         * 当imrnediate参数设为true时，
         * 如果交换器在将消息路由到队列时发现队列上并不存在任何消费者，那么这条消息将不会存入队列中。
         * 当与路由键匹配的所有队列都没有消费者时，该消息会通过Basic.Return返回至生产者。
         */
        channel.addReturnListener(new ReturnListener() {
			@Override
			public void handleReturn(int replyCode, String replyText, String exchange, 
					String routingKey, BasicProperties basicProperties, byte[] body)throws IOException {
				String message = new String(body);
				System.out.println("Basic.Return返回的结果是: "+message );				
			}
		});
        System.out.println("[x] Sent '" + message + "'");
        channel.close();
        connection.close();
    }
}