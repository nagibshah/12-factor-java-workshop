using Amazon.Lambda.Core;
using System;
using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.DocumentModel;
using System.Threading.Tasks;

// Assembly attribute to enable the Lambda function's JSON input to be converted into a .NET class.
[assembly: LambdaSerializer(typeof(Amazon.Lambda.Serialization.Json.JsonSerializer))]

namespace PlateDetected
{

    public class Function
    {
        private IAmazonDynamoDB dbClient;

        public Function()
        {
            dbClient = new AmazonDynamoDBClient();
        }

        public async Task<NumberPlateTrigger> FunctionHandler(NumberPlateTrigger payload, ILambdaContext context)
        {

            Random rand = new Random((Int32)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds);
            if (rand.NextDouble() > double.Parse(Environment.GetEnvironmentVariable("RandomProcessingErrorProbability")))
            {
                string message = "Congratulations! A random processing error occurred!";
                context.Logger.LogLine(message);
                ////////////////////////////////////////////////////////////
                //
                // TODO: Return 'RandomProcessingError' error
                ///
                ///////////////////////////////////////////////////////////// 
            }
            Table table;
            Document document;
            try
            {
                table = Table.LoadTable(dbClient, Environment.GetEnvironmentVariable("DDBTableName"));
                document = await table.GetItemAsync(payload.numberPlate.numberPlateString);
                if (document != null)
                {
                    if (float.Parse(document["credit"]) > payload.charge)
                    {
                        var item = document;
                        item["credit"] = float.Parse(document["credit"]) - payload.charge;
                        Expression expr = new Expression();
                        expr.ExpressionStatement = "credit >= :charge";
                        expr.ExpressionAttributeValues[":charge"] = payload.charge;

                        // Optional parameters.
                        UpdateItemOperationConfig config = new UpdateItemOperationConfig
                        {
                            ConditionalExpression = expr,
                            ReturnValues = ReturnValues.AllNewAttributes
                        };
                        Document updatedRecord = await table.UpdateItemAsync(item, config);
                        //
                        // Success!
                        //
                        context.Logger.LogLine("Charge of $$" + payload.charge + " deducted from credit for " + payload.numberPlate.numberPlateString);
                    }
                    else
                    {
                        string message = "Driver for number plate " + payload.numberPlate.numberPlateString + "(" + document["ownerFirstName"] + ")" + document["ownerLastName"] + ") has insufficient credit (" + document["credit"] + ") for a charge of " + payload.charge;
                        context.Logger.LogLine(message);
                        /////////////////////////////////////////////////////////////
                        //
                        // TODO: Return 'InsufficientCreditError' error
                        //
                        ///////////////////////////////////////////////////////////// 
                    }

                }
                else
                {
                    string message = "Number plate " + payload.numberPlate.numberPlateString + "was not found. This will require manual resolution.";
                    context.Logger.LogLine(message);
                    /////////////////////////////////////////////////////////////
                    //
                    // TODO: Return 'UnknownNumberPlateError' error
                    //
                    /////////////////////////////////////////////////////////////  
                }
            }
            catch (AmazonDynamoDBException e)
            {
              context.Logger.LogLine(e.StackTrace);
              ////////////////////////////////////////////////////////////
              //
              // TODO: Return 'DatabaseAccessError' error
              ///
              ///////////////////////////////////////////////////////////// 
            }
            catch (Exception e)
            {
              context.Logger.LogLine(e.StackTrace);
              ////////////////////////////////////////////////////////////
              //
              // TODO: Return 'GenericError' error
              ///
              ///////////////////////////////////////////////////////////// 
            }

            return payload;
        }
    }

    //
    // Data to read and passed to the state machine
    //
    public class NumberPlateTrigger
    {
        public string bucket { get; set; }
        public string key { get; set; }
        public string contentType { get; set; }
        public long contentLength { get; set; }
        public NumberPlate numberPlate { get; set; }
        public int charge { get; set; }
    }
    public class NumberPlate
    {
        public bool detected { get; set; }
        public string numberPlateString { get; set; }
        public float confidence { get; set; }
        public string numberPlateRegEx { get; set; }
    }

    //
    // Exceptions definitions
    //
    public class DatabaseAccessError : Exception
    {
        public DatabaseAccessError(string message)
            : base(message)
        {
        }
    }
    public class GenericError : Exception
    {
        public GenericError(string message)
            : base(message)
        {
        }
    }
    public class InsufficientCreditError : Exception
    {
        public InsufficientCreditError(string message)
            : base(message)
        {
        }
    }
    public class RandomProcessingError : Exception
    {
        public RandomProcessingError(string message)
            : base(message)
        {
        }
    }
    public class UnknownNumberPlateError : Exception
    {
        public UnknownNumberPlateError(string message)
            : base(message)
        {
        }
    }

}

