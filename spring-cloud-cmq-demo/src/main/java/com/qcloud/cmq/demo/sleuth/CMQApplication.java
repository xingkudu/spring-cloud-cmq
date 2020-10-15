package com.qcloud.cmq.demo.sleuth;

import brave.Span;
import brave.Tracing;
import brave.propagation.TraceContext;
import com.qcloud.cmq.Account;
import com.qcloud.cmq.Message;
import com.qcloud.cmq.Queue;
import com.qcloud.cmq.sleuth.instrument.TraceContextHodler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * @ClassName CMQApplication
 * @Description cmq spring 启动入口
 * @Author hugo
 * @Date 2020/10/14 下午8:51
 * @Version 1.0
 **/
@SpringBootApplication
public class CMQApplication {

    @Autowired
    private Account account;

    @Autowired
    private Tracing tracing;

    public void asyncReceiveMessage() {
        Thread thread = new Thread(() -> {
            Queue queue = account.getQueue("Queue1");
            try {
                //死循环发送消息
                while(true) {
                    Message message = queue.receiveMessage();
                    System.out.println("msgId:" + message.msgId + ", msgBody:" + message.msgBody);

                    //如果 后续操作需要和上游cmq调用链串起，则需要从上下文中恢复span。如果不需要串连，则略忽
                    TraceContext context = TraceContextHodler.remove();
                    if (context != null) {
                        Span span = tracing.tracer().joinSpan(context);
                        span.name("demo-todo").kind(Span.Kind.CLIENT);
                        span.tag("localComponent", "demo");
                        span.tag("remoteComponent", "ms");
                        span.tag("localInterface", "demo-method");
                        //todo something

                        span.tag("resultStatus", "success");
                        span.finish();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "receiveMessageThread");
        thread.start();
    }

    public void sendMessage() {
        Queue queue = account.getQueue("Queue1");
        try {
            //死循环发送消息
            while(true) {
                queue.send("test" + System.currentTimeMillis());
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext applicationContext = SpringApplication.run(CMQApplication.class, args);
        CMQApplication cmqApplication = applicationContext.getBean(CMQApplication.class);
        cmqApplication.asyncReceiveMessage();
        cmqApplication.sendMessage();
    }

}
