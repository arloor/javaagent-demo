package com.arloor.agent.transformer;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;

/**
 * @Author albon
 * @Date 2018/7/9
 */
public interface ITraceTransformer {
    /**
     *
     * @return 转换的目标类
     */
    String targetClass();

    /**
     * 修改此类
     *
     * @param clazz
     * @throws NotFoundException
     * @throws CannotCompileException
     * @throws IOException
     */
    void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException, IOException;

}
