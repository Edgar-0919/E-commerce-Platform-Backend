#!/bin/bash

# Create necessary directories with proper permissions
mkdir -p /home/rocketmq/store/commitlog
mkdir -p /home/rocketmq/store/consumequeue
mkdir -p /home/rocketmq/store/index
mkdir -p /home/rocketmq/store/schedule
mkdir -p /home/rocketmq/store/config

# Set permissions
chown -R rocketmq:rocketmq /home/rocketmq/store

# Create broker config file
cat > /home/rocketmq/store/config/broker.properties << EOF
brokerClusterName = DefaultCluster
brokerName = broker-a
brokerId = 0
deleteWhen = 04
fileReservedTime = 48
brokerRole = ASYNC_MASTER
flushDiskType = ASYNC_FLUSH
storePathRootDir = /home/rocketmq/store
storePathCommitLog = /home/rocketmq/store/commitlog
storePathConsumeQueue = /home/rocketmq/store/consumequeue
storePathIndex = /home/rocketmq/store/index
scheduleMessageService.schedulePath = /home/rocketmq/store/schedule
namesrvAddr = rocketmq-namesrv:9876
autoCreateTopicEnable = true
EOF

# Start broker using full path
exec /home/rocketmq/rocketmq-5.2.0/bin/mqbroker -n rocketmq-namesrv:9876 -c /home/rocketmq/store/config/broker.properties