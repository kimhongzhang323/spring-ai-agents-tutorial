package com.masterclass.microservices.messaging.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class SqsSnsTool {

    private static final Logger log = LoggerFactory.getLogger(SqsSnsTool.class);

    private final SqsClient sqsClient;
    private final SnsClient snsClient;
    private final String sqsQueueUrl;
    private final String snsTopicArn;

    public SqsSnsTool(SqsClient sqsClient, SnsClient snsClient,
                      @Value("${aws.sqs.queue-url:http://localhost:4566/000000000000/agent-queue}") String sqsQueueUrl,
                      @Value("${aws.sns.topic-arn:arn:aws:sns:us-east-1:000000000000:agent-topic}") String snsTopicArn) {
        this.sqsClient = sqsClient;
        this.snsClient = snsClient;
        this.sqsQueueUrl = sqsQueueUrl;
        this.snsTopicArn = snsTopicArn;
    }

    @Tool(description = """
            Sends an agent task message to AWS SQS (Simple Queue Service) via LocalStack.
            SQS is the standard pull-based queue for serverless AWS architectures.
            Downstream consumers (AWS Lambda, ECS tasks) poll SQS at their own pace.
            Use this when triggering serverless agent workers or integrating with AWS Step Functions.
            Input: message body string.
            Returns: the SQS message ID.
            """)
    public String sendToSqs(String messageBody) {
        var response = sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(sqsQueueUrl)
                .messageBody(messageBody)
                .build());
        log.debug("SQS message sent: id={}", response.messageId());
        return "SQS message sent. MessageId: " + response.messageId();
    }

    @Tool(description = """
            Publishes an agent notification to AWS SNS (Simple Notification Service) topic.
            SNS is a fan-out service — it instantly delivers the message to all subscribed
            endpoints: SQS queues, Lambda functions, HTTP endpoints, email, SMS.
            Use this when one agent decision needs to trigger multiple downstream systems at once.
            Input: subject (short label), message (notification body).
            Returns: the SNS message ID.
            """)
    public String publishToSns(String subject, String message) {
        var response = snsClient.publish(PublishRequest.builder()
                .topicArn(snsTopicArn)
                .subject(subject)
                .message(message)
                .build());
        log.debug("SNS message published: id={}", response.messageId());
        return "SNS message published. MessageId: " + response.messageId();
    }
}
