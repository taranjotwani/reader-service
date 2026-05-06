# AWS ECS EC2 Service with CodeDeploy: Command List

This document provides the AWS CLI commands to create an Amazon ECS service on the EC2 launch type using the `CODE_DEPLOY` deployment controller for blue/green deployments. The ECS service must use a target group associated with an Application Load Balancer, and for `awsvpc` network mode the target groups must use `ip` as the target type.[1][2][3]

## Assumptions

These commands use the following values from the current setup:

- Cluster name: `<CLUSTER_NAME>`
- Service name: `<SERVICE_NAME>`
- Task definition: `<TASK_DEFINITION>:<REVISION>`
- Container name: `<CONTAINER_NAME>`
- Container port: `<CONTAINER_PORT>`
- Security group: `<SECURITY_GROUP_ID>`
- Subnets: `<SUBNET_ID_1>`, `<SUBNET_ID_2>`, `<SUBNET_ID_3>`, `<SUBNET_ID_4>`
- Region: `<AWS_REGION>`

## 1. Get the VPC ID

The target groups must be created in the same VPC as the ECS tasks and load balancer.[3]

```bash
VPC_ID=$(aws ec2 describe-subnets \
  --subnet-ids <SUBNET_ID_1> \
  --query 'Subnets[0].VpcId' \
  --output text)

echo "$VPC_ID"
```

## 2. Create blue and green target groups

Because the task definition uses `awsvpc` network mode, the target groups must use `--target-type ip` rather than `instance`.[2][4]

```bash
BLUE_TG=$(aws elbv2 create-target-group \
  --name NewsReaderTG-blue \
  --protocol HTTP \
  --port 8080 \
  --target-type ip \
  --vpc-id "$VPC_ID" \
  --health-check-path /actuator/health \
  --health-check-port 8080 \
  --health-check-protocol HTTP \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text)

echo "$BLUE_TG"
```

```bash
GREEN_TG=$(aws elbv2 create-target-group \
  --name NewsReaderTG-green \
  --protocol HTTP \
  --port 8080 \
  --target-type ip \
  --vpc-id "$VPC_ID" \
  --health-check-path /actuator/health \
  --health-check-port 8080 \
  --health-check-protocol HTTP \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text)

echo "$GREEN_TG"
```

## 3. Create an Application Load Balancer

The target group used by the ECS service must already be associated with a load balancer listener before the service can be created.[5][3]

```bash
ALB_ARN=$(aws elbv2 create-load-balancer \
  --name news-reader-alb \
  --subnets <SUBNET_ID_1> <SUBNET_ID_2> \
  --security-groups <SECURITY_GROUP_ID> \
  --scheme internet-facing \
  --type application \
  --query 'LoadBalancers[0].LoadBalancerArn' \
  --output text)

echo "$ALB_ARN"
```

## 4. Create the production listener

This listener forwards production traffic to the blue target group initially.[6][3]

```bash
LISTENER_ARN=$(aws elbv2 create-listener \
  --load-balancer-arn "$ALB_ARN" \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=forward,TargetGroupArn="$BLUE_TG" \
  --query 'Listeners[0].ListenerArn' \
  --output text)

echo "$LISTENER_ARN"
```

## 5. Create the CodeDeploy service role

Amazon ECS blue/green deployments with CodeDeploy require a CodeDeploy service role. The AWS managed policy for this role is `service-role/AWSCodeDeployRoleForECS`.[7][8]

```bash
aws iam create-role \
  --role-name CodeDeployServiceRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": "codedeploy.amazonaws.com"
        },
        "Action": "sts:AssumeRole"
      }
    ]
  }'
```

```bash
aws iam attach-role-policy \
  --role-name CodeDeployServiceRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSCodeDeployRoleForECS
```

## 6. Create the ECS service with `CODE_DEPLOY`

The ECS service must be created with the deployment controller set to `CODE_DEPLOY` before it can be used in a CodeDeploy deployment group.[9][10][1]

```bash
aws ecs create-service \
  --service-name <SERVICE_NAME> \
  --cluster <CLUSTER_NAME> \
  --task-definition <TASK_DEFINITION>:<REVISION> \
  --deployment-controller type=CODE_DEPLOY \
  --desired-count 2 \
  --launch-type EC2 \
  --load-balancers "[{\"targetGroupArn\":\"$BLUE_TG\",\"containerName\":\"<CONTAINER_NAME>\",\"containerPort\":<CONTAINER_PORT>}]" \
  --network-configuration 'awsvpcConfiguration={subnets=[<SUBNET_ID_1>,<SUBNET_ID_2>,<SUBNET_ID_3>,<SUBNET_ID_4>],securityGroups=[<SECURITY_GROUP_ID>],assignPublicIp="DISABLED"}'
```

## 7. Create the CodeDeploy application

For ECS blue/green deployments, the CodeDeploy application must use the Amazon ECS compute platform.[11][12]

```bash
aws deploy create-application \
  --application-name NewsReaderApp \
  --compute-platform ECS
```

## 8. Create the CodeDeploy deployment group

The deployment group links the ECS cluster and service to the blue and green target groups and the production listener.[13][11][6]

```bash
aws deploy create-deployment-group \
  --application-name NewsReaderApp \
  --deployment-group-name NewsReaderDG \
  --service-role-arn arn:aws:iam::<AWS_ACCOUNT_ID>:role/CodeDeployServiceRole \
  --deployment-config-name CodeDeployDefault.ECSAllAtOnce \
  --deployment-style deploymentType=BLUE_GREEN,deploymentOption=WITH_TRAFFIC_CONTROL \
  --ecs-services clusterName=<CLUSTER_NAME>,serviceName=<SERVICE_NAME> \
  --load-balancer-info targetGroupPairInfoList='[{"targetGroups":[{"name":"NewsReaderTG-blue"},{"name":"NewsReaderTG-green"}],"prodTrafficRoute":{"listenerArns":["'"$LISTENER_ARN"'"]}}]' \
  --auto-rollback-configuration enabled=true,events=DEPLOYMENT_FAILURE
```

Replace `<AWS_ACCOUNT_ID>` with your AWS account ID before running the command.[11]

## 9. Verify the ECS service

These commands confirm that the ECS service was created and that the deployment controller is set correctly.[1]

```bash
aws ecs describe-services \
  --cluster <CLUSTER_NAME> \
  --services <SERVICE_NAME>
```

```bash
aws ecs describe-services \
  --cluster <CLUSTER_NAME> \
  --services <SERVICE_NAME> \
  --query 'services[0].deploymentController'
```

## 10. Verify target groups and listener

These checks confirm that the blue target group is associated with the load balancer and listener.[5][3]

```bash
aws elbv2 describe-target-groups \
  --target-group-arns "$BLUE_TG" "$GREEN_TG"
```

```bash
aws elbv2 describe-listeners \
  --load-balancer-arn "$ALB_ARN"
```

## Notes

- If the ECS service shows no running tasks, the cluster likely has no registered EC2 container instances yet.[14]
- For EC2 launch type, container instances must join the same ECS cluster and have enough CPU, memory, and ENI capacity for the tasks.[14][15]
- If the task definition uses `awsvpc`, the target group must always be `ip` type.[2]
- The production target group must be attached to an ALB listener before `aws ecs create-service` succeeds.[5][3]