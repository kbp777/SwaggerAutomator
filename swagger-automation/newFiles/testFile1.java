package newFiles;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/batchJob")
// TODO Fix permissions
@PermitAll
public class testFile1 {

    // *****************************************************************************************************
    // NOTE: Test case: What is the behavior if a method does not have a javaDoc Comment associated with it?
    // Test case: Make sure that the checkAnnotations() only adds annotations to methods that DO NOT
    // have ALL the required annotations
    // *****************************************************************************************************
    // TODO: Add @Operation, @Parameter annotation(s) here.
    @GET
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @StatusCodes({ @ResponseCode(code = 200, condition = "Success"), @ResponseCode(code = 400, condition = "required parameter is not provided in the request," + "or the location with a free device cannot be found"), @ResponseCode(code = 404, condition = "Not found") })
    @ApiResponse()
    @ApiResponse()
    @ApiResponse()
    public void isRegister(String str) {
    }

    // TODO: Add @Operation, @Parameter, @ApiResponse annotation(s) here.
    @PUT
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public void toTest(String parameter) {
        String test = parameter;
    }
}
// This file needs @Operation
