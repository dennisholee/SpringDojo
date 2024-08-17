package io.forest.curator.adapter;

import java.util.Optional;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import io.forest.curator.port.LeaderElectionProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class LeaderElectionZookeeperProvider implements LeaderElectionProvider {

	@NonNull
	CuratorFramework curatorFramework;

	@NonNull
	LeaderSelectorListener listener;

	@NonNull
	String nodeId;
	
	LeaderSelector leaderSelector;

	@EventListener(ContextRefreshedEvent.class)
	public void bootstrap(ContextRefreshedEvent event) throws Exception {
		log.info("Registering to the leader selector service.");
		this.leaderSelector = new LeaderSelector(curatorFramework, "/io/forest/curator/leader/outbox", listener);
//		this.leaderSelector.autoRequeue(); // Puts this instance back in contention for leadership if relinquished.
		leaderSelector.start();
		log.info("Leader selector service started.");
	}

	@EventListener(ContextClosedEvent.class)
	public void shutdown(ContextClosedEvent event) {
		log.info("Stopping leader selector service.");
		Optional.ofNullable(leaderSelector)
				.ifPresent(LeaderSelector::close);
		log.info("Leader selector service halted.");
	}
}
