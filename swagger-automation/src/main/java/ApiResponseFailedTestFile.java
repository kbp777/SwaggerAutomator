import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * This file tests the functionality of unsuccessfully adding a swagger @ApiResponse annotation. The expected behavior
 * for this file is for a @ApiResponse annotation to be added with a description parameter stating: "TODO: Add status
 * codes and descriptions here." In addition, the import statement for this annotation should be present at the top
 * of this file.
 * (among other things)
 */

@Path("/batchJob")
// TODO Fix permissions
@PermitAll

public class ApiResponseFailedTestFile {

    /**
     * This method is to be used for testing only.
     *
     * @param str The string to be used
     */
    @GET
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public void isRegister(String str) {

    }


    /**
     * This method is used for testing.
     *
     * @param parameter The parameter to be used
     */
    @PUT
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public void toTest(String parameter) {
        String test = parameter;
    }
}


