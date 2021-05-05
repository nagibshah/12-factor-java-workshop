# Building modern applications that align with 12-Factor methods

## Hands-on Lab Instructions

Welcome to the Hands-on Lab section of the workshop! Now that you have your IDE environment set up, you are ready to start building your Toll Road Gantry Management System.

The base bundle has been downloaded and the various components have been customised to your environment based on the answers you provided to the questions during the initialisation process. This lab guide has also been customised to match your environment, making it easy for you to simply cut-and-paste the various commands right from this guide. 


## Part 1: Isolation and Dependency Management

---

### Task 1: Deploy the Baseline Staging and Prod environments

---

In this task you will deploy the baseline environment for your Toll Road Gantry System. You will deploy the **Staging** baseline environment manually using the provided CLI commands and directly in the AWS Console. For the **Prod** environment, you will use the provided script to configure everything automatically.

### Instructions

#### Create an S3 bucket for provisioning artefacts

1. Create an S3 bucket to store any artefacts you need to upload during this lab. We will use this bucket to store templates, for example. You can create the bucket from the command line, by issuing this command in the AWS Cloud9 terminal window:

    ```bash
    aws s3 mb s3://{{cookiecutter.codeupload_bucket_name}} --region {{cookiecutter.AWS_region}}
    ```

#### Deploy the **Baseline** for the Staging environment

2. In the terminal window of the IDE, `cd` into the `repos/Baseline-Staging` folder using this command:
   ```bash
   cd ~/environment/{{cookiecutter.project_name}}/repos/Baseline-Staging
   ```

3. Run the following commands which use the *SAM CLI* to process the SAM template stored in `template.yml`. The first command produces the *template-export.yml* file by transforming the SAM template into CloudFormation, and specifies the bucket you created in the previous step as the location to push the transformed template to. The second line deploys the *template-export.yml* CloudFormation template as a new stack called **{{cookiecutter.project_name_baseline}}**.

    ```bash
    sam package --template template.yml --s3-bucket {{cookiecutter.codeupload_bucket_name}} --output-template-file template-export.yml --region {{cookiecutter.AWS_region}}
    sam deploy  --template-file template-export.yml --stack-name {{cookiecutter.project_name_baseline}}-Staging --region {{cookiecutter.AWS_region}} --capabilities CAPABILITY_IAM

    ```

4. The terminal window will show the progress of the script, and show 'Waiting for stack create/update to complete'. It will only take a moment to complete building the Baseline stack.

