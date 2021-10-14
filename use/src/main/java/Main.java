import com.arloor.agent.Agent;

public class Main {
    public static void main(String[] args) {
        Agent.attachToCurrentJvm();
        new Worker().work();
    }
}
