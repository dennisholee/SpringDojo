package io.forest.curator.adapter;

import java.util.Optional;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.RetryNTimes;

import io.forest.curator.port.GUIDProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GUIDZookeeperProvider implements GUIDProvider {

	@NonNull
	CuratorFramework curatorFramework;
	

	public Optional<Long> generateGUID() throws Exception {

		int maxRetries = 3;
		int sleepMsBetweenRetries = 1000;

		RetryPolicy retryPolicy = new RetryNTimes(maxRetries, sleepMsBetweenRetries);

		DistributedAtomicLong atomicLong = new DistributedAtomicLong(
				curatorFramework,
				"/io/forest/curator/counters/CounterA",
				retryPolicy);

		AtomicValue<Long> atomicValue = atomicLong.increment();

		return atomicValue.succeeded() ? Optional.of(atomicValue.postValue()) : Optional.empty();
	}

}
