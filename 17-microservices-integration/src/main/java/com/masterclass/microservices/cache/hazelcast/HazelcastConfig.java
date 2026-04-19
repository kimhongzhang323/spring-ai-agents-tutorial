package com.masterclass.microservices.cache.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setInstanceName("agent-hz");
        config.addMapConfig(new MapConfig("agent-shared-state")
                .setTimeToLiveSeconds(600)
                .setMaxIdleSeconds(300));
        return Hazelcast.newHazelcastInstance(config);
    }
}
