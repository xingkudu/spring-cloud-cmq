package com.qcloud.cmq.sleuth.instrument;

import brave.propagation.TraceContext;

/**
 * @ClassName TraceContextHodler
 * @Description TRACE CONTEXT
 * @Author hugo
 * @Date 2020/10/14 下午7:56
 * @Version 1.0
 **/
public class TraceContextHodler {

    private static ThreadLocal<TraceContext> CONTEXTS = new InheritableThreadLocal();

    public static void set(TraceContext context) {
        CONTEXTS.set(context);
    }

    public static TraceContext remove() {
        TraceContext context = CONTEXTS.get();
        CONTEXTS.remove();
        return context;
    }

}
