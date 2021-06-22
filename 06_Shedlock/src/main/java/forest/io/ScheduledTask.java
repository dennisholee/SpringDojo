package forest.io;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class ScheduledTask {

	@Scheduled(cron = "*/5 * * * * *")
	@SchedulerLock(name = "ConsoleDump")
	public void doTask() {

		System.out.println("Running ShedLock task");
		LockAssert.assertLocked();
	}

}
