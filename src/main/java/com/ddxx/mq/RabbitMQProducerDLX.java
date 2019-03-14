package com.ddxx.mq;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.ReturnListener;

/**
 * 消息变成死信一般是由于以下几种情况:
 * 1.消息被拒绝(Basic.Reject/Basic.Nack)，井且设置requeue 参数为false
 * 2.消息过期
 * 3.队列达到最大长度
 *
 */
public class RabbitMQProducerDLX {
    private final static String EXCHANGE_NAME = "normal_exchange";
    private final static String DLX_EXCHANGE_NAME = "dlx_exchange";

    private final static String ROUTING_KEY = "routing_key";
    private final static String DLX_QUEUE_NAME = "dlx_queue";
    private final static String QUEUE_NAME = "normal_queue";


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
        channel.exchangeDeclare(DLX_EXCHANGE_NAME, BuiltinExchangeType.DIRECT.getType() ,true ,false ,null);
      
        //创建一个type="direct" 、持久化的、非自动删除的交换器
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT.getType() ,true ,false ,null);

        //创建一个持久化、非排他的、非自动删除的队列
        Map<String, Object> argss = new HashMap<String , Object>();
        argss.put("x-dead-letter-exchange" , DLX_EXCHANGE_NAME);
        argss.put("x-message-ttl" , 6000);//第一种设置TTL的方式
        argss.put("x-dead-letter-routing-key" ,ROUTING_KEY);
        
        channel.queueDeclare(QUEUE_NAME, true, false, false, argss);
        channel.queueDeclare(DLX_QUEUE_NAME ,true, false, false, null);

        //将交换器与队列通过路由键绑定
        channel.queueBind(QUEUE_NAME ,EXCHANGE_NAME ,ROUTING_KEY);
        channel.queueBind(DLX_QUEUE_NAME ,DLX_EXCHANGE_NAME ,ROUTING_KEY);

        String message = "Dead Message "+ UUID.randomUUID();
        channel.basicPublish(EXCHANGE_NAME ,ROUTING_KEY ,true ,MessageProperties.PERSISTENT_TEXT_PLAIN ,message.getBytes());
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