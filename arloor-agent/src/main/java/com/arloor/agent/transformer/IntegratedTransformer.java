package com.arloor.agent.transformer;

import com.arloor.agent.util.ExceptionUtil;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class IntegratedTransformer implements ClassFileTransformer {
    private List<ITraceTransformer> transformletList = new ArrayList<ITraceTransformer>();

    public IntegratedTransformer() {
        transformletList.add(new ThreadPoolTransformer());
    }

    @Override
    public byte[] transform(ClassLoader loader, String classFile, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        try {
            // Lambda has no class file, no need to transform, just return.
            if (classFile == null) {
                return null;
            }

            final String className = toClassName(classFile);
            for (ITraceTransformer transformer : transformletList) {
                if (transformer.targetClass().equals(className)) {
                    if (classBeingRedefined == null) {
                        System.out.println(("Transforming class " + className + " @ classloading"));
                    } else {
                        System.out.println(("Transforming class " + className));
                    }

                    final CtClass clazz = getCtClass(classFileBuffer, loader);
                    transformer.doTransform(clazz);
                    return clazz.toBytecode();
                }
            }
        } catch (Throwable t) {
            final String msg = "Fail to transform class " + classFile + ", " + ExceptionUtil.getMessage(t);
            System.out.println(msg);
            throw new IllegalStateException(msg, t);
        }

        return null;
    }

    private String toClassName(final String classFile) {
        return classFile.replace('/', '.');
    }

    private CtClass getCtClass(byte[] classFileBuffer, ClassLoader classLoader) throws IOException {
        ClassPool classPool = new ClassPool(true);
        if (null != classLoader) {
            classPool.appendClassPath(new LoaderClassPath(classLoader));
        }

        CtClass clazz = classPool.makeClass(new ByteArrayInputStream(classFileBuffer), false);
        clazz.defrost();
        return clazz;
    }
}
