# Deploy Military Manager To AWS Lambda

## 1) Build Lambda artifact

```powershell
mvn clean package -DskipTests
```

Expected output jar:

`target/military-manager-0.0.1-SNAPSHOT-aws.jar`

## 2) Deploy with AWS SAM

```powershell
sam deploy --guided --template-file template.yaml
```

When prompted, provide:

- `JwtSecret`: base64 key for JWT signing
- `S3Bucket`: S3 bucket for personnel images
- `S3Prefix`: folder/prefix in the bucket (default `personnel`)

## 3) Test API

After deploy, SAM outputs `ApiEndpoint`.

Swagger endpoints:

- `{ApiEndpoint}v3/api-docs`
- `{ApiEndpoint}swagger-ui.html`

## Notes

- Lambda execution role needs `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` on your image bucket.
- Template `template.yaml` already grants both S3 and DynamoDB permissions.
- Tables `users`, `roles`, `military_personnel` are created automatically by SAM template.
