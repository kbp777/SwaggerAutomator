import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * This file tests the functionality of unsuccessfully adding a swagger @Operation annotation. The expected behavior
 * for this file is for a @Operation annotation to be present stating "TODO: Add description here."
 * Additionally, an import statement for @Operation annotations should be added to the top of this file.
 * (among other things)
 */

@Path("/batchJob")
// TODO Fix permissions
@PermitAll

public class OperationFailedTestFile {


    @GET
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @StatusCodes({@ResponseCode(code = 200, condition = "Success"), @ResponseCode(code = 400, condition = "required parameter is not provided in the request," + "or the location with a free device cannot be found"), @ResponseCode(code = 404, condition = "Not found")})
    public void isRegister(String str) {

    }

    @PUT
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public void toTest(String parameter) {
        String test = parameter;
    }
}


