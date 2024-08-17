package io.forest.curator.adapter;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.CancelLeadershipException;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;

import io.forest.curator.application.command.DispatchOutboxCommand;
import io.forest.curator.port.DispatchOutbox;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class LeaderElectionListener implements LeaderSelectorListener {
	
	@NonNull
	DispatchOutbox dispatchOutbox;

	@Override
	public void stateChanged(	CuratorFramework client,
								ConnectionState newState) {

		log.info("State changed [state={}]", newState.name());
		
		/*
		 * Applications must assume that they no longer have the leadership when they
		 * receive the events SUSPENDED or LOST. The recommended approach is to throw a
		 * CancelLeadershipException to interrupt the thread that is executing the take
		 * takeLeadership() method as we can not guarantee we are the only leader
		 * without an active Zookeeper connection.
		 * 
		 * Src: https://blog.scottlogic.com/2018/03/13/leadership-election-with-apache-curator.html
		 */
		switch (newState) {
		case SUSPENDED, LOST:
			throw new CancelLeadershipException();
		default:
		}

	}

	@Override
	public void takeLeadership(CuratorFramework client) throws Exception {
		log.info("Leadership acquired");
		
		DispatchOutboxCommand command = new DispatchOutboxCommand();
		
		log.info("Dispatching dispatch outbox command");
		dispatchOutbox.handleCommand(command );
	}

}
