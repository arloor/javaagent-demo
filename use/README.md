## 使用方式

### 静态attach

```shell
TARGET_DIR=/Users/ganghuanliu/IdeaProjects/arch/javaagent-demo/use/target
FAT_JAR=use-1.0-SNAPSHOT

cd $TARGET_DIR   
unzip -p  $FAT_JAR.jar "BOOT-INF/lib/agent-*.jar" > agent.jar   

if [ ! -f agent.jar ]; then
echo "依赖的 agent jar不存在"
  exit 0
fi

AGENT="agent.jar"
JAVA_AGENT="-Xbootclasspath/a:${AGENT} -javaagent:${AGENT}"

java $JAVA_AGENT -jar $FAT_JAR.jar
## macos的zsh下测试时请使用：
$(echo "java $JAVA_AGENT -jar $FAT_JAR.jar")
```

### 动态attach

```shell
java \
-Xbootclasspath/a:/Library/Java/JavaVirtualMachines/jdk1.8.0_251.jdk/Contents/Home/lib/tools.jar \
-jar use-1.0-SNAPSHOT.jar
```