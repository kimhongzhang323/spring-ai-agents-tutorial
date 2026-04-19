#!/bin/bash
# LocalStack init script — creates AWS resources on startup
echo ">>> LocalStack: creating SQS queue..."
awslocal sqs create-queue --queue-name agent-queue

echo ">>> LocalStack: creating SNS topic..."
awslocal sns create-topic --name agent-topic

echo ">>> LocalStack: subscribing SQS to SNS..."
QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/agent-queue \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' --output text)

TOPIC_ARN=$(awslocal sns list-topics --query 'Topics[0].TopicArn' --output text)

awslocal sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$QUEUE_ARN"

echo ">>> LocalStack: creating DynamoDB table..."
awslocal dynamodb create-table \
  --table-name AgentSessions \
  --attribute-definitions AttributeName=userId,AttributeType=S \
  --key-schema AttributeName=userId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

echo ">>> LocalStack: setup complete."
