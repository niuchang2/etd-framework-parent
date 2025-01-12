package org.etd.framework.starter.mybaits.aspect;


import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.etd.framework.common.core.spring.SpringContextHelper;
import org.etd.framework.starter.mybaits.annotation.DataSnapshot;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class DataSnapshotAspect {


    @Pointcut("execution(* com.baomidou.mybatisplus.core.mapper.BaseMapper.insert(..)) || execution(* com.baomidou.mybatisplus.core.mapper.BaseMapper.updateById(..))")
    public void generateDataSnapshot() {

    }

    @Around("generateDataSnapshot()")
    public Object generateDataSnapshot(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = joinPoint.proceed();
        Integer flag = (Integer) proceed;
        if (flag == 0) {
            return proceed;
        }
        Object params = joinPoint.getArgs()[0];
        DataSnapshot dataSnapshot = params.getClass().getDeclaredAnnotation(DataSnapshot.class);
        if (dataSnapshot == null) {
            return proceed;
        }
        log.info("-----------------生成数据快照----------");
        Object oldSequence = ReflectUtil.getFieldValue(params, dataSnapshot.sequenceField());
        // 如果物理主键为空则不生成快照信息
        if (ObjectUtils.isEmpty(oldSequence)) {
            return proceed;
        }

        Object snapshotObj = copyProperties(oldSequence, params, dataSnapshot);
        insertDataSnapshot(snapshotObj);

        return proceed;
    }


    @SneakyThrows
    private Object copyProperties(Object oldSequence, Object params, DataSnapshot dataSnapshot) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(params.getClass());
        Class<?> aClass = Class.forName(tableInfo.getCurrentNamespace());
        Object mapper = SpringContextHelper.getBean(aClass);
        Method selectById = ReflectUtil.getMethodByName(mapper.getClass(), "selectById");
        Object oldEntity = selectById.invoke(mapper, oldSequence);


        Object newEntity = dataSnapshot.snapshotClass().newInstance();
        BeanUtils.copyProperties(oldEntity, newEntity);

        //将原始表的物理主键映射快照中
        ReflectUtil.setFieldValue(newEntity, dataSnapshot.originalSequenceField(), oldSequence);
        //修改快照表的主键ID
        ReflectUtil.setFieldValue(newEntity, dataSnapshot.sequenceField(), IdWorker.getId());
        return newEntity;
    }

    @SneakyThrows
    private void insertDataSnapshot(Object newEntity) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(newEntity.getClass());
        Class<?> aClass = Class.forName(tableInfo.getCurrentNamespace());
        Object mapper = SpringContextHelper.getBean(aClass);
        Method insert = ReflectUtil.getMethodByName(mapper.getClass(), "insert");
        insert.invoke(mapper, newEntity);
    }
}
