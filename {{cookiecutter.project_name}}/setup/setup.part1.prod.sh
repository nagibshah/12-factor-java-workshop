#!sh

echo "Setup - Part 1 (Prod only)"

# Create the Prod metadata in Secrets Manager
aws secretsmanager create-secret --name "/Prod/{{cookiecutter.project_name}}/Metadata" --description "The regular expression to use to determine if detected text is a valid number plate" --secret-string "{\"NumberPlateRegEx\":\"{{cookiecutter.numberplate_regex}}\",\"Somekey\":\"SomeValue\"}" --region {{cookiecutter.AWS_region}} 
# Create the TollgateCharge paramater in SSM Parameter store
aws ssm put-parameter --name "/Prod/{{cookiecutter.project_name}}/TollgateCharge" --description "The charge for a vehicle to use the toll gate" --value "5" --type "String"  --region {{cookiecutter.AWS_region}} 
# Deploy the Baseline-Prod stack
cd ~/environment/{{cookiecutter.project_name}}/repos/Baseline-Prod
sam package --template-file template.yml --s3-bucket {{cookiecutter.codeupload_bucket_name}} --output-template-file template-export.yml --region {{cookiecutter.AWS_region}}
sam deploy  --template-file template-export.yml --stack-name {{cookiecutter.project_name_baseline}}-Prod --region {{cookiecutter.AWS_region}} --capabilities CAPABILITY_IAM
# Create a new AWS CodeCommit repository for Prod
aws codecommit create-repository --repository-name {{cookiecutter.project_name_baseline.replace(' ', '-')}}-Prod --region {{cookiecutter.AWS_region}}
# Check the Prod baseline into the AWS CodeCommit repository
git init
git add .
git commit -m "Initial commit of Baseline-Prod"
git remote add origin https://git-codecommit.{{cookiecutter.AWS_region}}.amazonaws.com/v1/repos/{{cookiecutter.project_name_baseline}}-Prod
git push -u origin master
# Populate the Prod DynamoDB table
aws dynamodb put-item --region {{cookiecutter.AWS_region}} --table-name {{cookiecutter.dynamodb_tablename}}-Prod --item "{\"numberPlate\": {\"S\": \"SSS650\"},\"credit\": {\"N\": \"12.50\"},\"ownerEmail\": {\"S\": \"{{cookiecutter.your_email_address}}\"},\"ownerFirstName\": {\"S\": \"John\"},\"ownerLastName\": {\"S\": \"Smith\"}}";
aws dynamodb put-item --region {{cookiecutter.AWS_region}} --table-name {{cookiecutter.dynamodb_tablename}}-Prod --item "{\"numberPlate\": {\"S\": \"PCF606\"},\"credit\": {\"N\": \"3.90\"},\"ownerEmail\": {\"S\": \"{{cookiecutter.your_email_address}}\"},\"ownerFirstName\": {\"S\": \"Jane\"},\"ownerLastName\": {\"S\": \"Doe\"}}";
aws dynamodb put-item --region {{cookiecutter.AWS_region}} --table-name {{cookiecutter.dynamodb_tablename}}-Prod --item "{\"numberPlate\": {\"S\": \"SOB640\"},\"credit\": {\"N\": \"21.70\"},\"ownerEmail\": {\"S\": \"{{cookiecutter.your_email_address}}\"},\"ownerFirstName\": {\"S\": \"Bruce\"},\"ownerLastName\": {\"S\": \"Wayne\"}}";
aws dynamodb put-item --region {{cookiecutter.AWS_region}} --table-name {{cookiecutter.dynamodb_tablename}}-Prod --item "{\"numberPlate\": {\"S\": \"TESTPLATE\"},\"credit\": {\"N\": \"29.10\"},\"ownerEmail\": {\"S\": \"{{cookiecutter.your_email_address}}\"},\"ownerFirstName\": {\"S\": \"Frank\"},\"ownerLastName\": {\"S\": \"Furt\"}}";
#
echo Done!
