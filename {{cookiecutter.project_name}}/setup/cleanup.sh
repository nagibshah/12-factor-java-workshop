#!sh

# Cleans up the lab
aws s3 rb s3://{{cookiecutter.codeupload_bucket_name}} --force
aws s3 rb s3://{{cookiecutter.imageupload_bucket_name}}-staging --force
aws s3 rb s3://{{cookiecutter.imageupload_bucket_name}}-prod --force

aws ssm delete-parameters --names "/Prod/{{cookiecutter.project_name}}/TollgateCharge" "/Staging/{{cookiecutter.project_name}}/TollgateCharge" --region {{cookiecutter.AWS_region}} 
aws secretsmanager delete-secret --secret-id "/Staging/{{cookiecutter.project_name}}/Metadata" --force-delete-without-recovery --region {{cookiecutter.AWS_region}} 
aws secretsmanager delete-secret --secret-id "/Prod/{{cookiecutter.project_name}}/Metadata" --force-delete-without-recovery --region {{cookiecutter.AWS_region}} 

aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_baseline}}-Staging --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_acquire}}-Staging --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_agent}}-Staging --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_process}}-Staging --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_website}}-Staging --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_baseline}}-Prod --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_acquire}}-Prod --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_agent}}-Prod --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_process}}-Prod --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_website}}-Prod --region {{cookiecutter.AWS_region}} 

echo
echo "Waiting for child stacks (Staging/Prod) to complete deletion.."

aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_baseline}}-Staging --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_acquire}}-Staging --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_agent}}-Staging --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_process}}-Staging --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_website}}-Staging --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_baseline}}-Prod --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_acquire}}-Prod --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_agent}}-Prod --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_process}}-Prod --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_website}}-Prod --region {{cookiecutter.AWS_region}} 

echo
echo "Continuing..."
echo

aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_acquire}}-CICD-Pipeline --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_agent}}-CICD-Pipeline --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_process}}-CICD-Pipeline --region {{cookiecutter.AWS_region}} 
aws cloudformation delete-stack --stack-name {{cookiecutter.project_name_website}}-CICD-Pipeline --region {{cookiecutter.AWS_region}} 
aws codecommit delete-repository --repository-name {{cookiecutter.project_name_baseline.replace(' ', '-')}}-Staging --region {{cookiecutter.AWS_region}} 
aws codecommit delete-repository --repository-name {{cookiecutter.project_name_baseline.replace(' ', '-')}}-Prod --region {{cookiecutter.AWS_region}} 
cd ~/environment
rm -rf {{cookiecutter.project_name}}
cd ~/environment

echo
echo "Waiting for CI/CD stacks to complete deletion. You can bail out if you like..."
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_acquire}}-CICD-Pipeline --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_agent}}-CICD-Pipeline --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_process}}-CICD-Pipeline --region {{cookiecutter.AWS_region}} 
aws cloudformation wait stack-delete-complete  --stack-name {{cookiecutter.project_name_website}}-CICD-Pipeline --region {{cookiecutter.AWS_region}} 

echo
echo "Done!"
echo
