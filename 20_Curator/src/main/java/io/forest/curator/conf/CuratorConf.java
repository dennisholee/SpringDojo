package io.forest.curator.conf;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CuratorConf {

	@Value("${application.zk.host}")
	String zkHost;

	@Value("${application.zk.port}")
	String zkPort;

	@Bean
	public CuratorFramework curatorFramework() {
		String zkConnection = String.format("%s:%s", zkHost, zkPort);
		
		log.info("Zookeeper connection details [conn={}]", zkConnection);

		CuratorFramework client = CuratorFrameworkFactory.newClient(zkConnection, new ExponentialBackoffRetry(1000, 3));
		client.start();
		return client;
	}
}
