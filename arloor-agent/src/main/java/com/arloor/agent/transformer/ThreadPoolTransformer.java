package com.arloor.agent.transformer;

import javassist.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ThreadPoolTransformer implements ITraceTransformer {
    private static final String THREAD_POOL_CLASS_FILE = "java.util.concurrent.ThreadPoolExecutor";
    static final Set<String> updateMethodNames = new HashSet<String>();

    static {
        updateMethodNames.add("execute");
    }

    @Override
    public String targetClass() {
        return THREAD_POOL_CLASS_FILE;
    }

    @Override
    public synchronized void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException, IOException {
        for (CtMethod method : clazz.getDeclaredMethods()) {
            updateMethod(clazz, method);
        }
    }

    static void updateMethod(CtClass clazz, CtMethod method) throws NotFoundException, CannotCompileException {
        if (!updateMethodNames.contains(method.getName())) {
            return;
        }
        if (method.getDeclaringClass() != clazz) {
            return;
        }
        final int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers)) {
            return;
        }
        method.insertBefore("System.out.println(\"hello from my agent\");");
    }
}
