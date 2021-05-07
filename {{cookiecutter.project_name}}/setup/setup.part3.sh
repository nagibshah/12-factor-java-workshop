#!sh

echo "Setup - Part 3"

cd ~/environment/{{cookiecutter.project_name}}
aws s3 cp s3://awlarau-workshops-us-east-1/12FactorWorkshop/numberplates.zip  ~/environment/{{cookiecutter.project_name}}/numberplates.zip
unzip ~/environment/{{cookiecutter.project_name}}/numberplates.zip
rm ~/environment/{{cookiecutter.project_name}}/numberplates.zip

aws ses verify-email-identity --email-address {{cookiecutter.your_email_address}}

# Fast-forward Acquire/UploadTrigger
rm ~/environment/{{cookiecutter.project_name}}/repos/Acquire/UploadTrigger/index.js
mv ~/environment/{{cookiecutter.project_name}}/repos/Acquire/UploadTrigger/index.full.js ~/environment/{{cookiecutter.project_name}}/repos/Acquire/UploadTrigger/index.js

# Fast-forward Acquire/template.yml
rm ~/environment/{{cookiecutter.project_name}}/repos/Acquire/template.yml
mv ~/environment/{{cookiecutter.project_name}}/repos/Acquire/template.full.yml ~/environment/{{cookiecutter.project_name}}/repos/Acquire/template.yml

cd ~/environment/{{cookiecutter.project_name}}/repos/Acquire
git init
git add .
git commit -m "Fast-forwarding to final solution"
git push 

# Fast-forward Process/PlateDetected
rm ~/environment/{{cookiecutter.project_name}}/repos/Process/PlateDetected/index.js
mv ~/environment/{{cookiecutter.project_name}}/repos/Process/PlateDetected/index.full.js ~/environment/{{cookiecutter.project_name}}/repos/Process/PlateDetected/index.js

# Fast-forward Process/template.yml
rm ~/environment/{{cookiecutter.project_name}}/repos/Process/template.yml
mv ~/environment/{{cookiecutter.project_name}}/repos/Process/template.full.yml ~/environment/{{cookiecutter.project_name}}/repos/Process/template.yml

cd ~/environment/{{cookiecutter.project_name}}/repos/Process
git init
git add .
git commit -m "Fast-forwarding to final solution"
git push

echo
echo "Now wait for the updated stacks to run through CI/CD and when finished, then start at step 94 in the lab guide to run tests. Don't forget you need to verify your email - check your email account at {{cookiecutter.your_email_address}}"
echo


