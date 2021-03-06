package com.darryl.activiti.redis;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * @Auther: Darryl
 * @Description: 基于redis的延时队列
 * @Date: created in 2020/3/12 19:51
 */

public class DelayListDemo {

    private static final String host="127.0.0.1";
    private static final int port=6379;
    private static JedisPool jedisPool = new JedisPool(host, port);

    private static Jedis getClient() {
        return jedisPool.getResource();
    }

    // 生产5条消息，分别5秒后再消费
    private void producer() {
        for (int i=0; i<5; i++) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 5+i);
            DelayListDemo.getClient().zadd("MSG",
                    calendar.getTimeInMillis()/1000, StringUtils.join("000",i));
            System.out.println("生产消息" + StringUtils.join("000",i)
                    + " 的时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            System.out.println((5+i) + "秒后执行。");
        }
    }

    // 消费者
    private void consumer() {
        Jedis client = DelayListDemo.getClient();
        while(true) {
            Set<Tuple> msgs = client.zrangeWithScores("MSG", 0, 0);
            if (CollectionUtils.isEmpty(msgs)) {
                System.out.println("无消息");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            Tuple msg = (Tuple) msgs.toArray()[0];
            double score = msg.getScore();
            Calendar calendar = Calendar.getInstance();
            long nowTime = calendar.getTimeInMillis()/1000;
            if (nowTime > score) {
                Long res = client.zrem("MSG", msg.getElement());
                if (res > 0) {
                    System.out.println("获取消息内容：" + msg.getElement() + " 的时间："
                        + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                }
            }
        }
    }

    public static void main(String[] args) {
        DelayListDemo demo = new DelayListDemo();
        demo.producer();
        demo.consumer();
    }

}
