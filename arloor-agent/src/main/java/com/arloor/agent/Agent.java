package com.arloor.agent;

import com.arloor.agent.transformer.IntegratedTransformer;
import com.arloor.agent.util.ExceptionUtil;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Agent {
    private static volatile Instrumentation inst;

    private Agent() {
        throw new InstantiationError("Must not instantiate this class");
    }

    /**
     * 命令行启动
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("premain start");
        install(agentArgs, instrumentation, false);
        System.out.println("premain end");
    }

    /**
     * 类加载调用
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("agentmain start");
        install(agentArgs, instrumentation, true);
        System.out.println("agentmain end");
    }

    static synchronized void install(String agentArgs, Instrumentation instrumentation, boolean atRuntime) {
        if (inst == null) {
            inst = instrumentation;
            ClassFileTransformer transformer = new IntegratedTransformer();
            try {
                // 主动寻找agentJar中的TraceRunnable等类
                // 否则会报 compile error: no such class: 该jar中的类
                if (atRuntime) {
                    instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(new File(agentArgs)));
                }
            } catch (IOException e) {
                System.out.println(ExceptionUtil.getMessage(e));
            }
            instrumentation.addTransformer(transformer, true);
            if (atRuntime) {
                // Transformer触发的时机是类加载时，redefine时和retransform时
                // ForkJoinTask会在之后进行类加载，那是会触发transformer
                // 而ThreadPoolExecutor和ScheduledThreadPoolExecutor在之前已经类加载了
                // 所以需要手动retransform一下
                try {
                    ClassLoader.getSystemClassLoader().loadClass("java.util.concurrent.ThreadPoolExecutor");
                    instrumentation.retransformClasses(ThreadPoolExecutor.class);
                } catch (Exception e) {
                    System.out.println(ExceptionUtil.getMessage(e));
                }
            }
        }
    }

    public static Instrumentation instrumentation() {
        return inst;
    }

    /**
     * 试验性功能，仅jdk8可用
     * 使用时请增加 -Xbootclasspath/a:/Library/Java/JavaVirtualMachines/jdk1.8.0_251.jdk/Contents/Home/lib/tools.jar
     */
    public static void attachToCurrentJvm() {
        try {
            final String javaVersion = System.getProperty("java.version");
            if (javaVersion.startsWith("1.8.0")) {
                final URL location = Agent.class.getProtectionDomain().getCodeSource().getLocation();
                String agentJarPath = location.getPath();
                System.out.println("raw agentJarPaht=" + agentJarPath);
                if (agentJarPath.contains(".jar!/BOOT-INF")) {//表明agentJar在fatJar中，做解压缩
                    if (agentJarPath.startsWith("file:")) {
                        agentJarPath = agentJarPath.substring(5);
                        final String[] split = agentJarPath.split("!");
                        if (split.length >= 2) {
                            String currentJar = split[0];
                            String agentJar = split[1];
                            File parent = new File(currentJar).getParentFile();
                            unZipIt(currentJar, parent.getAbsolutePath());
                            agentJarPath = parent.getPath() + agentJar;
                        }
                    }
                }
                System.out.println("agentJarPath= " + agentJarPath);
                if (!new File(agentJarPath).exists()) {
                    throw new IOException("agent jar not exist!");
                }
                String name = ManagementFactory.getRuntimeMXBean().getName();
                String pid = name.split("@")[0];
                final VirtualMachine attach = VirtualMachine.attach(pid);
                attach.loadAgent(agentJarPath, agentJarPath);
                attach.detach();
            }
        } catch (Throwable e) {
            System.out.println(ExceptionUtil.getMessage(e));
        }
    }

    /**
     * 解压文件
     *
     * @param zipFilePath  解压文件路径
     * @param outputFolder 输出解压文件路径
     */
    public static void unZipIt(String zipFilePath, String outputFolder) {
        byte[] buffer = new byte[1024];

        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }
        try {
            //get the zip file content
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                System.out.println(fileName);
                if (fileName.startsWith("BOOT-INF/lib/arloor-agent-")) {
                    File newFile = new File(outputFolder + File.separator + fileName);
                    System.out.println("file unzip : " + newFile.getAbsoluteFile());
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            System.out.println(ExceptionUtil.getMessage(e));
        }
    }
}
