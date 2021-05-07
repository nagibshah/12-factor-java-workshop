using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using Amazon.StepFunctions;
using Amazon.StepFunctions.Model;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System;
using System.Collections.Generic;
using System.Net;
using System.Threading.Tasks;

namespace TollRoadManagerWebsite.Pages
{
    public class IndexModel : PageModel
    {
        IAmazonDynamoDB ddbClient { get; set; }
        IAmazonStepFunctions stepFunctionsClient { get; set; }

        [BindProperty(SupportsGet = true)]
        public NumberPlateItem numberPlate { get; set; } = new NumberPlateItem();

        public IndexModel(IAmazonDynamoDB dynamoDB, IAmazonStepFunctions stepFunctions)
        {
            this.ddbClient = dynamoDB;
            this.stepFunctionsClient = stepFunctions;
        }

        public async Task OnGetAsync(string plate, [FromQuery] string taskToken)
        {
            Console.WriteLine("OnGetAsync() -> Plate: " + plate + " taskToken: " + taskToken);

            if (!String.IsNullOrEmpty(plate) && !String.IsNullOrEmpty(taskToken))
            {
                Console.WriteLine("Calling DynamoDB '" + Environment.GetEnvironmentVariable("DDBTableName") + "'");
                var response = await ddbClient.GetItemAsync(
                    tableName: Environment.GetEnvironmentVariable("DDBTableName"),
                    key: new Dictionary<string, AttributeValue>
                    {
                        {"numberPlate", new AttributeValue {S = plate.ToUpper()}}
                    }
                );

                if (response.IsItemSet)
                {
                    Console.WriteLine("     done ok");
                    numberPlate.email = response.Item["ownerEmail"].S;
                    numberPlate.ownerName = response.Item["ownerFirstName"].S + " " + response.Item["ownerLastName"].S;
                    numberPlate.credit = Convert.ToDouble(response.Item["credit"].N);
                    numberPlate.numberPlate = response.Item["numberPlate"].S;
                }
                else
                {
                    Console.WriteLine("     done - no item?");
                }
            }
        }

        public async Task<IActionResult> OnPostAsync(string plate, [FromQuery] string taskToken)
        {
            if (!ModelState.IsValid)
            {
                return Page();
            }

            if (!String.IsNullOrEmpty(plate) && !String.IsNullOrEmpty(taskToken))
            {
                try
                {
                    numberPlate.numberPlate = plate.ToUpper();

                    Console.WriteLine("OnPostAsync() -> Plate: " + plate + " taskToken: " + taskToken);
                    Console.WriteLine("Calling DynamoDB '" + Environment.GetEnvironmentVariable("DDBTableName") + "'");

                    var response = await ddbClient.UpdateItemAsync(
                        tableName: Environment.GetEnvironmentVariable("DDBTableName"),
                        key: new Dictionary<string, AttributeValue>
                        {
                            {"numberPlate", new AttributeValue {S = numberPlate.numberPlate}}
                        },
                        attributeUpdates: new Dictionary<string, AttributeValueUpdate>
                        {
                            {"credit", new AttributeValueUpdate {Action = "ADD", Value = new AttributeValue { N = numberPlate.accountToppedUpCredit.ToString() } } }
                        },
                        returnValues: "ALL_NEW"
                    );

                    numberPlate.email = response.Attributes["ownerEmail"].S;
                    numberPlate.ownerName = response.Attributes["ownerFirstName"].S + " " + response.Attributes["ownerLastName"].S;
                    numberPlate.credit = Convert.ToDouble(response.Attributes["credit"].N);
                    numberPlate.numberPlate = response.Attributes["numberPlate"].S;
                    numberPlate.accountToppedUp = true;

                    //
                    // Call back into Step Functions to inform that the 
                    // task should be retried now that the account is topped up
                    //
                    Console.WriteLine("Calling stepFunctionsClient.SendTaskSuccessAsync()");
                    var responseStep = await stepFunctionsClient.SendTaskSuccessAsync(
                        request: new SendTaskSuccessRequest
                        {
                            Output = "\"OK\"",
                            TaskToken = taskToken
                        });

                    numberPlate.errorOccurred = false;
                }
                catch (Exception ex)
                {
                    Console.WriteLine("EXCEPTION::" + ex);
                    numberPlate.errorOccurred = true;
                    numberPlate.accountToppedUp = false;
                }
            }
            else
            {
                Console.WriteLine("Parameters were null or empty");
            }

            return Page();
        }

    }

    public class NumberPlateItem
    {
        public string numberPlate { get; set; }
        public string ownerName { get; set; }
        public double credit { get; set; }
        public string email { get; set; }
        public bool accountToppedUp { get; set; }
        public double accountToppedUpCredit { get; set; }
        public bool errorOccurred { get; set; }
    }
}
