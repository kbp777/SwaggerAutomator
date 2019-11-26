package newFiles;
import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/batchJob")
// TODO Fix permissions
@PermitAll
public class testFile4 {

    // TODO: Add @Operation, @Parameter, @ApiResponse annotation(s) here.
    @GET
    @Path("/isregistered/{registrationKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public void isRegister() {
    }
}
