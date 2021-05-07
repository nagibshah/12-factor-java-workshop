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
    public class PlateParseModel : PageModel
    {
        IAmazonStepFunctions stepFunctionsClient { get; set; }

        [BindProperty(SupportsGet = true)]
        public string numberPlateDeterminedByUser { get; set; } = "";
        public bool errorOccurred = false;
        public string imageLinkNumberPlate = "";
        public bool submissionSucceeded = false;

        public PlateParseModel(IAmazonStepFunctions stepFunctions)
        {
            this.stepFunctionsClient = stepFunctions;
        }

        public void OnGet([FromQuery] string imageLink, [FromQuery] string taskToken)
        {

            if (!String.IsNullOrEmpty(imageLink) && !String.IsNullOrEmpty(taskToken))
            {
                imageLinkNumberPlate = imageLink;
            }
        }

        //
        // Handle Post-back
        //
        // Constructs payload to send as input to the StepFunction
        // and marks the activity as SUCCESSFUL
        //
        public async Task<IActionResult> OnPostAsync(string bucket, string key, int charge, [FromQuery] string taskToken, [FromQuery] string imageLink)
        {
            if (!ModelState.IsValid)
            {
                return Page();
            }

            if (!String.IsNullOrEmpty(imageLink) && !String.IsNullOrEmpty(taskToken))
            {
                try
                {
                    imageLinkNumberPlate = imageLink;

                    //
                    // Call back into Step Functions to inform that the 
                    // task should be retried now that the account is topped up
                    //
                    var result = new PlateParseResult
                    {
                        numberPlate = new NumberPlate
                        {
                            detected = true,
                            numberPlateString = numberPlateDeterminedByUser
                        },
                        bucket = bucket,
                        key = key,
                        charge = charge
                    };

                    var responseStep = await stepFunctionsClient.SendTaskSuccessAsync(
                            request: new SendTaskSuccessRequest
                            {
                                Output = Newtonsoft.Json.JsonConvert.SerializeObject(result),
                                TaskToken = taskToken
                            });

                    errorOccurred = false;
                    submissionSucceeded = true;
                }
                catch (Exception ex)
                {
                    Console.WriteLine("EXCEPTION::" + ex);
                    errorOccurred = true;
                }
            }
            else
            {
                Console.WriteLine("Parameters were null or empty");
            }

            return Page();
        }

    }

    public class PlateParseResult
    {
        public NumberPlate numberPlate;
        public int charge;
        public string bucket;
        public string key;
    }

    public class NumberPlate
    {
        public bool detected;
        public String numberPlateString;
    }

}
