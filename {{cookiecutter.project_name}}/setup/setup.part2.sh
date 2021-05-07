#!sh
echo "Setup - Part 2"
echo
echo "Setting up the *Acquire**, **Agent** and **Website** components"
echo

cd ~/environment/{{cookiecutter.project_name}}/repos/Acquire
sam package --template-file pipeline.yml --s3-bucket {{cookiecutter.codeupload_bucket_name}} --output-template-file pipeline-export.yml --region {{cookiecutter.AWS_region}}
sam deploy  --template-file pipeline-export.yml --stack-name {{cookiecutter.project_name_acquire}}-CICD-Pipeline --region {{cookiecutter.AWS_region}}  --capabilities CAPABILITY_IAM

git init
git add .
git commit -m "Initial commit"
git remote add origin https://git-codecommit.{{cookiecutter.AWS_region}}.amazonaws.com/v1/repos/{{cookiecutter.project_name_acquire}}
git push -u origin master

# Create next CI/CD pipeline
cd ~/environment/{{cookiecutter.project_name}}/repos/Website
sam package --template-file pipeline.yml --s3-bucket {{cookiecutter.codeupload_bucket_name}} --output-template-file pipeline-export.yml --region {{cookiecutter.AWS_region}}
sam deploy  --template-file pipeline-export.yml --stack-name {{cookiecutter.project_name_website}}-CICD-Pipeline --region {{cookiecutter.AWS_region}}  --capabilities CAPABILITY_IAM

# Wait for previous pipeline to deliver...
echo "Waiting for {{cookiecutter.project_name_acquire}}-Staging stack to be created..."
aws cloudformation wait stack-exists --stack-name {{cookiecutter.project_name_acquire}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-exists --stack-name {{cookiecutter.project_name_acquire}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-exists --stack-name {{cookiecutter.project_name_acquire}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-create-complete --stack-name {{cookiecutter.project_name_acquire}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-create-complete --stack-name {{cookiecutter.project_name_acquire}}-Staging --region {{cookiecutter.AWS_region}} 
echo "Continuing..."

# Check code in
cd ~/environment/{{cookiecutter.project_name}}/repos/Website
git init
git add .
git commit -m "Initial commit"
git remote add origin https://git-codecommit.{{cookiecutter.AWS_region}}.amazonaws.com/v1/repos/{{cookiecutter.project_name_website}}
git push -u origin master

# Create next CI/CD pipeline
cd ~/environment/{{cookiecutter.project_name}}/repos/Agent
sam package --template-file pipeline.yml --s3-bucket {{cookiecutter.codeupload_bucket_name}} --output-template-file pipeline-export.yml --region {{cookiecutter.AWS_region}}
sam deploy  --template-file pipeline-export.yml --stack-name {{cookiecutter.project_name_agent}}-CICD-Pipeline --region {{cookiecutter.AWS_region}}  --capabilities CAPABILITY_IAM

# Wait for previous pipeline to deliver...
echo "Waiting for {{cookiecutter.project_name_website}}-Staging stack to be created..."
aws cloudformation wait stack-exists --stack-name {{cookiecutter.project_name_website}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-exists --stack-name {{cookiecutter.project_name_website}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-exists --stack-name {{cookiecutter.project_name_website}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-create-complete --stack-name {{cookiecutter.project_name_website}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-create-complete --stack-name {{cookiecutter.project_name_website}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
echo "Continuing..."

cd ~/environment/{{cookiecutter.project_name}}/repos/Agent
git init
git add .
git commit -m "Initial commit"
git remote add origin https://git-codecommit.{{cookiecutter.AWS_region}}.amazonaws.com/v1/repos/{{cookiecutter.project_name_agent}}
git push -u origin master

echo "Waiting for {{cookiecutter.project_name_agent}}-Staging stack to be created..."
aws cloudformation wait stack-exists --stack-name {{cookiecutter.project_name_agent}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-exists --stack-name {{cookiecutter.project_name_agent}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-exists --stack-name {{cookiecutter.project_name_agent}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
aws cloudformation wait stack-create-complete --stack-name {{cookiecutter.project_name_agent}}-Staging --region {{cookiecutter.AWS_region}} 
echo Ignore the Max attempts exceeded error - this is normal...
echo "Done!"



