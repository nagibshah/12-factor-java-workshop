using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace TollRoadManagerWebsite.Pages
{
    public class HelpModel : PageModel
    {
        public string Message { get; set; }

        public void OnGet()
        {

        }
    }
}