5. Open the AWS CloudFormation Console [using this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/cloudformation/home?region={{cookiecutter.AWS_region}}#/stacks?filter=active) (be sure to open the link in a new tab or window). Check the box next to **{{cookiecutter.project_name_baseline}}** stack and then click the **Resources** tab in the lower part of the console to show the AWS resources that have been created as part of the stack. You will note that an Amazon DynamoDB table has been provisioned, and two parameters have been created in AWS Systems Manager Parameter Store.

#### Manually add a parameter in the SSM Parameter Store for the Staging environment

6. Open the AWS Systems Manager Parameter Store [using this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/systems-manager/parameters?region={{cookiecutter.AWS_region}}). Be sure to open the link in a new tab/window in the browser.

7. You will see the configuration parameters that have been created. You can click on each of the parameters to review the values stored for each one.

8. We have automated setting up two of the parameters, but have left one of the parameters for you to create manually. In the AWS Systems Manager Parameter Store console, click **Create parameter**.

9. For **Name** type `/Staging/{{cookiecutter.project_name}}/TollgateCharge`
10. For **Description** type `The charge for a vehicle to use the toll gate`
11. For **Type** select **String**
12. For **Value** type `5`
13. Click **Create parameter**

#### Create a new 'secret' in AWS Secrets Manager for the Staging environment

14. In order to prevent having to hard-code the regular expression that the system uses to determine if detected text in a toll gantry image is in fact a number plate, we will store the regular expression in AWS Secrets Manager. While this is not strictly a secret, we will use AWS Secrets manager to demonstrate how to retrieve the value at runtime.
    
    Open the AWS Secrets Manager console [using this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/secretsmanager/home?region={{cookiecutter.AWS_region}}#/home
). Be sure to open the link in a new tab/window in the browser.

15. Click **Store a new secret**
16. Click **Other type of secrets**
17. For **Secret key/value** type `NumberPlateRegEx` in the first box, and `{{cookiecutter.numberplate_regex}}` in the second box.
18. Click **Add Row**
19. In the new text fields that appear, for **Secret key/value** type `SomeKey` in the first box, and `SomeValue` in the second box.
20. Click **Plaintext** to see the JSON payload that will be stored with your key/value pairs. Note that you could have edited this JSON blob directly.
21. Click **Next**
22. For **Secret name** type `/Staging/{{cookiecutter.project_name}}/Metadata`
23. For **Description** type `Staging environment runtime metadata for Toll Gantry System`
24. Click **Next**
25. Leave all fields on the next page as defaults and click **Next**
26. On the next page, note that some boilerplate code is provided for you to use as a starting point to access the secret at runtime using the AWS SDK. We will come back to this boilerplate code later. For now, click **Store**

#### Create source code repository for the Staging environment

27. The 12-Factor App Manifesto states under **Admin Processes** that all parts of your application should be in source control. So even though the Baseline component is only run once per environment (dev/test/staging/prod/etc), it should be added into source control so it can be versioned. We will use AWS CodeCommit as our source control system. [Click this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/codesuite/codecommit/repository/create?region={{cookiecutter.AWS_region}}) to browse to the AWS CodeCommit console.
28. For **Repository name** type `{{cookiecutter.project_name_baseline.replace(' ', '-')}}-Staging`
29. For **RepositoryDescription** type `{{cookiecutter.project_name_baseline}} source code repository for Staging environment`
30. Click **Create repository**
31. Your repository will be created, and is now ready to have code checked in. Scroll to the bottom of the CodeCommit window, and not that there are no files in the repository - it has been created empty.
32. Back in the terminal window of your AWS Cloud9 IDE, enter the following commands to configure the credential helper to manage credentials for you automatically based on the IAM role attached to your user account:

    ```bash
    git config --global credential.helper '!aws codecommit credential-helper $@'
    git config --global credential.UseHttpPath true
    git config --global user.name "{{cookiecutter.your_name}}"
    git config --global user.email {{cookiecutter.your_email_address}}
    ```

33. Initialise a new git repository in the `Baseline-Staging` directory, add all the files contained in the directory and then commit and push to the AWS CodeCommit repository using the following commands:

    ```bash
    cd ~/environment/{{cookiecutter.project_name}}/repos/Baseline-Staging
    git init
    git add .
    git commit -m "Initial commit of Baseline-Staging"
    git remote add origin https://git-codecommit.{{cookiecutter.AWS_region}}.amazonaws.com/v1/repos/{{cookiecutter.project_name_baseline}}-Staging
    git push -u origin master
    ```

34. Switch back to the AWS CodeCommit console, and refresh the page to update it - you will now see the files you have aded to the repository and checked in.

#### Populate the Staging Amazon DynamoDB table with dummy data

35. The Baseline component created an Amazon DynamoDB table that we will use to hold the user accounts that link to number plates in the system. We need to populate this table with some data that we will use for testing later. Run the following commands in the IDE's terminal to add sample data to the table:

    ```bash
    aws dynamodb put-item --region {{cookiecutter.AWS_region}} --table-name {{cookiecutter.dynamodb_tablename}}-Staging --item "{\"numberPlate\": {\"S\": \"SSS650\"},\"credit\": {\"N\": \"12.50\"},\"ownerEmail\": {\"S\": \"{{cookiecutter.your_email_address}}\"},\"ownerFirstName\": {\"S\": \"John\"},\"ownerLastName\": {\"S\": \"Smith\"}}";
    aws dynamodb put-item --region {{cookiecutter.AWS_region}} --table-name {{cookiecutter.dynamodb_tablename}}-Staging --item "{\"numberPlate\": {\"S\": \"PCF606\"},\"credit\": {\"N\": \"3.90\"},\"ownerEmail\": {\"S\": \"{{cookiecutter.your_email_address}}\"},\"ownerFirstName\": {\"S\": \"Jane\"},\"ownerLastName\": {\"S\": \"Doe\"}}";
    aws dynamodb put-item --region {{cookiecutter.AWS_region}} --table-name {{cookiecutter.dynamodb_tablename}}-Staging --item "{\"numberPlate\": {\"S\": \"SOB640\"},\"credit\": {\"N\": \"21.70\"},\"ownerEmail\": {\"S\": \"{{cookiecutter.your_email_address}}\"},\"ownerFirstName\": {\"S\": \"Bruce\"},\"ownerLastName\": {\"S\": \"Wayne\"}}";
    aws dynamodb put-item --region {{cookiecutter.AWS_region}} --table-name {{cookiecutter.dynamodb_tablename}}-Staging --item "{\"numberPlate\": {\"S\": \"TESTPLATE\"},\"credit\": {\"N\": \"29.10\"},\"ownerEmail\": {\"S\": \"{{cookiecutter.your_email_address}}\"},\"ownerFirstName\": {\"S\": \"Frank\"},\"ownerLastName\": {\"S\": \"Furt\"}}";

    ```

36. [Click this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/dynamodb/home?region={{cookiecutter.AWS_region}}#tables:selected={{cookiecutter.dynamodb_tablename}}-Staging;tab=items) to browse to the Amazon DynamoDB console and confirm that the **{{cookiecutter.dynamodb_tablename}}-Staging** table contains sample data.


37. You have now fully configured your **Staging** baseline environment, with a code repository and configuration items stored in AWS Secrets Manager and AWS SSM Parameter Store. We now need to repeat this procedure for the baseline deployment of the **Prod** environment, but rather than do this manually, we will execute a script to perform the same functions. 

    Copy the following command and run it in the AWS Cloud9 IDE:

    ```bash
    sh ~/environment/{{cookiecutter.project_name}}/setup/setup.part1.prod.sh
    ```

    You have now fully configured your **Prod** baseline environment, with a code repository and configuration items stored in AWS Secrets Manager and AWS SSM Parameter Store.

## Part 2: Deployment Automation

---

### Task 2: Deploy the resources for the *Process* component

---

Now that we have our baseline infrastructure deployed (our datastore, configuration parameters, secrets) for both the **Staging** and **Prod** environments, we can start deploying the other components that make up our application. Each of these components will make use of the resources and configuration deployed as part of the baseline. And some components will make use of resources deployed by the other components - for example, the Acquire component makes use of the Process component's AWS Step Function - and so it is important that each of these components are deployed in the correct order, and that you pause where indicated to ensure dependencies have completed deployment before proceeding to the next step.

In this first step you will deploy the **Process** environment for your Toll Road Gantry System. The **Process** component creates an AWS Step Function to control the business workflow, and an AWS Lambda function to implement the processing logic.

### Instructions

#### Deploy CI/CD pipeline for the Process component

38. Back in the terminal window in your AWS Cloud9 IDE, `cd` into the *repos/Process* directory using this command:

    ```bash
    cd ~/environment/{{cookiecutter.project_name}}/repos/Process
    ```

39. Build the full CI/CD pipeline by running these two commands which make use of the AWS SAM CLI to transform the provided pipeline template into a CloudFormation stack:

    ```bash
    aws cloudformation package --template-file pipeline.yml --output-template-file pipeline-export.yml --s3-bucket {{cookiecutter.codeupload_bucket_name}} --region {{cookiecutter.AWS_region}} 

    aws cloudformation deploy --template-file pipeline-export.yml  --stack-name {{cookiecutter.project_name_process}}-CICD-Pipeline --capabilities CAPABILITY_IAM --region {{cookiecutter.AWS_region}} 
    ```

40. Review the CloudFormation console again [using this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/cloudformation/home?region={{cookiecutter.AWS_region}}#/stacks?filter=active), and note that a new stack called **{{cookiecutter.project_name_process}}-CICD-Pipeline** is being provisioned. While the stack is building, click on the **Events** tab in CloudFormation to watch the various resources being created. When the stack is marked as **CREATE_COMPLETE** you are ready to move to the next step.

#### Commit the Process component source code to the repository to trigger a build and deploy

41. In the terminal window in your AWS Cloud9 IDE, connect the codebase for the Process module to the code repository using the following `git` commands:

    ```bash
    cd ~/environment/{{cookiecutter.project_name}}/repos/Process
    git init
    git add .
    git commit -m "Initial commit for Process"
    git remote add origin https://git-codecommit.{{cookiecutter.AWS_region}}.amazonaws.com/v1/repos/{{cookiecutter.project_name_process}}
    git push -u origin master
    ```
  
  If all is well you will see the code committing to the repository.

42. Open the AWS CodeCommit repository in a new browser window/tab by [clicking this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/codesuite/codecommit/repositories/{{cookiecutter.project_name_process}}/browse?region={{cookiecutter.AWS_region}}). Confirm that the code has been checked in and is now browsable in the AWS console.

43. When you checked the code into the repository, AWS CodePipeline will have triggered a build. You can check the progress of the build [by clicking here to browse to the AWS CodePipeline console](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/codesuite/codepipeline/pipelines/{{cookiecutter.project_name_process}}/view?region={{cookiecutter.AWS_region}})

    Review the pipeline that has been created. It contains source, build and deployment phases for a staging environment, and a deployment phase for a production environment. The production environment has a manual review gate. Note that there is only one build phase for the pipeline? As per 12-Factor, there is a single immutable deployment artefact created by the build phase and deployed into the Staging environment. After review, the same artefact is deployed into the Prod environment without being rebuilt. This is one reason why it is critical that configuration is kept outside of the build artefact.

    You can review the progress of the build phase by clicking on the *Details* link in the build phase in AWS CodePipeline. This will open AWS CodeBuild and allow you to view the build logs.

    Once the build phase is complete, the pipeline will move to the Staging deployment. You will see a new CloudFormation Stack being deployed, called **{{cookiecutter.project_name_process.replace(' ', '-')}}-Staging**. Click the **Resources** tab to review the resources that are being created as part of this stack.

---

### Task 3: Deploy the CI/CD pipelines for the Acquire, Agent and Website components 

---

In this task you will deploy the CI/CD pipelines for the **Acquire**, **Agent** and **Website** components. In the previous step, you launched the CI/CD pipeline for the **Process** component and then checked the source code for the component into the repository to trigger a full build and deploy. In this task, you will run a script that automates this process for the **Acquire**, **Agent** and **Website** components.

#### Instructions

44. Back in the terminal window in your AWS Cloud9 IDE, run the following script:

    ```bash
    sh ~/environment/{{cookiecutter.project_name}}/setup/setup.part2.sh
    ```

    The script will commence the process of building out the remaining components.

45. While this deployment is progressing, take the time to review the CloudFormation console again, and note that a new stack called {{cookiecutter.project_name_acquire}}-CICD-Pipeline is being provisioned. While the stack is building, click on the **Events** tab in CloudFormation to watch the various resources being created. 

    Once the CI/CD pipeline is deployed and a build is triggered for the **Acquire** (when the source code is checked into the repository by the `setup.part2.sh` script that you are running), you will see another CloudFormation stack appear, called {{cookiecutter.project_name_acquire}}-Staging. This stack takes care of deploying the AWS Lambda functions, S3 bucket and so on defined in the `template.yml` file for the **Acquire** component, which is located in `repos/Acquire/template.yml`. Take some time to review this file to understand how the template defines the infrastructure for the **Acquire** component.

    The `setup.part2.sh` script will take some time to run. Because the **Acquire** component relies on outputs from the **Process** component deployment, and the **Website** component relies on outputs from teh **Acquire** component, the script will wait for the dependencies to build before proceeding, using `aws cloudformation wait stack-exists` commands. Note that these commands have a maximum wait time, and so the script calls the `wait` multiple times. 
    
    Review the `setup.part2.sh` script itself so you understand how it is working to automate the deployment of your infrastructure. 

    Also, make use of the time it takes to build out the infrastructure, and review the SAM templates that make up the components:

    | Component | Template Path  |
    |---|---|
    | Acquire  | repos/Acquire/template.yml  |
    | Process  | repos/Process/template.yml  |
    | Agent  | repos/Agent/template.yml  |
    | Website  | repos/Website/template.yml  |
    
---

### Task 4: Test the Acquire, Process, Agent & Website components (5 minutes)

---

Shortly, the last of the 4 Toll Gantry System components (Acquire, Process, Agent, Website) will have finished deployment, and you will be able to run a basic test of the system. The source code you have been provided for the Lambda functions is not quite complete, and the state machine defined in the AWS Step Function for the Process component is not implemented, but the basic deployment can be tested to check everything is configured correctly before you proceed to implement the missing parts.

#### Instructions

#### Verify your email address with Amazon SES

The Amazon Simple Email Service (SES) requires that you verify your identities (the domains or email addresses that you send email from) to confirm that you own them, and to prevent unauthorized use. This section includes information about verifying email address identities. For information about verifying domain identities, see [Verifying Domains in Amazon SES.](https://docs.aws.amazon.com/ses/latest/DeveloperGuide/verify-domains.html)

46. Open the Amazon Simple Email Service (SES) console [using this link to show the Verified Email Address panel](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/ses/home?region={{cookiecutter.AWS_region}}#verified-senders-email:) 
47. Click **Verify a New Email Address**
48. For **Email Address** type `{{cookiecutter.your_email_address}}`. It is important that you use the same email address that you provided during setup, since the Amazon DynamoDB table with the account stored in it refers to this same email address. You will receive emails from the system on this email address.
49. Click **Verify this email**
50. The verification will be sent to the email address. Click **Close**
51. Using your email client, check the email address you have requested verification for. In a moment or two, you should receive an email from Amazon SES with the subject *Amazon Web Services – Email Address Verification Request*
52. The email contains a link to click on to verify your email address. Click the link. Your browser will show a *Congratulations!* page informing you that your email address has now been verified. You can close this browser window.
53. Back in the Amazon SES console, refresh the list of verified email addresses and confirm that your email address status is showing as **verified**

----
#### Pause and confirm 
----

You must pause here until the **Agent** deployment is complete because the following steps rely on outputs from the **Agent** component deployment. Check the CloudFormation console and look for a stack called {{cookiecutter.project_name_agent}}-Staging. If it does not appear (refresh the list using the refresh button) or appears but is not in **CREATE_COMPLETE** state, you need to wait before proceeding, otherwise the testing process that follows will fail. 

Once the {{cookiecutter.project_name_agent}}-Staging is complete, proceed with these next steps to test your setup.

#### Test: Upload a number plate image to the 'upload' S3 bucket and confirm the Step Function is triggered

In this section, you will upload an image to test the system end-to-end. Note that no real processing will occur - you need to implement the relevant AWS Lambda functions and the AWS Step Function before  the system will correctly identify number plates. This test will simply confirm that the components are functioning as expected before we move ahead.

54. We have provided a set of test number plates that you can download to your AWS Cloud9 IDE. Run the following commands from your AWS Cloud9 IDE terminal window to retrieve the images:

    ```bash
    cd ~/environment/{{cookiecutter.project_name}}
    aws s3 cp s3://awlarau-workshops-us-east-1/12FactorWorkshop/numberplates.zip  ~/environment/{{cookiecutter.project_name}}/numberplates.zip
    unzip ~/environment/{{cookiecutter.project_name}}/numberplates.zip
    rm ~/environment/{{cookiecutter.project_name}}/numberplates.zip
    ```

55. To simulate a car passing through a toll gate gantry, we will upload an image of a number plate to the S3 bucket that has been configured to trigger the AWS Step Function in the *Acquire* component. Upload an image using the following command:

    ```bash
    aws s3 cp ~/environment/{{cookiecutter.project_name}}/numberplates/SSS650.jpeg s3://{{cookiecutter.imageupload_bucket_name}}-staging
    ```

#### Review the AWS Step Function for *AdminActionTest*

56. [Click on this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/states/home?region={{cookiecutter.AWS_region}}#/statemachines) to open the AWS Step Functions console and see a list of the available state machines.
57. Click on the link for the *{{cookiecutter.stepfunction_name}}-Staging* state machine, noting that it should show as having 1 execution in the *Running* state.
58. Click on the link for the execution shown in the list, and review the **Visual workflow** of the state machine. If everything is correctly configured, it will show a successful pass of the **state.process.NOOP** state and be *running* the **state.process.AdminActionTest**. If your execution is not showing this, please consult your instructor for assistance.

#### Confirm receipt of the email for admin action

59. In a moment or two, you should receive an email from the system to the email address you specified at setup (`{{cookiecutter.your_email_address}}`) corresponding to the **AdminActionTest**. The email will have the subject *[ACTION] - Manual Decision Required!* and will direct you (as the administrator) to follow the link to review the uploaded image. Click the link in the email to open the **Website** component.

#### Test the admin website functionality

60. The website will open to the **Toll Road Number Plate Manual Decider** page, and show the uploaded image. In the text entry field, type `TESTPLATE` and click **Submit this number plate**
61. The page will refresh and show **Thank you for helping, your submission was accepted!**
62. On the AWS Step Function console, note that the **Visual workflow** will now show the **state.process.AdminActiontest** state as complete (green) and the **state.process.InsufficientCreditTest** state as running (blue)

#### Confirm receipt of the email for insufficient credit

63. In a moment, you will receive another email - this one will be targeted to the account owner for the number plate that was uploaded. Check your email client and locate the email with subject **Your account credit is exhausted**
64. Click on the link in the email to open the website for the user account top up page.

#### Test the user account topup website functionality

65. The **Road Toll Account Top Up** page will be shown. In the text entry field, enter `10` and click **Top up my account**
66. The page will refresh and show **Your account was topped up by $10!**

#### Review the AWS Step Function for *AdminActionTest*

67. On the AWS Step Function console, note that the **Visual workflow** will now show the entire workflow as completed.

    You now have an end-to-end test suite on which to base your modifications as described in the following tasks.

## Part 3: Compute & Invocation

---

### Task 5: Implement the missing functionality for the Acquire and Process components (30 minutes)

---

The system so far is functional, but incomplete. In this task you will implement the following items in the **Acquire** and **Process** components:


##### Acquire

- Code in the *UploadTrigger* AWS Lambda function to pull the regular expression to be used for number plate parsing from AWS Secrets manager
- Code in the Upload trigger AWS Lambda function to trigger the AWS Step Function
- Update the `template.yml` file to trigger the **UploadTrigger** Lambda function instead of the placeholder **NOOP** function

##### Process

- Code in the *PlateDetected* AWS Lambda function to throw the various application-defined errors when specific error states are encountered during processing
- The full state machine definition as an AWS Step Function

#### Instructions


#### Implement code to retrieve the Number Plate RegEx from AWS Secrets Manager in the **repos/Acquire/UploadTrigger/index.js** file

68. Open the AWS Secrets Manager console [using this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/secretsmanager/home?region={{cookiecutter.AWS_region}}#/listSecrets), and click on the link for Staging/{{cookiecutter.project_name}}/Metadata. Be sure to open the link in a new tab/window in the browser.
69. Scroll down to the **Sample code** section and select **C#**
70. Note how the sample code instantiates the SecretsManager client?

    ```csharp
    IAmazonSecretsManager client = new AmazonSecretsManagerClient(RegionEndpoint.GetBySystemName(region));
    ```

    Once the client is instantiated, a secret can be retrieved by calling:

    ```csharp
    response = client.GetSecretValueAsync(request);
    ```

    In our case, we are using the SecretString parameter as a property bag, and the string itself is a JSON object. The number plate regular expression we need is stored in this serialised object, as a property called `NumberPlateRegEx`.

71. In your AWS Cloud9 IDE, locate the **repos/Acquire/UploadTrigger/Function.cs** Lambda function and open it in the IDE
72. Locate the function **getNumberPlateFromSecretsManager** in the source code and note that it simply returns `.*` as the regular expression. Your task here is to modify this function so that it correctly returns the `NumberPlateRegEx` value that is stored in AWS Secrets Manager, using the sample code as inspiration. The pseudo code for your implementation should do the following:

  -   Instantiate the SecretsManager client object from the AWS SDK (note that when you construct the client, you do not have to specify endpoint and region).
  -   Call `GetSecretValueAsync(context)` to retrieve the secret.
  -   Handle errors - in all cases, log the error and then simply return a valid regular expression such as `.*`. In a real environment implementation, you would handle errors robustly, but for this workshop, simply falling back to a 'catch all' expression is ok
  -   Assuming all is ok, dereference the `SecretString` field of the return object and parse it as JSON using `JsonConvert.DeserializeObject(...)`
  -   In the resulting object, dereference the `NumberPlateRegEx` property to get the value to return from the function.

      Note: If you get stuck and want to skip coding this function by hand, you will find a finished version of the function in UploadTrigger/getNumberPlateFromSecretsManager.txt

#### Implement code to trigger the AWS Step Function in the **repos/Acquire/UploadTrigger/Function.cs** file

73. In the same **UploadTrigger/Function.cs** file, locate the line `// TODO: Call the Step Function using the AWS SDK`.
74. [Refer to the documentation here](https://docs.aws.amazon.com/sdkfornet/v3/apidocs/items/StepFunctions/MStepFunctionsStartExecutionStartExecutionRequest.html) and call the `StartExecutionAsync` method of the `AmazonStepFunctionsClient` client object, passing in the `StartExecutionRequest` object that has already been constructed in the code provided. The StartExecutionRequest object has the ARN of the state machine to trigger, and the input object to pass in:

    ```csharp
    new StartExecutionRequest() { 
      StateMachineArn = Environment.GetEnvironmentVariable("NumberPlateProcessStateMachine"), 
      Input = JsonConvert.SerializeObject(result) }
    ```

    Note: If you get stuck and want to skip coding this function by hand, you will find a finished version of the function in UploadTrigger/Function.full

75. After making all the changes, save the **repos/Acquire/UploadTrigger/Function.cs** file in the AWS Cloud9 IDE.
76. The SAM template that was provided for you initially defines an AWS Lambda function as a placeholder, and that doesn't perform any real work - called **NOOP** (No Operation). In order for the S3 upload trigger to fire the **UploadTrigger** Lambda function, you need to edit the template so that it refers to the updated function. In the AWS Cloud9 IDE, open the file **repos/Acquire/template.yml**
77. Locate the **Resource** called `UploadTrigger`
78. Within this resource, locate the `Handler:` property and note that it is set to `NOOP::UploadTrigger.Function::FunctionHandler`
79. Modify the `Handler:` value, setting it to `UploadTrigger::UploadTrigger.Function::FunctionHandler` instead. This will instruct the template to update the S3 upload trigger to fire the `UploadTrigger` Lambda function

#### Deploy the changes using CI/CD

80. Now that you have made changes to the source code and SAM template, you need to check the changes into the source code repository to trigger an automated build and deploy to the Staging environment. In the AWS Cloud9 IDE terminal window, run the following commands:

      ```bash
      cd ~/environment/{{cookiecutter.project_name}}/repos/Acquire
      git add .
      git commit -m "Re-implemented UploadTrigger Lambda function to call AWS Step Function and retrieve NumberPlateRegEx from AWS Secrets Manager"
      git push
      ```
  
81. Confirm the push has succeeded by checking the output on the terminal. If all is well, [click here to open to the AWS CodePipeline console for the Acquire component](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/codesuite/codepipeline/pipelines/{{cookiecutter.project_name_acquire}}/view?region={{cookiecutter.AWS_region}})
82. In a moment or two you will see the pipeline commence the build and deploy process, pushing the changes you have made to the Lambda function into your Staging environment. You do not have to wait here until the deployment is complete, continue on with the following steps while the automated deployment takes place.

#### Implement code to throw various errors from the AWS Lambda Function in the **repos/Process/PlateDetected/Function.cs** file

83. In your AWS Cloud9 IDE, locate the **repos/Process/PlateDetected/Function.cs** Lambda function and open it in the IDE
84. Locate each of the `TODO:` items in the file. Your task here is to throw an exception at each of the points errors are detected, and return the application-specific error objects as specified in the source code. Each of the error types have been provided as objects defined in the `Functions.cs`. You simply need to use the `throw` statement to return a newly instantiated object of the various error types. When your code returns these objects, the state machine will be able to make branching logic decisions accordingly. Each of the `TODO` items specifies the types you need to pass to the `state mechine`, and where appropriate, you can use the related `message` variable and `context.Logger.LogLine` statements as inspiration for the error message content to construct the error objects with. For example, search for `TODO: Return 'errorInsufficientCredit' error`. The source code looks like this:

      ```csharp
      else
      {
          string message = "Driver for number plate " + payload.numberPlate.numberPlateString + "(" + document["ownerFirstName"] + ")" + document["ownerLastName"] + ") has insufficient credit (" + document["credit"] + ") for a charge of " + payload.charge;

          context.Logger.LogLine(message);

          /////////////////////////////////////////////////////////////
          //
          // TODO: Return 'errorInsufficientCredit' error
          //
          /////////////////////////////////////////////////////////////                            
      } 
      ```

      You need to replace the `TODO` section with `throw new InsufficientCreditError(message);` to direct the state machine to be able to handle the error condition for *InsufficientCredit*. Note that the `errorInsufficientCredit` object is constructed with a string parameter that is used as a message or reason for the error being returned. Be sure to implement the correct error names based on the conditions as stated in the source code `TODO` sections, otherwise the state machine logic will not make sense later.

      **Note:** If you get stuck and want to skip coding this function by hand, you will find a finished version of the full file at **Process/PlateDetected/Function.full**. To use it, simply replace the contents of **Process/PlateDetected/Function.cs** with the contents of **Process/PlateDetected/Function.full**. You can also use **Process/PlateDetected/Function.full** as inspiration if you get stuck.

85. After making all the changes, save the **repos/Process/PlateDetected/Function.cs** file in the AWS Cloud9 IDE.

#### Deploy the changes using CI/CD

86. Now that you have made changes to the source code, you need to check the changes into the source code repository to trigger an automated build and deploy to the Staging environment. In the AWS Cloud9 IDE terminal window, run the following commands:

      ```bash
      cd ~/environment/{{cookiecutter.project_name}}/repos/Process
      git add .
      git commit -m "Re-implemented PlateDetected Lambda function to return various application-defined errors"
      git push
      ```
  
87. Confirm the push has succeeded by checking the output on the terminal. If all is well, [click here to open to the AWS CodePipeline console for the Process component](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/codepipeline/home?region={{cookiecutter.AWS_region}}#/view/{{cookiecutter.project_name_process}})
88. In a moment or two you will see the pipeline commence the build and deploy process, pushing the changes you have made to the Lambda function into your Staging environment. You do not have to wait here until the deployment is complete, continue on with the following steps while the automated deployment takes place.

#### Implement the AWS Step Function to implement the {{cookiecutter.project_name}} system workflow logic

Note that this section requires you to manually create the AWS Step Function workflow from scratch.

89. [Click on this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/states/home?region={{cookiecutter.AWS_region}}#/statemachines) to open the AWS Step Functions console and see a list of the available state machines.
90. Click on the link for the *{{cookiecutter.stepfunction_name}}-Staging* state machine
91. Click **Edit** to edit the state machine definition
92. Replace the entire contents of the state machine definition with this starter JSON:

      ```JSON
      {
        "Comment": "Full implementation of the Toll Gantry System",
        "StartAt": "state.decision.NumberPlateParseSelector",
        "States": {
          "state.decision.NumberPlateParseSelector": {
            "Type": "Choice",
            "Choices" : [{
              "Variable" : "",
              "BooleanEquals" : false,
              "Next" : ""
            }],
            "Default" : ""
          },
          "state.process.Complete": {
            "Type": "Pass",
            "End": true
          }
        }
      }                                                    
      ```

      In this boilerplate JSON, you have a start node (`state.decision.NumberPlateParseSelector`) which is of type `Choice` and has a single `Choices` entry (which is not completely filled out yet) and a `Default` action if the single `Choices` entry does not equate to true. It also defines an `End` node called `state.process.Complete`.
      
      Based on this starting point, edit the workflow to implement the Toll Gantry logic, following the pseudo code below:

      - Modify the single `Choices` entry so that  if the `Variable` called `$.numberPlate.detected` has a `BooleanEquals` value of `true`, then the `Next` state should be `state.process.Type.NumberPlateDetected`, otherwise (the `Default` action), the next state should be `state.process.Type.ManualDecisionRequired`

        [Refer to the documentation](https://docs.aws.amazon.com/step-functions/latest/dg/amazon-states-language-choice-state.html) for the `Choice` state type for inspiration if you need to. 

      - Define a new step called `state.process.Type.ManualDecisionRequired` of type `Task` with a `TimeoutSeconds` of `360`, an `OutputPath` of `$`, a `ResultPath` of `$` and a `Next` state of `state.decision.NumberPlateParseSelector`. Note that this means that this step will loop back to the previous step when it is complete. In this same step, the `Resource` should be set to the **Activity** that has already been created for you automatically, called `{{cookiecutter.project_name}}-AdminAction-Staging`. You can select this from the drop-down list when you add the `Resource` property in the editor. Placing your cursor within the quotes for the `Resource` property value within the editor will pop up a code assist drop-down list of the ARNs for the available Lambda functions and Step Activities. These have been created by the SAM templates you have executed in the previous steps. 

        [Refer to the documentation](https://docs.aws.amazon.com/step-functions/latest/dg/amazon-states-language-task-state.html) for the `Task` state type for inspiration if you need to. 
        
      - Define another step called `state.process.Type.NumberPlateDetected` of type `Task` with a `Next` state of `state.process.Complete`. The `Resource` for this step should be set to `{{cookiecutter.project_name}}-PlateDetected-Staging`. You can select this from the drop-down list when you add the `Resource` property in the visual workflow editor. To this same step, add a `Retry` property with the following details:

        ```json
        "Retry": [{
          "ErrorEquals": ["RandomProcessingError"],
          "IntervalSeconds": 1,
          "BackoffRate": 2.0,
          "MaxAttempts": 2
        }]                
        ``` 

        This declares that in the case of the Task (in this case, {{cookiecutter.project_name}}-PlateDetected-Staging Lambda function) returning a well-known error called *RandomProcessingError* then the step will be retried twice, with an initial 1 second delay, and a back-off rate of 2 seconds on subsequent retries.

        Also add a `Catch` property like the following:

        ```json
        "Catch": [
        {
          "ErrorEquals": ["DatabaseAccessError"],
          "ResultPath": "$.Exception",
          "Next": "state.error.GeneralException"
        },
        {
          "ErrorEquals": ["GenericError"],
          "ResultPath": "$.Exception",
          "Next": "state.error.GeneralException"
        },
        {
          "ErrorEquals": ["InsufficientCreditError"],
          "ResultPath": "$.Exception",
          "Next": "state.error.InsufficientCreditError"
        },
        {
          "ErrorEquals": ["UnknownNumberPlateError"],
          "ResultPath": "$.Exception",
          "Next": "state.error.UnknownNumberPlateError"
        },
        {
          "ErrorEquals": ["States.ALL"],
          "ResultPath": "$.Exception",
          "Next": "state.error.GeneralException"
        }]        
        ```

        This declares that if this `Task` returns a `DatabaseAccessError`, a `GenericError` or any other error that is not otherwise defined (declared in the `States.ALL` clause) that the workflow will branch to the `state.error.GeneralException` state. If the `Task` returns an `InsufficientCreditError` error, the workflow will branch to the state `state.error.InsufficientCreditError` and if it returns a `UnknownNumberPlateError` error, it will branch to the `state.error.UnknownNumberPlateError` state. This is how we define our application-specific workflow logic using AWS Step Functions.

        [Refer to the documentation](https://docs.aws.amazon.com/step-functions/latest/dg/amazon-states-language-errors.html) for the `Errors` construct for inspiration if you need to. 

      - Define another step called `state.error.GeneralException` of type `Fail`, with an `Error` message `GENERAL_EXCEPTION` and a `Cause` message of `A general error has occured. This file cannot be processed!`

        [Refer to the documentation](https://docs.aws.amazon.com/step-functions/latest/dg/amazon-states-language-fail-state.html) for the `Fail` state type for inspiration if you need to. 

      - The next two steps are similar to `state.process.Type.ManualDecisionRequired` defined earlier, so you may want to cut-and-paste the definition, and make changes as per the following instructions (be careful to double check the differences!).
          
        Define a step called `state.error.UnknownNumberPlateError` of type `Task` with a `TimeoutSeconds` of `360`, an `OutputPath` of `$`, a `ResultPath` of `$` and a `Next` state of `state.decision.NumberPlateParseSelector`. Note that this means that this step will loop back to the previous step when it is complete. In this step, the `Resource` should be set to the **Activity** that has already been created for you automatically, called `{{cookiecutter.project_name}}-AdminAction-Staging`. You can select this from the drop-down list when you add the `Resource` property in the visual workflow editor.

      - Define a step called `state.error.InsufficientCreditError` of type `Task` with a `TimeoutSeconds` of `360`, an `OutputPath` of `$`, a `ResultPath` of `$.TopUpResult` and a `Next` state of `state.decision.NumberPlateParseSelector`. Note that this means that this step will loop back to the previous step when it is complete. In this step, the `Resource` should be set to the **Activity** that has already been created for you automatically, called `{{cookiecutter.project_name}}-InsufficientCredit-Staging`. You can select this from the drop-down list when you add the `Resource` property in the visual workflow editor.

93. Save the workflow. Ensure there are no errors indicated in the JSON for the state machine definition, and click the refresh icon in to re-render the visual workflow. The visual representation should reflect the definition of the steps in the JSON.

#### Test the workflow

94. To simulate a car passing through a toll gate gantry, we will upload an image of a number plate to the S3 bucket that has been configured to trigger the AWS Step Function in the *Acquire* component. Upload an image using the following command:

      ```bash
      aws s3 cp ~/environment/{{cookiecutter.project_name}}/numberplates/SSS650.jpeg s3://{{cookiecutter.imageupload_bucket_name}}-staging
      ```

#### Review the AWS Step Function for successful processing - Normal State

95. [Click on this link](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/states/home?region={{cookiecutter.AWS_region}}#/statemachines) to open the AWS Step Functions console to see a list of the available state machines, and click on the link for the *{{cookiecutter.stepfunction_name}}-Staging* state machine.
96. There should be a new execution in the list, that started recently (check the **Started** column). If there is no indication of a very recent execution, you may have a problem with your Acquire::UploadTrigger Lambda function. Check the CloudWatch Logs.
97. Assuming you have a recent execution, click on the link for that most-recent execution shown in the list, and review the **Visual workflow** of the state machine. If everything is correctly configured, it will show a successful run of the state machine. If your execution is not showing this, please consult your instructor for assistance.

#### Confirm receipt of the email and subsequent flow for the Insufficient Credit workflow

98. We will now run a test for a number plate for which the account doesn't have sufficient credit. Run the following command in the Cloud9 IDE terminal:

      ```bash
      aws s3 cp ~/environment/{{cookiecutter.project_name}}/numberplates/PCF606.jpeg s3://{{cookiecutter.imageupload_bucket_name}}-staging
      ```

99. Review the list of executions in the AWS Step Function console. You should see a recent execution that is in the **Running** state. Click on it.
100. Review the Visual workflow. The state machine execution should show that it is in the **state.error.InsufficientCreditError** state. 
101. In a moment or two, you should receive an email from the system to the email address you specified at setup (`{{cookiecutter.your_email_address}}`). The email will have the subject subject **Your account credit is exhausted**. Open the email and click on the link in the email to open the website for the user account top up page.

#### Test the user account topup website functionality

102. The **Road Toll Account Top Up** page will be shown. In the text entry field, enter `10` and click **Top up my account**
103. The page will refresh and show **Your account was topped up by $10!**

#### Review the AWS Step Function for the *Insufficient credit* workflow resolution

104. On the AWS Step Function console, note that the **Visual workflow** will now show the entire workflow as completed.

#### Confirm receipt of the email and subsequent flow for the Administrative Action workflow

105. We will now run a test for a number plate for which there is no account registered in the *Accounts* DynamoDB table. Run the following command in the Cloud9 IDE terminal to upload a test number plate:

      ```bash
      aws s3 cp ~/environment/{{cookiecutter.project_name}}/numberplates/KZ66ZYT.jpeg s3://{{cookiecutter.imageupload_bucket_name}}-staging
      ```

106. Review the list of executions in the AWS Step Function console. You should see a recent execution that is in the **Running** state. Click on it to review the Visual workflow.
107. The state machine execution should show that it is in the **state.process.Type.ManualDecisionRequired** state. 

#### Note the credit for a test account

108. [Click this link to show the Staging DynamoDB table items](https://{{cookiecutter.AWS_region}}.console.aws.amazon.com/dynamodb/home?region={{cookiecutter.AWS_region}}#tables:selected={{cookiecutter.dynamodb_tablename}}-Staging;tab=items) and make a note of the credit held for the number plate `SOB640`. In a moment we will refresh this list and confirm that the account has been debited.

#### Confirm receipt of the email for admin action

109. In a moment or two, you should receive an email from the system to the email address you specified at setup (`{{cookiecutter.your_email_address}}`) corresponding to the **AdminActionTest**. The email will have the subject *[ACTION] - Manual Decision Required!* and will direct you (as the administrator) to follow the link to review the uploaded image. Click the link in the email to open the **Website** component.

#### Test the admin website functionality

110. The website will open to the **Toll Road Number Plate Manual Decider** page, and show the uploaded image. In the text entry field, type `SOB640` and click **Submit this number plate**. Note that we are overriding the number plate that was detected, and injecting a juman-made decision into our state machine. There is a registration in our DynamoDB table for `SOB640` so submitting this will charge that account.
111. The page will refresh and show **Thank you for helping, your submission was accepted!**
112. On the AWS Step Function console, note that the **Visual workflow** will now show the entire workflow as completed.

#### Confirm the credit for the test account has been updated

113. Refer back to the DynamoDB table and refresh the list. Note that the account credit for `SOB640` has reduced by $5.

### Congratulations! You now have a fully-working end-to-end example of a Toll Gate Gantry Number Plate Scanning System!

---
### Optional Task 6: Update SAM template to deploy Step Function definition via CI/CD
---

In this optional task, you will retrofit the supplied SAM template (template.yml) in the **Process** component with the Step Function definition you manually updated directly in the AWS Console in the previous task.

#### Instructions

114. Copy the Step Function definition from the AWS Step Functions console into your clipboard
115. Open the `template.yml` file in the `Process` component in the AWS Cloud9 IDE
116. Locate the resource definition in the `template.yml` file for `TollGantryStateMachine:` and replace the JSON definition with the definition in your clipboard. Note that it is important that you paste the JSON definition only over the existing JSON definition, leaving the leading `Fn::Sub` and trailing parameters. For example:

      ```yaml

      'Fn::Sub':
        - |-
          {
            <<YOUR JSON GOES HERE>>
          }
        - lambdaNOOP:
            'Fn::GetAtt':
              - NOOP
              - Arn
      ```

117. The `template.yml` defines parameters in the state machine definition that can be used to dynamically inject the AWS resource identifiers (ARNs) of the Step Function activities and Lambda functions that are created as part of the template. When you manually edited the template in the console, you 'hard coded' the ARNs of the activities and Lambda functions, because you selected them out of the drop-down list. In an automated setup, you cannot know ahead of time what the ARNs will be, so the solution is to have the template dynamically inject the ARNs as the resources are created.

      The `template.yml` file defines three dynamic resources:

        ```yaml
        #Lambda function called when the input payload is flagged with 'detected=true'
        lambdaArnPlateDetected:
          'Fn::GetAtt':
            - PlateDetected
            - Arn
        #The ARN of the Step Function activity used when administrative intervention is required
        manualInspectionArn:
          Ref: TollGantryAdminAction
        #The ARN of the Step Function activity used when the account holder does not have enough credit    
        insufficientCreditArn:
          Ref: TollGantryInsufficientCredit
        ```
    
        You can 'inject' these dynamic parameters into your Step Function definition JSON, using the syntax `${PARAM_NAME}` - for example:

        ```
        ${manualInspectionArn}
        ```

        Update your Step Function definition JSON, replacing the hard-coded values with the dynamic parameters.

        If you get stuck, ask the instructor. You can also see a complete version of the definition in `final_stepfunction_definition.yml`

