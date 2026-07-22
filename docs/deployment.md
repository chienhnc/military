# Deployment

## Platform
AWS Lambda via SAM/CloudFormation (`template.yaml`), fronted by API Gateway HTTP API.

## Stack
- Name: `military-manager`
- Region: `ap-southeast-2`
- Endpoint: `https://xgour62062.execute-api.ap-southeast-2.amazonaws.com/`
- Managed artifact bucket: `aws-sam-cli-managed-default-samclisourcebucket-zrgzrsexyryj`
- App S3 bucket (images): `military-images-343540011407-ap-southeast-2`

## Prerequisites
- JDK 17 required for build (JDK 25 default silently breaks Lombok annotation processing).
  `export JAVA_HOME="/c/Program Files/Java/jdk-17.0.18"` (Windows Git Bash path).
- AWS CLI v2 configured with credentials that can update the `military-manager` stack.

## Deploy Command
```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17.0.18"
export PATH="$JAVA_HOME/bin:$PATH"

mvn -q clean package -DskipTests

aws cloudformation package \
  --template-file template.yaml \
  --s3-bucket aws-sam-cli-managed-default-samclisourcebucket-zrgzrsexyryj \
  --output-template-file target/packaged-template.yaml \
  --region ap-southeast-2

aws cloudformation create-change-set \
  --stack-name military-manager \
  --change-set-name deploy-$(date +%Y%m%d%H%M%S) \
  --template-body file://target/packaged-template.yaml \
  --capabilities CAPABILITY_IAM \
  --parameters \
    ParameterKey=JwtSecret,UsePreviousValue=true \
    ParameterKey=S3Bucket,UsePreviousValue=true \
    ParameterKey=S3Prefix,UsePreviousValue=true \
    ParameterKey=LeaveApprovalConfigsTableName,UsePreviousValue=true \
  --region ap-southeast-2

# review the change set (check no unwanted Replacement: True on DynamoDB tables), then:
aws cloudformation execute-change-set \
  --stack-name military-manager \
  --change-set-name <name-from-above> \
  --region ap-southeast-2

aws cloudformation wait stack-update-complete --stack-name military-manager --region ap-southeast-2
```

`UsePreviousValue=true` reuses the existing stack parameters (including the `NoEcho` JWT secret) so they never need to be re-entered.

## Environment Variables (set via CloudFormation, not local `.env`)
`JWT_SECRET`, `S3_BUCKET`, `S3_PREFIX`, `DYNAMODB_*_TABLE` (one per table) — see `template.yaml` `Environment.Variables`.

## Rollback
```bash
aws cloudformation describe-stack-events --stack-name military-manager --region ap-southeast-2 --max-items 20
# to roll back to the previous deployed artifact, re-run the deploy steps against a previous git commit,
# or use: aws cloudformation cancel-update-stack --stack-name military-manager --region ap-southeast-2 (only while UPDATE_IN_PROGRESS)
```

## Notes
- `AWS::Serverless-2016-10-31` transform is processed by CloudFormation itself — SAM CLI is not required, plain `aws cloudformation` commands are sufficient.
- Maven shade plugin produces `target/military-manager-0.0.1-SNAPSHOT-aws.jar` (classifier `aws`), matching `template.yaml`'s `CodeUri`.
