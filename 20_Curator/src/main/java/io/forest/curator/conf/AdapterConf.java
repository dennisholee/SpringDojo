package io.forest.curator.conf;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import io.forest.curator.adapter.GUIDZookeeperProvider;
import io.forest.curator.adapter.LeaderElectionListener;
import io.forest.curator.adapter.LeaderElectionZookeeperProvider;
import io.forest.curator.adapter.message.MessageGatewayAdapter;
import io.forest.curator.adapter.repository.OutboxSqlRepository;
import io.forest.curator.adapter.repository.jpa.OutboxJpaRepository;
import io.forest.curator.port.DispatchOutbox;
import io.forest.curator.port.GUIDProvider;
import io.forest.curator.port.LeaderElectionProvider;
import io.forest.curator.port.MessageGateway;
import io.forest.curator.port.OutboxRepository;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AdapterConf {

	@Value("{application.leaderElection.nodeId}")
	String leaderElectionNodeId;

	@Bean
	OutboxRepository taskRepository(@Autowired OutboxJpaRepository taskJpaRepository) {
		return new OutboxSqlRepository(taskJpaRepository);
	}

	@Bean
	MessageGateway messageGateway() {
		return new MessageGatewayAdapter();
	}

	@Bean
	GUIDProvider guidProvider(@Autowired CuratorFramework curatorFramework) {
		return new GUIDZookeeperProvider(curatorFramework);
	}

	@Bean
	LeaderElectionListener leaderElectionListener(@Autowired DispatchOutbox dispatchOutbox) {
		return new LeaderElectionListener(dispatchOutbox);
	}

	@Bean
	@DependsOn("leaderElectionListener")
	LeaderElectionProvider leaderElectionZookeeperProvider(	@Autowired CuratorFramework curatorFramework,
															@Autowired LeaderElectionListener leaderElectionListener) {
		log.info("Creating LeaderElectionZookeeperProvider with nodeId [{}]", this.leaderElectionNodeId);
		return new LeaderElectionZookeeperProvider(curatorFramework, leaderElectionListener, leaderElectionNodeId);
	}

}
