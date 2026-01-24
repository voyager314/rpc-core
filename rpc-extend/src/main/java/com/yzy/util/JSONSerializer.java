package com.yzy.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzy.model.RpcRequest;
import com.yzy.model.RpcResponse;

import java.io.IOException;

public class JSONSerializer implements Serializer {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public <T> byte[] serialize(T object) throws IOException {
        return mapper.writeValueAsBytes(object);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws IOException {
        T object = mapper.readValue(bytes, type);
        /*
        RpcRquest和RpcResponse的字段中含Object类型（Object数组）
        Jackson在反序列化该类型时会发生泛型擦除导致转为LinkedHashMap类型
        需要对转换结果中的Object数组特殊处理，若不是原始类型则再次序列化并根据
        参数类型数组进行正确反序列化
         */
        if (object instanceof RpcRequest) {
            return handleRpcRequest((RpcRequest) object, type);
        }else if (object instanceof RpcResponse) {
            return handleRpcResponse((RpcResponse) object, type);
        }
        return object;
    }

    /**
     * RpcRequest中含Object数组，jackson反序列化时会发生原始类型擦除被转为LinkedHashMap，需特殊处理
     * @param request
     * @param type
     * @return
     * @param <T>
     * @throws IOException
     */
    private <T> T handleRpcRequest(RpcRequest request, Class<T> type) throws IOException {
        Class<?>[] types = request.getParameterTypes();
        Object[] args = request.getArgs();
        for(int i = 0; i < types.length; i++) {
            Class<?> clazz = types[i];
            if(!clazz.isAssignableFrom(args[i].getClass())) {
                byte[] bytes = mapper.writeValueAsBytes(args[i]);
                args[i] = mapper.readValue(bytes, type);
            }
        }
        return type.cast(request);
    }

    /**
     * RpcResponse中含Object类型，jackson反序列化时会发生原始类型擦除被转为LinkedHashMap，需特殊处理
     * @param response
     * @param type
     * @return
     * @param <T>
     * @throws IOException
     */
    private <T> T handleRpcResponse(RpcResponse response, Class<T> type) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(response.getData());
        response.setData(mapper.readValue(bytes, response.getClazz()));
        return type.cast(response);
    }
}
