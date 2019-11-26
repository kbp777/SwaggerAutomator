import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * This file tests the case where a file already has existing @StatusCodes and @ApiResponse annotations. The expected
 * behavior for this file is for the @StatusCodes annotation to be removed along with their import statements and the
 *
 * @ApiResponse annotations should be left as is. Also, no TODO comment stating that a @ApiResponse should be added it
 * the file.
 * (among other things)
 */


@Path("/batchJob")
// TODO Fix permissions
@PermitAll
public class ApiResponseCornerCaseTestFile {


    @GET
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @StatusCodes({@ResponseCode(code = 200, condition = "Success"), @ResponseCode(code = 400, condition = "required parameter is not provided in the request," + "or the location with a free device cannot be found")})
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "required parameter is not provided in the request, or the location with a free device cannot be found")
    public void isRegister(String str) {

    }
}
