param(
  [Parameter(Mandatory = $true)]
  [string]$JwtSecret,
  [Parameter(Mandatory = $true)]
  [string]$S3Bucket,
  [string]$S3Prefix = "personnel",
  [string]$Region = "ap-southeast-1",
  [string]$StackName = "military-manager"
)

mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

sam deploy `
  --template-file template.yaml `
  --stack-name $StackName `
  --region $Region `
  --capabilities CAPABILITY_IAM `
  --no-confirm-changeset `
  --no-fail-on-empty-changeset `
  --resolve-s3 `
  --parameter-overrides `
    JwtSecret=$JwtSecret `
    S3Bucket=$S3Bucket `
    S3Prefix=$S3Prefix
