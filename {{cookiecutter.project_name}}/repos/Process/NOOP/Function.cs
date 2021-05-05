using Amazon.Lambda.Core;
using System;
using System.Threading.Tasks;
using Newtonsoft.Json;

// Assembly attribute to enable the Lambda function's JSON input to be converted into a .NET class.
[assembly: LambdaSerializer(typeof(Amazon.Lambda.Serialization.Json.JsonSerializer))]

namespace PlateDetected
{
  public class Response
  {
    public string message {get; set;}
  }

  public class Function
  {
    public NumberPlateTrigger FunctionHandler(NumberPlateTrigger payload, ILambdaContext context)
    {
      context.Logger.LogLine($"Process NOOP: process has started. Request={JsonConvert.SerializeObject(payload)}");
      string msg = "";
      if(payload.numberPlate.detected==true)
      {
        /////////////////////////////////////////////////////////////
        //
        // TODO: Read the credit value from the database and decrement it
        //
        /////////////////////////////////////////////////////////////  
        msg = "Number plate " + payload.numberPlate.numberPlateString + " was charged $$" + payload.charge + ".";
      }
      else
      {
        msg = "Number plate " + payload.numberPlate.numberPlateString + " was not found. This will require manual resolution.";
        /////////////////////////////////////////////////////////////
        //
        // TODO: Return 'errorUnknownNumberPlate' error
        //
        /////////////////////////////////////////////////////////////  
      }
      context.Logger.LogLine(msg);
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

}

