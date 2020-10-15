package com.qcloud.cmq.sleuth.instrument;


import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.qcloud.cmq.CMQClientException;
import com.qcloud.cmq.CMQClientInterceptor;
import com.qcloud.cmq.entity.CmqConfig;
import com.qcloud.cmq.json.JSONArray;
import com.qcloud.cmq.json.JSONObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName TraceCMQClientInterceptor
 * @Description trace埋点cmq client
 * @Author hugo
 * @Date 2020/10/14 下午2:27
 * @Version 1.0
 **/
public class TraceCMQClientInterceptor implements CMQClientInterceptor {

    private static final Propagation.Setter<JSONObject, String> SETTER = (carrier, key, value) -> carrier.put(key, value);

    private static final Propagation.Getter<JSONObject, String> GETTER = (carrier, key) -> carrier.getString(key);

    private static final List<String> SendMessageActions = Arrays.asList("SendMessage", "BatchSendMessage", "PublishMessage", "BatchPublishMessage");
    private static final List<String> ReceiveMessageAction = Arrays.asList("ReceiveMessage", "BatchReceiveMessage");

    private final Tracing tracing;

    private final CmqConfig cmqConfig;

    private final TraceContext.Extractor<JSONObject> extractor;

    private final TraceContext.Injector<JSONObject> injector;

    private static ConcurrentHashMap<TraceContext, List<Span>> holder = new ConcurrentHashMap();

    public TraceCMQClientInterceptor(CmqConfig cmqConfig) {
        this(Tracing.current(), cmqConfig);
    }

    public TraceCMQClientInterceptor(Tracing tracing, CmqConfig cmqConfig) {
        this.tracing = tracing;
        this.cmqConfig = cmqConfig;
        this.injector = tracing.propagation().injector(SETTER);
        this.extractor = tracing.propagation().extractor(GETTER);
    }

    @Override
    public String intercept(String action, Map<String, String> params, Chain chain) throws Exception{
        Span span = this.tracing.tracer().nextSpan().name(action);
        span.kind(Span.Kind.CLIENT);
        span.remoteServiceName("cmq");
        String[] ipAndPort = getIpAndPort();
        span.remoteIpAndPort(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
        span.tag("localComponent", "ms");
        span.tag("remoteComponent", "cmq");
        span.tag("localInterface", action);
        String remoteInterface = params.getOrDefault("queueName", params.getOrDefault("topicName", "unknow"));
        span.tag("remoteInterface", remoteInterface);
        boolean hasExecption = false;
        String result;
        try (Tracer.SpanInScope ws = this.tracing.tracer().withSpanInScope(span.start())) {
            //如果是SendMessage/publishMessage，则将上下文放置报文中传输至consumer端
            if (SendMessageActions.contains(action)) {
                JSONObject spanInject = new JSONObject();
                this.injector.inject(span.context(), spanInject);
                params.keySet().stream().filter(key -> key.startsWith("msgBody")).forEach(key -> {
                    params.put(key, spanInject.put("cmqbody", params.get(key)).toString());
                });
            }
            result = chain.call(action, params);
        }  catch (Exception e) {
            hasExecption = true;
            String exceptionMessage = this.getExceptionMessage(e);
            if (exceptionMessage != null) {
                span.tag("error", exceptionMessage);
            }
            throw e;
        } finally {
            if (!hasExecption) {
                span.tag("resultStatus", "success");
            } else {
                span.tag("resultStatus", "error");
            }
            span.finish();
        }
        //如果是ReceiveMessage,则从报文中恢复上下文
        if (ReceiveMessageAction.contains(action)) {
            List<JSONObject> extList = new ArrayList<>();
            JSONObject jsonObject = new JSONObject(result);
            if (action.equals("ReceiveMessage")) {
                String context = jsonObject.getString("msgBody");
                JSONObject jsonContext = new JSONObject(context);
                //获取原报文,并覆盖
                jsonObject.put("msgBody", jsonContext.remove("cmqbody"));
                extList.add(jsonContext);
            } else {
                JSONArray jsonArray = jsonObject.getJSONArray("msgInfoList");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String context = obj.getString("msgBody");
                    JSONObject jsonContext = new JSONObject(context);
                    //获取原报文,并覆盖
                    obj.put("msgBody", jsonContext.remove("cmqbody"));
                    extList.add(jsonContext);
                }
            }
            result = jsonObject.toString();

            TraceContextHodler.set(this.extractSpan(extList, action, remoteInterface));
//            if (traceContext != null) {
//                Span consumerSpan = this.tracing.tracer().joinSpan(traceContext);
//                this.setConsumerSpan(consumerSpan, action, remoteInterface);
//                consumerSpan.start();
//            }
        }
        return result;
    }

    private TraceContext extractSpan(List<JSONObject> extList, String action, String remoteInterface) {

        TraceContextOrSamplingFlags extract = null;
        TraceContext traceContext = null;

        for (JSONObject extractContext : extList) {
            extract = this.extractor.extract(extractContext);
            if (extract != null && extract.context() != null) {
                Span serverSpan = this.tracing.tracer().joinSpan(extract.context());
                serverSpan.start();
                this.setCMQServerSpan(serverSpan, action, remoteInterface);
                serverSpan.finish();
                traceContext = serverSpan.context();
            }
        }

        return traceContext;
    }

    private void setCMQServerSpan(Span span, String action, String remoteInterface) {
        span.name(action).kind(Span.Kind.SERVER);
        span.tag("localComponent", "cmq");
        span.tag("remoteComponent", "ms");
        span.tag("localInterface", remoteInterface);
        span.tag("resultStatus", "success");
    }
//
//
//    private void setConsumerSpan(Span span, String action, String remoteInterface) {
//        span.name("receiveMessage-biz-thread").kind(Span.Kind.CLIENT).tag("action", action);
//        span.remoteServiceName("cmq");
//        span.tag("localComponent", "ms");
//        span.tag("remoteInterface", remoteInterface);
//    }

    private String[] getIpAndPort() {
        String host = "";
        if (cmqConfig.getEndpoint().startsWith("https")) {
            host = cmqConfig.getEndpoint().substring(8);
        } else {
            host = cmqConfig.getEndpoint().substring(7);
        }
        String[] result = host.split(":");
        if (result.length != 2) {
            throw new CMQClientException("Invalid parameter:endpoint: " + cmqConfig.getEndpoint());
        }
        return result;
    }

    private String getExceptionMessage(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }

}
