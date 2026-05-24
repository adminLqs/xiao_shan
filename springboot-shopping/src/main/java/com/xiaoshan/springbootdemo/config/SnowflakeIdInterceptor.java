package com.xiaoshan.springbootdemo.config;

import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * MyBatis 拦截器 - 自动为实体类的 id 字段生成雪花算法 ID
 * 拦截所有 INSERT 操作，如果实体类的 id 字段为 null，则自动生成雪花 ID
 */
@Component
@RequiredArgsConstructor
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SnowflakeIdInterceptor implements Interceptor {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取执行的方法参数
        Object[] args = invocation.getArgs();
        if (args == null || args.length < 2) {
            return invocation.proceed();
        }

        Object parameter = args[1];
        if (parameter == null) {
            return invocation.proceed();
        }

        // 获取 MappedStatement
        MappedStatement ms = (MappedStatement) args[0];

        // 只处理 INSERT 操作，且必须有主键配置
        if (ms.getSqlCommandType() != SqlCommandType.INSERT) {
            return invocation.proceed();
        }

        // 获取主键属性配置（批量插入时可能为 null）
        String[] keyProperties = ms.getKeyProperties();
        if (keyProperties == null || keyProperties.length == 0) {
            return invocation.proceed();
        }

        // 获取实体对象
        MetaObject metaObject = SystemMetaObject.forObject(parameter);
        String idProperty = keyProperties[0];

        if (idProperty != null && !idProperty.isEmpty()) {
            Object idValue = metaObject.getValue(idProperty);
            if (idValue == null) {
                // 生成雪花 ID
                long snowflakeId = snowflakeIdGenerator.nextId();
                metaObject.setValue(idProperty, snowflakeId);
            }
        }

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 不需要额外配置
    }
}
