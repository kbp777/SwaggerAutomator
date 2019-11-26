import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * This file tests the functionality of unsuccessfully adding a swagger @Parameter annotation. The expected behavior
 * for this file is for a @Parameter annotation to be present stating "TODO: Add description and @Schema annotation
 * here." Additionally, a import statement for @Parameter annotations should be added to the top of this file.
 * (among other things)
 */


@Path("/batchJob")
// TODO Fix permissions
@PermitAll
public class ParameterFailedTestFile {


    @GET
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @StatusCodes({@ResponseCode(code = 200, condition = "Success"), @ResponseCode(code = 400, condition = "required parameter is not provided in the request," + "or the location with a free device cannot be found")})
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "required parameter is not provided in the request, or the location with a free device cannot be found")
    public void isRegister(String str) {

    }
}
