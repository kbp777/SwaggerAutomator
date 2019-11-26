# SwaggerAutomator

-Written by Kevin Pagkatipunan (7/9/19)

This project was created to automatically convert JavaDoc comments and Enunciate annotations into their equivalent 
Swagger annotations in REST resource RESTServicesFiles. Additionally, any DTO files present in any file being annotated
 will be annotated with Swagger annotations as well. This tool was created using the APIs provided by the open-source repository,
JavaParser.
 
**Classes:**

* The FilesToAnnotateFinder class finds the RESTServicesFiles that it should add annotations to. It determines this by 
searching for an @Path annotation. If a file includes this annotation, it will be added to the list of RESTServicesFiles to be 
annotated.

* The FileBuilder class iterates through the RESTServicesFiles to be annotated and adds any required annotations that do not already 
exist in the file, automatically adding their corresponding import statement. After completion, a list of new annotated
RESTServicesFiles will be generated. 

* The AbstractSyntaxTree class represents a parsed file's abstract syntax tree. It is a container for the abstract syntax 
tree and the file that it belongs to. 

* The Schema Label class represents any Schema #labels to add to the annotations in DTO files. This class is not used when
annotating REST service files. Currently, the only labels being added to schema annotations are the description, type,
and implementation labels. Check below to find current annotation rules when adding Schema annotations to DTO files.

**Rules:**

* Files that contain @PATH() annotations will be considered as a REST service and will be annotated with Swagger 
annotations

* Currently the only swagger annotations that get added to RESTServicesFiles are @Operation, @Parameter, @Schema, and @ApiResponse
annotations

* Any swagger annotation that needs to be added to a method declaration but cannot find its corresponding data from 
JavaDoc comments, Enunciate annotations, etc. will not be added and a TODO comment will be added instead, stating that 
the annotation should be added to the file manually. In the case that the method of interest has an existing JavaDoc 
comment associated with it will have the TODO comment appended into it instead of a separate comment being added to the 
file. (Check the Additional Notes section for more details)

* When using this tool, all output RESTServicesFiles will be saved in the same directory as the project directory. Saved in a folder 
called "newFiles". To use this on your system, change the paths in lines 282 and 284 of the fileBuilder.java file to 
your project's working directory path. Line 282 is responsible for creating the folder for the new output RESTServicesFiles to be 
saved to and line 284 is responsible for creating the RESTServicesFiles inside the folder that line 282 created. This will most 
likely be changed in the final iteration of this tool. 

**Swagger Annotation Details:**

* **@Operation annotation** - This annotation gets its description from the JavaDoc comment accompanying the method that 
it is annotating. This tool will include all java doc comments present before the @param tag, if any. So for this tool to
place the correct description for the method in swagger, all relevant method description should be placed before the @param tag.
If no JavaDoc comment exists, then a TODO: comment will be added.

* **@Parameter** - This annotation gets its data from the JavaDoc comment accompanying the method that it is annotating.
If no javaDoc comment is available, then a TODO: comment will be added to the file

* **@Schema** - This annotation will be added only if a @Parameter annotation can be added to the file. If so, it will be
added to the @Parameter  annotation and will describe the type of the parameter.

* **@ApiResponse** - This annotation gets its description from the @StatusCode enunciate annotation. If no @StatusCode 
is present then a TODO comment will be added.


**Annotation Rules for DTO Files** - This tool annotates DTO files if the corresponding DTO object is used in a
REST service file that is being annotated. Also, any DTO objects present in a DTO file that is currently being annotated
will also be queued up for annotation. In short, this tool annotates any DTO files that are used by the REST service file currently
being annotated AND any DTO files used by those DTO files and so on. All DTO files are annotated only with the Swagger
Schema annotation (at the time that this is being written) which can take any of the three labels: #description, #type,
and #implementation. The Schema annotations will be placed in the DTO file's getter methods only and #description,
#type, and #implementation labels will be added to the schema annotation if the return type of the getter is a DTO
object. Otherwise, only the #description and #type labels will be added.


**NOTE:** Any test RESTServicesFiles with "success" in the name tests that a swagger annotation is added into the file for the 
correct method and that the corresponding import statement is added. Any test RESTServicesFiles with "failed" in the name tests that
a TODO comment is placed in the file and documents the annotation to be added by hand. As of now, there are no test 
cases for the functionality of adding @Schema annotations. All test RESTServicesFiles starting with "DoNotAnnotate" test that the 
program correctly selects only REST service RESTServicesFiles to annotate.

**Additional Notes:** This project will have unexpected behavior when annotating a file that has methods with BOTH
JavaDoc comments and line comments above it. The Java Parser API does not support a model to represent parsed method 
declarations with both JavaDoc and Line comments. It is recommended to place only one javaDoc comment above each method 
if a comment is necessary. For this reason, any method that is determined to need a TODO comment that also has an 
existing Java Doc comment will have the TODO comment appended to the javaDoc comment instead of a separate comment 
being added to the method declaration.



Error Case Example) 
     
     **/
     * This method is to be used for testing only.
     * @param str The string to be used
     */
     //TODO: This todo comment will cause an error.
    @GET
    @Path("/getId/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @StatusCodes({@ResponseCode(code = 200, condition = "Success"), @ResponseCode(code = 400, condition =
            "required parameter is not provided in the request," +
                    "or the student cannot be found"),
            @ResponseCode(code = 404, condition = "Not found")})
    public void getStudentId(String str){

    }
    
Valid Case Example) 

     **/
     * This method is to be used for testing only.
     * @param str The string to be used
     */
    @GET
    @Path("/getId/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @StatusCodes({@ResponseCode(code = 200, condition = "Success"), @ResponseCode(code = 400, condition =
            "required parameter is not provided in the request," +
                    "or the student cannot be found"),
            @ResponseCode(code = 404, condition = "Not found")})
    public void getStudentId(String str){

    }


Input File Example) 
  
    /**
     * This method is to be used for testing only.
     * @param str1 The string to be used
     * @param int1 The integer to be used
     */
    @GET
    @Path("/getId/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @StatusCodes({@ResponseCode(code = 200, condition = "Success"), @ResponseCode(code = 400, condition =
            "required parameter is not provided in the request," +
                    "or the student cannot be found"),
            @ResponseCode(code = 404, condition = "Not found")})
    public void getStudentId(String str1){

    }

    /**
     * This method is used for testing.
     * @param parameter The string to be used
     * @param int1 The integer to be used
     */
    @PUT
    @Path("/test/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public void toTestMethod(String parameter, Integer int1){
        String test = parameter;
    }
}

Output File Example) 

    /**
     * This method is to be used for testing only.
     * @param str1 The string to be used
     * @param int1 The integer to be used
     */
    @GET
    @Path("/getId/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "This method is to be used for testing only.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "required parameter is not provided in the request,  or the location with a free device cannot be found")
    @ApiResponse(responseCode = "404", description = "Not found")
    public void getStudentId(@Parameter(description = "The string to be used", schema = @Schema(type = "String")) String str1, @Parameter(description = "The integer to be used", schema = @Schema(type = "Integer")) Integer int1) {
    }

    /**
     *  This method is used for testing.
     *  @param parameter The string to be used
     *  @param int1 The integer to be used
     *
     * TODO: Add @ApiResponse annotation(s) here.
     */
    @PUT
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "This method is used for testing.")
    public void toTestMethod(@Parameter(description = "The string to be used", schema = @Schema(type = "String")) String parameter, @Parameter(description = "The integer to be used", schema = @Schema(type = "Integer")) Integer int1) {
        String test = parameter;
    }
}





