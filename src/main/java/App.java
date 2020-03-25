import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String[] args) {
        HashedWheelTimer wheelTimer = new HashedWheelTimer(1l, TimeUnit.SECONDS, 8);
        wheelTimer.newTimeout(new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                System.out.println("5s后执行");
            }
        }, 5, TimeUnit.SECONDS);
        wheelTimer.newTimeout(new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                System.out.println("13s后执行");
            }
        }, 13, TimeUnit.SECONDS);
    }

}
